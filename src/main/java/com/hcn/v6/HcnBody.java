package com.hcn.v6;

import java.util.LinkedHashMap;
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
    private HcnBody smallerBody = null;
    private HcnBody largerBody = null;
    private LinkedHashMap<LastActivePrimeIndexGroup, Hcn> generatedHcns = new LinkedHashMap<>();

    public HcnBody getParent() {
        return parent;
    }
    public List<HcnBody> getOffsprings() {
        return offsprings;
    }
    public PrimeIndexPower getPip() {
        return pip;
    }
    public void setPip(PrimeIndexPower pip) {
        this.pip = pip;
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
    public boolean isProved() {
        return proved;
    }
    public void setProved(boolean proved) {
        this.proved = proved;
    }
    public HcnBody getSmallerBody() {return smallerBody;}
    public HcnBody getLargerBody() {return largerBody;}
    public void setSmallerBody(HcnBody smallerBody) {this.smallerBody = smallerBody;}
    public void setLargerBody(HcnBody largerBody) {this.largerBody = largerBody;}
    public boolean isDeactivated(){
        return !pip.getActiveHcnBodies().contains(this);
    }
    public String getBodyId() {
        return "p" + pip.getActivePrimeIndex().getIndex() + "^" + pip.getPower();
    }
    public String getOffspringPowers() {return offsprings.stream().map(o -> String.valueOf(o.pip.getPower())).collect(Collectors.joining(", "));}
    public LinkedHashMap<LastActivePrimeIndexGroup, Hcn> getGeneratedHcns() {return generatedHcns;}

    public HcnBody() {}

    public HcnBody(HcnBody parent, PrimeIndexPower pip) {
        this.parent = parent;
        this.pip = pip;
        
        ScientificNumber valueMultiplier = new ScientificNumber(Math.pow(PrimeCenter.getPrime(pip.getActivePrimeIndex().getIndex()), pip.getPower()), 0);
        ScientificNumber factorMultiplier = new ScientificNumber((pip.getPower() + 1), 0);

        if (parent != null) {
            value = parent.value.multiply(valueMultiplier);
            factor = parent.factor.multiply(factorMultiplier);
            parent.offsprings.add(this);
            if (!parent.generatedHcns.isEmpty()) {
                generatedHcns = parent.generatedHcns;
                parent.generatedHcns = new LinkedHashMap<>();
                generatedHcns.values().forEach(hcn -> hcn.setBody(this));
            }
        } else {
            value = valueMultiplier;
            factor = factorMultiplier;
        }
    }

    @Override
    public int compareTo(HcnBody other) {
        return this.value.compareTo(other.value);
    }

    @Override
    public String toString() {return parentChainString() + "v=" + value + " f=" + factor;}

    public String parentChainString() {return getFullChain().stream().map(HcnBody::getBodyId).collect(Collectors.toList()).toString();}

    public int lowestPossibleLapi() {
        if (pip.getPower() < 2) {
            return parent.lowestPossibleLapi();
        } else {
            return pip.getActivePrimeIndex().getIndex();
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

    public ScientificNumber getValueMultiplier(HcnBody hcnBody) {
        if (!this.pip.equals(hcnBody.pip)) {
            int powerdiff = hcnBody.pip.getPower() - pip.getPower();
            ScientificNumber localMultiplier = new ScientificNumber(Math.pow(PrimeCenter.getPrime(this.pip.getActivePrimeIndex().getIndex()), powerdiff), 0);
            if (parent == null) {
                return localMultiplier;
            } else {
                return parent.getValueMultiplier(hcnBody.getParent()).multiply(localMultiplier);
            }
        } else {
            if (parent == null) {
                return new ScientificNumber(1,0);
            } else {
                return parent.getValueMultiplier(hcnBody.getParent());
            }
        }
    }

    public ScientificNumber getFactorMultiplier(HcnBody hcnBody) {
        if (!this.pip.equals(hcnBody.pip)) {

            ScientificNumber localMultiplier = new ScientificNumber(((double) (hcnBody.pip.getPower() + 1) / (pip.getPower() + 1)), 0);

            if (parent == null) {
                return localMultiplier;
            } else {
                return parent.getFactorMultiplier(hcnBody.getParent()).multiply(localMultiplier);
            }
        } else {
            if (parent == null) {
                return new ScientificNumber(1,0);
            } else {
                return parent.getFactorMultiplier(hcnBody.getParent());
            }
        }
    }

    public Hcn generateNextHcn(LastActivePrimeIndexGroup lapiGroup) {
        if (generatedHcns.isEmpty()) {
            HcnBody referenceBody = smallerBody;

            while (!referenceBody.generatedHcns.containsKey(lapiGroup)) {
                referenceBody = referenceBody.smallerBody;
            }

            Hcn referenceHcn = referenceBody.generatedHcns.get(lapiGroup);

            Hcn newHcn = new Hcn(this, referenceHcn.getLastActivePrime());
            newHcn.setValue(referenceHcn.getValue().multiply(referenceBody.getValueMultiplier(this)));
            newHcn.setFactor(referenceHcn.getFactor().multiply(referenceBody.getFactorMultiplier(this)));
            generatedHcns.put(lapiGroup, newHcn);
            return newHcn;
        } else {
            Hcn preHcn = generatedHcns.get(lapiGroup.getLowerLapiGroup());
            Hcn newHcn = new Hcn(this, preHcn.getLastActivePrime() + 1);
            newHcn.setValue(preHcn.getValue().multiply(lapiGroup.getPrimeValue()));
            newHcn.setFactor(preHcn.getFactor().multiply(new ScientificNumber(2, 0)));
            generatedHcns.put(lapiGroup, newHcn);
            return newHcn;
        }
    }
}