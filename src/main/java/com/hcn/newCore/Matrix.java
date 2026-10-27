package com.hcn.newCore;

import com.hcn.db.DbInsertService;
import com.hcn.event.ActivityCenter;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
@Setter
@Builder
public class Matrix {

    //public static TransitionNode lastTransition;
    private static ScientificNumber targetValue;
    private static ScientificNumber provedLimit;
    private int lowestProvedLapiWithinInterval;
    private int provedCount;
    @Builder.Default
    private String dbName = null;
    private boolean dbMode;
    private DbInsertService dbInsertService;
    private static int passingCounter = 0;

    // Timing
    @Builder.Default
    private long totalTimeMs = 0;
    @Builder.Default
    private long matrixMaintainTimeMs = 0;
    @Builder.Default
    private long generateHcnListTimeMs = 0;

    public void initialize() {
        Prime.initialize();
        TransitionNode.initialize();
        Body.initialize();
        HcnGenerator.initialize();
        RecorderBody.initialize();

        lowestProvedLapiWithinInterval = 1;
        provedCount = 2;
        provedLimit = new ScientificNumber(6, 0);

        if (dbMode) {
            dbInsertService.submit(Interval.getGlobalReferenceInterval());
        }
    }

    public void proveNextLapi(int passingCount) {

        for (int i = 0; i < passingCount; i++) {
            Passing.processNextPassing();
            this.passingCounter++;
        }
        ActivityCenter.setProving(false);
    }

    /*
    private void deactivateMaintain() {
        List<Body> deletedHcnGenerators = lastTransition.deactivatedMaintain();
        ActivityCenter.submitBodyDeletionEvent(deletedHcnGenerators);
        MatrixNode potentialApiNode = lastTransition.prevMatrixNode;
        while (potentialApiNode != null) {
            potentialApiNode.transitionNodeTriggerCheck();
            potentialApiNode = potentialApiNode.prevMatrixNode;
        }
    }

     */

    public static ScientificNumber getTargetValue() {
        return targetValue;
    }

    public static ScientificNumber getProvedLimit() {
        return provedLimit;
    }

    public static void setProvedLimit(ScientificNumber proved) {
        provedLimit = proved;
    }

    public static int getPassingCounter() {
        return passingCounter;
    }

}
