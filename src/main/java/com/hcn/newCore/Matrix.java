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
    private int lowestProvedLapiWithinInterval;
    private int provedCount;
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
        Prime.initialize();
        ApiNode p0 = ApiNode.builder().prevMatrixNode(null).build();
        p0.getIndexes().add(Prime.getFirstPrime());
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
        lastTransition.indexes.add(Prime.getFirstPrime().getNextPrime());
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

        lastTransition.setBodyList(BodyList.builder().smallestBody(b11).largestBody(b32).size(5).build());

        b01.getOffsprings().add(b11);
        b02.getOffsprings().add(b21);
        b03.getOffsprings().add(b31);
        b02.getOffsprings().add(b22);
        b03.getOffsprings().add(b32);

        Prime.getFirstPrime().setValueMultiplier(new ScientificNumber((double) 1 / 3, 0));
        Prime.getFirstPrime().setFactorMultiplier(new ScientificNumber(0.5, 0));
        Prime.getFirstPrime().getNextPrime().setValueMultiplier(new ScientificNumber(1, 0));
        Prime.getFirstPrime().getNextPrime().setFactorMultiplier(new ScientificNumber(1, 0));
        Prime.getHcnProducerPrimes().add(Prime.getFirstPrime().getNextPrime());
        Prime.getHcnProducerPrimes().add(Prime.getFirstPrime());

        RecorderBody firstRecorder = RecorderBody.builder().body(b11).intervalLapiDistance(0).firstSuperiorHcn(0).build();
        RecorderBody lastRecorder = RecorderBody.builder().body(b21).intervalLapiDistance(0).firstSuperiorHcn(0).build();
        b11.setRecorderBody(firstRecorder);
        b21.setRecorderBody(lastRecorder);
        firstRecorder.setNextRecorder(lastRecorder);
        lastRecorder.setPreviousRecorder(firstRecorder);
        firstRecorder.setPreviousRecorder(lastRecorder);
        lastRecorder.setNextRecorder(firstRecorder);
        RecorderList.initialize(firstRecorder, 2);

        Hcn hcn1 = Hcn.builder().recorderBody(firstRecorder).lastActivePrime(Prime.getFirstPrime()).value(new ScientificNumber(2, 0))
                .factor(new ScientificNumber(2, 0)).build();
        Hcn hcn2 = Hcn.builder().recorderBody(lastRecorder).lastActivePrime(Prime.getFirstPrime()).value(new ScientificNumber(4, 0))
                .factor(new ScientificNumber(3, 0)).build();
        Hcn hcn11 = Hcn.builder().recorderBody(firstRecorder).lastActivePrime(Prime.getFirstPrime().getNextPrime()).value(new ScientificNumber(6, 0))
                .factor(new ScientificNumber(4, 0)).build();

        //force lapi0 deletion
        lowestProvedLapiWithinInterval = 1;
        provedCount = 2;
        provedLimit = new ScientificNumber(6, 0);
        Interval.setGlobalReferenceInterval(Interval.builder().lapi(0).prime(Prime.getFirstPrime()).hcnList(List.of(hcn1, hcn2)).build());
        Interval.getGlobalReferenceInterval().setReferenceInterval(Interval.getGlobalReferenceInterval());
        Interval.setCurrentInterval(Interval.builder().lapi(1).prime(Prime.getFirstPrime().getNextPrime()).hcnList(new ArrayList<>(List.of(hcn11))).lowestRecorderLapi(1).build());
        RecorderList.setCurrentRecorder(firstRecorder);

        if (dbMode) {
            dbInsertService.submit(Interval.getGlobalReferenceInterval());
        }

        HunterBody h31 = HunterBody.builder().body(b31).prayBody(firstRecorder).prayLapiDistance(1).build();
        b31.setHunterBody(h31);
        firstRecorder.getHunters().add(h31);

        HunterBody h22 = HunterBody.builder().body(b22).prayBody(lastRecorder).prayLapiDistance(1).build();
        b22.setHunterBody(h22);
        lastRecorder.getHunters().add(h22);

        HunterBody h32 = HunterBody.builder().body(b32).prayBody(lastRecorder).prayLapiDistance(1).build();
        b32.setHunterBody(h32);
        lastRecorder.getHunters().add(h32);
    }

    public void proveNextLapi() {
        Interval.processCurrentInterval();
        ActivityCenter.setProving(false);
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

    public static ScientificNumber getTargetValue() {
        return targetValue;
    }

    public static ScientificNumber getProvedLimit() {
        return provedLimit;
    }

    public static void setProvedLimit(ScientificNumber proved) {
        provedLimit = proved;
    }

}
