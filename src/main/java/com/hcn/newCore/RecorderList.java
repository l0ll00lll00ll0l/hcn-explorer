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

            sb.append("firstrecorder: "+ firstRecorder.getLastGeneratedHcn() +"[\n");

        sb.append("lastregorder:"+ lastRecorder.getLastGeneratedHcn() +"\n");
        //sb.append("firstrecorder next: "+ firstRecorder.getLastGeneratedHcn() +"[\n");
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

    private static int findBiggerPray(Hcn newRecorderHcn) {
        //log.debug(" newRecordHcn factor is BIGGER than currentRecorder");
        Body prayCandidate = Matrix.getCurrentRecorder();
        Integer lastHcnLapiDiff = 0;
        boolean prayFound = false;

        while (!prayFound) {
            ScientificNumber relativeFactor = getRelativeFactorForWalker(prayCandidate, lastHcnLapiDiff);

            if (relativeFactor.isNotSmallerThan(newRecorderHcn.getFactor())) {
                //log.debug("  relativeFactor bigger: {}", relativeFactor);
                prayFound = true;
            } else {
                //log.debug("  relativeFactoris still smaller: {}", relativeFactor);
                prayCandidate = prayCandidate.getNextRecorder();
                if (prayCandidate.getLastGeneratedHcn().getValue().isSmallerThan(prayCandidate.getPreviousRecorder().getLastGeneratedHcn().getValue())) {
                    lastHcnLapiDiff ++;
                    //log.debug("  lastHcnLapiDiff needs to be raised: {}", lastHcnLapiDiff);
                }
            }
        }
        newRecorderHcn.getBody().setPrayBody(prayCandidate);
        //log.debug("{} -> prayBody2: {}", newRecorderHcn, walker.getLastGeneratedHcn());
        return lastHcnLapiDiff;
    }

    private static int findSmallerPray(Hcn newRecorderHcn) {
        //log.debug("newRecordHcn factor is SMALLER than currentRecorder");

        Body walker = Matrix.getCurrentRecorder();
        Integer lastHcnLapiDiff = 0;
        boolean prayFound = false;
        Body lastHcnLapiDiffLowerTrigger = null;

        while (!prayFound) {
            ScientificNumber relativeFactor = getRelativeFactorForWalker(walker, lastHcnLapiDiff);

            if (relativeFactor.isSmallerThan(newRecorderHcn.getFactor())) {
                //log.debug("  relativeFactor smaller: {}", relativeFactor);
                prayFound = true;
            } else {
                //log.debug("  relativeFactoris still bigger: {}", relativeFactor);
                walker = walker.getPreviousRecorder();
                if (walker.getLastGeneratedHcn().getValue().isBiggerThan(walker.getNextRecorder().getLastGeneratedHcn().getValue())) {
                    lastHcnLapiDiff --;
                    lastHcnLapiDiffLowerTrigger = walker;
                    //log.debug("  lastHcnLapiDiff needs to be lowered: {}, lastHcnLapiDiffLowerTrigger: {}", lastHcnLapiDiff, lastHcnLapiDiffLowerTrigger);
                }
            }
        }
        newRecorderHcn.getBody().setPrayBody(walker.getNextRecorder());
        if (lastHcnLapiDiffLowerTrigger != null) {
            if (lastHcnLapiDiffLowerTrigger.equals(walker)) {
                return lastHcnLapiDiff + 1;
            }
        }
        //log.debug("{} -> prayBody3: {}", newRecorderHcn, walker.getNextRecorder().getLastGeneratedHcn());
        return lastHcnLapiDiff;
    }

    private static ScientificNumber getRelativeFactorForWalker(Body walker, Integer lastHcnLapiDiff) {
        ScientificNumber relativeFactorMultiplier = new ScientificNumber(Math.pow(2, lastHcnLapiDiff), 0);
        //log.debug("  relativeFactorMultiplier: {}", relativeFactorMultiplier);
        ScientificNumber relativeFactor = walker.getLastGeneratedHcn().getFactor().multiply(relativeFactorMultiplier);
        return relativeFactor;
    }

    public static void findPrayForHunterBody(Hcn newRecorderHcn) {

        //log.debug("");
        //log.debug("{} -> prayBody", newRecorderHcn);
        //log.debug("getCurrentRecorder: {}", Matrix.getCurrentRecorder().getLastGeneratedHcn());
        //log.debug("getProvedLimit: {}", Matrix.getProvedLimit());
        //log.debug("getTargetValue: {}", Matrix.getTargetValue());
        //log.debug(RecorderList.print());

        int lastHcnLapiDiff = 0;

        if (newRecorderHcn.getFactor().isBiggerThan(Matrix.getCurrentRecorder().getLastGeneratedHcn().getFactor())) {
            lastHcnLapiDiff = findBiggerPray(newRecorderHcn);
        } else if (newRecorderHcn.getFactor().isSmallerThan(Matrix.getCurrentRecorder().getLastGeneratedHcn().getFactor())) {
            lastHcnLapiDiff = findSmallerPray(newRecorderHcn);
        } else {
            newRecorderHcn.getBody().setPrayBody(Matrix.getCurrentRecorder());
            //log.debug("newRecordHcn factor EQUALS to currentRecorder");
            //log.debug("{} -> prayBody4: {}", newRecorderHcn, Matrix.getCurrentRecorder().getLastGeneratedHcn());
        }
        //log.debug(" ---------- ");
        //log.debug("");

        //log.debug("{} -> prayBody: {}", newRecorderHcn, newRecorderHcn.getBody().getPrayBody().getLastGeneratedHcn());
        //log.debug("newHcnindex: " + newRecorderHcn.getLapiIndex());
        //log.debug("prayindex: " + newRecorderHcn.getBody().getPrayBody().getLastGeneratedHcn().getLapiIndex());
        //log.debug("lastHcnLapiDiff: " + lastHcnLapiDiff);
        newRecorderHcn.getBody().getPrayBody().getHunters().add(newRecorderHcn.getBody());
        int prayLapiDiffToStore = newRecorderHcn.getBody().getPrayBody().getLastGeneratedHcn().getLapiIndex() - newRecorderHcn.getLapiIndex() + lastHcnLapiDiff;
        //log.debug("prayLapiDiffToStore: " + prayLapiDiffToStore);
        newRecorderHcn.getBody().setPrayLapiDistance(prayLapiDiffToStore);
    }

    public static void killBody(Body bodyToDelete) {
        //log.debug("  killBody bodyToDelete={}", bodyToDelete);
        //log.debug("deleted body {} has hunters: {}", bodyToDelete, bodyToDelete.getHunters());

        if (Lapi.getHighestLapi().getWalker().equals(bodyToDelete)) {
            //log.debug("    remove body={}: is highestLapiWlaker", bodyToDelete);
            Lapi.highestLapiWalkerDeleted();
        }

        if (Matrix.getCurrentRecorder().equals(bodyToDelete)) {
            Matrix.resetDeletedCurrentRecorder();
        }

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

        prev.setNextRecorder(next);
        next.setPreviousRecorder(prev);

        bodyToDelete.setPreviousRecorder(null);
        bodyToDelete.setNextRecorder(null);
        size--;
        bodyToDelete.setFirstDominatedHcn(bodyToDelete.getLastGeneratedHcn());
        HcnGeneratorList.remove(bodyToDelete);
        bodyToDelete.deactivate();

    }

    public static void addNewRecord(Hcn generatedHcn, boolean isPrayLapiDiffValid) {

        Body prayBody = generatedHcn.getBody().getPrayBody();
        Body referenceBody = prayBody.getPreviousRecorder();

        //log.debug("  addNewRecord referenceBody={}, generatedHcn: {}, nextHcn? {}", referenceHcn, generatedHcn, potentialNextHcn);
        generatedHcn.matrixMaintainCheck();
        prayBody.setPreviousRecorder(generatedHcn.getBody());
        generatedHcn.getBody().setNextRecorder(prayBody);

        referenceBody.setNextRecorder(generatedHcn.getBody());
        generatedHcn.getBody().setPreviousRecorder(referenceBody);
        if (lastRecorder.equals(referenceBody)) {
            lastRecorder = generatedHcn.getBody();
        }
        size++;

        generatedHcn.getBody().getPrayBody().getHunters().remove(generatedHcn.getBody());
        generatedHcn.getBody().setPrayBody(null);

        takeOverHuntersFromNextRecorder(generatedHcn, isPrayLapiDiffValid);
    }

    private static void takeOverHuntersFromNextRecorder(Hcn generatedHcn, boolean isPrayLapiDiffValid) {

        Body huntedBody = generatedHcn.getBody().getNextRecorder();

        final int relativeNextRecorderLapiDiff = isPrayLapiDiffValid ? 0 : 1;
        ScientificNumber nextHcnFactor = getRelativeFactorForWalker(generatedHcn.getBody().getNextRecorder(), relativeNextRecorderLapiDiff);

        if (!huntedBody.getLastGeneratedHcn().getBody().getHunters().isEmpty()) {
            log.debug("takeOverHuntersFromNextRecorder {}", generatedHcn);
            log.debug(" isPrayLapiDiffValid: {}", isPrayLapiDiffValid);
            log.debug(" generatedHcn: {}", generatedHcn.getFactor());
            log.debug(" nextHcnFactor: {}", nextHcnFactor);
        }

        huntedBody.getLastGeneratedHcn().getBody().getHunters().forEach(hunter -> {
            log.debug("   hunters to check:: {}", hunter.getLastGeneratedHcn());
            int storedLapiDiff = hunter.getPrayLapiDistance();
            log.debug("   storedLapiDiff: {}", storedLapiDiff);
            int actualDiff = huntedBody.getLastGeneratedHcn().getLapiIndex() - hunter.getLastGeneratedHcn().getLapiIndex();
            log.debug("   actualDiff: {}", actualDiff);
            int relativeHunterLapiDiff = relativeNextRecorderLapiDiff;
            if (storedLapiDiff != actualDiff) {
                log.debug("    VALID lapiDiff");
            } else {
                log.debug("    INVALID lapiDiff");
            }
        });

        if (nextHcnFactor.isNotBiggerThan(generatedHcn.getFactor())) {
            log.debug(" KillBody required generatedFactor: {}, nextHcnFactor: {}", generatedHcn.getFactor(), nextHcnFactor);
            killBody(generatedHcn.getBody().getNextRecorder());
        }

        log.debug(" ---------------------------------------------------------------------------------------------------------------------- ");
    }

}
