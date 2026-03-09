package com.hcn.v3;

import java.util.ArrayList;
import java.util.List;

public class Matrix {
    private ActivePrimeIndex lastActivePrimeIndex;
    private List<FixedPowerGroup> fixedPowerGroups = new ArrayList<>();
    private FilteredHcnSet hcnList = new FilteredHcnSet();
    private List<Hcn> provedHcnList = new ArrayList<>();

    private ScientificNumber lowLimit = new ScientificNumber(1.0, 0);
    private ScientificNumber upperLimit = new ScientificNumber(2, 0);
    
    public Matrix() {
        // Create p0 with pip1
        ActivePrimeIndex p0 = new ActivePrimeIndex(0);
        PrimeIndexPower pip1 = new PrimeIndexPower(p0, 1);
        p0.getPips().put(1, pip1);
        lastActivePrimeIndex = p0;
        
        // Create initial HcnBody with null parent and pip1
        HcnBody firstBody = new HcnBody(null, pip1);
        p0.getHcnBodyList().addGroup(List.of(firstBody));
        
        // Create initial Hcn
        Hcn firstHcn = firstBody.getHcnsBetween(lowLimit, upperLimit).get(0);
        hcnList.add(firstHcn);
    }
    
    public ActivePrimeIndex getLastActivePrimeIndex() {
        return lastActivePrimeIndex;
    }
    
    public List<FixedPowerGroup> getFixedPowerGroups() {
        return fixedPowerGroups;
    }
    
    public FilteredHcnSet getHcnList() {
        return hcnList;
    }
    
    public List<Hcn> getProvedHcnList() {
        return provedHcnList;
    }

    public ScientificNumber getLowLimit() {
        return lowLimit;
    }

    public ScientificNumber getUpperLimit() {
        return upperLimit;
    }

    public void proveNextHcn() {
        Hcn provedHcn = hcnList.first();
        provedHcnList.add(provedHcn);

        if (!provedHcn.getBody().isDeactivated()) {
            //HcnBody hcnBodyToExtend = provedHcn.getBody().setProvedRecursive();
            if (lastActivePrimeIndex.extendMatrix(provedHcn.getBody())) {

                if (!lastActivePrimeIndex.isLastActivePrimeIndex()) {
                    lastActivePrimeIndex = lastActivePrimeIndex.getNextActivePrimeIndex();
                }

                lowLimit = provedHcn.getValue();

                List<HcnBody> bodySnapshot = new ArrayList<>(lastActivePrimeIndex.getHcnBodyList());
                for (HcnBody body : bodySnapshot) {
                    //System.out.println("Body to check (vertical): " + body);
                    body.getHcnsBetween(lowLimit, upperLimit).forEach(hcn -> {
                        if (hcnList.add(hcn)) {
                        }
                    });
                }
            }
        }

        if (hcnList.size() == 1) {
            lowLimit = upperLimit;
            System.out.println("newLimitBody = " + lastActivePrimeIndex.getHcnBodyList().first());
            System.out.println("HCN = " + lastActivePrimeIndex.getHcnBodyList().first().getHcnFactory().getLimitHcn());
            System.out.println("limit = " + lastActivePrimeIndex.getHcnBodyList().first().getHcnFactory().getLimitHcn().getValue());
            //upperLimit = provedHcn.getValue().multiply(new ScientificNumber(PrimeCenter.getPrime(provedHcn.getLastActivePrime() + 1), 0));
            upperLimit = lastActivePrimeIndex.getHcnBodyList().first().getHcnFactory().getLimitHcn().getValue();
            System.out.println("upperLimit = " + upperLimit);

            List<HcnBody> bodySnapshot = new ArrayList<>(lastActivePrimeIndex.getHcnBodyList());
            for (HcnBody body : bodySnapshot) {
                //System.out.println("Body to check (vertical): " + body);
                body.getHcnsBetween(lowLimit, upperLimit).forEach(hcn -> hcnList.add(hcn));
            }
        }

        hcnList.remove(provedHcn);
    }
}
