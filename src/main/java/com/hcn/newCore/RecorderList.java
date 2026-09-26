package com.hcn.newCore;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;


@Slf4j
@Builder
public class RecorderList {
    private static Body firstRecorder;
    private static Body lastRecorder;
    private static Body currentRecorder;
    private static int size;
    private static final List<Body> bodiesWaitingToJoin = new ArrayList<>();

    public static Body getFirstRecorder() {
        return firstRecorder;
    }

    public static int getSize() {
        return size;
    }

    public static List<Body> getBodiesWaitingToJoin() {
        return bodiesWaitingToJoin;
    }

    public static Body getCurrentRecorder() {
        return currentRecorder;
    }

    public static void setCurrentRecorder(Body currentRecorder) {
        RecorderList.currentRecorder = currentRecorder;
    }

    public static void setSize(int size) {
        RecorderList.size = size;
    }

    public static void initialize(Body first, int count) {
        firstRecorder = first;
        size = count;
    }

    public static void print() {
        log.debug("Recorderlist size: {}", size);
        Body current = firstRecorder;
        int i = 0;
        do {
            log.debug("Nr {} - {}, intervalLapiDistance {}, factor: {}", i, current, current.getIntervalLapiDistance(), current.getCurrentFactor());
            current.getHunters().forEach(hunter -> {
                log.debug("    {} hunter - {}, factor: {}", hunter.getPrayLapiDistance(), hunter, hunter.getCurrentFactor());
            });
            current = current.getNextRecorder();
        } while (current != firstRecorder);
    }

    public static void findPray(Body hunter) {

        Body referenceBody = hunter.getSmallerHcnGenerator();
        ScientificNumber referenceFactor = referenceBody.getCurrentFactor();
        ScientificNumber hunterFactor = referenceFactor.multiply(hunter.getFactor()).divide(referenceBody.getFactor());
        Body prayCandidate = referenceBody.getPrayBody();
        int referenceDistance = prayCandidate.getIntervalLapiDistance() + referenceBody.getPrayLapiDistance();
        ScientificNumber prayFactor = prayCandidate.getCurrentFactor();

        int baseMultiplier = 0;

        while (hunterFactor.isBiggerThan(prayFactor)) {
            prayCandidate = prayCandidate.getNextRecorder();
            if (prayCandidate.equals(firstRecorder)) {
                baseMultiplier++;
            }
            prayFactor = prayCandidate.getCurrentFactor().multiply(new ScientificNumber(Math.pow(2, baseMultiplier), 0));
        }

        int prayLapiDiff = referenceDistance - prayCandidate.getIntervalLapiDistance() + baseMultiplier;
        hunter.setPrayBody(prayCandidate);
        hunter.setPrayLapiDistance(prayLapiDiff);
        prayCandidate.getHunters().add(hunter);
    }


    public static void killCurrentRecorder() {

        Body bodyToDelete = currentRecorder;

        Body prev = bodyToDelete.getPreviousRecorder();
        Body next = bodyToDelete.getNextRecorder();

        prev.setNextRecorder(next);
        next.setPreviousRecorder(prev);

        currentRecorder = prev;
        if (firstRecorder.equals(bodyToDelete)) {
            Body potentialFirstRecorder = firstRecorder.getNextRecorder();
            while (potentialFirstRecorder.getIntervalLapiDistance() > 0) {
                potentialFirstRecorder.setIntervalLapiDistance(potentialFirstRecorder.getIntervalLapiDistance() - 1);
                potentialFirstRecorder = potentialFirstRecorder.getNextRecorder();
            }
            firstRecorder = potentialFirstRecorder;
        }

        bodyToDelete.setPreviousRecorder(null);
        bodyToDelete.setNextRecorder(null);
        size--;
        bodyToDelete.setFirstDominatedHcn(bodyToDelete.getLastGeneratedHcn());
        HcnGeneratorList.remove(bodyToDelete);
        bodyToDelete.deactivate();

    }

    public static void addNewRecord(Hcn newRecorderHcn) {

        Interval.addRecorderHcn(newRecorderHcn);
        Body prayBody = newRecorderHcn.getBody().getPrayBody();
        Body referenceBody = prayBody.getPreviousRecorder();

        prayBody.setPreviousRecorder(newRecorderHcn.getBody());
        newRecorderHcn.getBody().setNextRecorder(prayBody);

        referenceBody.setNextRecorder(newRecorderHcn.getBody());
        newRecorderHcn.getBody().setPreviousRecorder(referenceBody);
        size++;

        newRecorderHcn.getBody().setPrayBody(null);

        int intervalLapiDistance = prayBody.getIntervalLapiDistance() + newRecorderHcn.getBody().getPrayLapiDistance();
        if (prayBody.equals(firstRecorder)) {
            intervalLapiDistance--;
        }
        newRecorderHcn.getBody().setIntervalLapiDistance(intervalLapiDistance);
        newRecorderHcn.getBody().setPrayLapiDistance(null);
        newRecorderHcn.matrixMaintainCheck();
        moveHuntersFromPrayBody(newRecorderHcn, prayBody);
    }

    private static void moveHuntersFromPrayBody(Hcn newRecorderHcn, Body prayBody) {
        Set<Body> toMove = new HashSet<>();
        ScientificNumber multiplier = new ScientificNumber((prayBody.equals(firstRecorder) ? 2 : 1), 0);
        prayBody.getHunters().forEach(hunter -> {
            ScientificNumber hunterHcnFactor = hunter.getCurrentFactor().multiply(multiplier);
            if (hunterHcnFactor.isNotBiggerThan(newRecorderHcn.getFactor())) {
                toMove.add(hunter);
            }
        });

        final int baseDistance = 0 - (prayBody.equals(firstRecorder) ? 1 : 0);
        toMove.forEach(hunter -> {
            prayBody.getHunters().remove(hunter);
            hunter.setPrayBody(newRecorderHcn.getBody());
            int distance = hunter.getPrayLapiDistance() + prayBody.getIntervalLapiDistance() - newRecorderHcn.getBody().getIntervalLapiDistance();
            hunter.setPrayLapiDistance(baseDistance + distance);
            newRecorderHcn.getBody().getHunters().add(hunter);
        });
    }

    public static boolean isCurrentRecorderFirstRecorder() {
        if (currentRecorder.equals(firstRecorder)) {
            return true;
        }
        return false;
    }
}
