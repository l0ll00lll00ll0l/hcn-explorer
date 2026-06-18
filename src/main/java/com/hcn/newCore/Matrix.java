package com.hcn.newCore;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Getter
@Setter
@Builder
public class Matrix {

    private TransitionNode lastTransition;
    private final List<Hcn> provedHcns = new ArrayList<>();
    private Lapi nextLapi;
    private Lapi lowestLapi;
    private Lapi highestLapi;
    private int lowestProvedLapiWithinInterval;
    private int provedCount;
    private final PrimeCenter lapiPrimeCenter = new PrimeCenter();
    private ScientificNumber provedLimit;

    // Timing
    @Builder.Default
    private long totalTimeMs = 0;
    @Builder.Default
    private long matrixMaintainTimeMs = 0;
    @Builder.Default
    private long generateHcnListTimeMs = 0;

    // Progress tracking
    @Builder.Default
    private volatile boolean proving = false;
    @Builder.Default
    private volatile int proveTarget = 0;
    @Builder.Default
    private volatile int proveProgress = 0;

    public void initialize() {

        ApiNode p0 = ApiNode.builder().prevMatrixNode(null).build();
        p0.getIndexes().add(lapiPrimeCenter.getPrime(0));
        BodyNode pip01 = BodyNode.builder().parentNode(p0).bodyNodeId(1).proved(true)
                .value(new ScientificNumber(2,0))
                .factor(new ScientificNumber(2, 0)).build();
        BodyNode pip02 = BodyNode.builder().parentNode(p0).bodyNodeId(2).proved(true)
                .value(new ScientificNumber(4,0))
                .factor(new ScientificNumber(3, 0)).build();
        BodyNode pip03 = BodyNode.builder().parentNode(p0).bodyNodeId(3)
                .value(new ScientificNumber(8,0))
                .factor(new ScientificNumber(4, 0)).build();
        p0.getBodyNodes().put(1, pip01);
        p0.getBodyNodes().put(2, pip02);
        p0.getBodyNodes().put(3, pip03);

        PrimeCenter primeCenter = new PrimeCenter();
        lastTransition = TransitionNode.builder()
                .transitionFrom(2).transitionTo(1).primeCenter(primeCenter).build();
        lastTransition.indexes.add(primeCenter.getPrime(1));
        lastTransition.indexes.add(primeCenter.getPrime(2));
        BodyNode t1 = BodyNode.builder().parentNode(lastTransition).bodyNodeId(1)
                .value(new ScientificNumber(1, 0))
                .factor(new ScientificNumber(1, 0)).proved(true).build();
        lastTransition.getBodyNodes().put(1, t1);
        BodyNode t2 = BodyNode.builder()
                .parentNode(lastTransition).bodyNodeId(2).value(new ScientificNumber(9, 0))
                .factor(new ScientificNumber(3, 0)).build();
        lastTransition.getBodyNodes().put(2, t2);

        p0.setNextMatrixNode(lastTransition);
        lastTransition.setPrevMatrixNode(p0);

        Body b01 = Body.builder().bodyNode(pip01).parent(null).value(new ScientificNumber(2, 0))
                .factor(new ScientificNumber(2, 0)).proved(true).build();
        Body b02 = Body.builder().bodyNode(pip02).parent(null).value(new ScientificNumber(4, 0))
                .factor(new ScientificNumber(3, 0)).proved(true).build();
        Body b03 = Body.builder().bodyNode(pip03).parent(null).value(new ScientificNumber(8, 0))
                .factor(new ScientificNumber(4, 0)).proved(false).build();

        pip01.getActiveBodies().add(b01);
        pip02.getActiveBodies().add(b02);
        pip03.getActiveBodies().add(b03);

        b01.setSmallerBody(null);
        b01.setLargerBody(b02);
        b02.setSmallerBody(b01);
        b02.setLargerBody(b03);
        b03.setSmallerBody(b02);
        b03.setLargerBody(null);

        p0.setBodyList(BodyList.builder().smallestBody(b01).build());

        Body b11 = Body.builder().bodyNode(t1).parent(b01).value(new ScientificNumber(6, 0))
                .factor(new ScientificNumber(4, 0)).proved(true).build();
        Body b21 = Body.builder().bodyNode(t1).parent(b02).value(new ScientificNumber(12, 0))
                .factor(new ScientificNumber(6, 0)).proved(true).build();
        Body b31 = Body.builder().bodyNode(t1).parent(b03).value(new ScientificNumber(24, 0))
                .factor(new ScientificNumber(8, 0)).build();
        Body b22 = Body.builder().bodyNode(t2).parent(b02).value(new ScientificNumber(36, 0))
                .factor(new ScientificNumber(9, 0)).build();
        Body b32 = Body.builder().bodyNode(t2).parent(b03).value(new ScientificNumber(72, 0))
                .factor(new ScientificNumber(12, 0)).build();

        t1.getActiveBodies().add(b11);
        t1.getActiveBodies().add(b21);
        t1.getActiveBodies().add(b31);
        t2.getActiveBodies().add(b22);
        t2.getActiveBodies().add(b32);

        b11.setSmallerBody(null);
        b11.setLargerBody(b21);
        b21.setSmallerBody(b11);
        b21.setLargerBody(b31);
        b31.setSmallerBody(b21);
        b31.setLargerBody(b22);
        b22.setSmallerBody(b31);
        b22.setLargerBody(b32);
        b32.setSmallerBody(b22);
        b32.setLargerBody(null);

        lastTransition.setBodyList(BodyList.builder().smallestBody(b11).build());

        b01.getOffsprings().add(b11);
        b02.getOffsprings().add(b21);
        b03.getOffsprings().add(b31);
        b02.getOffsprings().add(b22);
        b03.getOffsprings().add(b32);

        Hcn hcn1 = Hcn.builder().body(b11).lapi(0).value(new ScientificNumber(2, 0))
                .factor(new ScientificNumber(2, 0)).build();
        Hcn hcn2 = Hcn.builder().body(b21).lapi(0).value(new ScientificNumber(4, 0))
                .factor(new ScientificNumber(3, 0)).build();
        Hcn hcn11 = Hcn.builder().body(b11).lapi(1).value(new ScientificNumber(6, 0))
                .factor(new ScientificNumber(4, 0)).build();
        Hcn hcn3 = Hcn.builder().body(b11).lapi(0).value(new ScientificNumber(8, 0))
                .factor(new ScientificNumber(4, 0)).build();

        b11.setFirstHcn(hcn1);
        b11.setFirstSuperiorHcn(hcn1);
        b11.setLastGeneratedHcn(hcn11);

        b21.setFirstHcn(hcn2);
        b21.setFirstSuperiorHcn(hcn2);
        b21.setLastGeneratedHcn(hcn2);

        b31.setFirstHcn(hcn3);
        b31.setLastGeneratedHcn(hcn3);

        provedHcns.add(hcn1);
        provedHcns.add(hcn2);

        nextLapi = Lapi.builder().prime(lapiPrimeCenter.getPrime(1)).walker(b11)
                .valueMultiplier(new ScientificNumber(1, 0))
                .factorMultiplier(new ScientificNumber(1, 0)).build();
        nextLapi.getHcnList().add(hcn2);
        lowestLapi = Lapi.builder().prime(lapiPrimeCenter.getPrime(0)).walker(b31).build();
        lowestLapi.getHcnList().add(hcn3);
        highestLapi = lowestLapi;

        //force lapi0 deletion
        lowestProvedLapiWithinInterval = 1;
        provedCount = 2;
        provedLimit = new ScientificNumber(6, 0);
    }

