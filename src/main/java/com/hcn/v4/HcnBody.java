package com.hcn.v4;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class HcnBody implements Comparable<HcnBody> {
    private HcnBody parent;
    private List<HcnBody> offspring = new ArrayList<>();
    private List<HcnBody> deactivatedOffsprings = new ArrayList<>();
    private List<HcnBody> neverActivatedOffsprings = new ArrayList<>();
    private PrimeIndexPower pip;
    private boolean proved = false;
    private ScientificNumber value;
    private ScientificNumber factor;
    private HcnFactory hcnFactory = null;
    private HcnBody superiorBody = null;
    private FixedPowerGroup offspringFixedPowerGroup = null;
    private FixedPowerGroup parentFixedPowerGroup = null;

    public HcnBody(HcnBody parent, PrimeIndexPower pip) {
        this.parent = parent;
        this.pip = pip;
        
        ScientificNumber valueMultiplier = new ScientificNumber(Math.pow(PrimeCenter.getPrime(pip.getActivePrimeIndex().getIndex()), pip.getPower()), 0);
        ScientificNumber factorMultiplier = new ScientificNumber((pip.getPower() + 1), 0);

        if (parent != null) {
            value = parent.value.multiply(valueMultiplier);
            factor = parent.factor.multiply(factorMultiplier);
            parent.offspring.add(this);
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

    public HcnBody getParent() {
        return parent;
    }

    public List<HcnBody> getOffspring() {
        return offspring;
    }

    public String getOffspringPowers() {
        return offspring.stream().map(o -> String.valueOf(o.pip.getPower())).collect(Collectors.joining(", "));
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

    public HcnBody getSuperiorBody() {
        return superiorBody;
    }

    public void setSuperiorBody(HcnBody superiorBody) {
        this.superiorBody = superiorBody;
    }

    public boolean isProved() {
        return proved;
    }

    public void setProved(boolean proved) {
        this.proved = proved;
    }

    public List<HcnBody> getDeactivatedOffsprings() {
        return deactivatedOffsprings;
    }

    public List<HcnBody> getNeverActivatedOffsprings() {
        return neverActivatedOffsprings;
    }

    public boolean isDeactivated(){
        return !pip.getActiveHcnBodies().contains(this);
    }

    public String getBodyId() {
        return "p" + pip.getActivePrimeIndex().getIndex() + "^" + pip.getPower();
    }

    @Override
    public int compareTo(HcnBody other) {
        return this.value.compareTo(other.value);
    }

    public List<Hcn> getHcnsBetween(ScientificNumber lowLimit, ScientificNumber upperLimit) {
        if (hcnFactory == null) {
            List<Hcn> returnList = new ArrayList<>();
            for (HcnBody hcnBody : offspring) {
                returnList.addAll(hcnBody.getHcnsBetween(lowLimit, upperLimit));
            }
            return returnList;
        } else {
            return hcnFactory.getHcnsBetween(lowLimit, upperLimit);
        }
    }

    private List<HcnBody> getFullChain() {
        List<HcnBody> chain = new ArrayList<>();
        HcnBody current = this;
        while (current != null) {
            chain.add(0, current);
            current = current.parent;
        }
        return chain;
    }

    @Override
    public String toString() {
        return parentChainString() +
                ", value=" + value +
                ", factor=" + factor +
                ", limitHcn: " + (hcnFactory != null ? hcnFactory.getLimitHcn() : "null")
                ;
    }

    public String parentChainString() {
        return getFullChain().stream().map(HcnBody::getBodyId).collect(Collectors.toList()).toString();
    }

    public void deactivateFromLists() {
        if (isDeactivated()) {
            return;
        }
        
        pip.getActivePrimeIndex().getHcnBodyList().remove(this);
        if (proved) {
            pip.getActivePrimeIndex().getDeactivatedBodyList().add(this);
        }  else {
            pip.getActivePrimeIndex().getNeverActivatedBodyList().add(this);
        }

        pip.removeActiveHcnBody(this);
    }

    public void removeFixedHcnBody(FixedPowerGroup fixedPowerGroup) {
        offspring.forEach(offspring -> {
            offspring.parent = parent;
            parent.offspring.remove(this);
            parent.offspring.add(offspring);
        });
    }

    public HcnBody addReactivateHcnBodyFromOffspring(ActivePrimeIndex reactivatedPrimeIndex) {
        HcnBody reactivateHcnBody = new HcnBody(parent, reactivatedPrimeIndex.getLastPip());
        reactivateHcnBody.getPip().addActiveHcnBody(reactivateHcnBody);

        if (proved) {
            reactivateHcnBody.proved = true;
        }

        parent.offspring.remove(this);
        reactivateHcnBody.offspring.add(this);
        this.parent = reactivateHcnBody;
        return reactivateHcnBody;
    }

    public HcnBody addReactivateHcnBodyFromParent(ActivePrimeIndex reactivatedPrimeIndex) {

        List<HcnBody> offspringSnapshot = new ArrayList<>(this.offspring);
        HcnBody reactivateHcnBody = new HcnBody(this, reactivatedPrimeIndex.getLastPip());
        reactivateHcnBody.getPip().addActiveHcnBody(reactivateHcnBody);
        if (proved) {
            reactivateHcnBody.proved = true;
        }

        offspringSnapshot.forEach(offspring -> {

            this.offspring.remove(offspring);
            reactivateHcnBody.offspring.add(offspring);
            offspring.parent = reactivateHcnBody;
        });

        return reactivateHcnBody;
    }
}