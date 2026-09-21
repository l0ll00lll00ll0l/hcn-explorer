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
    private static ScientificNumber targetValue;
    private static ScientificNumber provedLimit;
    private static Body currentRecorder;
    private int lowestProvedLapiWithinInterval;
    private int provedCount;
    private final PrimeCenter lapiPrimeCenter = new PrimeCenter();
    @Builder.Default
    private String dbName = null;
    private boolean dbMode;
    private DbInsertService dbInsertService;

    // Timing
    @Builder.Default
    private long totalTimeMs = 0;
    @Builder.Default
    private long matrixMaintainTimeMs = 0;
    @Builder.Default
    private long generateHcnListTimeMs = 0;

    public void initialize() {
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

        lastTransition = TransitionNode.builder()
                .transitionFrom(2).transitionTo(1).build();
        lastTransition.indexes.add(PrimeCenter.getPrime(1));
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

        Lapi.setLowestLapi(Lapi.builder().prime(lapiPrimeCenter.getPrime(0)).walker(b21)
                .valueMultiplier(new ScientificNumber((double) 1 / 3, 0))
                .factorMultiplier(new ScientificNumber(0.5, 0))
                .build());
        Lapi.setHighestLapi(Lapi.builder().prime(lapiPrimeCenter.getPrime(1)).walker(b11)
                .valueMultiplier(new ScientificNumber(1, 0))
                .factorMultiplier(new ScientificNumber(1, 0)).lowerLapi(Lapi.getLowestLapi())
                .build());
        Lapi.getLowestLapi().setHigherLapi(Lapi.getHighestLapi());

        Hcn hcn1 = Hcn.builder().body(b11).lapi(Lapi.getLowestLapi()).value(new ScientificNumber(2, 0))
                .factor(new ScientificNumber(2, 0)).build();
        Hcn hcn2 = Hcn.builder().body(b21).lapi(Lapi.getLowestLapi()).value(new ScientificNumber(4, 0))
                .factor(new ScientificNumber(3, 0)).build();
        Hcn hcn11 = Hcn.builder().body(b11).lapi(Lapi.getHighestLapi()).value(new ScientificNumber(6, 0))
                .factor(new ScientificNumber(4, 0)).build();

        b11.setFirstHcn(hcn1);
        b11.setFirstSuperiorHcn(hcn1);
        b11.setLastGeneratedHcn(hcn11);

        b21.setFirstHcn(hcn2);
        b21.setFirstSuperiorHcn(hcn2);
        b21.setLastGeneratedHcn(hcn2);

        //force lapi0 deletion
        lowestProvedLapiWithinInterval = 1;
        provedCount = 2;
        provedLimit = new ScientificNumber(6, 0);
        Interval.setGlobalReferenceInterval(Interval.builder().lapi(0).value(hcn1.getValue()).factor(hcn1.getFactor()).hcnList(List.of(hcn1, hcn2)).build());
        Interval.getGlobalReferenceInterval().setReferenceInterval(Interval.getGlobalReferenceInterval());
        Interval.setCurrentInterval(Interval.builder().lapi(1).value(hcn11.getValue()).factor(hcn11.getFactor()).hcnList(new ArrayList<>(List.of(hcn11))).lowestRecorderLapi(1).build());
        currentRecorder = b11;

        if (dbMode) {
            dbInsertService.submit(Interval.getGlobalReferenceInterval());
        }
    }

    /*
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

     */

    public void proveNextLapi() {
        //log.debug("********************************* START NEXT LAPI *********************************");
        maintainLapiGroups();
        determineTargetValue();
        //log.debug("HCN Generation Phase // targetValue {}", targetValue);
        Lapi.generateHcnsUntilTargetValue();
        Lapi.huntingPhase();
        Interval.populateHcnList();
        deactivateMaintain();
        provedLimit = targetValue;
        ActivityCenter.setProving(false);
        //maintainProvedHcns();
        //log.debug("");
    }

    private void maintainLapiGroups() {

        /*
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

         */
    }


    private void createHcnsUntilTargetValue() {


        /*
        boolean candidateIsSuperior;
        int counter = 0;
        do {
            counter++;
            boolean nextLapiFound = extendLapiHcnListsUntilTarget();
            //log.debug("nextLapiFound: {}", nextLapiFound);
            candidateIsSuperior = true;


            if (!nextLapiFound) {
                candidateIsSuperior = false;
                currentInterval.setTargetValue(determineTargetValue());
                //log.debug("targetValue updated to: {}", currentInterval.getTargetValue());
            }

            if (counter > 5) {
                candidateIsSuperior = true;
            }
            //RecorderList.print();
        } while (!candidateIsSuperior);

         */
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

    /*
    private boolean extendLapiHcnListsUntilTarget() {
        //log.debug("HCN Generation Phase: {}", Interval.getCurrentInterval().getLapi());
        Prime prevLastMatrixIndex = lastTransition.getLastPrime();
        //Lapi.getLowestLapi().hcnGenerationPhase();
        //log.debug("");
        //log.debug("Hunting Phase: {}", Interval.getCurrentInterval().getLapi());
        Lapi.getLowestLapi().huntingPhase();
        //log.debug("");
        //log.debug(RecorderList.print());

        boolean nextLapiFound = Interval.getCurrentInterval().postHcnGenerateMaintainFoundNextLapi();
        //log.debug("");
        if (!nextLapiFound) {
            //nextLapi.setWalker(HcnGeneratorList.getSmallestBody());
            deactivateMaintain();
            //log.debug("firstBodyDeleted");
        }

        Prime currentLastMatrixIndex = lastTransition.getLastPrime();
        if (prevLastMatrixIndex != currentLastMatrixIndex) {
            Lapi.getLowestLapi().recalculateMultipliers(currentLastMatrixIndex);
        }

        provedLimit = targetValue;

        return nextLapiFound;
    }


     */

    /*
    private void maintainProvedHcns() {
        //remove as first member left there intentionally from previous interval to keep superior factor value to compare

        provedCount = provedCount + currentInterval.getHcnList().size();
        currentInterval.setValue(currentInterval.getHcnList().get(0).getValue());
        currentInterval.setFactor(currentInterval.getHcnList().get(0).getFactor());
        currentInterval.setActiveBodyCount(HcnGeneratorList.getSize());
        globalReferenceInterval = currentInterval.referenceCheck(globalReferenceInterval);

        if (dbMode) {
            dbInsertService.submit(currentInterval);
        }
    }


     */

    private void determineTargetValue() {
        if (Lapi.getLowestLapi().getWalker() == null) {
            //log.debug("lowest lapi is null for lapi: {}", Lapi.getLowestLapi().getPrime().getIndex());
            Lapi.getLowestLapi().restoreWalker();
        } else if (!Lapi.getLowestLapi().getWalker().isActive()) {
            //log.debug("lowest lapi is deactivated for lapi: {}", Lapi.getLowestLapi().getPrime().getIndex());
            Lapi.getLowestLapi().restoreWalker();
        }
        targetValue = Lapi.getLowestLapi().getWalker().getValue().multiply(Lapi.getLowestLapi().getHigherLapi().getValueMultiplier());
        //log.debug("targetValue = {}, walker: {}", targetValue, Lapi.getLowestLapi().getWalker());
        //return HcnGeneratorList.getSmallestBody().getValue().multiply(nextLapi.getValueMultiplier());
    }

    public static ScientificNumber getTargetValue() {
        return targetValue;
    }

    public static ScientificNumber getProvedLimit() {
        return provedLimit;
    }

    public static void setProvedLimit(ScientificNumber proved) {
        provedLimit = proved;
    }

    public static Body getCurrentRecorder() {
        return currentRecorder;
    }

    public static void setCurrentRecorder(Body currentRecorder) {
        Matrix.currentRecorder = currentRecorder;
    }

    public static void resetDeletedCurrentRecorder() {
        //log.debug("resetDeletedCurrentRecorder currentRecorder={}", currentRecorder);
        //log.debug("recorderlist: {}", RecorderList.print());
        currentRecorder = currentRecorder.getPreviousRecorder();
    }
}
