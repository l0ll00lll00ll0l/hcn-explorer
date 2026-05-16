package com.hcn.v7;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class LastActivePrimeIndexGroup {
    private int lastActivePrimeIndex;
    private ScientificNumber primeValue;
    private LastActivePrimeIndexGroup lowerLapiGroup = null;
    private LastActivePrimeIndexGroup higherLapiGroup = null;
    private HcnBody walkerBody = null;
    private ArrayList<Hcn> hcnList = new ArrayList<>();

    public int getLastActivePrimeIndex() {return lastActivePrimeIndex;}
    public ScientificNumber getPrimeValue() {return primeValue;}
    public void setLastActivePrimeIndex(int lastActivePrimeIndex) {this.lastActivePrimeIndex = lastActivePrimeIndex;}
    public LastActivePrimeIndexGroup getLowerLapiGroup() {return lowerLapiGroup;}
    public void setLowerLapiGroup(LastActivePrimeIndexGroup lowerLapiGroup) {this.lowerLapiGroup = lowerLapiGroup;}
    public LastActivePrimeIndexGroup getHigherLapiGroup() {return higherLapiGroup;}
    public void setHigherLapiGroup(LastActivePrimeIndexGroup higherLapiGroup) {this.higherLapiGroup = higherLapiGroup;}
    public ArrayList<Hcn> getHcnList() {return hcnList;}
    public HcnBody getWalkerBody() {return walkerBody;}
    public void setWalkerBody(HcnBody walkerBody) {this.walkerBody = walkerBody;}

    public LastActivePrimeIndexGroup(int lastActivePrimeIndex, HcnBody currentLowBody) {
        primeValue = new ScientificNumber(PrimeCenter.getPrime(lastActivePrimeIndex), 0);
        this.lastActivePrimeIndex = lastActivePrimeIndex;
        this.walkerBody = currentLowBody;
        currentLowBody.setWalkerBodyForLapi(this);
    }



    @Override
    public String toString() {
        return "LastActivePrimeIndexGroup{" +
                "lastActivePrimeIndex=" + lastActivePrimeIndex +
                '}';
    }

    public Hcn getFirstHcn() {
        return null;
    }

    public boolean isReadyToDelete() {
        return false;
    }

    public void generateHcnList(ScientificNumber provedLimit, ScientificNumber targetValue) {

        System.out.println("");
        System.out.println("Generate Hcn list for lapigroup: " + this.lastActivePrimeIndex);
        //System.out.println("walkerBody " + this.walkerBody);
        System.out.println("ProvedLimit " + provedLimit + " - TargetValue " + targetValue);
        HcnBody prevWalker = walkerBody;

        // first local hcns might be dominated by lower lapi hcn recorder
        if (lowerLapiGroup != null) {
            //check needed only if local recorder is from lower lapi
            if (hcnList.get(hcnList.size() - 1).getLastActivePrime() < lastActivePrimeIndex) {
                while (walkerBody.getLastGeneratedHcn().getFactor().isNotBiggerThan(hcnList.get(hcnList.size() - 1).getFactor())) {
                    //System.out.println("Hcn PRESKIPPEDt: " + walkerBody.getLastGeneratedHcn());
                    walkerBody.gotDominated();
                    walkerBody = walkerBody.getLargerBody();
                    walkerBody.generateNextHcn(this);
                }

            }

        }

        //After making sure about starterHcn from now on all local HCN can be added
        while (walkerBody.getLastGeneratedHcn().getValue().isSmallerThan(targetValue)) {
            //System.out.println("Hcn added to lapi hcnlist: " + walkerBody.getLastGeneratedHcn());
            hcnList.add(walkerBody.getLastGeneratedHcn());
            walkerBody = walkerBody.getLargerBody();
            walkerBody.generateNextHcn(this);
            //System.out.println("Walkerbody hcn for lapi " + this.lastActivePrimeIndex + ": " + walkerBody.getLastGeneratedHcn());
        }

        //If lower lapi exists, their hcns must be checked locally
        if (lowerLapiGroup != null) {
            mergeLowerHcnlist(provedLimit, targetValue);
        }

        prevWalker.setWalkerBodyForLapi(null);
        walkerBody.setWalkerBodyForLapi(this);
        //System.out.println("hcnlist after generateHcnList: " + hcnList);

        if (higherLapiGroup != null) {
            higherLapiGroup.generateHcnList(provedLimit, targetValue);
        }
    }

    private void mergeLowerHcnlist(ScientificNumber provedLimit, ScientificNumber targetValue) {

        int localSuperiorIndex = 0;
        int lowerLapiNextHcnIndex = computeLowerLapiNextHcnIndex(provedLimit);


        while (lowerLapiNextHcnIndex < lowerLapiGroup.hcnList.size()) {

            Hcn lowerLapiNextHcn = lowerLapiGroup.hcnList.get(lowerLapiNextHcnIndex);

            while ((hcnList.size() > localSuperiorIndex + 1) && hcnList.get(localSuperiorIndex + 1).getValue().isSmallerThan(lowerLapiNextHcn.getValue())) {
                localSuperiorIndex++;
            }

            Hcn localSuperiorHcn = hcnList.get(localSuperiorIndex);

            if (lowerLapiNextHcn.getFactor().isBiggerThan(localSuperiorHcn.getFactor())) {
                System.out.println("localSuperiorIndex: " + localSuperiorIndex);
                System.out.println("localSuperiorHcn: " + localSuperiorHcn);
                System.out.println("lowerLapiNextHcnIndex: " + lowerLapiNextHcnIndex);
                System.out.println("lowerLapiNextHcnIndex hcn: " + lowerLapiNextHcn);
                hcnList.add(localSuperiorIndex + 1, lowerLapiNextHcn);
                int indexToFactorCheck = localSuperiorIndex + 2;
                System.out.println("hcn index to factor check: " + indexToFactorCheck);
                while (indexToFactorCheck < hcnList.size() && hcnList.get(indexToFactorCheck).getFactor().isNotBiggerThan(lowerLapiNextHcn.getFactor())) {
                    //delete body
                    hcnList.get(indexToFactorCheck).getBody().gotDominated();
                    System.out.println("hcn removed" + hcnList.get(indexToFactorCheck));
                    hcnList.remove(indexToFactorCheck);
                }
            }

            lowerLapiNextHcnIndex++;


        }
        //System.out.println("hcnlist after mergeLowerHcnlist: " + hcnList);
    }

    private int computeLowerLapiNextHcnIndex(ScientificNumber provedLimit) {
        for (int i = 0; i < lowerLapiGroup.hcnList.size(); i++ ) {
            if (lowerLapiGroup.hcnList.get(i).getValue().isBiggerThan(provedLimit)) {
                return i;
            }
        }
        return -1;
    }

    public void extendExistingRangeWithNewBodies(ScientificNumber provedLimit, List<HcnBody> newBodies) {


        for (HcnBody newBody : newBodies) {
            if (newBody.getValue().isNotSmallerThan(walkerBody.getValue())) {
                break;
            }
            System.out.println("newBody: " + newBody);
            System.out.println("walkerBody: " + walkerBody);
            if (newBody.getValue().isBiggerThan(provedLimit)) {
                System.out.println("inserting new body into existing range: " + newBody);
            }
        }
        
        if (higherLapiGroup != null) {
            higherLapiGroup.extendExistingRangeWithNewBodies(provedLimit, newBodies);
        }
    }

    public void maintainAfterInterval(ScientificNumber provedLimit) {

        Hcn last = hcnList.get(hcnList.size()-1);
        hcnList.clear();
        hcnList.add(last);

        System.out.println("hcnlist after: " + hcnList);
    }
}
