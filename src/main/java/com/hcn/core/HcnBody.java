package com.hcn.core;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class HcnBody implements Comparable<HcnBody> {
    private HcnBody parent;
    private Long tempId;
    public Long getTempId() { return tempId; }
    public void setTempId(Long tempId) { this.tempId = tempId; }
    private List<HcnBody> offsprings = new ArrayList<>();
    private PrimeIndexPower pip;
    private boolean proved = false;
    private ScientificNumber value;
    private ScientificNumber factor;
    private HcnBody smallerBody = null;
    private HcnBody largerBody = null;
    private HcnGenerator hcnGenerator = null;
    private List<LastActivePrimeIndexGroup> walkerBodyForLapi = new ArrayList<>();

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
    public void setParent(HcnBody parent) {this.parent = parent;}
    public void setParentForLoad(HcnBody parent) {this.parent = parent;}
    private boolean deactivated = false;
    public boolean isDeactivated(){
        return deactivated;
    }
    public String getBodyId() {
        return "p" + pip.getActivePrimeIndex().getIndex() + "^" + pip.getPower();
    }
    public String getOffspringPowers() {return offsprings.stream().map(o -> String.valueOf(o.pip.getPower())).collect(Collectors.joining(", "));}
    public Hcn getLastGeneratedHcn() {return hcnGenerator != null ? hcnGenerator.getLastGeneratedHcn() : null;}
    public void setLastGeneratedHcn(Hcn hcn) {
        if (hcnGenerator == null) hcnGenerator = new HcnGenerator(this);
        hcnGenerator.setLastGeneratedHcn(hcn);
    }
    public HcnGenerator getHcnGenerator() {return hcnGenerator;}
    public void setHcnGenerator(HcnGenerator hcnGenerator) {this.hcnGenerator = hcnGenerator;}
    public List<LastActivePrimeIndexGroup> getWalkerBodyForLapi() {return walkerBodyForLapi;}
    public void addWalkerBodyForLapi(LastActivePrimeIndexGroup walkerBodyForLapi) {this.walkerBodyForLapi.add(walkerBodyForLapi);}
    public void removeWalkerBodyForLapi(LastActivePrimeIndexGroup walkerBodyForLapi) {this.walkerBodyForLapi.remove(walkerBodyForLapi);}

    public HcnBody() {}

    public void setDeactivated(boolean deactivated) { this.deactivated = deactivated; }

    private boolean matchesChain(String chain) {
        return parentChainString().equals(chain);
    }

    public HcnBody(HcnBody parent, PrimeIndexPower pip) {
        this.parent = parent;
        this.pip = pip;
        
        ScientificNumber valueMultiplier = new ScientificNumber(Math.pow(PrimeCenter.getPrime(pip.getActivePrimeIndex().getIndex()), pip.getPower()), 0);
        ScientificNumber factorMultiplier = new ScientificNumber((pip.getPower() + 1), 0);

        if (parent != null) {
            value = parent.value.multiply(valueMultiplier);
            factor = parent.factor.multiply(factorMultiplier);
            if (parent.isDeactivated()) {
                deactivated = true;
            } else {
                parent.offsprings.add(this);
                if (parent.getHcnGenerator() != null) {
                    hcnGenerator = parent.hcnGenerator;
                    parent.hcnGenerator = null;
                    hcnGenerator.setCurrentHcnBody(this);
                }
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
    public String toString() {return parentChainString() + " v=" + value + " f=" + factor;}

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

    public void deactivateFromLists() {
        if (deactivated) {
            return;
        }
        deactivated = true;
        pip.getActivePrimeIndex().getHcnBodyList().remove(this);
        pip.removeActiveHcnBody(this);
        if (hcnGenerator != null) {
            if (!hcnGenerator.getWalkerGeneratorForLapi().isEmpty()) {
                for (LastActivePrimeIndexGroup lapi : hcnGenerator.getWalkerGeneratorForLapi()) {
                    lapi.setWalkerDeleted(true);
                    //System.out.println("lapi " + lapi.getLastActivePrimeIndex() + " was set walkerDeleted by " + this.parentChainString());
                }
            }
            if (hcnGenerator.getSmallerGenerator() != null) {
                hcnGenerator.getSmallerGenerator().setLargerGenerator(hcnGenerator.getLargerGenerator());
            }
            if (hcnGenerator.getLargerGenerator() != null) {
                hcnGenerator.getLargerGenerator().setSmallerGenerator(hcnGenerator.getSmallerGenerator());
            }
            hcnGenerator.setLargerGenerator(null);
            hcnGenerator.setSmallerGenerator(null);

            hcnGenerator = null;
        }
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
        if (hcnGenerator == null) {
            hcnGenerator = new HcnGenerator(this);
        }
        return hcnGenerator.generateNextHcn(lapiGroup);
    }

    public void gotDominated() {
        deactivated = true;
        pip.removeActiveHcnBody(this);
        pip.getActivePrimeIndex().deactivateRecursive(this, false);
    }
}