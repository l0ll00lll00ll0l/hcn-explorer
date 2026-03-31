package com.hcn.v7;

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

    private long totalNanos = 0;
    private long extendMatrixNanos = 0;
    private long lapiGroupNanos = 0;
    private long cleanupJobs = 0;

    public ActivePrimeIndex getLastActivePrimeIndex() { return lastActivePrimeIndex; }
    public int getProvedCount() { return provedCount; }
    public LastActivePrimeIndexGroup getLowestLapiGroup() { return lowestLapiGroup; }
    public LastActivePrimeIndexGroup getHighestLapiGroup() { return highestLapiGroup; }
    public long getTotalMs() { return totalNanos / 1_000_000; }
    public long getExtendMatrixMs() { return extendMatrixNanos / 1_000_000; }
    public long getLapiGroupMs() { return lapiGroupNanos / 1_000_000; }
    public long getCleanupMs() { return cleanupJobs / 1_000_000; }

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

    public void proveUntilPrimeIndex(int step) {
        int target = ((lastProvedPrimeIndex / step) + 1) * step;
        while (lastProvedPrimeIndex < target) {
            proveNextSuperior();
        }
    }

    public Hcn proveNextSuperior() {
        long t0 = System.nanoTime();

        Hcn provedHcn = highestLapiGroup.getFirstHcn();

            provedCount++;

            if (provedHcn.getLastActivePrime() == highestLapiGroup.getLastActivePrimeIndex()) {
                addNewLapiGroup();
            }

            if (!provedHcn.getBody().isDeactivated()) {
                long tExt = System.nanoTime();
                List<HcnBody> newLeafBodies = lastActivePrimeIndex.extendMatrix(provedHcn.getBody());
                if (!lastActivePrimeIndex.isLastActivePrimeIndex()) {
                    lastActivePrimeIndex = lastActivePrimeIndex.getNextActivePrimeIndex();
                }
                extendMatrixNanos += System.nanoTime() - tExt;

                long tLapi = System.nanoTime();
                newLeafBodies.forEach(newBody -> insertNewBodyHcns(newBody));
                lapiGroupNanos += System.nanoTime() - tLapi;
            }

        long del = System.nanoTime();
            processDeactivationBin();

        if (provedHcn.getLastActivePrime() > lastProvedPrimeIndex) {
            lastProvedPrimeIndex = provedHcn.getLastActivePrime();
        }
        highestLapiGroup.removeFirstHcn(provedHcn);

        if (lowestLapiGroup.isReadyToDelete()) {
            lowestLapiGroup = lowestLapiGroup.getHigherLapiGroup();
            lowestLapiGroup.getLowerLapiGroup().remove();
        }
        cleanupJobs += System.nanoTime() - del;
        totalNanos += System.nanoTime() - t0;
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
