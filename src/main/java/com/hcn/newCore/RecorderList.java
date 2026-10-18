package com.hcn.newCore;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;


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


    public static void findPray(HcnGenerator hunter) {


        HcnGenerator referenceBody = hunter.getPreviousHunter();

        //log.info("hunter: {}, referenceBody: {}", hunter, referenceBody);

        int referenceDistance = referenceBody.getIntervalLapiDistance();
        Hcn referenceHunterHcn = referenceBody.getHunterHcn();
        Hcn prayHcn = referenceBody.getHunterPassingZone().getPrayBody().getPrayHcn();
        ScientificNumber prayHcnFactor = prayHcn.getFactor();

        ScientificNumber referenceBodyFactor = referenceBody.getBody().getFactor();
        ScientificNumber hunterBaseFactor = hunter.getBody().getFactor();
        ScientificNumber referenceMultiplier = hunterBaseFactor.divide(referenceBodyFactor);

        ScientificNumber hunterFactor = referenceHunterHcn.getFactor().multiply(referenceMultiplier);
        RecorderBody prayCandidate = referenceBody.getHunterPassingZone().getPrayBody();
        ScientificNumber referencePrayBodyFactor = prayCandidate.getBody().getFactor();

        int baseMultiplier = 0;

        while (hunterFactor.isBiggerThan(prayHcnFactor)) {
            prayCandidate = prayCandidate.getNextRecorder();

            if (prayCandidate.equals(firstRecorder)) {
                baseMultiplier++;
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
        prayCandidate.getPassingZone().placeHunter2(hunter);
    }

    public static void recorderToDeactivate(Hcn activityHcn) {

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
        //bodyToDelete.setFirstDominatedHcn(activityHcn);
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
                //potentialFirstRecorder.getHunters().forEach(hunter -> {
                    //hunter.setIntervalLapiDistance(hunter.getIntervalLapiDistance() - 1);
                //});
                potentialFirstRecorder = potentialFirstRecorder.getNextRecorder();
            }
            firstRecorder = potentialFirstRecorder;
        }

        bodyToDelete.setPreviousRecorder(null);
        bodyToDelete.setNextRecorder(null);
        size--;
        //bodyToDelete.setFirstDominatedHcn(null);

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

    public static void hunterToRecorder(Hcn activityHcn) {
        HcnGenerator recorderHcnGenerator = activityHcn.getHcnGenerator();
        RecorderBody nextRecorder = recorderHcnGenerator.getHunterPassingZone().getPrayBody();
        RecorderBody prevRecorder = nextRecorder.getPreviousRecorder();
        int intervalLapiDistance = nextRecorder.equals(firstRecorder) ? -1 : 0;

        RecorderBody newRecorderBody = RecorderBody.builder().body(recorderHcnGenerator.getBody())
                .intervalLapiDistance(intervalLapiDistance + recorderHcnGenerator.getIntervalLapiDistance())
                .previousRecorder(prevRecorder).nextRecorder(nextRecorder).prayHcn(activityHcn)
                .generatedHunterHcns(recorderHcnGenerator.getGeneratedHunterHcns()).build();

        recorderHcnGenerator.getBody().setHcnGenerator(newRecorderBody);
        //activityHcn.setHcnGenerator(newRecorderBody);
        activityHcn.setHcnStatus(HcnStatus.FIRST_RECORDER);
        newRecorderBody.getRecorderHcn(activityHcn.getLastActivePrime(), HcnStatus.FIRST_RECORDER);
        newRecorderBody.setPassingZone(new PassingZone(newRecorderBody));

        //log.info("newRecorderBody: {}", newRecorderBody);
        
        nextRecorder.setPreviousRecorder(newRecorderBody);
        prevRecorder.setNextRecorder(newRecorderBody);
        size++;

        //newRecorderBody.getBody().matrixMaintainCheck();

        List<HcnGenerator> toMove = new ArrayList<>();
        for (int intervalDistance : recorderHcnGenerator.getHunterPassingZone().getHunters().keySet()) {
            for (HcnGenerator hcnGenerator : recorderHcnGenerator.getHunterPassingZone().getHunters().get(intervalDistance)) {
                if (hcnGenerator.getHunterHcn().getFactor().isNotBiggerThan(activityHcn.getFactor())) {
                    toMove.add(hcnGenerator);
                }
            }
        }

        if (!toMove.isEmpty()) {

            toMove.forEach(hcnGeneratorToMove -> {
                recorderHcnGenerator.getHunterPassingZone().removeFromHunters(hcnGeneratorToMove);
                hcnGeneratorToMove.setIntervalLapiDistance(hcnGeneratorToMove.getIntervalLapiDistance() + intervalLapiDistance);
                newRecorderBody.getPassingZone().placeHunter2(hcnGeneratorToMove);
            });
        }
        newRecorderBody.getBody().matrixMaintainCheck();
        newRecorderBody.getPassingZone().recorderHunterCheck();
    }

    public static Hcn addNewRecord(Hcn hunterHcn) {
/*
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

        } else
            */
        {
            RecorderBody passingRecorderBody= (RecorderBody) hunterHcn.getHcnGenerator().getBody().getHcnGenerator();
            RecorderBody prayBody = passingRecorderBody.getPrayBody();
            RecorderBody referenceBody = prayBody.getPreviousRecorder();

            if (prayBody.equals(firstRecorder)) {
                passingRecorderBody.setIntervalLapiDistance(passingRecorderBody.getIntervalLapiDistance() - 1);
                //passingRecorderBody.getHunters().forEach(hunter -> hunter.setIntervalLapiDistance(hunter.getIntervalLapiDistance() - 1));
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
