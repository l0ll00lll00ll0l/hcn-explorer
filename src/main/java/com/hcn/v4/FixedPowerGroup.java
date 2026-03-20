package com.hcn.v4;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class FixedPowerGroup {
    private List<ActivePrimeIndex> fixedPowerGroup = new ArrayList<>();
    private ScientificNumber value = new ScientificNumber(1.0, 0);
    private ScientificNumber factor = new ScientificNumber(1.0, 0);
    private ActivePrimeIndex parentPrimeIndex = null;
    private ActivePrimeIndex offspringPrimeIndex = null;

    public void receivePrimeIndex(ActivePrimeIndex primeIndex) {

        fixedPowerGroup.add(primeIndex);
        value = value.multiply(new ScientificNumber(Math.pow(PrimeCenter.getPrime(primeIndex.getIndex()), primeIndex.getPips().firstEntry().getValue().getPower()), 0));
        factor = factor.multiply(new ScientificNumber((primeIndex.getPips().firstEntry().getValue().getPower() + 1), 0));
        primeIndex.getHcnBodyList().stream().forEach(hcnBody -> hcnBody.removeFixedHcnBody(this));
        offspringPrimeIndex = primeIndex.getNextActivePrimeIndex();
        primeIndex.getHcnBodyList().clear();
        primeIndex.getLastPip().getActiveHcnBodies().clear();
    }

    public ActivePrimeIndex reactivatePrimeIndex() {
        ActivePrimeIndex reactivatedPrimeIndex = fixedPowerGroup.remove(0);

        //reactivatedPrimeIndex.getHcnBodyList().addGroup(offspringPrimeIndex.getHcnBodyList().stream().map(hcnBody -> hcnBody.addReactivateHcnBody(reactivatedPrimeIndex)).collect(Collectors.toList()));
        reactivatedPrimeIndex.getHcnBodyList().addGroup(parentPrimeIndex.getHcnBodyList().stream().map(hcnBody -> hcnBody.addReactivateHcnBodyFromParent(reactivatedPrimeIndex)).collect(Collectors.toList()));

        parentPrimeIndex = reactivatedPrimeIndex;
        value = value.divide(new ScientificNumber(Math.pow(PrimeCenter.getPrime(reactivatedPrimeIndex.getIndex()), reactivatedPrimeIndex.getPips().firstEntry().getValue().getPower()), 0));
        factor = factor.divide(new ScientificNumber((reactivatedPrimeIndex.getPips().firstEntry().getValue().getPower() + 1), 0));


        return reactivatedPrimeIndex;

    }

    public List<ActivePrimeIndex> getFixedPowerGroup() {
        return fixedPowerGroup;
    }

    public ScientificNumber getValue() {
        return value;
    }

    public ScientificNumber getFactor() {
        return factor;
    }

    public ActivePrimeIndex getParentPrimeIndex() {
        return parentPrimeIndex;
    }

    public ActivePrimeIndex getOffspringPrimeIndex() {
        return offspringPrimeIndex;
    }

    public void setParentPrimeIndex(ActivePrimeIndex parentPrimeIndex) {
        this.parentPrimeIndex = parentPrimeIndex;
    }

    public String getPrimeRangeDisplay() {
        if (fixedPowerGroup.isEmpty()) {
            return "";
        }
        int firstPrime = fixedPowerGroup.get(0).getIndex();
        int lastPrime = fixedPowerGroup.get(fixedPowerGroup.size() - 1).getIndex();
        if (firstPrime == lastPrime) {
            return String.valueOf(firstPrime);
        }
        return firstPrime + "-" + lastPrime;
    }
}
