package com.hcn.v4;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

public class Matrix {
    private ActivePrimeIndex lastActivePrimeIndex;
    private List<FixedPowerGroup> fixedPowerGroups = new ArrayList<>();
    //private FilteredHcnSet hcnList = new FilteredHcnSet();
    private List<Hcn> provedHcnList = new ArrayList<>();
    private HcnFilter hcnFilter = new HcnFilter();

    private ScientificNumber lowLimit = new ScientificNumber(1.0, 0);
    private ScientificNumber upperLimit = new ScientificNumber(2, 0);

    private int lastProvedPrimeIndex = -1;
    private TreeSet<Integer> lastActivePrimeIndexesWithinIntervals = new TreeSet<>();
    
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
        //hcnList.add(firstHcn);
        TreeSet<Hcn> tree = new TreeSet<>();
        tree.add(firstHcn);
        hcnFilter.addFromExpandedRange(tree);
    }
    
    public ActivePrimeIndex getLastActivePrimeIndex() {
        return lastActivePrimeIndex;
    }
    
    public List<FixedPowerGroup> getFixedPowerGroups() {
        return fixedPowerGroups;
    }
    
    public List<Hcn> getHcnList() {
        return hcnFilter.getMaxLevelCandidates();
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

    public void proveUntilPrimeIndex(int provedIndex) {
        while (lastProvedPrimeIndex < provedIndex) {
            proveNextHcn();
        }
    }
    
    public void proveUntilCount(int count) {
        while (provedHcnList.size() < count) {
            proveNextHcn();
        }
    }

    public void proveNextHcn() {
        Hcn provedHcn = hcnFilter.getMaxLevelCandidates().get(0);
        provedHcnList.add(provedHcn);
        this.lastActivePrimeIndexesWithinIntervals.add(provedHcn.getLastActivePrime());

        if (!provedHcn.getBody().isDeactivated()) {
            if (lastActivePrimeIndex.extendMatrix(provedHcn.getBody())) {

                if (!lastActivePrimeIndex.isLastActivePrimeIndex()) {
                    lastActivePrimeIndex = lastActivePrimeIndex.getNextActivePrimeIndex();
                }

                lowLimit = provedHcn.getValue();

                List<HcnBody> bodySnapshot = new ArrayList<>(lastActivePrimeIndex.getHcnBodyList());
                TreeSet<Hcn> sortedRawHcn = new TreeSet<>();
                for (HcnBody body : bodySnapshot) {
                    List<Hcn> generatedHcns = body.getHcnsBetween(lowLimit, upperLimit);
                    sortedRawHcn.addAll(generatedHcns);
                }
                hcnFilter.addAfterExtension(sortedRawHcn);
                //hcnFilter.lowLimitUpdate(lowLimit);
            }
        }

        while (hcnFilter.getMaxLevelCandidates().size() == 1) {
            lowLimit = upperLimit;
            upperLimit = lastActivePrimeIndex.getHcnBodyList().first().getHcnFactory().getLimitHcn().getValue();

            List<HcnBody> bodySnapshot = new ArrayList<>(lastActivePrimeIndex.getHcnBodyList());

            TreeSet<Hcn> sortedRawHcn = new TreeSet<>();
            for (HcnBody body : bodySnapshot) {
                List<Hcn> generatedHcns = body.getHcnsBetween(lowLimit, upperLimit);
                sortedRawHcn.addAll(generatedHcns);
            }
            hcnFilter.addFromExpandedRange(sortedRawHcn);
            hcnFilter.discardBelowLimit(lowLimit);
        }

        if (provedHcn.getLastActivePrime() > lastProvedPrimeIndex) {
            lastProvedPrimeIndex = provedHcn.getLastActivePrime();
            int lowestRecordedLastActivePrimeIndex = lastActivePrimeIndexesWithinIntervals.first();
            lastActivePrimeIndexesWithinIntervals.clear();
            hcnFilter.removeLevelBelow(lowestRecordedLastActivePrimeIndex);
        }
        hcnFilter.removeProvedFirst();
    }
}
