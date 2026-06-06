package com.hcn.core;

import java.util.ArrayList;
import java.util.List;

public class LastActivePrimeIndexGroup {
    private int lastActivePrimeIndex;
    private ScientificNumber primeValue;
    private LastActivePrimeIndexGroup lowerLapiGroup = null;
    private LastActivePrimeIndexGroup higherLapiGroup = null;
    private HcnGenerator walkerGenerator = null;
    private ArrayList<Hcn> hcnList = new ArrayList<>();
    private boolean walkerDeleted = false;

    public int getLastActivePrimeIndex() {return lastActivePrimeIndex;}
    public ScientificNumber getPrimeValue() {return primeValue;}
    public void setLastActivePrimeIndex(int lastActivePrimeIndex) {
        this.lastActivePrimeIndex = lastActivePrimeIndex;
        this.primeValue = new ScientificNumber(PrimeCenter.getPrime(lastActivePrimeIndex), 0);
    }
    public LastActivePrimeIndexGroup getLowerLapiGroup() {return lowerLapiGroup;}
    public void setLowerLapiGroup(LastActivePrimeIndexGroup lowerLapiGroup) {this.lowerLapiGroup = lowerLapiGroup;}
    public LastActivePrimeIndexGroup getHigherLapiGroup() {return higherLapiGroup;}
    public void setHigherLapiGroup(LastActivePrimeIndexGroup higherLapiGroup) {this.higherLapiGroup = higherLapiGroup;}
    public ArrayList<Hcn> getHcnList() {return hcnList;}
    public HcnGenerator getWalkerGenerator() {return walkerGenerator;}
    public void setWalkerGenerator(HcnGenerator walkerGenerator) {this.walkerGenerator = walkerGenerator;}
    public boolean isWalkerDeleted() {return walkerDeleted;}
    public void setWalkerDeleted(boolean walkerDeleted) {this.walkerDeleted = walkerDeleted;}

    // Legacy accessors for transition period
    public HcnBody getWalkerBody() {return walkerGenerator != null ? walkerGenerator.getCurrentHcnBody() : null;}
    public void setWalkerBody(HcnBody walkerBody) {
        this.walkerGenerator = walkerBody != null ? walkerBody.getHcnGenerator() : null;
    }

    public LastActivePrimeIndexGroup() {}

    public LastActivePrimeIndexGroup(int lastActivePrimeIndex, HcnGenerator startGenerator) {
        primeValue = new ScientificNumber(PrimeCenter.getPrime(lastActivePrimeIndex), 0);
        this.lastActivePrimeIndex = lastActivePrimeIndex;
        this.walkerGenerator = startGenerator;
        this.walkerGenerator.addWalkerGeneratorForLapi(this);
    }

    @Override
    public String toString() {
        return "LastActivePrimeIndexGroup{" +
                "las" +
                "tActivePrimeIndex=" + lastActivePrimeIndex +
                '}';
    }

    public void generateHcnList(ScientificNumber provedLimit, ScientificNumber targetValue, ActivePrimeIndex lastActivePrimeIndex) {

        if (walkerDeleted) {
            walkerResetAfterDeletion(provedLimit);
        }


        HcnGenerator prevWalker = walkerGenerator;
        // first local hcns might be dominated by lower lapi hcn recorder
        if (lowerLapiGroup != null) {
            //check needed only if local recorder is from lower lapi
            if (hcnList.get(hcnList.size() - 1).getLastActivePrime() < lastActivePrimeIndex.getIndex()) {
                while (walkerGenerator.getLastGeneratedHcn().getFactor().isNotBiggerThan(hcnList.get(hcnList.size() - 1).getFactor())) {
                    HcnGenerator dominated = walkerGenerator;
                    walkerGenerator = walkerGenerator.getLargerGenerator();
                    dominated.gotDominated();
                    walkerGenerator.generateNextHcn(this);
                }
            }
        }
        //After making sure about starterHcn from now on all local HCN can be added
        while (walkerGenerator.getLastGeneratedHcn().getValue().isSmallerThan(targetValue)) {
            hcnList.add(walkerGenerator.getLastGeneratedHcn());
            if (walkerGenerator.getLargerGenerator() == null) {
                break;
            }
            walkerGenerator = walkerGenerator.getLargerGenerator();
            walkerGenerator.generateNextHcn(this);
        }
        if (lowerLapiGroup != null) {
            mergeLowerHcnlist(provedLimit, targetValue);
        }
        prevWalker.removeWalkerGeneratorForLapi(this);
        walkerGenerator.addWalkerGeneratorForLapi(this);

        if (higherLapiGroup != null) {
            higherLapiGroup.generateHcnList(provedLimit, targetValue, lastActivePrimeIndex);
        }
    }

    private void walkerResetAfterDeletion(ScientificNumber provedLimit) {
        //System.out.println("LAPI " + this.lastActivePrimeIndex + " walker deleted");
        //System.out.println("walkerGenerator " + walkerGenerator.getLastGeneratedHcn() + " walker deleted");
        //System.out.println("hcnList " + hcnList + " walker deleted");

        HcnBody newBodyWalker = hcnList.get(hcnList.size() - 1).getHcnGenerator().getCurrentHcnBody();
        //System.out.println("newBodyWalker " + newBodyWalker);
        if (!newBodyWalker.getPip().getActivePrimeIndex().isLastActivePrimeIndex()) {
            //System.out.println("AJAJAJAJAJAJAJAJAJAJAJAJAJAJA");
        }

        while (newBodyWalker.isDeactivated()) {
            newBodyWalker = newBodyWalker.getSmallerBody();
            //System.out.println("newBodyWalker candidate " + newBodyWalker);
        }

        HcnGenerator newWalker = newBodyWalker.getHcnGenerator();
        //System.out.println("provedLimit " + provedLimit);
        if (newWalker.getLastGeneratedHcn() == null) {
            newWalker.generateNextHcn(this);
        }
        Hcn firstHcnAboveProvedLimit = newWalker.getLastGeneratedHcn();
        //System.out.println("firstHcnAboveProvedLimit " + firstHcnAboveProvedLimit);


        while (firstHcnAboveProvedLimit.getValue().isSmallerThan(provedLimit) && newWalker.getLargerGenerator() != null) {
            newWalker = newWalker.getLargerGenerator();
            if (newWalker.getLastGeneratedHcn() == null) {
                newWalker.generateNextHcn(this);
                //System.out.println("firstHcnAboveProvedLimit generated " + newWalker.getLastGeneratedHcn());
            } else {
                //System.out.println("firstHcnAboveProvedLimit existed " + newWalker.getLastGeneratedHcn());
            }
            firstHcnAboveProvedLimit = newWalker.getLastGeneratedHcn();
        }

        walkerGenerator.removeWalkerGeneratorForLapi(this);
        walkerGenerator = newWalker;
        walkerGenerator.addWalkerGeneratorForLapi(this);
        this.walkerDeleted = false;
        //System.out.println("newWalker " + newWalker.getCurrentHcnBody().parentChainString() + " walker deleted");
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
                    hcnList.get(indexToFactorCheck).getHcnGenerator().gotDominated();
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

    public void shiftWalkerGenerator() {
        walkerGenerator.removeWalkerGeneratorForLapi(this);
        walkerGenerator = walkerGenerator.getLargerGenerator();
        walkerGenerator.addWalkerGeneratorForLapi(this);
    }

}
