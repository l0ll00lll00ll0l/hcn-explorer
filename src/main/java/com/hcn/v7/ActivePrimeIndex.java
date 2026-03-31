package com.hcn.v7;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class ActivePrimeIndex {
    private int index;
    private TreeMap<Integer, PrimeIndexPower> pips = new TreeMap<>();
    //private FilteredHcnBodySet hcnBodyList = new FilteredHcnBodySet();
    private BodyList hcnBodyList = new BodyList();
    private ActivePrimeIndex nextActivePrimeIndex = null;
    private ActivePrimeIndex parentActivePrimeIndex = null;
    private FixedPowerGroup offspringFixedPowerGroup = null;
    private FixedPowerGroup parentFixedPowerGroup = null;
    private BodyList bodyList = new BodyList();
    
    public ActivePrimeIndex(int index) {
        this.index = index;
    }
    
    public int getIndex() {
        return index;
    }
    
    public TreeMap<Integer, PrimeIndexPower> getPips() {
        return pips;
    }
    
    public BodyList getHcnBodyList() {return hcnBodyList;}

    public ActivePrimeIndex getNextActivePrimeIndex() {
        return nextActivePrimeIndex;
    }

    public ActivePrimeIndex getParentActivePrimeIndex() {
        return parentActivePrimeIndex;
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
    
    public List<HcnBody> extendMatrix(HcnBody provedBody) {

        if (provedBody.isProved()) {
            return List.of();
        }

        provedBody.setProved(true);
        List<HcnBody> newLeafBodies = new ArrayList<>();

        if (!provedBody.getPip().isProved()) {
            provedBody.getPip().setProved(true);

            if (provedBody.getPip().equals(getLastPip())) {

                if (offspringFixedPowerGroup != null) {
                    reactivateFixedPowerGroupMember();
                    nextActivePrimeIndex.addNextPrimeIndexPower();
                    newLeafBodies = mergeSortedBodies(newLeafBodies, integrateExistingBodiesAfterReactivate());
                }

                if (nextActivePrimeIndex != null) {
                    if (nextActivePrimeIndex.getLastPip().isProved()) {
                        if (nextActivePrimeIndex.pips.lastKey() < provedBody.getPip().getPower()) {
                            newLeafBodies = mergeSortedBodies(newLeafBodies, extendNextActive());
                        }
                    }
                } else if (isLastActivePrimeIndex() && provedBody.getPip().getPower() == 2) {
                    newLeafBodies = mergeSortedBodies(newLeafBodies, extendWithNextActivePrimeIndex());
                }

                boolean localExtensionRequired = parentActivePrimeIndex == null && parentFixedPowerGroup == null ||
                        parentActivePrimeIndex != null && parentActivePrimeIndex.getHighestProved() > getLastPip().getPower();

                if (localExtensionRequired) {
                    newLeafBodies = mergeSortedBodies(newLeafBodies, extendMatrixLocally());
                }
            }
        }

        if (parentActivePrimeIndex != null) {
            if (provedBody.getParent() != null) {
                newLeafBodies = mergeSortedBodies(newLeafBodies, parentActivePrimeIndex.extendMatrix(provedBody.getParent()));
            }
        } else if (parentFixedPowerGroup != null) {
            if (provedBody.getParent() != null) {
                newLeafBodies = mergeSortedBodies(newLeafBodies, parentFixedPowerGroup.getParentPrimeIndex().extendMatrix(provedBody.getParent()));
            }
        }

        return newLeafBodies;
    }

    private List<HcnBody> extendNextActive() {
        nextActivePrimeIndex.addNextPrimeIndexPower();

        Set<HcnBody> createdBodies = this.pips.values().stream()
                .filter(pip -> pip.getPower() >= nextActivePrimeIndex.getLastPip().getPower())
                .flatMap(pip -> pip.getActiveHcnBodies().stream())
                .map(parentBody -> new HcnBody(parentBody, nextActivePrimeIndex.getLastPip()))
                .collect(Collectors.toSet());
        List<HcnBody> successfullyAdded = nextActivePrimeIndex.hcnBodyList.addGroup(createdBodies);

        if (nextActivePrimeIndex.nextActivePrimeIndex != null) {
            return nextActivePrimeIndex.nextActivePrimeIndex.generateHcnBodies(successfullyAdded);
        }
        return successfullyAdded;
    }

    private List<HcnBody> integrateExistingBodiesAfterReactivate() {
        Set<HcnBody> createdBodies = this.pips.values().stream()
                .filter(pip -> pip.getPower() >= nextActivePrimeIndex.getLastPip().getPower())
                .flatMap(pip -> pip.getActiveHcnBodies().stream())
                .map(parentBody -> new HcnBody(parentBody, nextActivePrimeIndex.getLastPip()))
                .collect(Collectors.toSet());
        List<HcnBody> successfullyAdded = nextActivePrimeIndex.hcnBodyList.addGroup(createdBodies);

        if (nextActivePrimeIndex.offspringFixedPowerGroup != null) {
            return nextActivePrimeIndex.offspringFixedPowerGroup.getOffspringPrimeIndex().generateHcnBodies(successfullyAdded);
        } else {
            return nextActivePrimeIndex.nextActivePrimeIndex.generateHcnBodies(successfullyAdded);
        }
    }

    private List<HcnBody> extendMatrixLocally() {
        addNextPrimeIndexPower();

        Set<HcnBody> createdBodies = generateLocalStarterBodies();
        List<HcnBody> successfullyAdded = hcnBodyList.addGroup(createdBodies);

        if (nextActivePrimeIndex != null) {
            return nextActivePrimeIndex.generateHcnBodies(successfullyAdded);
        }
        return successfullyAdded;
    }

    private List<HcnBody> extendWithNextActivePrimeIndex() {
        nextActivePrimeIndex = new ActivePrimeIndex(index + 1);
        nextActivePrimeIndex.parentActivePrimeIndex = this;

        PrimeIndexPower pip1 = new PrimeIndexPower(nextActivePrimeIndex, 1);
        nextActivePrimeIndex.pips.put(1, pip1);
        pip1.setProved(true);

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

        Set<HcnBody> pip2Bodies = pips.values().stream()
                .filter(pip -> pip.getPower() >= 2)
                .flatMap(pip -> pip.getActiveHcnBodies().stream())
                .map(parentBody -> new HcnBody(parentBody, pip2))
                .collect(Collectors.toSet());

        nextActivePrimeIndex.hcnBodyList.addGroup(pip2Bodies);

        // only pip2 bodies are truly new — pip1 bodies inherited generatedHcns from parent
        return nextActivePrimeIndex.hcnBodyList.stream()
                .filter(body -> body.getPip().getPower() >= 2)
                .collect(Collectors.toList());
    }

    public List<HcnBody> generateHcnBodies(Collection<HcnBody> previousBodies) {
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

        List<HcnBody> successfullyAdded = hcnBodyList.addGroup(createdBodies);

        if (nextActivePrimeIndex != null) {
            return nextActivePrimeIndex.generateHcnBodies(successfullyAdded);
        } else if (offspringFixedPowerGroup != null) {
            return offspringFixedPowerGroup.getOffspringPrimeIndex().generateHcnBodies(successfullyAdded);
        }
        return successfullyAdded;
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

    static List<HcnBody> mergeSortedBodies(List<HcnBody> existing, List<HcnBody> newBodies) {
        if (existing.isEmpty()) return newBodies;
        if (newBodies.isEmpty()) return existing;

        List<HcnBody> result = new ArrayList<>();
        int e = 0, n = 0;
        ScientificNumber maxFactor = null;

        while (e < existing.size() && n < newBodies.size()) {
            if (existing.get(e).getValue().isSmallerThan(newBodies.get(n).getValue())) {
                HcnBody body = existing.get(e++);
                if (maxFactor == null || body.getFactor().isBiggerThan(maxFactor)) {
                    result.add(body);
                    maxFactor = body.getFactor();
                }
            } else {
                HcnBody body = newBodies.get(n++);
                result.add(body);
                maxFactor = body.getFactor();
            }
        }

        while (e < existing.size()) {
            HcnBody body = existing.get(e++);
            if (maxFactor == null || body.getFactor().isBiggerThan(maxFactor)) {
                result.add(body);
                maxFactor = body.getFactor();
            }
        }

        while (n < newBodies.size()) {
            result.add(newBodies.get(n++));
        }

        return result;
    }

    public void deactivateRecursive(HcnBody defeated) {


        if (defeated.getPip().getActiveHcnBodies().isEmpty()) {
            // pip is deletable
            pips.remove(defeated.getPip().getPower());

            if (pips.size() == 1) {
                // activePrimeIndex is fixed
                fixPowerMaintain();
            }
        }

        if (defeated.getParent() != null) {
            // activePrimeIndex must be removed from parent's offsprings
            defeated.getParent().getOffsprings().remove(defeated);


            if (defeated.getParent().getOffsprings().isEmpty()) {
                // parent is also deletable
                defeated.getParent().deactivateFromLists();
                defeated.getParent().getPip().getActivePrimeIndex().deactivateRecursive(defeated.getParent());
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
