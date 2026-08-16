package com.hcn.newCore;

import com.hcn.db.DbInsertService;
import com.hcn.event.ActivityCenter;
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

    public static TransitionNode lastTransition;
    private Lapi nextLapi;
    private Lapi lowestLapi;
    private Lapi highestLapi;
    private int lowestProvedLapiWithinInterval;
    private int provedCount;
    private final PrimeCenter lapiPrimeCenter = new PrimeCenter();
    private ScientificNumber provedLimit;
    @Builder.Default
    private String dbName = null;
    private boolean dbMode;
    private Interval referenceInterval;
    private static Interval currentInterval;
    private DbInsertService dbInsertService;
    private Body currentRecorder;

    // Timing
    @Builder.Default
    private long totalTimeMs = 0;
    @Builder.Default
    private long matrixMaintainTimeMs = 0;
    @Builder.Default
    private long generateHcnListTimeMs = 0;

    public void initialize2() {
        PrimeCenter.initialize();
        ApiNode p0 = ApiNode.builder().prevMatrixNode(null).build();
        p0.getIndexes().add(PrimeCenter.getPrime(0));
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

        lastTransition = TransitionNode.builder().nextMatrixNode(null)
                .transitionFrom(2).transitionTo(1).build();
        lastTransition.getIndexes().add(PrimeCenter.getPrime(1));
        BodyNode t1 = BodyNode.builder().parentNode(lastTransition).bodyNodeId(1)
                .value(new ScientificNumber(1, 0))
                .factor(new ScientificNumber(1, 0)).proved(true).build();
        BodyNode t2 = BodyNode.builder()
                .parentNode(lastTransition).bodyNodeId(2).value(new ScientificNumber(9, 0))
                .factor(new ScientificNumber(3, 0)).build();
        lastTransition.getBodyNodes().put(2, t2);
        lastTransition.getBodyNodes().put(1, t1);
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
        p0.setBodyList(BodyList.builder().smallestBody(b01).largestBody(b03).size(3).build());

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

        b01.getOffsprings().add(b11);
        b02.getOffsprings().add(b21);
        b03.getOffsprings().add(b31);
        b02.getOffsprings().add(b22);
        b03.getOffsprings().add(b32);

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
        lastTransition.setBodyList(BodyList.builder().smallestBody(b11).largestBody(b32).size(5).build());

        b11.setSmallerHcnGenerator(null);
        b11.setLargerHcnGenerator(b21);
        b21.setSmallerHcnGenerator(b11);
        b21.setLargerHcnGenerator(b31);
        b31.setSmallerHcnGenerator(b21);
        b31.setLargerHcnGenerator(b22);
        b22.setSmallerHcnGenerator(b31);
        b22.setLargerHcnGenerator(b32);
        b32.setSmallerHcnGenerator(b22);
        b32.setLargerHcnGenerator(null);
        HcnGeneratorList.initialize(b11);

        b11.setPreviousRecorder(b21);
        b11.setNextRecorder(b21);
        b21.setPreviousRecorder(b11);
        b21.setNextRecorder(b11);
        RecorderList.initialize(b11, b22, 2);

        Lapi.setLowestLapi(Lapi.builder().prime(PrimeCenter.getPrime(0)).lowerLapi(null)
                .valueMultiplier(new ScientificNumber(1, 0))
                .factorMultiplier(new ScientificNumber(1, 0))
                .build());
        Lapi.setHighestLapi(Lapi.builder().prime(PrimeCenter.getPrime(1)).lowerLapi(Lapi.getLowestLapi()).higherLapi(null)
                .valueMultiplier(new ScientificNumber(1, 0))
                .factorMultiplier(new ScientificNumber(1, 0))
                .build());
        Lapi.getLowestLapi().setHigherLapi(Lapi.getHighestLapi());

        Hcn hcn1 = Hcn.builder().body(b11).lapi(Lapi.getLowestLapi()).value(new ScientificNumber(2, 0))
                .factor(new ScientificNumber(2, 0)).build();
        Hcn hcn2 = Hcn.builder().body(b21).lapi(Lapi.getLowestLapi()).value(new ScientificNumber(4, 0))
                .factor(new ScientificNumber(3, 0)).build();
        Hcn hcn11 = Hcn.builder().body(b11).lapi(Lapi.getHighestLapi()).value(new ScientificNumber(6, 0))
                .factor(new ScientificNumber(4, 0)).build();
        Hcn hcn31 = Hcn.builder().body(b31).lapi(Lapi.getLowestLapi()).value(new ScientificNumber(8, 0))
                .factor(new ScientificNumber(4, 0)).build();
        Hcn hcn22 = Hcn.builder().body(b22).lapi(Lapi.getHighestLapi()).value(new ScientificNumber(36, 0))
                .factor(new ScientificNumber(9, 0)).build();
        Hcn hcn32 = Hcn.builder().body(b32).lapi(Lapi.getHighestLapi()).value(new ScientificNumber(72, 0))
                .factor(new ScientificNumber(12, 0)).build();

        b11.setFirstHcn(hcn1);
        b11.setFirstSuperiorHcn(hcn1);
        b11.setLastGeneratedHcn(hcn11);

        b21.setFirstHcn(hcn2);
        b21.setFirstSuperiorHcn(hcn2);
        b21.setLastGeneratedHcn(hcn2);

        b31.setLastGeneratedHcn(hcn31);
        b31.setFirstHcn(hcn31);
        b22.setLastGeneratedHcn(hcn22);
        b22.setFirstHcn(hcn22);
        b32.setLastGeneratedHcn(hcn32);
        b32.setFirstHcn(hcn32);

        b11.getHunters().add(b31);
        RecorderList.getBodiesWaitingToJoin().add(b22);
        RecorderList.getBodiesWaitingToJoin().add(b32);

        currentRecorder = b11;
        provedCount = 2;
        provedLimit = new ScientificNumber(6, 0);
        referenceInterval = Interval.builder().lapi(0).value(hcn1.getValue()).factor(hcn1.getFactor()).hcnList(List.of(hcn1, hcn2)).lowestRecorderLapi(1).build();
        currentInterval = Interval.builder().lapi(1).value(hcn11.getValue()).factor(hcn11.getFactor()).hcnList(new ArrayList<>(List.of(hcn11))).build();
        referenceInterval.setReferenceInterval(referenceInterval);

    }

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

        PrimeCenter.initialize();
        lastTransition = TransitionNode.builder()
                .transitionFrom(2).transitionTo(1).build();
        //lastTransition.indexes.add(primeCenter.getPrime(1));
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

        p0.setBodyList(BodyList.builder().smallestBody(b01).largestBody(b03).size(3).build());

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

        b11.setSmallerHcnGenerator(null);
        b11.setLargerHcnGenerator(b21);
        b21.setSmallerHcnGenerator(b11);
        b21.setLargerHcnGenerator(b31);
        b31.setSmallerHcnGenerator(b21);
        b31.setLargerHcnGenerator(b22);
        b22.setSmallerHcnGenerator(b31);
        b22.setLargerHcnGenerator(b32);
        b32.setSmallerHcnGenerator(b22);
        b32.setLargerHcnGenerator(null);

        lastTransition.setBodyList(BodyList.builder().smallestBody(b11).largestBody(b32).size(5).build());
        HcnGeneratorList.initialize(b11);
        b01.getOffsprings().add(b11);
        b02.getOffsprings().add(b21);
        b03.getOffsprings().add(b31);
        b02.getOffsprings().add(b22);
        b03.getOffsprings().add(b32);

        b11.setPreviousRecorder(b21);
        b11.setNextRecorder(b21);
        b21.setPreviousRecorder(b11);
        b21.setNextRecorder(b11);

        RecorderList.initialize(b11, b21, 2);

        Lapi.setLowestLapi(Lapi.builder().prime(lapiPrimeCenter.getPrime(0)).build());
        Lapi lapi1 = Lapi.builder().prime(lapiPrimeCenter.getPrime(1))
                .valueMultiplier(new ScientificNumber(1, 0))
                .factorMultiplier(new ScientificNumber(1, 0)).lowerLapi(Lapi.getLowestLapi()).build();
        Lapi.getLowestLapi().setHigherLapi(lapi1);

        nextLapi = Lapi.builder().prime(lapiPrimeCenter.getPrime(1)).walker(b21)
                .valueMultiplier(new ScientificNumber(1, 0))
                .factorMultiplier(new ScientificNumber(1, 0)).build();
        lowestLapi = Lapi.builder().prime(lapiPrimeCenter.getPrime(0)).walker(b31).higherLapi(nextLapi).build();
        nextLapi.setLowerLapi(lowestLapi);

        Hcn hcn1 = Hcn.builder().body(b11).lapi(lowestLapi).value(new ScientificNumber(2, 0))
                .factor(new ScientificNumber(2, 0)).build();
        Hcn hcn2 = Hcn.builder().body(b21).lapi(lowestLapi).value(new ScientificNumber(4, 0))
                .factor(new ScientificNumber(3, 0)).build();
        Hcn hcn11 = Hcn.builder().body(b11).lapi(nextLapi).value(new ScientificNumber(6, 0))
                .factor(new ScientificNumber(4, 0)).build();

        b11.setFirstHcn(hcn1);
        b11.setFirstSuperiorHcn(hcn1);
        b11.setLastGeneratedHcn(hcn11);
        b11.getGeneratedHcns().add(hcn1);
        b11.getGeneratedHcns().add(hcn11);

        b21.setFirstHcn(hcn2);
        b21.setFirstSuperiorHcn(hcn2);
        b21.setLastGeneratedHcn(hcn2);
        b21.getGeneratedHcns().add(hcn2);

        //force lapi0 deletion
        lowestProvedLapiWithinInterval = 1;
        provedCount = 2;
        provedLimit = new ScientificNumber(6, 0);
        referenceInterval = Interval.builder().lapi(0).value(hcn1.getValue()).factor(hcn1.getFactor()).hcnList(List.of(hcn1, hcn2)).lowestRecorderLapi(1).build();
        referenceInterval.setReferenceInterval(referenceInterval);

        if (dbMode) {
            dbInsertService.submit(referenceInterval);
        }
    }

    public void proveNextRecorder() {
        log.debug("********************************* START proveNextRecorder *********************************");
        Body nextRecorder = currentRecorder.getNextRecorder();
        Hcn nextRecorderHcn = nextRecorder.generateNextHcn();
        log.debug("nextRecorderHcn: {}", nextRecorderHcn);

        generateHunetrHcns(nextRecorderHcn);

        Hcn potentialNext = currentRecorder.getNextRecorder().getLastGeneratedHcn();

        bodyActivationCheck(potentialNext, nextRecorderHcn);

        Hcn nextHcn = currentRecorder.getNextRecorder().getLastGeneratedHcn();

        if (nextHcn.getLapiIndex() > currentInterval.getLapi()) {
            newIntervalFound(nextHcn);
        }
        currentInterval.getHcnList().add(nextHcn);

        currentRecorder = currentRecorder.getNextRecorder();
        ActivityCenter.setProving(false);
    }

    private void newIntervalFound(Hcn nextHcn) {
        referenceInterval = currentInterval;
        currentInterval = Interval.builder().lapi(nextHcn.getLapiIndex()).hcnList(new ArrayList<>())
                .value(nextHcn.getValue()).factor(nextHcn.getFactor()).build();
    }

    private void generateHunetrHcns(Hcn potentialRecorder) {
        log.debug("generating hunetr hcns for {}", potentialRecorder);
        List<Hcn> generatedHcns = new ArrayList<>();
        potentialRecorder.getBody().getHunters().forEach(hunter -> {
            generatedHcns.add(hunter.generateNextHcn());
        });
        generatedHcns.forEach(generatedHcn -> {
            log.debug("generatedHcn: {}", generatedHcn);
            if (generatedHcn.getValue().isSmallerThan(potentialRecorder.getValue())) {
                log.debug("generatedHcn is smaller than potentialRecorder, adding as recorder");
                RecorderList.addNewRecorder(currentRecorder, generatedHcn.getBody(), potentialRecorder.getBody());
                if (generatedHcn.getFactor().isNotSmallerThan(potentialRecorder.getFactor())) {
                    RecorderList.killBody(potentialRecorder.getBody());
                }
            }
        });
    }

    private void bodyActivationCheck(Hcn potentialNext, Hcn nextRecorderHcn) {

        while (shouldNextWaitingBodyJoin(potentialNext)) {

            Hcn potentialNewHcn = RecorderList.getBodiesWaitingToJoin().get(0).getLastGeneratedHcn();
            if (potentialNewHcn.getValue().isSmallerThan(potentialNext.getValue())) {
                if (potentialNewHcn.getFactor().isBiggerThan(currentRecorder.getLastGeneratedHcn().getFactor())) {
                    RecorderList.addNewRecorder(currentRecorder, potentialNewHcn.getBody(), nextRecorderHcn.getBody());
                    currentInterval.getHcnList().add(potentialNewHcn);
                    currentRecorder = potentialNewHcn.getBody();
                    RecorderList.getBodiesWaitingToJoin().remove(0);
                    if (potentialNewHcn.getFactor().isNotSmallerThan(nextRecorderHcn.getFactor())) {
                        RecorderList.killBody(nextRecorderHcn.getBody());
                    }
                } else {
                    log.debug("potentialNewHcn factor is not bigger than currentRecorder factor: {}", potentialNewHcn);
                    break;
                }

            } else {
                log.debug("potentialNewHcn {} should be a hunter of: {}", potentialNewHcn, nextRecorderHcn);
                potentialNewHcn.getBody().setPrayBody(nextRecorderHcn.getBody());
                nextRecorderHcn.getBody().getHunters().add(potentialNewHcn.getBody());
                RecorderList.getBodiesWaitingToJoin().remove(0);
            }
        }
    }

    private boolean shouldNextWaitingBodyJoin(Hcn potentialNext) {
        if (RecorderList.getBodiesWaitingToJoin().isEmpty()) {
            log.debug("no new hcn candidate present");
            return false;
        }
        Hcn potentialNewHcn = RecorderList.getBodiesWaitingToJoin().get(0).getLastGeneratedHcn();

        if (potentialNewHcn.getBody().isDeactivated()) {
            RecorderList.getBodiesWaitingToJoin().remove(0);
            potentialNewHcn = RecorderList.getBodiesWaitingToJoin().get(0).getLastGeneratedHcn();
        }

        if (potentialNewHcn.getFactor().isNotBiggerThan(potentialNext.getFactor())) {
            return true;
        } else {
            log.debug("potentialNewHcn factor is too big to join: {}", potentialNewHcn);
            return false;
        }
    }

    public void proveLapi(int count) {
        long start = System.currentTimeMillis();
        ActivityCenter.initialize(nextLapi.getPrime().getIndex(), dbMode);
        for (int i = 0; i < count; i++) {
            proveNextLapi();
            ActivityCenter.setProveProgress(i + 1);
            ActivityCenter.setCurrentLapi(highestLapi.getPrime().getIndex());
            if (dbMode && dbInsertService.isQueueAbovePauseLimit()) {
                ActivityCenter.finishMatrixMainActivity(highestLapi.getPrime().getIndex());
                while (!dbInsertService.isQueueBelowResumeLimit()) {
                    try { Thread.sleep(10); } catch (InterruptedException e) { Thread.currentThread().interrupt(); return; }
                }
                ActivityCenter.resume(highestLapi.getPrime().getIndex());
            }
        }
        ActivityCenter.finishMatrixMainActivity(highestLapi.getPrime().getIndex());
        if (dbMode) {
            try {
                dbInsertService.finalFlush(highestLapi.getPrime().getIndex());
                ActivityCenter.completeRun();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        totalTimeMs += System.currentTimeMillis() - start;
        ActivityCenter.setProving(false);
    }

    private void proveNextLapi() {
        log.debug("********************************* START NEXT LAPI *********************************");
        maintainLapiGroups();
        findLastSuperiorHcn();
        maintainProvedHcns();
        log.debug("");
    }

    private void maintainLapiGroups() {

        // involve nextlapi for upcoming hcn generation
        highestLapi = nextLapi;

        // delete dead lapis
        while (referenceInterval.getLowestRecorderLapi() > lowestLapi.getPrime().getIndex()) {
            lowestLapi = lowestLapi.deleteLapi();
        }

        Prime newPrime = lapiPrimeCenter.getPrime(highestLapi.getPrime().getIndex() + 1);
        nextLapi = Lapi.builder().prime(newPrime).lowerLapi(highestLapi)
                .walker(HcnGeneratorList.getSmallestBody())
                .valueMultiplier(highestLapi.getValueMultiplier().multiply(newPrime.getValue()))
                .factorMultiplier(highestLapi.getFactorMultiplier().multiply(new ScientificNumber(2, 0))).build();
        Hcn nextIntervalStarter = highestLapi.getWalker().getPrevActiveBody().getHcnForLapi(highestLapi);
        currentInterval = Interval.builder().lapi(highestLapi.getPrime().getIndex()).hcnList(new ArrayList<>(List.of(nextIntervalStarter))).lowestRecorderLapi(highestLapi.getPrime().getIndex()).build();
        highestLapi.setHigherLapi(nextLapi);
    }


    private void findLastSuperiorHcn() {
        currentInterval.setTargetValue(determineTargetValue());
        //log.debug("Finding last superior HCN for target value: {}", currentInterval.getTargetValue());
        boolean candidateIsSuperior;
        int counter = 0;
        do {
            counter++;
            boolean nextLapiFound = extendLapiHcnListsUntilTarget();
            log.debug("nextLapiFound: {}", nextLapiFound);
            candidateIsSuperior = true;


            if (!nextLapiFound) {
                candidateIsSuperior = false;
                currentInterval.setTargetValue(determineTargetValue());
                log.debug("targetValue updated to: {}", currentInterval.getTargetValue());
            }

            if (counter > 5) {
                candidateIsSuperior = true;
            }
            //RecorderList.print();
        } while (!candidateIsSuperior);
    }

    private void deactivateMaintain() {
        List<Body> deletedHcnGenerators = lastTransition.deactivatedMaintain();
        ActivityCenter.submitBodyDeletionEvent(deletedHcnGenerators);
        MatrixNode potentialApiNode = lastTransition.prevMatrixNode;
        while (potentialApiNode != null) {
            potentialApiNode.transitionNodeTriggerCheck();
            potentialApiNode = potentialApiNode.prevMatrixNode;
        }
    }

    private boolean extendLapiHcnListsUntilTarget() {
        log.debug("HCN Generation Phase: {}", currentInterval.getLapi());
        Prime prevLastMatrixIndex = lastTransition.getLastPrime();
        lowestLapi.hcnGenerationPhase(provedLimit);
        log.debug("");
        log.debug("Hunting Phase: {}", currentInterval.getLapi());
        lowestLapi.huntingPhase();
        log.debug("");
        //log.debug(RecorderList.print());

        boolean nextLapiFound = currentInterval.postHcnGenerateMaintainFoundNextLapi();
        log.debug("");
        if (!nextLapiFound) {
            nextLapi.setWalker(HcnGeneratorList.getSmallestBody());
            deactivateMaintain();
            //log.debug("firstBodyDeleted");
        }

        Prime currentLastMatrixIndex = lastTransition.getLastPrime();
        if (prevLastMatrixIndex != currentLastMatrixIndex) {
            lowestLapi.recalculateMultipliers(currentLastMatrixIndex);
        }

        provedLimit = currentInterval.getTargetValue();

        return nextLapiFound;
    }


    private void maintainProvedHcns() {
        //remove as first member left there intentionally from previous interval to keep superior factor value to compare

        provedCount = provedCount + currentInterval.getHcnList().size();
        currentInterval.setValue(currentInterval.getHcnList().get(0).getValue());
        currentInterval.setFactor(currentInterval.getHcnList().get(0).getFactor());
        currentInterval.setActiveBodyCount(HcnGeneratorList.getSize());
        referenceInterval = currentInterval.referenceCheck(referenceInterval);

        if (dbMode) {
            dbInsertService.submit(currentInterval);
        }
    }

    private ScientificNumber determineTargetValue() {
        return HcnGeneratorList.getSmallestBody().getValue().multiply(nextLapi.getValueMultiplier());
    }

    private ScientificNumber determineTargetFactor() {
        return HcnGeneratorList.getSmallestBody().getFactor().multiply(nextLapi.getFactorMultiplier());
    }

    public static Interval getCurrentInterval() {
        return currentInterval;
    }
}
