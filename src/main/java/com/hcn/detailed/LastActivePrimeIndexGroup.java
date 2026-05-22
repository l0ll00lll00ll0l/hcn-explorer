package com.hcn.detailed;

import java.util.ArrayList;

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
        this.walkerBody.addWalkerBodyForLapi(this);
    }

    @Override
    public String toString() {
        return "LastActivePrimeIndexGroup{" +
                "lastActivePrimeIndex=" + lastActivePrimeIndex +
                '}';
    }

    public void generateHcnList(ScientificNumber provedLimit, ScientificNumber targetValue) {
        HcnBody prevWalker = walkerBody;
        // first local hcns might be dominated by lower lapi hcn recorder
        if (lowerLapiGroup != null) {
            //check needed only if local recorder is from lower lapi
            if (hcnList.get(hcnList.size() - 1).getLastActivePrime() < lastActivePrimeIndex) {
                while (walkerBody.getLastGeneratedHcn().getFactor().isNotBiggerThan(hcnList.get(hcnList.size() - 1).getFactor())) {
                    walkerBody.gotDominated();
                    walkerBody = walkerBody.getLargerBody();
                    walkerBody.generateNextHcn(this);
                }
            }
        }
        //After making sure about starterHcn from now on all local HCN can be added
        while (walkerBody.getLastGeneratedHcn().getValue().isSmallerThan(targetValue)) {
            hcnList.add(walkerBody.getLastGeneratedHcn());
            if (walkerBody.getLargerBody() == null) {
                break;
            }
            walkerBody = walkerBody.getLargerBody();
            walkerBody.generateNextHcn(this);
        }
        if (lowerLapiGroup != null) {
            mergeLowerHcnlist(provedLimit, targetValue);
        }
        prevWalker.removeWalkerBodyForLapi(this);
        walkerBody.addWalkerBodyForLapi(this);
        if (higherLapiGroup != null) {
            higherLapiGroup.generateHcnList(provedLimit, targetValue);
        }
    }

    private void mergeLowerHcnlist(ScientificNumber provedLimit, ScientificNumber targetValue) {

        int localSuperiorIndex = 0;
        int lowerLapiNextHcnIndex = lowerLapiGroup.computeIndexForPovedLimitBefore(provedLimit);

        while (lowerLapiNextHcnIndex < lowerLapiGroup.hcnList.size()) {
            Hcn lowerLapiNextHcn = lowerLapiGroup.hcnList.get(lowerLapiNextHcnIndex);
            while ((hcnList.size() > localSuperiorIndex + 1) && hcnList.get(localSuperiorIndex + 1).getValue().isSmallerThan(lowerLapiNextHcn.getValue())) {
                localSuperiorIndex++;
            }
            Hcn localSuperiorHcn = hcnList.get(localSuperiorIndex);
            if (lowerLapiNextHcn.getFactor().isBiggerThan(localSuperiorHcn.getFactor())) {
                hcnList.add(localSuperiorIndex + 1, lowerLapiNextHcn);
                int indexToFactorCheck = localSuperiorIndex + 2;
                while (indexToFactorCheck < hcnList.size() && hcnList.get(indexToFactorCheck).getFactor().isNotBiggerThan(lowerLapiNextHcn.getFactor())) {
                    hcnList.get(indexToFactorCheck).getBody().gotDominated();
                    hcnList.remove(indexToFactorCheck);
                }
            }
            lowerLapiNextHcnIndex++;
        }
    }

    private int computeIndexForPovedLimitBefore(ScientificNumber provedLimit) {
        for (int i = 0; i < hcnList.size(); i++ ) {
            if (hcnList.get(i).getValue().isBiggerThan(provedLimit)) {
                return i;
            }
        }
        return hcnList.size();
    }

    public void maintainAfterInterval() {
        Hcn last = hcnList.get(hcnList.size()-1);
        hcnList.clear();
        hcnList.add(last);
    }

    public void shiftWalkerBody() {
        walkerBody.removeWalkerBodyForLapi(this);
        setWalkerBody(walkerBody.getLargerBody());
        walkerBody.addWalkerBodyForLapi(this);
    }
}
