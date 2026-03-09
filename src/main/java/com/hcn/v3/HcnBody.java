package com.hcn.v3;

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
    private final ScientificNumber value;
    private final ScientificNumber factor;
    private HcnFactory hcnFactory = null;
    private HcnBody superiorBody = null;

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

    public PrimeIndexPower getPip() {
        return pip;
    }

    public ScientificNumber getValue() {
        return value;
    }

    public ScientificNumber getFactor() {
        return factor;
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
        pip.getActivePrimeIndex().getHcnBodyList().remove(this);
        if (proved) {
            pip.getActivePrimeIndex().getDeactivatedBodyList().add(this);
        }  else {
            pip.getActivePrimeIndex().getNeverActivatedBodyList().add(this);
        }

        pip.removeActiveHcnBody(this);
    }

    public void deactivateRecursive(HcnBody superiorBody) {

        // 2. Beállítja a superiorHcnBody-t
        this.superiorBody = superiorBody;
        
        // 3. Lecsekkolja, hogy ez volt-e az utolsó aktív HcnBody a pip-ben
        if (pip.getActiveHcnBodies().isEmpty()) {
            // Töröljük EZT a pip-et, nem a firstKey-t!
            pip.getActivePrimeIndex().getDeactivatedPips().add(pip);
            pip.getActivePrimeIndex().getPips().remove(pip.getPower());
        }
        
        // 4-5. Ha van parent, deaktiválja magát a parentben, és ha kell, rekurzívan a parentet is
        if (parent != null) {
            parent.offspring.remove(this);

            if (proved) {
                parent.deactivatedOffsprings.add(this);
            } else {
                parent.neverActivatedOffsprings.add(this);
            }

            
            if (parent.offspring.isEmpty()) {
                // Parent mindig benne van a listákban, töröljük
                parent.deactivateFromLists();
                parent.deactivateRecursive(superiorBody != null ? superiorBody.parent : null);
            }
        }
    }
}