    public void proveLapi(int count) {
        long start = System.currentTimeMillis();
        for (int i = 0; i < count; i++) {
            proveNextLapi();
            proveProgress = i + 1;
        }
        totalTimeMs += System.currentTimeMillis() - start;
        proving = false;
    }

    private void proveNextLapi() {
        maintainLapiGroups();
        Prime newPrime = lapiPrimeCenter.getPrime(highestLapi.getPrime().getIndex() + 1);
        nextLapi = Lapi.builder().prime(newPrime)
                .walker(lastTransition.getBodyList().getSmallestBody())
                .valueMultiplier(highestLapi.getValueMultiplier().multiply(newPrime.getValue()))
                .factorMultiplier(highestLapi.getFactorMultiplier().multiply(new ScientificNumber(2, 0))).build();
        nextLapi.getHcnList().add(findLastSuperiorHcn(determineTargetValue()));
        maintainProvedHcns();
    }

    private void maintainLapiGroups() {
        // clear lapi hcnLists from last intervals leftover
        lowestLapi.maintainAfterInterval();

        // involve nextlapi for upcoming hcn generation
        nextLapi.setLowerLapi(highestLapi);
        highestLapi.setHigherLapi(nextLapi);
        highestLapi = nextLapi;
        nextLapi = null;

        // delete dead lapis
        while (lowestProvedLapiWithinInterval > lowestLapi.getPrime().getIndex()) {
            lowestLapi = lowestLapi.deleteLapi();
        }
    }

