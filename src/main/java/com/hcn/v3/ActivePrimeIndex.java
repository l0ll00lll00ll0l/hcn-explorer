package com.hcn.v3;

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

    public void addNextPrimeIndexPower() {
        int nextPowerValue = pips.lastKey() + 1;
        PrimeIndexPower newPower = new PrimeIndexPower(this, nextPowerValue);
        pips.put(nextPowerValue, newPower);
    }
    
    public PrimeIndexPower getLastPip() {
        return pips.get(pips.lastKey());
    }

    public List<HcnBody> getBodiesForNextPipExtension(int power) {
        return hcnBodyList.stream().filter(body -> body.getPip().getPower() >= power).collect(Collectors.toList());
    }

    public boolean isLastActivePrimeIndex() {
        return nextActivePrimeIndex == null && offspringFixedPowerGroup == null;
    }
    
    @Override
    public String toString() {
        return "p" + index;
    }
    
    public boolean extendMatrix(HcnBody provedBody) {

        if (provedBody.isProved()) {
            return false;
        }

        provedBody.setProved(true);
        provedBody.getPip().setProved(true);
        boolean bodiesCreated = false;

        //check if extension is required
        if (provedBody.getPip().equals(getLastPip())) {

            boolean canWeExtendHere = parentActivePrimeIndex == null || parentActivePrimeIndex.pips.containsKey(getLastPip().getPower() + 1) && parentActivePrimeIndex.pips.get(getLastPip().getPower() + 1).isProved();

            if (canWeExtendHere) {
                addNextPrimeIndexPower();

                if (offspringFixedPowerGroup != null) {
                    ActivePrimeIndex toReactivate = offspringFixedPowerGroup.reactivatePrimeIned();
                    //System.out.println("toReactivate.parentActivePrimeIndex" + toReactivate.parentActivePrimeIndex);

                    if (offspringFixedPowerGroup.getFixedPowerGroup().isEmpty()) {
                        System.out.println("FixedGroup to delete");
                        offspringFixedPowerGroup.getOffspringPrimeIndex().parentFixedPowerGroup = null;
                        offspringFixedPowerGroup.getOffspringPrimeIndex().parentActivePrimeIndex = toReactivate;
                        toReactivate.nextActivePrimeIndex = offspringFixedPowerGroup.getOffspringPrimeIndex();
                        toReactivate.offspringFixedPowerGroup = null;
                        toReactivate.parentFixedPowerGroup = null;
                        toReactivate.parentActivePrimeIndex = this;
                        this.offspringFixedPowerGroup = null;
                        this.nextActivePrimeIndex = toReactivate;
                    } else {
                        System.out.println("FixedGroup remains");
                        toReactivate.offspringFixedPowerGroup = offspringFixedPowerGroup;
                        toReactivate.nextActivePrimeIndex = null;
                        this.offspringFixedPowerGroup = null;
                        this.nextActivePrimeIndex = toReactivate;
                    }


                    /*
                    offspringFixedPowerGroup.getOffspringPrimeIndex().getHcnBodyList().forEach(hcnBody -> {
                        System.out.println("offspring hcnbody: " + hcnBody);
                        hcnBody.g
                    });

                     */
                    this.getHcnBodyList().forEach(hcnBody -> System.out.println("this hcnbody: " + hcnBody));
                }

                Set<HcnBody> createdBodies;

                if (parentActivePrimeIndex != null) {
                    createdBodies = parentActivePrimeIndex.pips.values().stream()
                            .filter(pip -> pip.getPower() >= getLastPip().getPower())
                            .flatMap(pip -> pip.getActiveHcnBodies().stream())
                            .map(parentBody -> new HcnBody(parentBody, getLastPip()))
                            .collect(Collectors.toSet());
                } else if (parentFixedPowerGroup != null) {

                    createdBodies = parentFixedPowerGroup.getParentPrimeIndex().pips.values().stream()
                            .filter(pip -> pip.getPower() >= getLastPip().getPower())
                            .flatMap(pip -> pip.getActiveHcnBodies().stream())
                            .map(parentBody -> new HcnBody(parentBody, getLastPip()))
                            .collect(Collectors.toSet());
                } else {
                    createdBodies = Set.of(new HcnBody(null, getLastPip()));
                }

                Set<HcnBody> successfullyAdded = hcnBodyList.addGroup(createdBodies);
                bodiesCreated = !successfullyAdded.isEmpty();


                if (nextActivePrimeIndex != null) {
                    if (nextActivePrimeIndex.getLastPip().isProved()) {
                        if (nextActivePrimeIndex.pips.lastKey() < provedBody.getPip().getPower()) {
                            nextActivePrimeIndex.addNextPrimeIndexPower();
                        }
                    }

                    nextActivePrimeIndex.generateHcnBodies(successfullyAdded);

                } else if (provedBody.getPip().getPower() == 2) {
                    generateNextActivePrimeIndex();
                }

            } else {
                if (isLastActivePrimeIndex()) {
                    if (provedBody.getPip().getPower() == 2) {
                        generateNextActivePrimeIndex();
                        bodiesCreated = true;
                    }
                }
            }
        }

        if (parentActivePrimeIndex != null) {
            boolean parentBodiesCreated = parentActivePrimeIndex.extendMatrix(provedBody.getParent());
            bodiesCreated = bodiesCreated || parentBodiesCreated;
        } else if (parentFixedPowerGroup != null) {
            System.out.println("Parent fixed should pass on extendMatrix chain and return bodiesCreated");
            boolean parentBodiesCreated = parentFixedPowerGroup.getParentPrimeIndex().extendMatrix(provedBody.getParent());
            bodiesCreated = bodiesCreated || parentBodiesCreated;
        }
        
        return bodiesCreated;
    }

    public void generateHcnBodies(Set<HcnBody> previousBodies) {
        Set<HcnBody> createdBodies = previousBodies.stream()
                .flatMap(previousBody -> pips.values().stream()
                        .filter(pip -> pip.getPower() <= previousBody.getPip().getPower())
                        .map(pip -> new HcnBody(previousBody, pip)))
                .collect(Collectors.toSet());

        Set<HcnBody> successfullyAdded = hcnBodyList.addGroup(createdBodies);

        if (nextActivePrimeIndex != null) {
            nextActivePrimeIndex.generateHcnBodies(successfullyAdded);
        } else if (offspringFixedPowerGroup != null) {
            System.out.println("Bodies extended by fpg and propagated");
            successfullyAdded.forEach(hcnBody -> {
                hcnBody.setValue(hcnBody.getValue().multiply(offspringFixedPowerGroup.getValue()));
                hcnBody.setValue(hcnBody.getValue().multiply(offspringFixedPowerGroup.getFactor()));
            });
            offspringFixedPowerGroup.getOffspringPrimeIndex().generateHcnBodies(successfullyAdded);
        }
    }

    private void generateNextActivePrimeIndex() {
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

        // 2. Beállítja a superiorHcnBody-t
        defeated.setSuperiorBody(superiorBody);

        // 3. Lecsekkolja, hogy ez volt-e az utolsó aktív HcnBody a pip-ben
        if (defeated.getPip().getActiveHcnBodies().isEmpty()) {
            // Töröljük EZT a pip-et, nem a firstKey-t!
            deactivatedPips.add(defeated.getPip());
            pips.remove(defeated.getPip().getPower());

            if (pips.size() == 1) {

                fixPowerMaintain();
            }
        }

        // 4-5. Ha van parent, deaktiválja magát a parentben, és ha kell, rekurzívan a parentet is
        if (defeated.getParent() != null) {
            defeated.getParent().getOffspring().remove(defeated);

            if (defeated.isProved()) {
                defeated.getParent().getDeactivatedOffsprings().add(defeated);
            } else {
                defeated.getParent().getNeverActivatedOffsprings().add(defeated);
            }


            if (defeated.getParent().getOffspring().isEmpty()) {
                // Parent mindig benne van a listákban, töröljük
                defeated.getParent().deactivateFromLists();
                defeated.getParent().getPip().getActivePrimeIndex().deactivateRecursive(defeated.getParent(), superiorBody.getParent());
            }
        }
    }

    private void fixPowerMaintain() {
        if (parentActivePrimeIndex != null) {
            System.out.println("This Active Prime Index should initiate fix group " + this);
            FixedPowerGroup fixedPowerGroup = new FixedPowerGroup();
            fixedPowerGroup.receivePrimeIndex(this);
            fixedPowerGroup.setParentPrimeIndex(parentActivePrimeIndex);
            parentActivePrimeIndex.nextActivePrimeIndex = null;
            parentActivePrimeIndex.offspringFixedPowerGroup = fixedPowerGroup;
            nextActivePrimeIndex.parentFixedPowerGroup = fixedPowerGroup;
            nextActivePrimeIndex.parentActivePrimeIndex = null;
        } else {
            System.out.println("This Active Prime Index should join fix group " + this);
            parentFixedPowerGroup.receivePrimeIndex(this);
            nextActivePrimeIndex.parentFixedPowerGroup = parentFixedPowerGroup;
            nextActivePrimeIndex.parentActivePrimeIndex = null;
        }
    }
}
