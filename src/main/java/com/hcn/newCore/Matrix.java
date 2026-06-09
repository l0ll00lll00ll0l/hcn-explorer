package com.hcn.newCore;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

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
    private int lastProvedPrimeIndex;
    private final PrimeCenter lapiPrimeCenter = new PrimeCenter();
    private ScientificNumber provedLimit;

    // Progress tracking
    @Builder.Default
    private boolean proving = false;
    @Builder.Default
    private int proveTarget = 0;
    @Builder.Default
    private int proveProgress = 0;

    public void initialize() {

        ApiNode p0 = ApiNode.builder().index(0).prevMatrixNode(null).prime(new ScientificNumber(2, 0)).build();
        Pip pip01 = Pip.builder().api(p0).bodyNodeId(1).proved(true)
                .value(new ScientificNumber(2,0))
                .factor(new ScientificNumber(2, 0)).build();
        Pip pip02 = Pip.builder().api(p0).bodyNodeId(2).proved(true)
                .value(new ScientificNumber(4,0))
                .factor(new ScientificNumber(3, 0)).build();
        Pip pip03 = Pip.builder().api(p0).bodyNodeId(3)
                .value(new ScientificNumber(8,0))
                .factor(new ScientificNumber(4, 0)).build();
        p0.getBodyNodes().put(1, pip01);
        p0.getBodyNodes().put(2, pip02);
        p0.getBodyNodes().put(3, pip03);

        lastTransition = TransitionNode.builder()
                .transitionFrom(2).transitionTo(1).firstIndex(1).lastIndex(2).primeCenter(new PrimeCenter()).build();
        Transition t1 = Transition.builder().transitionNode(lastTransition).bodyNodeId(1)
                .value(new ScientificNumber(1, 0))
                .factor(new ScientificNumber(1, 0)).proved(true).build();
        lastTransition.getBodyNodes().put(1, t1);
        Transition t2 = Transition.builder()
                .transitionNode(lastTransition).bodyNodeId(2).value(new ScientificNumber(9, 0))
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

        nextLapi = Lapi.builder().lapi(1).prime(new ScientificNumber(3, 0)).walker(b11).build();
        //nextLapi.getHcnList().add(hcn11);
        lowestLapi = Lapi.builder().lapi(0).prime(new ScientificNumber(2, 0)).walker(b31).build();
        lowestLapi.getHcnList().add(hcn3);
        highestLapi = lowestLapi;

        //force lapi0 deletion
        lowestProvedLapiWithinInterval = 1;
        lastProvedPrimeIndex = 0;
        provedCount = 2;
        provedLimit = new ScientificNumber(6, 0);
    }

    public void proveLapi(int count) {
        proving = true;
        proveTarget = count;
        proveProgress = 0;
        for (int i = 0; i < count; i++) {
            proveNextLapi();
            proveProgress = i + 1;
        }
        proving = false;
    }

    private void proveNextLapi() {
        maintainLapiGroups();
        nextLapi = Lapi.builder().prime(new ScientificNumber(lapiPrimeCenter.getPrime(highestLapi.getLapi() + 1), 0))
                .lapi(highestLapi.getLapi() + 1).walker(lastTransition.getBodyList().getSmallestBody()).build();
        nextLapi.getHcnList().add(findLastSuperiorHcn(determineTargetValue()));
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
        while (lowestProvedLapiWithinInterval > lowestLapi.getLapi()) {
            lowestLapi = lowestLapi.deleteLapi();
        }
    }

    private ScientificNumber determineTargetValue() {
        return  nextLapi.getWalker().getLastGeneratedHcn().getValue().multiply(nextLapi.getPrime());
    }

    private Hcn findLastSuperiorHcn(ScientificNumber targetValue) {
        Hcn largestGeneratedSuperiorHcn;
        boolean candidateIsSuperior;
        do {
            largestGeneratedSuperiorHcn = extendLapiHcnListsUntilTarget(targetValue);
            candidateIsSuperior = true;
            Hcn targetHcn = nextLapi.getWalker().generateNextHcn(nextLapi);

            if (largestGeneratedSuperiorHcn.getFactor().isNotSmallerThan(targetHcn.getFactor())) {
                candidateIsSuperior = false;
                nextLapi.setWalker(nextLapi.getWalker().getNextActiveBody());
                targetHcn.getBody().gotDominated();
                lastTransition.deactivatedMaintain();
                targetValue = determineTargetValue();
            }

        } while (!candidateIsSuperior);
        return largestGeneratedSuperiorHcn;
    }

    private Hcn extendLapiHcnListsUntilTarget(ScientificNumber targetValue) {
        // hcnlist creation, dominated bodies removed 1 by 1
        lowestLapi.generateHcnList(provedLimit, targetValue);

        matrixMaintainCheck();

        Hcn largestGeneratedSuperiorHcn = highestLapi.getHcnList().get(highestLapi.getHcnList().size() - 1);
        provedLimit = targetValue;
        return largestGeneratedSuperiorHcn;
    }

    private void matrixMaintainCheck() {
        highestLapi.getHcnList().forEach(hcn -> {
            if (!hcn.getBody().isDeactivated()) {
                hcn.getBody().matrixMaintainCheck();
            }
        });
    }
}
