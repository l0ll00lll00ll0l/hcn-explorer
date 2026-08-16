package com.hcn.newCore;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;


@Slf4j
@Builder
public class RecorderList {
    private static Body firstRecorder;
    private static Body lastRecorder;
    private static int size;
    private static final List<Body> bodiesWaitingToJoin = new ArrayList<>();

    public static Body getFirstRecorder() {
        return firstRecorder;
    }

    public static int getSize() {
        return size;
    }

    public static Body getLastRecorder() {
        return lastRecorder;
    }

    public static void setFirstRecorder(Body first) {
        firstRecorder = first;
    }

    public static void setLastRecorder(Body last) {
        lastRecorder = last;
    }

    public static List<Body> getBodiesWaitingToJoin() {
        return bodiesWaitingToJoin;
    }

    public static void initialize(Body first, Body last, int count) {
        firstRecorder = first;
        lastRecorder = last;
        size = count;
    }

    public static String print() {
        StringBuilder sb = new StringBuilder();
        if (firstRecorder.getGeneratedHcns().size() > 1) {
            sb.append("firstrecorder: "+ firstRecorder.getGeneratedHcns().get(firstRecorder.getGeneratedHcns().size() - 2) +"[\n");
        } else {
            sb.append("firstrecorder: "+ firstRecorder.getGeneratedHcns().get(firstRecorder.getGeneratedHcns().size() - 1) +"[\n");
        }
        sb.append("lastregorder:"+ lastRecorder.getLastGeneratedHcn() +"\n");
        sb.append("firstrecorder next: "+ firstRecorder.getLastGeneratedHcn() +"[\n");
        sb.append("size: "+ size +"[\n");
        sb.append("RecordeList: [\n");
        Body current = firstRecorder;

        int i = 0;
        while (i < size) {
            sb.append("  ").append(i++).append(": ").append(current.getLastGeneratedHcn()).append("\n");
            current = current.getNextRecorder();
        }
        sb.append("]");
        return sb.toString();
    }

    /*
    public static void placeNewActiveBody(Hcn newRecorderHcn) {


        log.debug("placeNewActiveBody newRecorderHcn={}", newRecorderHcn);

        Body referenceBody = firstRecorder;
        while (referenceBody.getNextRecorder() != null && referenceBody.getNextRecorder().getLastGeneratedHcn().getValue().isSmallerThan(newRecorderHcn.getValue())) {
            referenceBody = referenceBody.getNextRecorder();
        }

        if (newRecorderHcn.getFactor().isBiggerThan(referenceBody.getLastGeneratedHcn().getFactor())) {
            newRecorderBodyPlacement(referenceBody, newRecorderHcn);
        } else {
            findPrayForHunterBody(newRecorderHcn);
        }
    }


     */

    public static void findPrayForHunterBody(Hcn newRecorderHcn) {
        log.debug(" findPrayForHunterBody newRecorderHcn: {}", newRecorderHcn);
        if (newRecorderHcn.getLapi().getHigherLapi() != null) {
            Body prayCandidate = newRecorderHcn.getLapi().getHigherLapi().getWalker().getSmallerHcnGenerator();
            //log.debug("1findPrayForHunterBody prayCandidate={}", prayCandidate.getHcnForLapi(newRecorderHcn.getLapi().getHigherLapi()));

            while (prayCandidate != null && prayCandidate.getHcnForLapi(newRecorderHcn.getLapi().getHigherLapi()).getFactor().isBiggerThan(newRecorderHcn.getFactor())) {
                prayCandidate = prayCandidate.getSmallerHcnGenerator();
            }

            if (prayCandidate != null) {
                Hcn prayHcn = prayCandidate.getHcnForLapi(newRecorderHcn.getLapi().getHigherLapi());
                //log.debug("findPrayFor HunterBody: {} prayHcn={}", newRecorderHcn, prayHcn);
                if (prayHcn.getFactor().isSmallerThan(newRecorderHcn.getFactor())) {
                    prayCandidate = prayCandidate.getLargerHcnGenerator();
                }
                log.debug(" findPrayFor HunterBody: {} prayCandidate={}", newRecorderHcn, prayCandidate.getHcnForLapi(newRecorderHcn.getLapi().getHigherLapi()));

                newRecorderHcn.getBody().setPrayBody(prayCandidate);
                prayCandidate.getHunters().add(newRecorderHcn.getBody());


            } else {
                log.debug(" findPrayFor HunterBody: {} NOT FOUND",newRecorderHcn);
            }

        }
    }


/*
    public static void placeBodyAfterPraySkip(Hcn generatedHcn, Body potentialPrayProviderBody) {

        log.debug("placeBodyAfterPraySkip: {} , potentialPrayProviderBody={}", generatedHcn, potentialPrayProviderBody);
        if (potentialPrayProviderBody.getHcnForLapi(generatedHcn.getLapi().getHigherLapi()).getValue().isBiggerThan(generatedHcn.getValue())) {
            log.debug("placeBodyAfterPraySkip: {} > {} -> place before", potentialPrayProviderBody.getHcnForLapi(generatedHcn.getLapi().getHigherLapi()).getValue(), generatedHcn.getValue());
        } else {
            log.debug("Potential pray provider identified: {}", potentialPrayProviderBody);
            if (generatedHcn.getFactor().isBiggerThan(potentialPrayProviderBody.getHcnForLapi(generatedHcn.getLapi().getHigherLapi()).getValue())) {
                newRecorderBodyPlacement(potentialPrayProviderBody, generatedHcn);
            } else {
                log.debug("skiped pray was bigger factor, pray body has to be identified");
            }
        }
    }


 */
    public static void prayHunted(Hcn generatedHcn) {
        Hcn prayHcn = generatedHcn.getBody().getPrayBody().getHcnForLapi(generatedHcn.getLapi().getHigherLapi());

        if (prayHcn.getBody().isRecorder()) {
            log.debug(" Pray hunted: {} RECORDER pray: {}", generatedHcn, prayHcn);
            addNewRecordWithPrayBody(generatedHcn);

            if (generatedHcn.getFactor().isNotSmallerThan(prayHcn.getFactor())) {
                killBody(prayHcn.getBody());
            }
            generatedHcn.matrixMaintainCheck();
        } else {
            log.debug(" Pray hunted: {} ACTIVE NON-RECORDER pray: {}", generatedHcn, prayHcn);
        }
    }

