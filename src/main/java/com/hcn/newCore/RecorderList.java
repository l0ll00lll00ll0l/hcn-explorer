package com.hcn.newCore;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Set;


@Slf4j
@Builder
public class RecorderList {
    private static RecorderBody firstRecorder;
    private static RecorderBody lastRecorder;
    private static RecorderBody currentRecorder;
    private static int size;

    public static RecorderBody getFirstRecorder() {
        return firstRecorder;
    }

    public static int getSize() {
        return size;
    }

    public static RecorderBody getCurrentRecorder() {
        return currentRecorder;
    }

    public static void setCurrentRecorder(RecorderBody RecorderBody) {
        currentRecorder = RecorderBody;
    }

    public static void setSize(int size) {
        RecorderList.size = size;
    }

    public static void initialize(RecorderBody first, int count) {
        firstRecorder = first;
        size = count;
    }

    public static void print() {
        log.debug("Recorderlist size: {}", size);
        RecorderBody current = firstRecorder;
        int i = 0;
        do {
            log.debug("Nr {} - {}, intervalLapiDistance {}, factor: {}", i, current, current.getIntervalLapiDistance(), current.getCurrentFactor());
            current.getHunters().forEach(hunter -> {
                log.debug("    {} hunter - {}, factor: {}", hunter.getPrayLapiDistance(), hunter, hunter.getCurrentHcnFactor());
            });
            current = current.getNextRecorder();
        } while (current != firstRecorder);
    }

    public static void findPray(HunterBody hunter) {

        HunterBody referenceBody = hunter.getPreviousHunter();
        ScientificNumber referenceFactor = referenceBody.getCurrentHcnFactor();
        ScientificNumber hunterFactor = referenceFactor.multiply(hunter.getBody().getFactor()).divide(referenceBody.getBody().getFactor());
        RecorderBody prayCandidate = referenceBody.getPrayBody();
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
        log.debug("Finding pray for hunter - {}, pray: {}", hunter, hunter.getPrayBody());
    }


    public static void killCurrentRecorder() {

        RecorderBody bodyToDelete = currentRecorder;

        RecorderBody prev = bodyToDelete.getPreviousRecorder();
        RecorderBody next = bodyToDelete.getNextRecorder();

        prev.setNextRecorder(next);
        next.setPreviousRecorder(prev);

        currentRecorder = prev;
        if (firstRecorder.equals(bodyToDelete)) {
            RecorderBody potentialFirstRecorder = firstRecorder.getNextRecorder();
            while (potentialFirstRecorder.getIntervalLapiDistance() > 0) {
                potentialFirstRecorder.setIntervalLapiDistance(potentialFirstRecorder.getIntervalLapiDistance() - 1);
                potentialFirstRecorder = potentialFirstRecorder.getNextRecorder();
            }
            firstRecorder = potentialFirstRecorder;
        }

        bodyToDelete.setPreviousRecorder(null);
        bodyToDelete.setNextRecorder(null);
        size--;
        bodyToDelete.setFirstDominatedHcn(null);
        //HcnGeneratorList.remove(bodyToDelete);
        bodyToDelete.getBody().deactivate();

    }

    public static void deleteHunterBody(HunterBody hunterBody) {
        hunterBody.getPrayBody().getHunters().remove(hunterBody);
        hunterBody.setPrayBody(null);
    }

    public static RecorderBody addNewRecord(HunterHcn hunterHcn) {

        HunterBody newRecorder = hunterHcn.getHunterBody();
        RecorderBody prayBody = newRecorder.getPrayBody();
        RecorderBody referenceBody = prayBody.getPreviousRecorder();
        int intervalLapiDistance = prayBody.getIntervalLapiDistance() + newRecorder.getPrayLapiDistance();
        if (prayBody.equals(firstRecorder)) {
            intervalLapiDistance--;
        }

        RecorderBody newRecorderBody = RecorderBody.builder().body(newRecorder.getBody()).intervalLapiDistance(intervalLapiDistance)
                .previousRecorder(referenceBody).nextRecorder(prayBody)
                .firstSuperiorHcn(Prime.getPrimeByLapiDistance(intervalLapiDistance).getIndex()).build();
        newRecorder.getBody().setRecorderBody(newRecorderBody);


        prayBody.setPreviousRecorder(newRecorderBody);
        referenceBody.setNextRecorder(newRecorderBody);
        size++;

        newRecorderBody.getBody().matrixMaintainCheck();
        moveHuntersFromPrayBody(prayBody, newRecorderBody, hunterHcn.getFactor());
        return newRecorderBody;
    }

    private static void moveHuntersFromPrayBody(RecorderBody prayBody, RecorderBody newRecorderBody, ScientificNumber newRecorderFactor) {
        Set<HunterBody> toMove = new HashSet<>();
        prayBody.getHunters().forEach(hunter -> {
            HunterHcn hunterHcn = hunter.getProcessingHunterHcn();
            if (hunterHcn.getFactor().isNotBiggerThan(newRecorderFactor)) {
                toMove.add(hunter);
            }
        });

        final int baseDistance = 0 - (prayBody.equals(firstRecorder) ? 1 : 0);
        toMove.forEach(hunter -> {
            prayBody.getHunters().remove(hunter);
            hunter.setPrayBody(newRecorderBody);
            int distance = hunter.getPrayLapiDistance() + prayBody.getIntervalLapiDistance() - newRecorderBody.getIntervalLapiDistance();
            hunter.setPrayLapiDistance(baseDistance + distance);
            newRecorderBody.getHunters().add(hunter);
        });
    }

    public static boolean isCurrentRecorderFirstRecorder() {
        if (currentRecorder.equals(firstRecorder)) {
            return true;
        }
        return false;
    }
}
