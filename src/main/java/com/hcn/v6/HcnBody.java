package com.hcn.v6;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class HcnBody implements Comparable<HcnBody> {
    private HcnBody parent;
    private List<HcnBody> offsprings = new ArrayList<>();
    private PrimeIndexPower pip;
    private boolean proved = false;
    private ScientificNumber value;
    private ScientificNumber factor;
    private HcnFactory hcnFactory = null;

    public HcnBody getParent() {
        return parent;
    }

    public List<HcnBody> getOffsprings() {
        return offsprings;
    }

    public PrimeIndexPower getPip() {
        return pip;
    }

    public ScientificNumber getValue() {
        return value;
    }

    public ScientificNumber getFactor() {
        return factor;
    }

    public void setValue(ScientificNumber value) {
        this.value = value;
    }

    public void setFactor(ScientificNumber factor) {
        this.factor = factor;
    }

    public HcnFactory getHcnFactory() {
        return hcnFactory;
    }

    public void setHcnFactory(HcnFactory hcnFactory) {
        this.hcnFactory = hcnFactory;
    }

    public boolean isProved() {
        return proved;
    }

    public void setProved(boolean proved) {
        this.proved = proved;
    }

    public boolean isDeactivated(){
        return !pip.getActiveHcnBodies().contains(this);
    }

    public String getBodyId() {
        return "p" + pip.getActivePrimeIndex().getIndex() + "^" + pip.getPower();
    }

    public String getOffspringPowers() {return offsprings.stream().map(o -> String.valueOf(o.pip.getPower())).collect(Collectors.joining(", "));}

    public HcnBody(HcnBody parent, PrimeIndexPower pip) {
        this.parent = parent;
        this.pip = pip;
        
        ScientificNumber valueMultiplier = new ScientificNumber(Math.pow(PrimeCenter.getPrime(pip.getActivePrimeIndex().getIndex()), pip.getPower()), 0);
        ScientificNumber factorMultiplier = new ScientificNumber((pip.getPower() + 1), 0);

        if (parent != null) {
            value = parent.value.multiply(valueMultiplier);
            factor = parent.factor.multiply(factorMultiplier);
            parent.offsprings.add(this);
            if (parent.hcnFactory != null) {
                this.hcnFactory = parent.hcnFactory;
                parent.hcnFactory = null;
                this.hcnFactory.inheritAfterNewActivePrimeIndex(this);
            }
        } else {
            value = valueMultiplier;
            factor = factorMultiplier;
        }

        if (hcnFactory == null && pip.getActivePrimeIndex().isLastActivePrimeIndex()) {
            hcnFactory = new HcnFactory(this);
        }
    }

    @Override
    public int compareTo(HcnBody other) {
        return this.value.compareTo(other.value);
    }

    @Override
    public String toString() {return parentChainString() + ", value=" + value + ", factor=" + factor + ", limitHcn: " + (hcnFactory != null ? hcnFactory.getLimitHcn() : "null");}

    public String parentChainString() {return getFullChain().stream().map(HcnBody::getBodyId).collect(Collectors.toList()).toString();}

    private List<HcnBody> getFullChain() {
        List<HcnBody> chain = new ArrayList<>();
        HcnBody current = this;
        while (current != null) {
            chain.add(0, current);
            current = current.parent;
        }
        return chain;
    }

    public List<Hcn> getHcnsBetween(ScientificNumber lowLimit, ScientificNumber upperLimit) {
        if (hcnFactory == null) {
            List<Hcn> returnList = new ArrayList<>();
            offsprings.forEach(offspring -> returnList.addAll(offspring.getHcnsBetween(lowLimit, upperLimit)));
            return returnList;
        } else {
            return hcnFactory.getHcnsBetween(lowLimit, upperLimit);
        }
    }

    public void deactivateFromLists() {
        if (isDeactivated()) {
            return;
        }
        
        pip.getActivePrimeIndex().getHcnBodyList().remove(this);

        pip.removeActiveHcnBody(this);
    }

    public void removeFixedHcnBody() {
        offsprings.forEach(offspring -> {
            offspring.parent = parent;
            parent.offsprings.remove(this);
            parent.offsprings.add(offspring);
        });
    }

    public HcnBody addReactivateHcnBodyFromParent(ActivePrimeIndex reactivatedPrimeIndex) {

        List<HcnBody> offspringSnapshot = new ArrayList<>(this.offsprings);
        HcnBody reactivateHcnBody = new HcnBody(this, reactivatedPrimeIndex.getLastPip());
        reactivateHcnBody.getPip().addActiveHcnBody(reactivateHcnBody);
        if (proved) {
            reactivateHcnBody.proved = true;
        }

        offspringSnapshot.forEach(snapShotOffspring -> {
            this.offsprings.remove(snapShotOffspring);
            reactivateHcnBody.offsprings.add(snapShotOffspring);
            snapShotOffspring.parent = reactivateHcnBody;
        });

        return reactivateHcnBody;
    }
}