    private static void addNewRecordWithPrayBody(Hcn generatedHcn) {

        log.debug("  addNewRecordWithPrayBody generatedHcn={}", generatedHcn);
        Body prayBody = generatedHcn.getBody().getPrayBody();
        Hcn potentialNextHcn = firstRecorder.getNextRecorder().getLastGeneratedHcn();
        while (potentialNextHcn.getValue().isSmallerThan(generatedHcn.getValue())) {
            potentialNextHcn = potentialNextHcn.getBody().getNextRecorder().getLastGeneratedHcn();
        }
        Hcn referenceHcn = potentialNextHcn.getBody().getPreviousRecorder().getLastGeneratedHcn();
        addNewRecord(referenceHcn, generatedHcn, potentialNextHcn);

        prayBody.getHunters().remove(generatedHcn.getBody());
        generatedHcn.getBody().setPrayBody(null);
    }

    public static void killBody(Body bodyToDelete) {
        log.debug("  killBody bodyToDelete={}", bodyToDelete);
        Body prev = bodyToDelete.getPreviousRecorder();
        Body next = bodyToDelete.getNextRecorder();

        if (firstRecorder.equals(bodyToDelete)) {
            firstRecorder = next;
            lastRecorder.setNextRecorder(next);
        }

        if (lastRecorder.equals(bodyToDelete)) {
            lastRecorder = prev;
            prev.setNextRecorder(firstRecorder);
        }

        if (!bodyToDelete.getHunters().isEmpty()) {
            bodyToDelete.getHunters().forEach(hunter -> hunter.setPrayBody(null));
        }
        bodyToDelete.getHunters().clear();

        prev.setNextRecorder(next);
        next.setPreviousRecorder(prev);

        bodyToDelete.setPreviousRecorder(null);
        bodyToDelete.setNextRecorder(null);
        size--;
        bodyToDelete.setFirstDominatedHcn(bodyToDelete.getLastGeneratedHcn());
        HcnGeneratorList.remove(bodyToDelete);
        bodyToDelete.deactivate();
        //log.debug("    remove body={}: size={}", bodyToDelete.getLastGeneratedHcn(), size);
        //log.debug(RecorderList.print());
    }

    public static void placeBodyWithoutPray(Hcn generatedHcn) {
        log.debug("  placeBodyWithoutPray generatedHcn={}", generatedHcn);
        Hcn potentialNextHcn = firstRecorder.getNextRecorder().getLastGeneratedHcn();
        while (potentialNextHcn.getValue().isSmallerThan(generatedHcn.getValue())) {
            potentialNextHcn = potentialNextHcn.getBody().getNextRecorder().getLastGeneratedHcn();
        }
        Hcn referenceHcn = potentialNextHcn.getBody().getPreviousRecorder().getLastGeneratedHcn();
        log.debug("  placeBodyWithoutPray referenceBody={}, generatedHcn: {}, nextHcn? {}", referenceHcn, generatedHcn, potentialNextHcn);
        
        if (probablyNotNeededCheckButYetToProve(referenceHcn, generatedHcn, potentialNextHcn)) {
            addNewRecord(referenceHcn, generatedHcn, potentialNextHcn);
        } else {
            log.error("  BODY WITHOUT PRAYBODY IS NOT RECORDER referenceBody={}, generatedHcn: {}, nextHcn? {}", referenceHcn, generatedHcn, potentialNextHcn);
        }
        //log.debug(RecorderList.print());
    }

