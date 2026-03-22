package com.hcn.v5;

import java.util.ArrayList;
import java.util.List;

public class HcnFactory {

    private Hcn limitHcn = null;
    private ScientificNumber limitCleared = new ScientificNumber(1, 0);
    private HcnBody hcnBody;
    private List<Hcn> previousHcns = new ArrayList<>();

    public HcnFactory(HcnBody hcnBody) {
        this.hcnBody = hcnBody;
    }
    public Hcn getLimitHcn() {
        return limitHcn;
    }


    public List<Hcn> getHcnsBetween(ScientificNumber lowLimit, ScientificNumber upperLimit) {

        List<Hcn> hcnList = new ArrayList<>();

        if (upperLimit.compareTo(limitCleared) < 1) {
            return hcnList;
        }

        if (limitHcn == null) {
            initializeHcnFactory();
        }

        if (limitHcn.getValue().compareTo(lowLimit) > 0 && limitHcn.getValue().compareTo(upperLimit) < 1) {
            hcnList.add(limitHcn);
        }

        while (limitHcn.getValue().compareTo(upperLimit) < 1) {
            generateNextLimitHcn(lowLimit, upperLimit, hcnList);
        }

        limitCleared = upperLimit;

        return hcnList;
    }

    private void generateNextLimitHcn(ScientificNumber lowLimit, ScientificNumber upperLimit, List<Hcn> hcnList) {

        while (!previousHcns.isEmpty() && previousHcns.get(0).getValue().isSmallerThan(lowLimit)) {
            previousHcns.remove(0);
        }

        Hcn nextHcn = new Hcn(hcnBody, limitHcn.getLastActivePrime() + 1);
        nextHcn.setValue(new ScientificNumber(PrimeCenter.getPrime(nextHcn.getLastActivePrime()), 0).multiply(limitHcn.getValue()));
        nextHcn.setFactor(limitHcn.getFactor().multiply(new ScientificNumber(2, 0)));
        previousHcns.add(limitHcn);
        limitHcn = nextHcn;
        if (limitHcn.getValue().compareTo(lowLimit) > 0 && limitHcn.getValue().compareTo(upperLimit) < 1) {
            hcnList.add(limitHcn);
        }
    }

    private void initializeHcnFactory() {

        int lastBodyIndex = hcnBody.getPip().getActivePrimeIndex().getIndex();
        limitHcn = new Hcn(hcnBody, lastBodyIndex);
        limitHcn.setValue(hcnBody.getValue());
        limitHcn.setFactor(hcnBody.getFactor());
    }


    public void inheritAfterNewActivePrimeIndex(HcnBody hcnBody) {

        this.hcnBody = hcnBody;
        if (limitHcn != null){
            limitHcn.setBody(hcnBody);
        }
        previousHcns.forEach(hcn -> hcn.setBody(hcnBody));
    }
}
