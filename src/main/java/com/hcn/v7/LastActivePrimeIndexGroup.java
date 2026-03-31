package com.hcn.v7;

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
        Hcn currentFloor = firstHcn;
        HcnBody body = bodyList.getSmallestBody();
        while (body != null) {
            Hcn newHcn = body.generateNextHcn(this);
            currentFloor = mergeOneHcn(currentFloor, newHcn, deactivationBin);
            body = body.getLargerBody();
        }
    }

    void mergeNewHcns(java.util.List<Hcn> sortedNewHcns) {
        Hcn currentFloor = firstHcn;
        for (Hcn newHcn : sortedNewHcns) {
            currentFloor = mergeOneHcn(currentFloor, newHcn, null);
        }
    }

    private Hcn mergeOneHcn(Hcn currentFloor, Hcn newHcn, Set<HcnBody> deactivationBin) {
        // advance floor to just before newHcn's position
        while (currentFloor.getLargerHcns().get(this) != null
                && currentFloor.getLargerHcns().get(this).getValue().isSmallerThan(newHcn.getValue())) {
            currentFloor = currentFloor.getLargerHcns().get(this);
        }

        if (!newHcn.getFactor().isBiggerThan(currentFloor.getFactor())) {
            // dominated — don't insert
            if (deactivationBin != null) {
                checkDeactivation(newHcn, currentFloor, deactivationBin);
            }
            return currentFloor;
        }

        // remove dominated ceiling Hcns
        Hcn ceiling = currentFloor.getLargerHcns().get(this);
        while (ceiling != null && ceiling.getFactor().isNotBiggerThan(newHcn.getFactor())) {
            if (deactivationBin != null) {
                checkDeactivation(ceiling, newHcn, deactivationBin);
            }
            Hcn next = ceiling.getLargerHcns().get(this);
            ceiling.getSmallerHcns().remove(this);
            ceiling.getLargerHcns().remove(this);
            ceiling = next;
        }

        // insert newHcn between floor and ceiling
        currentFloor.getLargerHcns().put(this, newHcn);
        newHcn.getSmallerHcns().put(this, currentFloor);
        newHcn.getLargerHcns().put(this, ceiling);
        if (ceiling != null) ceiling.getSmallerHcns().put(this, newHcn);

        return newHcn;
    }

    /**
     * Insert newHcn into this level's chain using anchor from lower level.
     * Returns true if accepted, false if dominated.
     */
    boolean insertIntoChain(Hcn newHcn, LastActivePrimeIndexGroup lowerLevel, Set<HcnBody> deactivationBin) {
        // find anchor: walk backwards on lower level to find an Hcn that exists in this level
        Hcn anchor = null;
        if (lowerLevel != null) {
            Hcn walker = newHcn.getSmallerHcns().get(lowerLevel);
            while (walker != null) {
                if (walker.getSmallerHcns().containsKey(this)) {
                    anchor = walker;
                    break;
                }
                walker = walker.getSmallerHcns().get(lowerLevel);
            }
        }

        // find floor: from anchor, walk forward in this level to find position
        Hcn prev;
        if (anchor != null) {
            prev = anchor;
            Hcn next = prev.getLargerHcns().get(this);
            while (next != null && next.getValue().isSmallerThan(newHcn.getValue())) {
                prev = next;
                next = prev.getLargerHcns().get(this);
            }
        } else {
            // no anchor found — newHcn might go before firstHcn, or walk from start
            if (firstHcn == null || !firstHcn.getValue().isSmallerThan(newHcn.getValue())) {
                prev = null;
            } else {
                prev = firstHcn;
                Hcn next = prev.getLargerHcns().get(this);
                while (next != null && next.getValue().isSmallerThan(newHcn.getValue())) {
                    prev = next;
                    next = prev.getLargerHcns().get(this);
                }
            }
        }

        // check if dominated by floor
        if (prev != null && !newHcn.getFactor().isBiggerThan(prev.getFactor())) {
            if (deactivationBin != null) {
                checkDeactivation(newHcn, prev, deactivationBin);
            }
            return false;
        }

        // link newHcn into chain
        Hcn current = (prev != null) ? prev.getLargerHcns().get(this) : firstHcn;
        if (prev != null) {
            prev.getLargerHcns().put(this, newHcn);
        } else {
            firstHcn = newHcn;
        }
        newHcn.getSmallerHcns().put(this, prev);

        // remove dominated ceiling Hcns
        Hcn ceiling = current;
        while (ceiling != null && ceiling.getFactor().isNotBiggerThan(newHcn.getFactor())) {
            if (deactivationBin != null) {
                checkDeactivation(ceiling, newHcn, deactivationBin);
            }
            Hcn next = ceiling.getLargerHcns().get(this);
            ceiling.getSmallerHcns().remove(this);
            ceiling.getLargerHcns().remove(this);
            ceiling = next;
        }

        newHcn.getLargerHcns().put(this, ceiling);
        if (ceiling != null) ceiling.getSmallerHcns().put(this, newHcn);

        return true;
    }

    public boolean isBodyAllowedToGenerate(HcnBody newBody) {
        return newBody.lowestPossibleLapi() <= this.lastActivePrimeIndex;
    }

    public boolean insertHcnIntoAllLevels(Hcn newHcn, Set<HcnBody> deactivationBin) {
        LastActivePrimeIndexGroup group = this;
        LastActivePrimeIndexGroup lowerLevel = this.lowerLapiGroup;
        while (group != null) {
            if (!group.insertIntoChain(newHcn, lowerLevel, deactivationBin)) {
                return false;
            }
            lowerLevel = group;
            group = group.getHigherLapiGroup();
        }
        return true;
    }

    private void checkDeactivation(Hcn defeated, Hcn superior, Set<HcnBody> deactivationBin) {
        if (defeated.getBody() == null || defeated.getBody().isDeactivated()) return;
        if (defeated.getBody().isProved() || superior.getLastActivePrime() <= defeated.getLastActivePrime()) {
            deactivationBin.add(defeated.getBody());
        }
    }

    public void removeFirstHcn(Hcn provedHcn) {

        Hcn deleted;

        do {
            deleted = firstHcn;
            Hcn next = firstHcn.getLargerHcns().get(this);
            firstHcn = next;
            firstHcn.getSmallerHcns().put(this, null);
            deleted.getSmallerHcns().remove(this);
            deleted.getLargerHcns().remove(this);
        } while (deleted != provedHcn);

        if (lowerLapiGroup != null && lowerLapiGroup.lastActivePrimeIndex >= provedHcn.getLastActivePrime()) lowerLapiGroup.removeFirstHcn(provedHcn);
    }

    @Override
    public String toString() {
        return "LastActivePrimeIndexGroup{" +
                "lastActivePrimeIndex=" + lastActivePrimeIndex +
                '}';
    }

    public void remove() {
        // clean up all Hcns in this group's chain
        Hcn current = firstHcn;
        while (current != null) {
            Hcn next = current.getLargerHcns().get(this);
            current.getSmallerHcns().remove(this);
            current.getLargerHcns().remove(this);
            if (current.getBody() != null) {
                current.getBody().getGeneratedHcns().remove(this);
            }
            current = next;
        }
        firstHcn = null;
        higherLapiGroup.lowerLapiGroup = null;
        higherLapiGroup = null;
    }

    public boolean isReadyToDelete() {
        Hcn current = firstHcn;
        while (current != null) {
            if (current.getSmallerHcns().containsKey(higherLapiGroup)) return false;
            current = current.getLargerHcns().get(this);
        }
        return true;
    }
}
