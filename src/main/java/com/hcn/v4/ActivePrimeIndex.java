package com.hcn.v4;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class ActivePrimeIndex {
    private int index;
    private TreeMap<Integer, PrimeIndexPower> pips = new TreeMap<>();
    private FilteredHcnBodySet hcnBodyList = new FilteredHcnBodySet();
    private List<HcnBody> deactivatedBodyList = new ArrayList<>();
    private List<HcnBody> neverActivatedBodyList = new ArrayList<>();
    private List<PrimeIndexPower> deactivatedPips = new ArrayList<>();
    private ActivePrimeIndex nextActivePrimeIndex = null;
    private ActivePrimeIndex parentActivePrimeIndex = null;
    private FixedPowerGroup offspringFixedPowerGroup = null;
    private FixedPowerGroup parentFixedPowerGroup = null;
    
    public ActivePrimeIndex(int index) {
        this.index = index;
    }
    
    public int getIndex() {
        return index;
    }
    
    public TreeMap<Integer, PrimeIndexPower> getPips() {
        return pips;
    }
    
    public FilteredHcnBodySet getHcnBodyList() {
        return hcnBodyList;
    }

    public List<HcnBody> getDeactivatedBodyList() {
        return deactivatedBodyList;
    }

    public List<HcnBody> getNeverActivatedBodyList() {
        return neverActivatedBodyList;
    }

    public ActivePrimeIndex getNextActivePrimeIndex() {
        return nextActivePrimeIndex;
    }

    public ActivePrimeIndex getParentActivePrimeIndex() {
        return parentActivePrimeIndex;
    }

    public List<PrimeIndexPower> getDeactivatedPips() {
        return deactivatedPips;
    }

    public FixedPowerGroup getOffspringFixedPowerGroup() {
        return offspringFixedPowerGroup;
    }

    public FixedPowerGroup getParentFixedPowerGroup() {
        return parentFixedPowerGroup;
    }
    
    public PrimeIndexPower getLastPip() {return pips.get(pips.lastKey());}

    public boolean isLastActivePrimeIndex() {return nextActivePrimeIndex == null && offspringFixedPowerGroup == null;}
    
    @Override
    public String toString() {return "p" + index;}

    public void addNextPrimeIndexPower() {
        int nextPowerValue = pips.lastKey() + 1;
        PrimeIndexPower newPower = new PrimeIndexPower(this, nextPowerValue);
        pips.put(nextPowerValue, newPower);
    }

    public int getHighestProved() {
        if (pips.lastEntry().getValue().isProved()) {
            return pips.lastKey();
        } else {
            return pips.lastKey() - 1;
        }
    }
    
    public boolean extendMatrix(HcnBody provedBody) {

        if (provedBody.isProved()) {
            return false;
        }

        provedBody.setProved(true);
        boolean bodiesCreated = false;

        //check if extension is required

        if (!provedBody.getPip().isProved()) {
            provedBody.getPip().setProved(true);

            if (provedBody.getPip().equals(getLastPip())) {

                if (offspringFixedPowerGroup != null) {

                    reactivateFixedPowerGroupMember();
                    nextActivePrimeIndex.addNextPrimeIndexPower();
                    integrateExistingBodiesAfterReactivate();
                }

                if (nextActivePrimeIndex != null) {
                    if (nextActivePrimeIndex.getLastPip().isProved()) {
                        if (nextActivePrimeIndex.pips.lastKey() < provedBody.getPip().getPower()) {
                            extendNextActive();
                            bodiesCreated = true;
                        }
                    }


                } else if (isLastActivePrimeIndex() && provedBody.getPip().getPower() == 2) {
                    extendWithNextActivePrimeIndex();
                    bodiesCreated = true;
                }

                boolean localExtensionRequired = parentActivePrimeIndex == null && parentFixedPowerGroup == null ||
                        parentActivePrimeIndex != null && parentActivePrimeIndex.getHighestProved() > getLastPip().getPower();

                if (localExtensionRequired) {
                    bodiesCreated = extendMatrixLocally();
                }
            }
        }

        if (parentActivePrimeIndex != null) {
            boolean parentBodiesCreated = parentActivePrimeIndex.extendMatrix(provedBody.getParent());
            bodiesCreated = bodiesCreated || parentBodiesCreated;
        } else if (parentFixedPowerGroup != null) {
            boolean parentBodiesCreated = parentFixedPowerGroup.getParentPrimeIndex().extendMatrix(provedBody.getParent());
            bodiesCreated = bodiesCreated || parentBodiesCreated;
        }

        return bodiesCreated;
    }

    private void extendNextActive() {
        nextActivePrimeIndex.addNextPrimeIndexPower();

        Set<HcnBody> createdBodies = this.pips.values().stream()
                .filter(pip -> pip.getPower() >= nextActivePrimeIndex.getLastPip().getPower())
                .flatMap(pip -> pip.getActiveHcnBodies().stream())
                .map(parentBody -> new HcnBody(parentBody, nextActivePrimeIndex.getLastPip()))
                .collect(Collectors.toSet());
        Set<HcnBody> successfullyAdded = nextActivePrimeIndex.hcnBodyList.addGroup(createdBodies);

        if (nextActivePrimeIndex.nextActivePrimeIndex != null) {
            nextActivePrimeIndex.nextActivePrimeIndex.generateHcnBodies(successfullyAdded);
        }
    }

    private void integrateExistingBodiesAfterReactivate() {
        Set<HcnBody> createdBodies = this.pips.values().stream()
                .filter(pip -> pip.getPower() >= nextActivePrimeIndex.getLastPip().getPower())
                .flatMap(pip -> pip.getActiveHcnBodies().stream())
                .map(parentBody -> new HcnBody(parentBody, nextActivePrimeIndex.getLastPip()))
                .collect(Collectors.toSet());
        Set<HcnBody> successfullyAdded = nextActivePrimeIndex.hcnBodyList.addGroup(createdBodies);

        if (nextActivePrimeIndex.offspringFixedPowerGroup != null) {
            nextActivePrimeIndex.offspringFixedPowerGroup.getOffspringPrimeIndex().generateHcnBodies(successfullyAdded);
        } else {
            nextActivePrimeIndex.nextActivePrimeIndex.generateHcnBodies(successfullyAdded);
        }
    }

    private boolean extendMatrixLocally() {
        boolean bodiesCreated;
        addNextPrimeIndexPower();

        Set<HcnBody> createdBodies = generateLocalStarterBodies();
        Set<HcnBody> successfullyAdded = hcnBodyList.addGroup(createdBodies);
        bodiesCreated = !successfullyAdded.isEmpty();

        if (nextActivePrimeIndex != null) {
            nextActivePrimeIndex.generateHcnBodies(successfullyAdded);
        }
        return bodiesCreated;
    }

    private Set<HcnBody> generateLocalStarterBodies() {
        Set<HcnBody> createdBodies;
        if (parentActivePrimeIndex != null) {
            createdBodies = parentActivePrimeIndex.pips.values().stream()
                    .filter(pip -> pip.getPower() >= getLastPip().getPower())
                    .flatMap(pip -> pip.getActiveHcnBodies().stream())
                    .map(parentBody -> new HcnBody(parentBody, getLastPip()))
                    .collect(Collectors.toSet());
        } else {
            createdBodies = Set.of(new HcnBody(null, getLastPip()));
        }
        return createdBodies;
    }

    private void reactivateFixedPowerGroupMember() {
        ActivePrimeIndex toReactivate = offspringFixedPowerGroup.reactivatePrimeIndex();

        if (offspringFixedPowerGroup.getFixedPowerGroup().isEmpty()) {
            offspringFixedPowerGroup.getOffspringPrimeIndex().parentFixedPowerGroup = null;
            offspringFixedPowerGroup.getOffspringPrimeIndex().parentActivePrimeIndex = toReactivate;
            toReactivate.nextActivePrimeIndex = offspringFixedPowerGroup.getOffspringPrimeIndex();
            toReactivate.offspringFixedPowerGroup = null;
            toReactivate.parentFixedPowerGroup = null;
            toReactivate.parentActivePrimeIndex = this;
            this.offspringFixedPowerGroup = null;
            this.nextActivePrimeIndex = toReactivate;
        } else {
            toReactivate.offspringFixedPowerGroup = offspringFixedPowerGroup;
            toReactivate.nextActivePrimeIndex = null;
            toReactivate.parentActivePrimeIndex = this;
            toReactivate.parentFixedPowerGroup = null;
            this.offspringFixedPowerGroup = null;
            this.nextActivePrimeIndex = toReactivate;
        }
    }

    public void generateHcnBodies(Set<HcnBody> previousBodies) {
        Set<HcnBody> createdBodies = previousBodies.stream()
                .flatMap(previousBody -> pips.values().stream()
                        .filter(pip -> pip.getPower() <= previousBody.getPip().getPower())
                        .map(pip -> new HcnBody(previousBody, pip)))
                .collect(Collectors.toSet());

        if (parentFixedPowerGroup != null) {
            createdBodies.forEach(hcnBody -> {
                hcnBody.setValue(hcnBody.getValue().multiply(parentFixedPowerGroup.getValue()));
                hcnBody.setFactor(hcnBody.getFactor().multiply(parentFixedPowerGroup.getFactor()));
            });
        }

        Set<HcnBody> successfullyAdded = hcnBodyList.addGroup(createdBodies);

        if (nextActivePrimeIndex != null) {
            nextActivePrimeIndex.generateHcnBodies(successfullyAdded);
        } else if (offspringFixedPowerGroup != null) {
            offspringFixedPowerGroup.getOffspringPrimeIndex().generateHcnBodies(successfullyAdded);
        }
    }

    private void extendWithNextActivePrimeIndex() {
        nextActivePrimeIndex = new ActivePrimeIndex(index + 1);
        nextActivePrimeIndex.parentActivePrimeIndex = this;

        PrimeIndexPower pip1 = new PrimeIndexPower(nextActivePrimeIndex, 1);
        nextActivePrimeIndex.pips.put(1, pip1);
        pip1.setProved(true);

        // Create bodies with pip1 from all current bodies
        Set<HcnBody> pip1Bodies = hcnBodyList.stream().map(previousBody -> {
            HcnBody newBody = new HcnBody(previousBody, pip1);
            if (previousBody.isProved()) {
                newBody.setProved(true);
            }
            return newBody;
        }).collect(Collectors.toSet());

        nextActivePrimeIndex.hcnBodyList.addGroup(pip1Bodies);

        PrimeIndexPower pip2 = new PrimeIndexPower(nextActivePrimeIndex, 2);
        nextActivePrimeIndex.pips.put(2, pip2);

        // Create bodies with pip2 from parent's pip2+ bodies
        Set<HcnBody> pip2Bodies = pips.values().stream()
                .filter(pip -> pip.getPower() >= 2)
                .flatMap(pip -> pip.getActiveHcnBodies().stream())
                .map(parentBody -> new HcnBody(parentBody, pip2))
                .collect(Collectors.toSet());

        nextActivePrimeIndex.hcnBodyList.addGroup(pip2Bodies);
    }

    public void deactivateRecursive(HcnBody defeated, HcnBody superiorBody) {

        defeated.setSuperiorBody(superiorBody);

        if (defeated.getPip().getActiveHcnBodies().isEmpty()) {
            // pip is deletable
            deactivatedPips.add(defeated.getPip());
            pips.remove(defeated.getPip().getPower());

            if (pips.size() == 1) {
                // activePrimeIndex is fixed
                fixPowerMaintain();
            }
        }

        if (defeated.getParent() != null) {
            // activePrimeIndex must be removed from parent's offsprings
            defeated.getParent().getOffsprings().remove(defeated);

            if (defeated.isProved()) {
                defeated.getParent().getDeactivatedOffsprings().add(defeated);
            } else {
                defeated.getParent().getNeverActivatedOffsprings().add(defeated);
            }


            if (defeated.getParent().getOffsprings().isEmpty()) {
                // parent is also deletable
                defeated.getParent().deactivateFromLists();
                defeated.getParent().getPip().getActivePrimeIndex().deactivateRecursive(defeated.getParent(), superiorBody.getParent());
            }
        }
    }

    private void fixPowerMaintain() {
        if (parentActivePrimeIndex != null) {
            FixedPowerGroup fixedPowerGroup = new FixedPowerGroup();
            fixedPowerGroup.receivePrimeIndex(this);
            fixedPowerGroup.setParentPrimeIndex(parentActivePrimeIndex);
            parentActivePrimeIndex.nextActivePrimeIndex = null;
            parentActivePrimeIndex.offspringFixedPowerGroup = fixedPowerGroup;
            nextActivePrimeIndex.parentFixedPowerGroup = fixedPowerGroup;
            nextActivePrimeIndex.parentActivePrimeIndex = null;
        } else {
            parentFixedPowerGroup.receivePrimeIndex(this);
            nextActivePrimeIndex.parentFixedPowerGroup = parentFixedPowerGroup;
            nextActivePrimeIndex.parentActivePrimeIndex = null;
        }
    }
}
