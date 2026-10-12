package com.hcn.newCore;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
        //log.debug("Recorderlist size: {}", size);
        RecorderBody current = firstRecorder;
        int i = 0;
        do {
            //log.debug("Nr {} - {}, intervalLapiDistance {}, factor: {}", i, current, current.getIntervalLapiDistance(), current.getProcessingHcn().getFactor());
            current.getHunters().forEach(hunter -> {
                //log.debug("    {} hunter - {}", hunter.getIntervalLapiDistance(), hunter);
            });
            current = current.getNextRecorder();
        } while (current != firstRecorder);
    }

    public static void findPray(HunterBody hunter) {


        HunterBody referenceBody = hunter.getPreviousHunter();

        //log.debug("-*-*-*-*-*-*-*-*-*-*-*- referenceBody: {}, hunter: {} -*-*-*-*-*-*-*-*-*-*-*- ", referenceBody, hunter);
        //ScientificNumber referenceFactor = referenceBody.getProcessingHcn().getFactor();
        //ScientificNumber hunterFactor = referenceFactor.multiply(hunterBaseFactor).divide(referenceBodyFactor);
        //RecorderBody prayCandidate = referenceBody.getPassingZone().getPrayBody();

        //ScientificNumber prayFactor = prayCandidate.getProcessingHcn().getFactor();

        //log.debug("prayCandidate: {}, hunterFactor: {}, prayFactor: {}", prayCandidate, hunterFactor, prayFactor);

        int referenceDistance = referenceBody.getIntervalLapiDistance();
        Hcn referenceHunterHcn = referenceBody.getHunterHcn();
        Hcn prayHcn = referenceBody.getPassingZone().getPrayHcn();
        ScientificNumber prayHcnFactor = prayHcn.getFactor();

        ScientificNumber referenceBodyFactor = referenceBody.getBody().getFactor();
        ScientificNumber hunterBaseFactor = hunter.getBody().getFactor();
        ScientificNumber referenceMultiplier = hunterBaseFactor.divide(referenceBodyFactor);
        //log.debug("referenceMultiplier: {}", referenceMultiplier);

        ScientificNumber hunterFactor = referenceHunterHcn.getFactor().multiply(referenceMultiplier);
        RecorderBody prayCandidate = referenceBody.getPassingZone().getPrayBody();
        ScientificNumber referencePrayBodyFactor = prayCandidate.getBody().getFactor();

        //log.debug("prayCandidate: {}, hunterFactor: {}, prayHcnFactor: {}}", prayCandidate, hunterFactor, prayHcnFactor);

        /*
        int baseMultiplier = 0;
        while (hunterFactor.isBiggerThan(prayFactor)) {
            prayCandidate = prayCandidate.getNextRecorder();

            if (prayCandidate.equals(firstRecorder)) {
                baseMultiplier++;
            }
            prayFactor = prayCandidate.getProcessingHcn().getFactor().multiply(new ScientificNumber(Math.pow(2, baseMultiplier), 0));
            //log.debug("{} prayCandidate: {}, hunterFactor: {}, prayFactor: {}", baseMultiplier, prayCandidate, hunterFactor, prayFactor);
        }

         */

        int baseMultiplier = 0;

        while (hunterFactor.isBiggerThan(prayHcnFactor)) {
            prayCandidate = prayCandidate.getNextRecorder();

            if (prayCandidate.equals(firstRecorder)) {
                baseMultiplier++;
                //log.debug("baseMultiplier: {}", baseMultiplier);
            }
            ScientificNumber intervalDistanceMultiplier = new ScientificNumber(Math.pow(2, prayCandidate.getIntervalLapiDistance()), 0);
            //log.debug(" intervalDistanceMultiplier: {}", intervalDistanceMultiplier);
            ScientificNumber originalReferenceMultiplier = prayCandidate.getBody().getFactor().divide(referencePrayBodyFactor);
            //log.debug(" originalReferenceMultiplier: {}", originalReferenceMultiplier);
            ScientificNumber basMulti = new ScientificNumber(Math.pow(2, baseMultiplier), 0);
            //log.debug(" basMulti: {}", basMulti);
            prayHcnFactor = prayHcnFactor.multiply(originalReferenceMultiplier).multiply(intervalDistanceMultiplier).multiply(basMulti);
            //log.debug("prayCandidate: {}, hunterFactor: {}, prayHcnFactor: {}", prayCandidate, hunterFactor, prayHcnFactor);
        }


        hunter.setIntervalLapiDistance(referenceDistance + baseMultiplier);
        prayCandidate.getHunters().add(hunter);

        PassingZone passingZone = prayCandidate.getPassingZone();
        if (passingZone == null) {
            prayCandidate.setPassingZone(new PassingZone(prayCandidate, hunter));
            //log.debug("new passingzone created: {}", prayCandidate.getPassingZone().getPassingZone());
        } else {
            //log.debug("hunter placed in existing passingzone");
            prayCandidate.getPassingZone().placeHunter(hunter);
        }
        //log.debug("-*-*-*-*-*-*-*-*-*-*-*- hunter: {}, pz: {} -*-*-*-*-*-*-*-*-*-*-*- ", hunter.getHunterHcn(), hunter.getPassingZone().getPrayHcn());
    }

    private static void recorderToDeactivate(Hcn activityHcn) {

        RecorderBody bodyToDelete = (RecorderBody) activityHcn.getHcnGenerator();
        RecorderBody prev = bodyToDelete.getPreviousRecorder();
        RecorderBody next = bodyToDelete.getNextRecorder();
        prev.setNextRecorder(next);
        next.setPreviousRecorder(prev);

        if (firstRecorder.equals(bodyToDelete)) {
            RecorderBody potentialFirstRecorder = firstRecorder.getNextRecorder();
            while (potentialFirstRecorder.getIntervalLapiDistance() > 0) {
                potentialFirstRecorder.setIntervalLapiDistance(potentialFirstRecorder.getIntervalLapiDistance() - 1);
                potentialFirstRecorder.getPassingZone().decreaseIntervalLapiDistances();
                potentialFirstRecorder = potentialFirstRecorder.getNextRecorder();
            }
            firstRecorder = potentialFirstRecorder;
        }

        bodyToDelete.setPreviousRecorder(null);
        bodyToDelete.setNextRecorder(null);
        size--;
        bodyToDelete.setFirstDominatedHcn(activityHcn);

        /*
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
         */
        bodyToDelete.getBody().deactivate();
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


    public static ScientificNumber processActivityHcn(Hcn activityHcn, ScientificNumber previousRecorderFactor) {
        if (activityHcn.getHcnStatus() == HcnStatus.HUNTER_IN_PASSINGZONE) {
            log.info("act: {}", activityHcn);
            if (activityHcn.getFactor().isBiggerThan(previousRecorderFactor)) {
                hunterToRecorder(activityHcn);
                return activityHcn.getFactor();
            } else {
                //TODO hunter switching pray implementation required
                return previousRecorderFactor;
            }
        } else if (activityHcn.getHcnStatus() == HcnStatus.RECORDER_IN_PASSINGZONE) {
            PassingZone passingZone = PassingZone.getGlobalNextPassingZone();
            if (activityHcn.getFactor().isBiggerThan(previousRecorderFactor)) {
                passingZone.prayBodySurvivedCreateNewPassingZone();
                return activityHcn.getFactor();
            } else {
                recorderToDeactivate(activityHcn);
                passingZone.prayBodyDeactivated();
                return previousRecorderFactor;
            }
        }
        return previousRecorderFactor;
    }

    private static void hunterToRecorder(Hcn activityHcn) {
        HunterBody newRecorder = (HunterBody) activityHcn.getHcnGenerator();
        RecorderBody nextRecorder = newRecorder.getPassingZone().getPrayBody();
        RecorderBody prevRecorder = nextRecorder.getPreviousRecorder();
        int intervalLapiDistance = nextRecorder.equals(firstRecorder) ? -1 : 0;

        if (intervalLapiDistance == -1) {
            log.info("intervaldist = -1 at  {}", activityHcn);
        }

        RecorderBody newRecorderBody = RecorderBody.builder().body(newRecorder.getBody())
                .intervalLapiDistance(intervalLapiDistance + newRecorder.getIntervalLapiDistance())
                .previousRecorder(prevRecorder).nextRecorder(nextRecorder)
                .firstSuperiorHcn(activityHcn).build();
        newRecorder.getBody().setRecorderBody(newRecorderBody);
        activityHcn.setHcnGenerator(newRecorderBody);
        nextRecorder.setPreviousRecorder(newRecorderBody);
        prevRecorder.setNextRecorder(newRecorderBody);
        size++;

        newRecorderBody.getBody().matrixMaintainCheck();

        List<Hcn> toMove = new ArrayList<>();
        nextRecorder.getPassingZone().getNonPassingHcns().forEach(nonPassingHcn -> {
            if (nonPassingHcn.getFactor().isNotBiggerThan(activityHcn.getFactor())) {
                log.info("added toMove: {} ", nonPassingHcn);
                toMove.add(nonPassingHcn);
            }
        });

        if (!toMove.isEmpty()) {
            if (intervalLapiDistance == -1) {
                log.info("itoMove at  {} toMove: {}", activityHcn, toMove);
            }
            HunterBody firstHunter = (HunterBody) toMove.get(0).getHcnGenerator();
            firstHunter.setIntervalLapiDistance(firstHunter.getIntervalLapiDistance() + intervalLapiDistance);
            newRecorderBody.setPassingZone(new PassingZone(newRecorderBody, firstHunter));
            //firstHunter.setPassingZone(newRecorderBody.getPassingZone());
            nextRecorder.getPassingZone().getNonPassingHcns().remove(toMove.get(0));
            toMove.remove(0);
            toMove.forEach(hcnToMove -> {
                HunterBody hunterToMove = (HunterBody) hcnToMove.getHcnGenerator();
                //hunterToMove.setPassingZone(newRecorderBody.getPassingZone());
                hunterToMove.setIntervalLapiDistance(hunterToMove.getIntervalLapiDistance() + intervalLapiDistance);
                newRecorderBody.getPassingZone().placeHunter(hunterToMove);
                nextRecorder.getPassingZone().getNonPassingHcns().remove(hcnToMove);
            });
        }
    }

    public static Hcn addNewRecord(Hcn hunterHcn) {

        if (hunterHcn.getHcnGenerator() instanceof HunterBody) {
            /*
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

             */
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

        //PassingZone passingZone = ((RecorderBody)newRecorderHcn.getHcnGenerator()).

        /*
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
            //hunter.setPrayBody(newRecorderBody);
            int distance = hunter.getIntervalLapiDistance();
            hunter.setIntervalLapiDistance(baseDistance + distance);
            newRecorderBody.getHunters().add(hunter);
        });

         */
    }

    public static boolean isCurrentRecorderFirstRecorder() {
        if (currentRecorder.equals(firstRecorder)) {
            return true;
        }
        return false;
    }

}