    private static void addNewRecord(Hcn referenceHcn, Hcn generatedHcn, Hcn potentialNextHcn) {
        log.debug("  addNewRecord referenceBody={}, generatedHcn: {}, nextHcn? {}", referenceHcn, generatedHcn, potentialNextHcn);
        generatedHcn.matrixMaintainCheck();
        potentialNextHcn.getBody().setPreviousRecorder(generatedHcn.getBody());
        generatedHcn.getBody().setNextRecorder(potentialNextHcn.getBody());

        referenceHcn.getBody().setNextRecorder(generatedHcn.getBody());
        generatedHcn.getBody().setPreviousRecorder(referenceHcn.getBody());
        if (lastRecorder.equals(referenceHcn.getBody())) {
            lastRecorder = generatedHcn.getBody();
        }
        size++;

    }

    public static void addNewRecorder(Body prevBody, Body newBody, Body nextBody) {
        log.debug("  addNewRecord referenceBody={}, newBody: {}, nextHcn? {}", prevBody, newBody, nextBody);
        newBody.matrixMaintainCheck();
        nextBody.setPreviousRecorder(newBody);
        newBody.setNextRecorder(nextBody);

        prevBody.setNextRecorder(newBody);
        newBody.setPreviousRecorder(prevBody);
        if (lastRecorder.equals(prevBody)) {
            lastRecorder = newBody;
        }
        size++;

    }

    private static boolean probablyNotNeededCheckButYetToProve(Hcn referenceHcn, Hcn generatedHcn, Hcn potentialNextHcn) {
        boolean areValuesOk = referenceHcn.getValue().isSmallerThan(generatedHcn.getValue()) && generatedHcn.getValue().isSmallerThan(potentialNextHcn.getValue());
        boolean areFactorsOk = referenceHcn.getFactor().isSmallerThan(generatedHcn.getFactor()) && generatedHcn.getFactor().isSmallerThan(potentialNextHcn.getFactor());
        return areValuesOk && areFactorsOk;
    }

    public static void placeNewBodies(Body newActiveBody) {
        log.debug(" - Created new body: {}", newActiveBody);
        Lapi firstHcnLapi = newActiveBody.getPrevActiveBody().getLastGeneratedHcn().getLapi();
        Hcn firstHcn = newActiveBody.generateHcn(firstHcnLapi);
        if (bodiesWaitingToJoin.isEmpty()) {
            bodiesWaitingToJoin.add(newActiveBody);
        } else {
            int firstNotSmaller = -1;
            for (int i = 0; i < bodiesWaitingToJoin.size(); i++) {
                log.debug("i={} factor={}", i, bodiesWaitingToJoin.get(i).getLastGeneratedHcn().getFactor());
                if (bodiesWaitingToJoin.get(i).getLastGeneratedHcn().getFactor().isNotSmallerThan(firstHcn.getFactor())) {
                    firstNotSmaller = i;
                    log.debug("firstNotSmaller={}", firstNotSmaller);
                    break;
                }
            }

            if (firstNotSmaller == -1) {
                log.debug("appending at end");
                bodiesWaitingToJoin.add(newActiveBody);
            } else if (bodiesWaitingToJoin.get(firstNotSmaller).getLastGeneratedHcn().getFactor().isBiggerThan(firstHcn.getFactor())) {
                log.debug("inserting at {}", firstNotSmaller);
                bodiesWaitingToJoin.add(firstNotSmaller, newActiveBody);
            } else {
                log.debug("appending at {}", firstNotSmaller);
                if (firstHcn.getValue().isSmallerThan(bodiesWaitingToJoin.get(firstNotSmaller).getLastGeneratedHcn().getValue())) {
                    log.debug("replacing at {}", firstNotSmaller);
                    bodiesWaitingToJoin.add(firstNotSmaller, newActiveBody);
                } else {
                    for (int i = firstNotSmaller + 1; i < bodiesWaitingToJoin.size(); i++) {
                        log.debug("i={} value={}", i, bodiesWaitingToJoin.get(i).getLastGeneratedHcn().getValue());
                        if (bodiesWaitingToJoin.get(i).getLastGeneratedHcn().getValue().isBiggerThan(firstHcn.getValue())) {
                            log.debug("inserting at {}", i - 1);
                            bodiesWaitingToJoin.add(i - 1, newActiveBody);
                        }
                    }
                }
            }
        }
    }

    /*
    public static void newSmallestActiveBodyMaintain() {

        log.debug(" before newSmallestActiveBodyMaintain" + print());
        log.debug(" hunters={}", firstRecorder.getHunters());
        Body toRemove = firstRecorder;

        firstRecorder.setFirstDominatedHcn(firstRecorder.getLastGeneratedHcn());
        HcnGeneratorList.remove(firstRecorder);
        firstRecorder.deactivate();
        Body newFirstRecorder = firstRecorder.getNextRecorder();
        newFirstRecorder.setPreviousRecorder(null);
        firstRecorder = newFirstRecorder;
        toRemove.getHunters().forEach(hunter -> {
            log.debug("hunters={}", hunter.getLastGeneratedHcn());
            hunter.setPrayBody(null);
            findPrayForHunterBody(hunter.getLastGeneratedHcn());
        });
        log.debug(" after newSmallestActiveBodyMaintain" + print());
    }

     */
}
