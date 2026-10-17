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

        RecorderBody firstRecorder = RecorderBody.builder().body(b11).intervalLapiDistance(0).build();
        RecorderBody lastRecorder = RecorderBody.builder().body(b21).intervalLapiDistance(0).build();
        b11.setHcnGenerator(firstRecorder);
        b21.setHcnGenerator(lastRecorder);
        firstRecorder.setNextRecorder(lastRecorder);
        lastRecorder.setPreviousRecorder(firstRecorder);
        firstRecorder.setPreviousRecorder(lastRecorder);
        lastRecorder.setNextRecorder(firstRecorder);
        RecorderList.initialize(firstRecorder, 2);

        Hcn hcn1 = firstRecorder.getRecorderHcn(Prime.getFirstPrime(), HcnStatus.FIRST_RECORDER);
        Hcn hcn2 = lastRecorder.getRecorderHcn(Prime.getFirstPrime(), HcnStatus.FIRST_RECORDER);

        lowestProvedLapiWithinInterval = 1;
        provedCount = 2;
        provedLimit = new ScientificNumber(6, 0);
        Interval.setGlobalReferenceInterval(Interval.builder().lapi(0).prime(Prime.getFirstPrime()).hcnList(List.of(hcn1, hcn2)).build());
        Interval.getGlobalReferenceInterval().setReferenceInterval(Interval.getGlobalReferenceInterval());

        Interval.setCurrentInterval(Interval.builder().lapi(1).prime(Prime.getFirstPrime().getNextPrime()).hcnList(new ArrayList<>()).lowestRecorderLapi(1).build());
        Interval.getGlobalReferenceInterval().setPreviousInterval(Interval.getGlobalReferenceInterval());
        Interval.getGlobalReferenceInterval().setNextInterval(Interval.getCurrentInterval());
        Interval.getCurrentInterval().setNextInterval(null);
        Interval.getCurrentInterval().setPreviousInterval(null);
        RecorderList.setCurrentRecorder(firstRecorder);

        if (dbMode) {
            dbInsertService.submit(Interval.getGlobalReferenceInterval());
        }

        HcnGenerator h31 = HcnGenerator.builder().body(b31).intervalLapiDistance(1).generatedHunterHcns(new ArrayList<>()).build();
        b31.setHcnGenerator(h31);
        //firstRecorder.getHunters().add(h31);

        PassingZone firstPassingZone = new PassingZone(firstRecorder);
        firstPassingZone.placeHunter2(h31);
        PassingZone.setGlobalNextPassingZone(firstPassingZone);
        firstRecorder.setPassingZone(firstPassingZone);
        
        PassingZone secondPassingZone = new PassingZone(lastRecorder);
        lastRecorder.setPassingZone(secondPassingZone);
        //h31.setPassingZone(PassingZone.getGlobalNextPassingZone());
        //Prime.calculatePassingZone(h31);

        b22.setHcnGenerator(HcnGenerator.builder().body(b22).generatedHunterHcns(new ArrayList<>()).build());
        RecorderList.findPray(b22.getHcnGenerator());
        b32.setHcnGenerator(HcnGenerator.builder().body(b32).generatedHunterHcns(new ArrayList<>()).build());
        RecorderList.findPray(b32.getHcnGenerator());

    }

    public void proveNextLapi() {
        //Interval.processCurrentInterval();
        PassingZone.processNextPassingZone();
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
