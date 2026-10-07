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
            log.debug("Nr {} - {}, intervalLapiDistance {}, factor: {}", i, current, current.getIntervalLapiDistance(), current.getCurrentHcnFactor());
            current.getHunters().forEach(hunter -> {
                log.debug("    {} hunter - {}", hunter.getIntervalLapiDistance(), hunter);
            });
            current = current.getNextRecorder();
        } while (current != firstRecorder);
    }

    public static void findPray(HunterBody hunter) {

        HunterBody referenceBody = hunter.getPreviousHunter();
        ScientificNumber referenceFactor = referenceBody.getCurrentHcnFactor();
        ScientificNumber hunterFactor = referenceFactor.multiply(hunter.getBody().getFactor()).divide(referenceBody.getBody().getFactor());
        RecorderBody prayCandidate = referenceBody.getPrayBody();
        int referenceDistance = referenceBody.getIntervalLapiDistance();
        ScientificNumber prayFactor = prayCandidate.getCurrentHcnFactor();

        int baseMultiplier = 0;
        while (hunterFactor.isBiggerThan(prayFactor)) {
            prayCandidate = prayCandidate.getNextRecorder();
            if (prayCandidate.equals(firstRecorder)) {
                baseMultiplier++;
            }
            prayFactor = prayCandidate.getCurrentHcnFactor().multiply(new ScientificNumber(Math.pow(2, baseMultiplier), 0));
        }

        hunter.setPrayBody(prayCandidate);
        hunter.setIntervalLapiDistance(referenceDistance + baseMultiplier);
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
                potentialFirstRecorder.getHunters().forEach(hunter -> {
                    hunter.setIntervalLapiDistance(hunter.getIntervalLapiDistance() - 1);
                });
                potentialFirstRecorder = potentialFirstRecorder.getNextRecorder();
            }
            firstRecorder = potentialFirstRecorder;
        }

        bodyToDelete.setPreviousRecorder(null);
        bodyToDelete.setNextRecorder(null);
        size--;
        bodyToDelete.setFirstDominatedHcn(null);

        if (bodyToDelete.getPrayBody() != null) {
            bodyToDelete.getPrayBody().getRecorderHunters().remove(bodyToDelete);
            bodyToDelete.setPrayBody(null);
        }
        if (!bodyToDelete.getRecorderHunters().isEmpty()) {
            bodyToDelete.getRecorderHunters().forEach(recorderHunter -> {
                //TODO reste prayBody for recorder Hunter
                recorderHunter.setPrayBody(null);
            });
            bodyToDelete.getRecorderHunters().clear();
        }

        bodyToDelete.getBody().deactivate();

    }

    public static void deleteHunterBody(HunterBody hunterBody) {
        hunterBody.getPrayBody().getHunters().remove(hunterBody);
        hunterBody.setPrayBody(null);
    }

    public static Hcn addNewRecord(Hcn hunterHcn) {

        if (hunterHcn.getHcnGenerator() instanceof HunterBody) {
            HunterBody newRecorder = hunterHcn.getHcnGenerator().getBody().getHunterBody();
            RecorderBody prayBody = newRecorder.getPrayBody();
            RecorderBody referenceBody = prayBody.getPreviousRecorder();
            int intervalLapiDistance = newRecorder.getIntervalLapiDistance();
            if (prayBody.equals(firstRecorder)) {
                intervalLapiDistance--;
            }

            RecorderBody newRecorderBody = RecorderBody.builder().body(newRecorder.getBody()).intervalLapiDistance(intervalLapiDistance)
                    .previousRecorder(referenceBody).nextRecorder(prayBody)
                    .firstSuperiorHcn(hunterHcn).build();
            newRecorder.getBody().setRecorderBody(newRecorderBody);
            hunterHcn.setHcnGenerator(newRecorderBody);
            prayBody.setPreviousRecorder(newRecorderBody);
            referenceBody.setNextRecorder(newRecorderBody);
            size++;

            newRecorderBody.getBody().matrixMaintainCheck();
            moveHuntersFromPrayBody(prayBody, hunterHcn);
            if (intervalLapiDistance > 0) {
                RecorderBody prayCandidate = newRecorderBody.getPreviousRecorder();
                while (prayCandidate.getIntervalLapiDistance() > intervalLapiDistance - 1) {
                    prayCandidate = prayCandidate.getPreviousRecorder();
                }
                newRecorderBody.setPrayBody(prayCandidate);
                prayCandidate.getRecorderHunters().add(newRecorderBody);
                //log.debug("RecorderBody {} added to recorderHunters of {}", newRecorderBody, prayCandidate);
            }
        } else {
            RecorderBody passingRecorderBody= hunterHcn.getHcnGenerator().getBody().getRecorderBody();
            RecorderBody prayBody = passingRecorderBody.getPrayBody();
            RecorderBody referenceBody = prayBody.getPreviousRecorder();

            if (prayBody.equals(firstRecorder)) {
                passingRecorderBody.setIntervalLapiDistance(passingRecorderBody.getIntervalLapiDistance() - 1);
                passingRecorderBody.getHunters().forEach(hunter -> hunter.setIntervalLapiDistance(hunter.getIntervalLapiDistance() - 1));
            }

            prayBody.setNextRecorder(passingRecorderBody.getNextRecorder());
            passingRecorderBody.getNextRecorder().setPreviousRecorder(prayBody);
            prayBody.setPreviousRecorder(passingRecorderBody);
            passingRecorderBody.setNextRecorder(prayBody);
            passingRecorderBody.setPreviousRecorder(referenceBody);
            referenceBody.setNextRecorder(passingRecorderBody);

            moveHuntersFromPrayBody(prayBody, hunterHcn);
            prayBody.getRecorderHunters().remove(passingRecorderBody);
            passingRecorderBody.setPrayBody(null);


            if (passingRecorderBody.getIntervalLapiDistance() > 0) {
                RecorderBody prayCandidate = passingRecorderBody.getPreviousRecorder();
                while (prayCandidate.getIntervalLapiDistance() > passingRecorderBody.getIntervalLapiDistance() - 1) {
                    prayCandidate = prayCandidate.getPreviousRecorder();
                }
                passingRecorderBody.setPrayBody(prayCandidate);
                prayCandidate.getRecorderHunters().add(passingRecorderBody);
                //log.debug("RecorderBody {} added to recorderHunters of {}", newRecorderBody, prayCandidate);
            }
        }

        return hunterHcn;
    }

    private static void moveHuntersFromPrayBody(RecorderBody prayBody, Hcn newRecorderHcn) {
        Set<HunterBody> toMove = new HashSet<>();
        prayBody.getHunters().forEach(hunter -> {
            Hcn hunterHcn = hunter.getProcessingHcn();
            if (hunterHcn.getFactor().isNotBiggerThan(newRecorderHcn.getFactor())) {
                toMove.add(hunter);
            }
        });

        final int baseDistance = 0 - (prayBody.equals(firstRecorder) ? 1 : 0);

        RecorderBody newRecorderBody = newRecorderHcn.getHcnGenerator().body.getRecorderBody();

        toMove.forEach(hunter -> {
            prayBody.getHunters().remove(hunter);
            hunter.setPrayBody(newRecorderBody);
            int distance = hunter.getIntervalLapiDistance();
            hunter.setIntervalLapiDistance(baseDistance + distance);
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