    private ScientificNumber determineTargetValue() {
        //log.debug("nextLapi: {}", nextLapi);
        //log.debug("nextLapi.getWalker(): {}", nextLapi.getWalker());
        //log.debug("bodylist: {}", lastTransition.bodyList);
        return  nextLapi.getWalker().getLastGeneratedHcn().getValue().multiply(nextLapi.getPrime().getValue());
    }

    private Hcn findLastSuperiorHcn(ScientificNumber targetValue) {
        Hcn largestGeneratedSuperiorHcn;
        boolean candidateIsSuperior;
        do {
            largestGeneratedSuperiorHcn = extendLapiHcnListsUntilTarget(targetValue);
            candidateIsSuperior = true;
            Hcn targetHcn = nextLapi.generateHcn(nextLapi.getWalker());

            //log.debug("targetHcn: {} for targetValue: {}", targetHcn, targetValue);

            if (largestGeneratedSuperiorHcn.getFactor().isNotSmallerThan(targetHcn.getFactor())) {
                candidateIsSuperior = false;
                nextLapi.setWalker(nextLapi.getWalker().getNextActiveBody());
                targetHcn.gotDominated();
                lastTransition.deactivatedMaintain();
                targetValue = determineTargetValue();
            }

        } while (!candidateIsSuperior);
        return largestGeneratedSuperiorHcn;
    }

    private Hcn extendLapiHcnListsUntilTarget(ScientificNumber targetValue) {
        long t0 = System.currentTimeMillis();
        lowestLapi.generateHcnList(provedLimit, targetValue, lastTransition.getBodyList().getSmallestBody());
        generateHcnListTimeMs += System.currentTimeMillis() - t0;

        long t1 = System.currentTimeMillis();
        matrixMaintainCheck();
        matrixMaintainTimeMs += System.currentTimeMillis() - t1;

        Hcn largestGeneratedSuperiorHcn = highestLapi.getHcnList().get(highestLapi.getHcnList().size() - 1);
        provedLimit = targetValue;
        return largestGeneratedSuperiorHcn;
    }

    private void matrixMaintainCheck() {
        Prime prevLastMatrixIndex = lastTransition.getIndexes().get(lastTransition.getIndexes().size() - 1);
        highestLapi.getHcnList().forEach(hcn -> {
            if (!hcn.getBody().isDeactivated()) {
                hcn.matrixMaintainCheck();
            }
        });
        if (prevLastMatrixIndex != lastTransition.getIndexes().get(lastTransition.getIndexes().size() - 1)) {
            lowestLapi.recalculateMultipliers(prevLastMatrixIndex);
            nextLapi.recalculateMultipliers(prevLastMatrixIndex);
        }
    }

    private void maintainProvedHcns() {
        //remove as first member left there intentionally from previous interval to keep superior factor value to compare
        highestLapi.getHcnList().remove(0);
        lowestProvedLapiWithinInterval = Integer.MAX_VALUE;
        highestLapi.getHcnList().forEach(hcn -> {
            if (hcn.getLapi() < lowestProvedLapiWithinInterval) {
                lowestProvedLapiWithinInterval = hcn.getLapi();
            }
            provedCount++;
        });
    }
}
