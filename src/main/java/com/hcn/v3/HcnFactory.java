package com.hcn.v3;

import java.util.ArrayList;
import java.util.List;

public class HcnFactory {

    private Hcn limitHcn = null;
    //private List<Hcn> superiorHcns = new ArrayList<>();
    private ScientificNumber limitCleared = new ScientificNumber(1, 0);
    private HcnBody hcnBody;
    private List<Hcn> previousHcns = new ArrayList<>();

    public HcnFactory(HcnBody hcnBody) {
        this.hcnBody = hcnBody;
    }

    public Hcn getLimitHcn() {
        return limitHcn;
    }

    /*
    public void addSuperiorHcn(Hcn superiorHcn) {
        this.superiorHcns.add(superiorHcn);
    }

    */

    public List<Hcn> getHcnsBetween(ScientificNumber lowLimit, ScientificNumber upperLimit) {

        //System.out.println("  Body " + hcnBody.getBodyId() + " checking limit " + lowLimit + " and " + upperLimit + " limitCleared: " + limitCleared);
        List<Hcn> hcnList = new ArrayList<>();

        if (upperLimit.compareTo(limitCleared) < 1) {
            //System.out.println(" checking limit failed upperlimit is not bigger, upperlimit: " + upperLimit + " limitCleared: " + limitCleared);
            return hcnList;
        }

        if (limitHcn == null) {
            initializeHcnFactory();
        }

        if (limitHcn.getValue().compareTo(lowLimit) > 0 && limitHcn.getValue().compareTo(upperLimit) < 1) {
            //System.out.println("  current limitHcn fits: " + limitHcn);
            hcnList.add(limitHcn);
        }

        while (limitHcn.getValue().compareTo(upperLimit) < 1) {

            Hcn nextHcn = new Hcn(hcnBody, limitHcn.getLastActivePrime() + 1);
            nextHcn.setValue(new ScientificNumber(PrimeCenter.getPrime(nextHcn.getLastActivePrime()), 0).multiply(limitHcn.getValue()));
            nextHcn.setFactor(limitHcn.getFactor().multiply(new ScientificNumber(2, 0)));
            previousHcns.add(limitHcn);
            //System.out.println("  nextHcn is calculated" + nextHcn);
            limitHcn = nextHcn;
            if (limitHcn.getValue().compareTo(lowLimit) > 0 && limitHcn.getValue().compareTo(upperLimit) < 1) {
                //System.out.println("  current nextHcn fits: " + limitHcn);
                hcnList.add(limitHcn);
            }
        }

        limitCleared = upperLimit;

        return hcnList;
    }

    private void initializeHcnFactory() {

        //System.out.println("Initialize: " + hcnBody);

        int lastBodyIndex = hcnBody.getPip().getActivePrimeIndex().getIndex();
        limitHcn = new Hcn(hcnBody, lastBodyIndex);
        limitHcn.setValue(hcnBody.getValue());
        limitHcn.setFactor(hcnBody.getFactor());

        //System.out.println("Created matchedLimitHcn: " + limitHcn);
    }


    public void inheritAfterNewActivePrimeIndex(HcnBody hcnBody) {
        this.hcnBody = hcnBody;
        if (limitHcn != null){
            limitHcn.setBody(hcnBody);
        }

        for (Hcn previousHcn : previousHcns) {
            previousHcn.setBody(hcnBody);
        }
    }
}
