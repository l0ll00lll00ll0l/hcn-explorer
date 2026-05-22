package com.hcn.detailed;

import com.hcn.detailed.optional.ExtendedHcnBodyData;

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
    private Hcn lastGeneratedHcn = null;
    private List<LastActivePrimeIndexGroup> walkerBodyForLapi = new ArrayList<>();
    private ExtendedHcnBodyData extendedHcnBodyData = null;

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
    public boolean isDeactivated(){
        return !pip.getActiveHcnBodies().contains(this);
    }
    public String getBodyId() {
        return "p" + pip.getActivePrimeIndex().getIndex() + "^" + pip.getPower();
    }
    public String getOffspringPowers() {return offsprings.stream().map(o -> String.valueOf(o.pip.getPower())).collect(Collectors.joining(", "));}
    public Hcn getLastGeneratedHcn() {return lastGeneratedHcn;}
    public void setLastGeneratedHcn(Hcn lastGeneratedHcn) {this.lastGeneratedHcn = lastGeneratedHcn;}
    public List<LastActivePrimeIndexGroup> getWalkerBodyForLapi() {return walkerBodyForLapi;}
    public void addWalkerBodyForLapi(LastActivePrimeIndexGroup walkerBodyForLapi) {this.walkerBodyForLapi.add(walkerBodyForLapi);}
    public void removeWalkerBodyForLapi(LastActivePrimeIndexGroup walkerBodyForLapi) {this.walkerBodyForLapi.remove(walkerBodyForLapi);}
    public ExtendedHcnBodyData getExtendedHcnBodyData() {return extendedHcnBodyData;}
    public void setExtendedHcnBodyData(ExtendedHcnBodyData extendedHcnBodyData) {this.extendedHcnBodyData = extendedHcnBodyData;}

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
            if (parent.lastGeneratedHcn != null) {
                lastGeneratedHcn = parent.lastGeneratedHcn;
                parent.lastGeneratedHcn = null;
                lastGeneratedHcn.setBody(this);
            }
            if (!parent.walkerBodyForLapi.isEmpty()) {
                this.walkerBodyForLapi.addAll(parent.walkerBodyForLapi);
                parent.walkerBodyForLapi.clear();
                walkerBodyForLapi.forEach(lapi -> lapi.setWalkerBody(this));
            }

            // detailed specific codes
            if (GeneratorConfig.isExtendedHcnBodyData()) {
                if (parent.extendedHcnBodyData == null) {
                    extendedHcnBodyData = new ExtendedHcnBodyData();
                } else {
                    extendedHcnBodyData = parent.extendedHcnBodyData;
                    parent.extendedHcnBodyData = new ExtendedHcnBodyData();
                }

            }
        } else {
            value = valueMultiplier;
            factor = factorMultiplier;

            // detailed specific codes
            if (GeneratorConfig.isExtendedHcnBodyData()) {
                extendedHcnBodyData = new ExtendedHcnBodyData();
            }
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

        // detailed specific codes
        boolean isFirstHcn = false;
        if (GeneratorConfig.isExtendedHcnBodyData()) {
            if (lastGeneratedHcn == null) {
                isFirstHcn = true;
            }
        }

        if (canExtendFromPreviousLapi(lapiGroup)) {
            lastGeneratedHcn = extendFromPreviousLapi(lapiGroup);
        } else {
            lastGeneratedHcn = computeFromReference(lapiGroup);
        }

        // detailed specific codes
        if (isFirstHcn) {
            extendedHcnBodyData.setFirstGeneratedHcn(lastGeneratedHcn);
        }
        return lastGeneratedHcn;
    }

    private boolean canExtendFromPreviousLapi(LastActivePrimeIndexGroup lapiGroup) {
        return lastGeneratedHcn != null
                && lastGeneratedHcn.getLastActivePrime() + 1 == lapiGroup.getLastActivePrimeIndex();
    }

    private Hcn extendFromPreviousLapi(LastActivePrimeIndexGroup lapiGroup) {
        Hcn newHcn = new Hcn(this, lastGeneratedHcn.getLastActivePrime() + 1);
        newHcn.setValue(lastGeneratedHcn.getValue().multiply(lapiGroup.getPrimeValue()));
        newHcn.setFactor(lastGeneratedHcn.getFactor().multiply(new ScientificNumber(2, 0)));
        return newHcn;
    }

    private Hcn computeFromReference(LastActivePrimeIndexGroup lapiGroup) {
        HcnBody referenceBody = findReferenceBody(lapiGroup);

        if (referenceBody == null) {
            Hcn referenceHcn = lapiGroup.getLowerLapiGroup().getWalkerBody().lastGeneratedHcn;
            Hcn newHcn = new Hcn(this, referenceHcn.getLastActivePrime() + 1);
            newHcn.setValue(referenceHcn.getValue().multiply(referenceBody.getValueMultiplier(this)).multiply(lapiGroup.getPrimeValue()));
            newHcn.setFactor(referenceHcn.getFactor().multiply(referenceBody.getFactorMultiplier(this)).multiply(new ScientificNumber(2, 0)));
            return newHcn;
        }

        Hcn referenceHcn = referenceBody.lastGeneratedHcn;
        Hcn newHcn = new Hcn(this, referenceHcn.getLastActivePrime());
        newHcn.setValue(referenceHcn.getValue().multiply(referenceBody.getValueMultiplier(this)));
        newHcn.setFactor(referenceHcn.getFactor().multiply(referenceBody.getFactorMultiplier(this)));
        return newHcn;
    }

    private HcnBody findReferenceBody(LastActivePrimeIndexGroup lapiGroup) {
        if (smallerBody == null) {
            return null;
        }
        HcnBody referenceBody = smallerBody;
        while (referenceBody.lastGeneratedHcn == null || referenceBody.lastGeneratedHcn.getLastActivePrime() != lapiGroup.getLastActivePrimeIndex()) {
            referenceBody = referenceBody.smallerBody;
        }
        return referenceBody;
    }

    public void gotDominated() {
        deactivateFromLists();
        pip.getActivePrimeIndex().deactivateRecursive(this);

        // detailed specific codes
        if (GeneratorConfig.isExtendedHcnBodyData()) {
            if (extendedHcnBodyData.getFirstSuperiorHcn() == null) {
                extendedHcnBodyData.setFirstPreProvedNonSuperiorHcn(lastGeneratedHcn);
            } else {
                extendedHcnBodyData.setFirstPostProvedNonSuperiorHcn(lastGeneratedHcn);
            }
        }
    }
}