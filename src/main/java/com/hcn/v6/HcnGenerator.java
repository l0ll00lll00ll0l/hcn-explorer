package com.hcn.v6;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class HcnGenerator {
    private ActivePrimeIndex lastActivePrimeIndex;
    private LastActivePrimeIndexGroup lowestLapiGroup;
    private LastActivePrimeIndexGroup highestLapiGroup;
    private HcnBody largestActiveBody;
    private Set<HcnBody> deactivationBin = new HashSet<>();

    private int provedCount = 0;
    private int lastProvedPrimeIndex = -1;

    public ActivePrimeIndex getLastActivePrimeIndex() { return lastActivePrimeIndex; }
    public int getProvedCount() { return provedCount; }
    public LastActivePrimeIndexGroup getLowestLapiGroup() { return lowestLapiGroup; }
    public LastActivePrimeIndexGroup getHighestLapiGroup() { return highestLapiGroup; }

    public void initialize() {

        // Create p0 with pip1
        ActivePrimeIndex p0 = new ActivePrimeIndex(0);
        PrimeIndexPower pip1 = new PrimeIndexPower(p0, 1);
        p0.getPips().put(1, pip1);
        lastActivePrimeIndex = p0;

        // Create initial HcnBody with null parent and pip1
        largestActiveBody = new HcnBody(null, pip1);
        p0.getHcnBodyList().addGroup(List.of(largestActiveBody));

        // create lapiGroups
        highestLapiGroup = new LastActivePrimeIndexGroup(0);
        lowestLapiGroup = highestLapiGroup;
        Hcn firstHcn = new Hcn(largestActiveBody, 0);
        largestActiveBody.getGeneratedHcns().put(highestLapiGroup, firstHcn);
        firstHcn.setValue(largestActiveBody.getValue());
        firstHcn.setFactor(largestActiveBody.getFactor());
        firstHcn.getSmallerHcns().put(highestLapiGroup, null);
        firstHcn.getLargerHcns().put(highestLapiGroup, null);
        highestLapiGroup.setFirstHcn(firstHcn);
    }

    public void proveUntilPrimeIndex(int provedIndex) {
        while (lastProvedPrimeIndex < provedIndex) {
            proveNextSuperior();
        }
    }

    public Hcn proveNextSuperior() {

        Hcn provedHcn = highestLapiGroup.getFirstHcn();
        while (provedHcn.getSuperiorHcn() != null) {
            highestLapiGroup.removeFirstHcn(provedHcn);
            provedHcn = highestLapiGroup.getFirstHcn();
        }

            provedCount++;

            if (provedHcn.getLastActivePrime() == highestLapiGroup.getLastActivePrimeIndex()) {
                addNewLapiGroup();
            }

            if (!provedHcn.getBody().isDeactivated()) {
                List<HcnBody> newLeafBodies = lastActivePrimeIndex.extendMatrix(provedHcn.getBody());
                if (!lastActivePrimeIndex.isLastActivePrimeIndex()) {
                    lastActivePrimeIndex = lastActivePrimeIndex.getNextActivePrimeIndex();
                }
                newLeafBodies.forEach(newBody -> insertNewBodyHcns(newBody));
            }

            processDeactivationBin();

        if (provedHcn.getLastActivePrime() > lastProvedPrimeIndex) {
            lastProvedPrimeIndex = provedHcn.getLastActivePrime();
        }
        highestLapiGroup.removeFirstHcn(provedHcn);

        if (lowestLapiGroup.isReadyToDelete()) {
            lowestLapiGroup = lowestLapiGroup.getHigherLapiGroup();
            lowestLapiGroup.getLowerLapiGroup().remove();
        }

        return provedHcn;
    }

    private void processDeactivationBin() {
        deactivationBin.forEach(body -> {
            body.deactivateFromLists();
            body.getPip().getActivePrimeIndex().deactivateRecursive(body);
        });
        deactivationBin.clear();
    }


    private void insertNewBodyHcns(HcnBody newBody) {
        LastActivePrimeIndexGroup group = lowestLapiGroup;
        while (group != null) {
            if (group.isBodyAllowedToGenerate(newBody)) {
                Hcn newHcn = newBody.generateNextHcn(group);
                group.insertHcnIntoAllLevels(newHcn, deactivationBin);
            }
            group = group.getHigherLapiGroup();
        }
    }

    private void addNewLapiGroup() {
        LastActivePrimeIndexGroup newGroup = new LastActivePrimeIndexGroup(highestLapiGroup.getLastActivePrimeIndex() + 1);
        newGroup.setLowerLapiGroup(highestLapiGroup);
        highestLapiGroup.setHigherLapiGroup(newGroup);
        highestLapiGroup = newGroup;
        highestLapiGroup.generateHcnsForNewLapiGroup(lastActivePrimeIndex.getHcnBodyList(), deactivationBin);
    }

}
