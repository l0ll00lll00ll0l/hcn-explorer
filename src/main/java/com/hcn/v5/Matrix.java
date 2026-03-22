package com.hcn.v5;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

public class Matrix {
    private ActivePrimeIndex lastActivePrimeIndex;
    private HcnFilter hcnFilter = new HcnFilter();
    private int provedCount = 0;
    private ScientificNumber lowLimit = new ScientificNumber(1.0, 0);
    private ScientificNumber upperLimit = new ScientificNumber(2, 0);
    private int lastProvedPrimeIndex = -1;

    private long totalNanos = 0;
    private long extendMatrixNanos = 0;
    private long generateHcnNanos = 0;
    private long filterNanos = 0;

    public long getTotalMs() { return totalNanos / 1_000_000; }
    public long getExtendMatrixMs() { return extendMatrixNanos / 1_000_000; }
    public long getGenerateHcnMs() { return generateHcnNanos / 1_000_000; }
    public long getFilterMs() { return filterNanos / 1_000_000; }

    public ActivePrimeIndex getLastActivePrimeIndex() {
        return lastActivePrimeIndex;
    }

    public List<Hcn> getHcnList() {
        return hcnFilter.getMaxLevelCandidates();
    }

    public HcnFilter getHcnFilter() {return hcnFilter;}

    public int getProvedCount() {
        return provedCount;
    }

    public ScientificNumber getLowLimit() {
        return lowLimit;
    }

    public ScientificNumber getUpperLimit() {
        return upperLimit;
    }

    public int getLastProvedPrimeIndex() {return lastProvedPrimeIndex;}
    
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
        while (provedCount < count) {
            proveNextHcn();
        }
    }

    public Hcn proveNextHcn() {
        long t0 = System.nanoTime();

        Hcn provedHcn = hcnFilter.getMaxLevelCandidates().get(0);
        provedCount++;

        if (!provedHcn.getBody().isDeactivated()) {
            long tExt = System.nanoTime();
            boolean extended = lastActivePrimeIndex.extendMatrix(provedHcn.getBody());
            extendMatrixNanos += System.nanoTime() - tExt;

            if (extended) {
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

        totalNanos += System.nanoTime() - t0;
        return provedHcn;
    }

    private void newLastActivePrimeIndexFound(Hcn provedHcn) {

        lastProvedPrimeIndex = provedHcn.getLastActivePrime();
        hcnFilter.removeLevelBelow();
    }

    private void generateHcnNextRange() {

        // set new range until potential new lastActivePrimeIndex
        lowLimit = upperLimit;
        upperLimit = lastActivePrimeIndex.getHcnBodyList().getSmallestBody().getHcnFactory().getLimitHcn().getValue();

        long tGen = System.nanoTime();
        TreeSet<Hcn> sortedRawHcn = generateHcnsBetweenLimits();
        generateHcnNanos += System.nanoTime() - tGen;

        long tFilt = System.nanoTime();
        hcnFilter.addFromExpandedRange(sortedRawHcn);
        filterNanos += System.nanoTime() - tFilt;
    }

    private void generateHcnsAfterExtension(Hcn provedHcn) {
        
        if (!lastActivePrimeIndex.isLastActivePrimeIndex()) {
            // entrypoint update after nextActiveIndexPrime
            lastActivePrimeIndex = lastActivePrimeIndex.getNextActivePrimeIndex();
        }
        // set remaining range with same upperLimit
        lowLimit = provedHcn.getValue();

        long tGen = System.nanoTime();
        TreeSet<Hcn> sortedRawHcn = generateHcnsBetweenLimits();
        generateHcnNanos += System.nanoTime() - tGen;

        long tFilt = System.nanoTime();
        hcnFilter.addAfterExtension(sortedRawHcn);
        filterNanos += System.nanoTime() - tFilt;
    }

    private TreeSet<Hcn> generateHcnsBetweenLimits() {
        
        // new extension won't produce Hcns below lowLimit, hcnFilter can be maintained
        hcnFilter.discardBelowLimit(lowLimit);
        
        return lastActivePrimeIndex.getHcnBodyList().stream()
                .flatMap(body -> body.getHcnsBetween(lowLimit, upperLimit).stream())
                .collect(java.util.stream.Collectors.toCollection(TreeSet::new));
    }
}
