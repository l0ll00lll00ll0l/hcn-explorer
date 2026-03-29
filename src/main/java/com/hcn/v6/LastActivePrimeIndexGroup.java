package com.hcn.v6;

import java.util.Set;

public class LastActivePrimeIndexGroup {
    private int lastActivePrimeIndex;
    private ScientificNumber primeValue;
    private Hcn firstHcn;
    private LastActivePrimeIndexGroup lowerLapiGroup;
    private LastActivePrimeIndexGroup higherLapiGroup;

    public int getLastActivePrimeIndex() {return lastActivePrimeIndex;}
    public ScientificNumber getPrimeValue() {return primeValue;}
    public void setLastActivePrimeIndex(int lastActivePrimeIndex) {this.lastActivePrimeIndex = lastActivePrimeIndex;}
    public Hcn getFirstHcn() {return firstHcn;}
    public void setFirstHcn(Hcn firstHcn) {this.firstHcn = firstHcn;}
    public LastActivePrimeIndexGroup getLowerLapiGroup() {return lowerLapiGroup;}
    public void setLowerLapiGroup(LastActivePrimeIndexGroup lowerLapiGroup) {this.lowerLapiGroup = lowerLapiGroup;}
    public LastActivePrimeIndexGroup getHigherLapiGroup() {return higherLapiGroup;}
    public void setHigherLapiGroup(LastActivePrimeIndexGroup higherLapiGroup) {this.higherLapiGroup = higherLapiGroup;}

    public LastActivePrimeIndexGroup(int lastActivePrimeIndex) {
        primeValue = new ScientificNumber(PrimeCenter.getPrime(lastActivePrimeIndex), 0);
        this.lastActivePrimeIndex = lastActivePrimeIndex;
    }

    public void generateHcnsForNewLapiGroup(BodyList bodyList, java.util.Set<HcnBody> deactivationBin) {
        copyHcnsFromLowerLapiGroup();
        mergeNewGeneratedHcns(bodyList, deactivationBin);
    }

    void copyHcnsFromLowerLapiGroup() {
        Hcn lowerHcn = lowerLapiGroup.firstHcn;
        firstHcn = lowerHcn;
        firstHcn.getSmallerHcns().put(this, null);

        while (lowerHcn.getLargerHcns().get(lowerLapiGroup) != null) {
            Hcn nextHcn = lowerHcn.getLargerHcns().get(lowerLapiGroup);
            lowerHcn.getLargerHcns().put(this, nextHcn);
            nextHcn.getSmallerHcns().put(this, lowerHcn);
            lowerHcn = nextHcn;
        }
        lowerHcn.getLargerHcns().put(this, null);
    }

    private void mergeNewGeneratedHcns(BodyList bodyList, java.util.Set<HcnBody> deactivationBin) {
        HcnBody body = bodyList.getSmallestBody();
        while (body != null) {
            Hcn newHcn = body.generateNextHcn(this);
            insertIntoChain(newHcn, deactivationBin);
            body = body.getLargerBody();
        }
    }

    void mergeNewHcns(java.util.List<Hcn> sortedNewHcns) {
        for (Hcn newHcn : sortedNewHcns) {
            insertIntoChain(newHcn, null);
        }
    }

    /**
     * Insert newHcn into this lapiGroup's chain at the correct position by value.
     * Then determine domination: either newHcn is dominated by the effective floor,
     * or newHcn dominates some larger Hcns.
     */
    private void insertIntoChain(Hcn newHcn, Set<HcnBody> deactivationBin) {
        // find position: walk to the last Hcn smaller than newHcn
        Hcn prev = null;
        Hcn current = firstHcn;
        while (current != null && current.getValue().isSmallerThan(newHcn.getValue())) {
            prev = current;
            current = current.getLargerHcns().get(this);
        }

        // link newHcn into chain between prev and current
        if (prev != null) {
            prev.getLargerHcns().put(this, newHcn);
        } else {
            firstHcn = newHcn;
        }
        newHcn.getSmallerHcns().put(this, prev);
        newHcn.getLargerHcns().put(this, current);
        if (current != null) current.getSmallerHcns().put(this, newHcn);

        // find effective floor: nearest smaller non-dominated Hcn
        Hcn effectiveFloor = prev;
        while (effectiveFloor != null && effectiveFloor.isDominated()) {
            effectiveFloor = effectiveFloor.getSmallerHcns().get(this);
        }

        if (effectiveFloor != null && !newHcn.getFactor().isBiggerThan(effectiveFloor.getFactor())) {
            // newHcn is dominated
            newHcn.setSuperiorHcn(effectiveFloor);
            if (deactivationBin != null) {
                checkDeactivation(newHcn, effectiveFloor, deactivationBin);
            }
        } else {
            // newHcn is active — mark dominated larger Hcns
            Hcn walker = newHcn.getLargerHcns().get(this);
            while (walker != null && walker.getFactor().isNotBiggerThan(newHcn.getFactor())) {
                if (!walker.isDominated()) {
                    walker.setSuperiorHcn(newHcn);
                    if (deactivationBin != null) {
                        checkDeactivation(walker, newHcn, deactivationBin);
                    }
                }
                walker = walker.getLargerHcns().get(this);
            }
        }
    }

    public boolean isBodyAllowedToGenerate(HcnBody newBody) {
        return newBody.lowestPossibleLapi() <= this.lastActivePrimeIndex;
    }

    public void insertHcnIntoAllLevels(Hcn newHcn, Set<HcnBody> deactivationBin) {
        LastActivePrimeIndexGroup group = this;
        while (group != null) {
            group.insertIntoChain(newHcn, deactivationBin);
            group = group.getHigherLapiGroup();
        }
    }

    private void checkDeactivation(Hcn defeated, Hcn superior, Set<HcnBody> deactivationBin) {
        if (defeated.getBody() == null || defeated.getBody().isDeactivated()) return;
        if (defeated.getBody().isProved() || superior.getLastActivePrime() <= defeated.getLastActivePrime()) {
            deactivationBin.add(defeated.getBody());
        }
    }

    public void removeFirstHcn(Hcn firstHcn) {
        if (this.firstHcn != firstHcn) return;
        Hcn next = firstHcn.getLargerHcns().get(this);
        this.firstHcn = next;
        if (next != null) next.getSmallerHcns().put(this, null);
        if (lowerLapiGroup != null) lowerLapiGroup.removeFirstHcn(firstHcn);
    }

    @Override
    public String toString() {
        return "LastActivePrimeIndexGroup{" +
                "lastActivePrimeIndex=" + lastActivePrimeIndex +
                '}';
    }

    public void remove() {
        higherLapiGroup.lowerLapiGroup = null;
        higherLapiGroup = null;
    }

    public boolean isReadyToDelete() {
        Hcn current = firstHcn;
        while (current != null) {
            if (!current.isDominated()) return false;
            current = current.getLargerHcns().get(this);
        }
        return true;
    }
}
