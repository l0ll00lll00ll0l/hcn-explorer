package com.hcn.detailed;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Matrix {
    private ActivePrimeIndex lastActivePrimeIndex;
    private LastActivePrimeIndexGroup lowestLapiGroup;
    private LastActivePrimeIndexGroup highestLapiGroup;
    private LastActivePrimeIndexGroup nextLapiGroup;
    private Set<HcnBody> deactivationBin = new HashSet<>();
    private List<Hcn> provedHcns = new ArrayList<>();
    private ScientificNumber provedLimit;

    private int provedCount = 0;
    private int lastProvedPrimeIndex = -1;
    private int lowestProvedLapiWithinInterval = 1;

    private long totalNanos = 0;
    private long extendMatrixNanos = 0;
    private long generateHcnListNanos = 0;


    public ActivePrimeIndex getLastActivePrimeIndex() { return lastActivePrimeIndex; }
    public int getProvedCount() { return provedCount; }
    public LastActivePrimeIndexGroup getLowestLapiGroup() { return lowestLapiGroup; }
    public LastActivePrimeIndexGroup getHighestLapiGroup() { return highestLapiGroup; }
    public long getTotalMs() { return totalNanos / 1_000_000; }
    public long getExtendMatrixMs() { return extendMatrixNanos / 1_000_000; }
    public long getGenerateHcnListMs() { return generateHcnListNanos / 1_000_000; }

    public void initialize() {

        // === ActivePrimeIndexes ===
        ActivePrimeIndex p0 = new ActivePrimeIndex(0);
        PrimeIndexPower p0pip1 = new PrimeIndexPower(p0, 1); p0pip1.setProved(true);
        PrimeIndexPower p0pip2 = new PrimeIndexPower(p0, 2); p0pip2.setProved(true);
        PrimeIndexPower p0pip3 = new PrimeIndexPower(p0, 3);
        p0.getPips().put(1, p0pip1);
        p0.getPips().put(2, p0pip2);
        p0.getPips().put(3, p0pip3);

        ActivePrimeIndex p1 = new ActivePrimeIndex(1);
        PrimeIndexPower p1pip1 = new PrimeIndexPower(p1, 1); p1pip1.setProved(true);
        PrimeIndexPower p1pip2 = new PrimeIndexPower(p1, 2);
        p1.getPips().put(1, p1pip1);
        p1.getPips().put(2, p1pip2);

        p0.setNextActivePrimeIndex(p1);
        p1.setParentActivePrimeIndex(p0);
        lastActivePrimeIndex = p1;

        // === p0 Bodies ===
        // b_p0_1: [p0^1], v=2, f=2, proved
        HcnBody b_p0_1 = new HcnBody();
        b_p0_1.setPip(p0pip1); b_p0_1.setValue(new ScientificNumber(2, 0)); b_p0_1.setFactor(new ScientificNumber(2, 0)); b_p0_1.setProved(true);
        p0pip1.addActiveHcnBody(b_p0_1);

        // b_p0_2: [p0^2], v=4, f=3, proved
        HcnBody b_p0_2 = new HcnBody();
        b_p0_2.setPip(p0pip2); b_p0_2.setValue(new ScientificNumber(4, 0)); b_p0_2.setFactor(new ScientificNumber(3, 0)); b_p0_2.setProved(true);
        p0pip2.addActiveHcnBody(b_p0_2);

        // b_p0_3: [p0^3], v=8, f=4, not proved
        HcnBody b_p0_3 = new HcnBody();
        b_p0_3.setPip(p0pip3); b_p0_3.setValue(new ScientificNumber(8, 0)); b_p0_3.setFactor(new ScientificNumber(4, 0));
        p0pip3.addActiveHcnBody(b_p0_3);

        // p0 BodyList: b_p0_1 → b_p0_2 → b_p0_3
        b_p0_1.setLargerBody(b_p0_2); b_p0_2.setSmallerBody(b_p0_1);
        b_p0_2.setLargerBody(b_p0_3); b_p0_3.setSmallerBody(b_p0_2);
        p0.getHcnBodyList().addGroup(List.of(b_p0_1, b_p0_2, b_p0_3));

        // parent/offspring for p0 bodies
        b_p0_1.getOffsprings().add(b_p0_2); // not real parent, but offspring tracking
        // Actually from dump: b_p0_1 has offspring [b_p1_11], b_p0_2 has offspring [b_p1_21, b_p1_22], b_p0_3 has offspring [b_p1_31, b_p1_32]
        // Let me set parents correctly below

        // === p1 Bodies ===
        // b_p1_11: [p0^1, p1^1], v=6, f=4, proved, parent=b_p0_1
        HcnBody b_p1_11 = new HcnBody();
        b_p1_11.setPip(p1pip1); b_p1_11.setValue(new ScientificNumber(6, 0)); b_p1_11.setFactor(new ScientificNumber(4, 0)); b_p1_11.setProved(true);
        b_p1_11.setParent(b_p0_1);
        p1pip1.addActiveHcnBody(b_p1_11);

        // b_p1_21: [p0^2, p1^1], v=12, f=6, proved, parent=b_p0_2
        HcnBody b_p1_21 = new HcnBody();
        b_p1_21.setPip(p1pip1); b_p1_21.setValue(new ScientificNumber(12, 0)); b_p1_21.setFactor(new ScientificNumber(6, 0)); b_p1_21.setProved(true);
        b_p1_21.setParent(b_p0_2);
        p1pip1.addActiveHcnBody(b_p1_21);

        // b_p1_31: [p0^3, p1^1], v=24, f=8, not proved, parent=b_p0_3
        HcnBody b_p1_31 = new HcnBody();
        b_p1_31.setPip(p1pip1); b_p1_31.setValue(new ScientificNumber(24, 0)); b_p1_31.setFactor(new ScientificNumber(8, 0));
        b_p1_31.setParent(b_p0_3);
        p1pip1.addActiveHcnBody(b_p1_31);

        // b_p1_22: [p0^2, p1^2], v=36, f=9, not proved, parent=b_p0_2
        HcnBody b_p1_22 = new HcnBody();
        b_p1_22.setPip(p1pip2); b_p1_22.setValue(new ScientificNumber(36, 0)); b_p1_22.setFactor(new ScientificNumber(9, 0));
        b_p1_22.setParent(b_p0_2);
        p1pip2.addActiveHcnBody(b_p1_22);

        // b_p1_32: [p0^3, p1^2], v=72, f=12, not proved, parent=b_p0_3
        HcnBody b_p1_32 = new HcnBody();
        b_p1_32.setPip(p1pip2); b_p1_32.setValue(new ScientificNumber(72, 0)); b_p1_32.setFactor(new ScientificNumber(12, 0));
        b_p1_32.setParent(b_p0_3);
        p1pip2.addActiveHcnBody(b_p1_32);

        // p1 BodyList: b_p1_11 → b_p1_21 → b_p1_31 → b_p1_22 → b_p1_32
        b_p1_11.setLargerBody(b_p1_21); b_p1_21.setSmallerBody(b_p1_11);
        b_p1_21.setLargerBody(b_p1_31); b_p1_31.setSmallerBody(b_p1_21);
        b_p1_31.setLargerBody(b_p1_22); b_p1_22.setSmallerBody(b_p1_31);
        b_p1_22.setLargerBody(b_p1_32); b_p1_32.setSmallerBody(b_p1_22);
        p1.getHcnBodyList().addGroup(List.of(b_p1_11, b_p1_21, b_p1_31, b_p1_22, b_p1_32));

        // offspring links
        b_p0_1.getOffsprings().clear();
        b_p0_1.getOffsprings().add(b_p1_11);
        b_p0_2.getOffsprings().clear();
        b_p0_2.getOffsprings().add(b_p1_21);
        b_p0_2.getOffsprings().add(b_p1_22);
        b_p0_3.getOffsprings().clear();
        b_p0_3.getOffsprings().add(b_p1_31);
        b_p0_3.getOffsprings().add(b_p1_32);

        PrimeIndexPower p0pip0 = new PrimeIndexPower(p0, 0);
        HcnBody b_p0_0 = new HcnBody();
        b_p0_0.setPip(p0pip0);
        b_p0_0.setValue(new ScientificNumber(1, 0));
        b_p0_0.setFactor(new ScientificNumber(1, 0));

        HcnGenerator gen0 = new HcnGenerator(b_p0_0);
        Hcn hcn0 = new Hcn(gen0, 0);
        hcn0.setValue(new ScientificNumber(1, 0));
        hcn0.setFactor(new ScientificNumber(1, 0));

        HcnGenerator gen1 = new HcnGenerator(b_p0_1);
        Hcn hcn1 = new Hcn(gen1, 0);
        hcn1.setValue(new ScientificNumber(2, 0));
        hcn1.setFactor(new ScientificNumber(2, 0));

        HcnGenerator gen2 = new HcnGenerator(b_p0_2);
        Hcn hcn2 = new Hcn(gen2, 0);
        hcn2.setValue(new ScientificNumber(4, 0));
        hcn2.setFactor(new ScientificNumber(3, 0));

        provedHcns.add(hcn0);
        provedHcns.add(hcn1);
        provedHcns.add(hcn2);

        b_p1_11.setHcnGenerator(new HcnGenerator(b_p1_11));
        Hcn hcn3 = new Hcn(b_p1_11.getHcnGenerator(), 1);
        hcn3.setValue(new ScientificNumber(6, 0));
        hcn3.setFactor(new ScientificNumber(4, 0));
        b_p1_11.getHcnGenerator().setLastGeneratedHcn(hcn3);

        b_p1_21.setHcnGenerator(new HcnGenerator(b_p1_21));
        Hcn hcn4 = new Hcn(b_p1_21.getHcnGenerator(), 1);
        hcn4.setValue(new ScientificNumber(12, 0));
        hcn4.setFactor(new ScientificNumber(6, 0));
        b_p1_21.getHcnGenerator().setLastGeneratedHcn(hcn4);

        // === LapiGroup ===
        nextLapiGroup = new LastActivePrimeIndexGroup(1, b_p1_21);
        nextLapiGroup.getHcnList().add(hcn2);
        nextLapiGroup.getHcnList().add(hcn3);
        highestLapiGroup = new LastActivePrimeIndexGroup(0, b_p1_11);
        highestLapiGroup.getHcnList().add(hcn2);
        lowestLapiGroup = highestLapiGroup;

        // === Generator state ===
        provedCount = 3;
        lastProvedPrimeIndex = 0;
        provedLimit = new ScientificNumber(6, 0);

    }

    public void proveLapi(int count) {
        if (!GeneratorConfig.isLocked()) {
            GeneratorConfig.lock();
            initialize();
        }
        for (int i = 0; i < count; i++) {
            proveNextLapi();
        }
    }

    public void proveNextLapi() {
        long t0 = System.nanoTime();
        maintainLapiGroups();

        //create next lapi group
        nextLapiGroup = new LastActivePrimeIndexGroup(highestLapiGroup.getLastActivePrimeIndex() + 1, lastActivePrimeIndex.getHcnBodyList().getSmallestBody());

        nextLapiGroup.getHcnList().add(findLastSuperiorHcn(determineTargetValue()));
        maintainProvedHcns();
        lastProvedPrimeIndex++;
        totalNanos += System.nanoTime() - t0;
    }

    private void maintainProvedHcns() {
        //remove as first member left there intentionally from previous interval to keep superior factor value to compare
        highestLapiGroup.getHcnList().remove(0);
        lowestProvedLapiWithinInterval = Integer.MAX_VALUE;
        highestLapiGroup.getHcnList().forEach(hcn -> {
            if (hcn.getLastActivePrime() < lowestProvedLapiWithinInterval) {
                lowestProvedLapiWithinInterval = hcn.getLastActivePrime();
            }
            provedCount++;
        });
    }

    private Hcn findLastSuperiorHcn(ScientificNumber targetValue) {
        Hcn largestGeneratedSuperiorHcn;
        boolean candidateIsSuperior;
        do {
            largestGeneratedSuperiorHcn = extendLapiHcnListsUntilTarget(targetValue);
            candidateIsSuperior = true;
            Hcn targetHcn = nextLapiGroup.getWalkerBody().generateNextHcn(nextLapiGroup);

            if (largestGeneratedSuperiorHcn.getFactor().isNotSmallerThan(targetHcn.getFactor())) {
                candidateIsSuperior = false;
                nextLapiGroup.shiftWalkerBody();
                targetHcn.getBody().gotDominated();
                targetValue = determineTargetValue();
            }

        } while (!candidateIsSuperior);
        return largestGeneratedSuperiorHcn;
    }

    private Hcn extendLapiHcnListsUntilTarget(ScientificNumber targetValue) {
        long tGen = System.nanoTime();
        lowestLapiGroup.generateHcnList(provedLimit, targetValue);
        generateHcnListNanos += System.nanoTime() - tGen;

        long tExt = System.nanoTime();
        matrixMaintainCheck();
        extendMatrixNanos += System.nanoTime() - tExt;

        Hcn largestGeneratedSuperiorHcn = highestLapiGroup.getHcnList().get(highestLapiGroup.getHcnList().size() - 1);
        provedLimit = targetValue;
        return largestGeneratedSuperiorHcn;
    }

    private void matrixMaintainCheck() {
        highestLapiGroup.getHcnList().forEach(hcn -> {
            if (!hcn.getBody().isDeactivated()) {
                lastActivePrimeIndex.extendMatrix(hcn.getBody());
                if (!lastActivePrimeIndex.isLastActivePrimeIndex()) {
                    lastActivePrimeIndex = lastActivePrimeIndex.getNextActivePrimeIndex();
                }
            }
        });
    }

    private ScientificNumber determineTargetValue() {
        return nextLapiGroup.getPrimeValue().multiply(nextLapiGroup.getWalkerBody().getLastGeneratedHcn().getValue());
    }

    public Hcn proveNextSuperior() {
        if (provedHcns.isEmpty()) {
            proveNextLapi();
            provedHcns.addAll(highestLapiGroup.getHcnList());
        }
        return provedHcns.remove(0);
    }

    private void maintainLapiGroups() {

        LastActivePrimeIndexGroup lapi = lowestLapiGroup;
        lapi.maintainAfterInterval();
        while (lapi.getHigherLapiGroup() != null) {
            lapi = lapi.getHigherLapiGroup();
            lapi.maintainAfterInterval();
        }

        nextLapiGroup.setLowerLapiGroup(highestLapiGroup);
        highestLapiGroup.setHigherLapiGroup(nextLapiGroup);
        highestLapiGroup = nextLapiGroup;
        nextLapiGroup = null;

        while (lowestProvedLapiWithinInterval > lowestLapiGroup.getLastActivePrimeIndex()) {
            LastActivePrimeIndexGroup lapiToRemove = lowestLapiGroup;
            lapiToRemove.getWalkerBody().removeWalkerBodyForLapi(lapiToRemove);
            lapiToRemove.setWalkerBody(null);
            lowestLapiGroup = lowestLapiGroup.getHigherLapiGroup();
            lapiToRemove.setHigherLapiGroup(null);
            lowestLapiGroup.setLowerLapiGroup(null);
        }
    }

}
