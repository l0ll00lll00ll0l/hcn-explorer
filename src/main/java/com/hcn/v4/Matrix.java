package com.hcn.v4;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

public class Matrix {
    private ActivePrimeIndex lastActivePrimeIndex;
    private List<Hcn> provedHcnList = new ArrayList<>();
    private HcnFilter hcnFilter = new HcnFilter();
    private ScientificNumber lowLimit = new ScientificNumber(1.0, 0);
    private ScientificNumber upperLimit = new ScientificNumber(2, 0);
    private int lastProvedPrimeIndex = -1;
    private TreeSet<Integer> lastActivePrimeIndexesWithinIntervals = new TreeSet<>();

    public ActivePrimeIndex getLastActivePrimeIndex() {
        return lastActivePrimeIndex;
    }

    public List<Hcn> getHcnList() {
        return hcnFilter.getMaxLevelCandidates();
    }

    public List<Hcn> getProvedHcnList() {
        return provedHcnList;
    }

    public ScientificNumber getLowLimit() {
        return lowLimit;
    }

    public ScientificNumber getUpperLimit() {
        return upperLimit;
    }
    
    public Matrix() {initializeMatrix();}

    private void initializeMatrix() {
        // Create p0 with pip1
        ActivePrimeIndex p0 = new ActivePrimeIndex(0);
        PrimeIndexPower pip1 = new PrimeIndexPower(p0, 1);
        p0.getPips().put(1, pip1);
        lastActivePrimeIndex = p0;

        // Create initial HcnBody with null parent and pip1
        HcnBody firstBody = new HcnBody(null, pip1);
        p0.getHcnBodyList().addGroup(List.of(firstBody));

        // Create initial Hcn
        Hcn firstHcn = firstBody.getHcnsBetween(lowLimit, upperLimit).get(0);
        //hcnList.add(firstHcn);
        TreeSet<Hcn> tree = new TreeSet<>();
        tree.add(firstHcn);
        hcnFilter.addFromExpandedRange(tree);
    }

    public void proveUntilPrimeIndex(int provedIndex) {
        while (lastProvedPrimeIndex < provedIndex) {
            proveNextHcn();
        }
    }
    
    public void proveUntilCount(int count) {
        while (provedHcnList.size() < count) {
            proveNextHcn();
        }
    }

    public void proveNextHcn() {

        Hcn provedHcn = hcnFilter.getMaxLevelCandidates().get(0);
        provedHcnList.add(provedHcn);
        this.lastActivePrimeIndexesWithinIntervals.add(provedHcn.getLastActivePrime());

        if (!provedHcn.getBody().isDeactivated()) {
            if (lastActivePrimeIndex.extendMatrix(provedHcn.getBody())) {
                generateHcnsAfterExtension(provedHcn);
            }
        }

        while (hcnFilter.getMaxLevelCandidates().size() == 1) {
            generateHcnNextRange();
        }

        if (provedHcn.getLastActivePrime() > lastProvedPrimeIndex) {
            newLastActivePrimeIndexFound(provedHcn);
        }
        hcnFilter.removeProvedFirst();
    }

    private void newLastActivePrimeIndexFound(Hcn provedHcn) {

        lastProvedPrimeIndex = provedHcn.getLastActivePrime();
        int lowestRecordedLastActivePrimeIndex = lastActivePrimeIndexesWithinIntervals.first();
        lastActivePrimeIndexesWithinIntervals.clear();
        hcnFilter.removeLevelBelow(lowestRecordedLastActivePrimeIndex);
    }

    private void generateHcnNextRange() {

        // set new range until potential new lastActivePrimeIndex
        lowLimit = upperLimit;
        upperLimit = lastActivePrimeIndex.getHcnBodyList().first().getHcnFactory().getLimitHcn().getValue();

        TreeSet<Hcn> sortedRawHcn = generateHcnsBetweenLimits();
        hcnFilter.addFromExpandedRange(sortedRawHcn);
    }

    private void generateHcnsAfterExtension(Hcn provedHcn) {
        
        if (!lastActivePrimeIndex.isLastActivePrimeIndex()) {
            // entrypoint update after nextActiveIndexPrime
            lastActivePrimeIndex = lastActivePrimeIndex.getNextActivePrimeIndex();
        }
        // set remaining range with same upperLimit
        lowLimit = provedHcn.getValue();

        TreeSet<Hcn> sortedRawHcn = generateHcnsBetweenLimits();
        hcnFilter.addAfterExtension(sortedRawHcn);
    }

    private TreeSet<Hcn> generateHcnsBetweenLimits() {
        
        // new extension won't produce Hcns below lowLimit, hcnFilter can be maintained
        hcnFilter.discardBelowLimit(lowLimit);
        
        // snapshot required to avoid concurrentModificationException
        List<HcnBody> bodySnapshot = new ArrayList<>(lastActivePrimeIndex.getHcnBodyList());
        
        return bodySnapshot.stream()
                .flatMap(body -> body.getHcnsBetween(lowLimit, upperLimit).stream())
                .collect(java.util.stream.Collectors.toCollection(TreeSet::new));
    }
}
