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
            initializeHcnFactory(lowLimit, upperLimit);
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

    private void initializeHcnFactory(ScientificNumber lowLimit, ScientificNumber upperLimit) {

        //System.out.println("Initialize " + this.hcnBody);
        Hcn referenceHcn = null;
        if (this.hcnBody.getSmallerBody() != null) {
            //System.out.println("Initialize smallerBody exists " + this.hcnBody.getSmallerBody());

            if (hcnBody.getSmallerBody().getHcnFactory().previousHcns.isEmpty()) {
                referenceHcn = this.hcnBody.getSmallerBody().getHcnFactory().limitHcn;
            } else {
                //System.out.println("previous hcns: " + hcnBody.getSmallerBody().getHcnFactory().previousHcns.size());
                referenceHcn = hcnBody.getSmallerBody().getHcnFactory().previousHcns.get(0);
            }
        }

        if (referenceHcn == null) {
            int lastBodyIndex = hcnBody.getPip().getActivePrimeIndex().getIndex();
            limitHcn = new Hcn(hcnBody, lastBodyIndex);
            limitHcn.setValue(hcnBody.getValue());
            limitHcn.setFactor(hcnBody.getFactor());
        } else if (referenceHcn.getLastActivePrime() < hcnBody.getPip().getActivePrimeIndex().getIndex()) {

            int lastBodyIndex = hcnBody.getPip().getActivePrimeIndex().getIndex();
            limitHcn = new Hcn(hcnBody, lastBodyIndex);
            limitHcn.setValue(hcnBody.getValue());
            limitHcn.setFactor(hcnBody.getFactor());
        } else {

            limitHcn = referenceHcn.createHcnByReference(hcnBody);

            if (limitHcn.getValue().isBiggerThan(lowLimit)) {
                //System.out.println("limHcn bigger than lowlimit " + limitHcn + ", " + lowLimit);
                setLimitHcnFirstAftreLimit(lowLimit);
                //System.out.println("limHcn now smaller than lowlimit " + limitHcn + ", " + lowLimit);
            } else {
                //System.out.println("limHcn already smaller than lowlimit " + limitHcn + ", " + lowLimit);
            }
        }
    }

    private void setLimitHcnFirstAftreLimit(ScientificNumber lowLimit) {
        int lapi = limitHcn.getLastActivePrime();
        ScientificNumber potentialValueBefore = limitHcn.getValue().divide(new ScientificNumber(PrimeCenter.getPrime(lapi), 0));
        //System.out.println("potential value: " + potentialValueBefore);
        //System.out.println("lapi: " + lapi);
        //System.out.println("hcnBody.getPip().getActivePrimeIndex().getIndex(): " + hcnBody.getPip().getActivePrimeIndex().getIndex());
        while (lapi > hcnBody.getPip().getActivePrimeIndex().getIndex() && potentialValueBefore.isBiggerThan(lowLimit)) {
            lapi--;
            limitHcn.setLastActivePrime(lapi);
            limitHcn.setValue(potentialValueBefore);
            limitHcn.setFactor(limitHcn.getFactor().divide(new ScientificNumber(2, 0)));
            potentialValueBefore = limitHcn.getValue().divide(new ScientificNumber(PrimeCenter.getPrime(lapi), 0));
            //System.out.println("  potential value: " + potentialValueBefore);
        }
    }


    public void inheritAfterNewActivePrimeIndex(HcnBody hcnBody) {

        this.hcnBody = hcnBody;
        if (limitHcn != null){
            limitHcn.setBody(hcnBody);
        }
        previousHcns.forEach(hcn -> hcn.setBody(hcnBody));
    }
}
