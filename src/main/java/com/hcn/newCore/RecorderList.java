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

    public static String print() {
        StringBuilder sb = new StringBuilder();

            sb.append("firstrecorder: "+ firstRecorder.getLastGeneratedHcn() +"[\n");

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

    public static void findPray(Body hunter) {

        log.debug("findPray: {}", hunter);
        log.debug("prime: {}", Prime.print());
        Body referenceCandidate = hunter.getSmallerHcnGenerator();
        log.debug("referenceCandidate: {}", referenceCandidate);
        Body prayCandidate = referenceCandidate.getPrayBody();
        log.debug("prayCandidate: {}", prayCandidate);
        log.debug("referenceCandidate.getPrayLapiDistance(): {}, prayCandidate.getIntervalLapiDistance(): {}", referenceCandidate.getPrayLapiDistance(), prayCandidate.getIntervalLapiDistance());
        int hunterPrimeDistance = referenceCandidate.getPrayLapiDistance() + prayCandidate.getIntervalLapiDistance();
        log.debug("hunterPrimeDistance: {}", hunterPrimeDistance);
        Prime hcnProducerPrime = Prime.getHcnProducerPrimes().get(hunterPrimeDistance);
        log.debug("hcnProducerPrime: {}, v: {}, f: {}", hcnProducerPrime, hcnProducerPrime.getValueMultiplier(), hcnProducerPrime.getFactorMultiplier());
        ScientificNumber hunterFactor = hunter.getHcnFactor(hcnProducerPrime);
        log.debug("hunterFactor: {}", hunterFactor);

        ScientificNumber prayFactor = prayCandidate.getHcnFactor(Prime.getPrimeByLapiDistance(prayCandidate.getIntervalLapiDistance()));
        ScientificNumber previousPrayFactor = null;
        log.debug("prayFactor: {}", prayFactor);

        Hcn currentPrayHcn = prayCandidate.getCurrentHcn();
        log.debug("currentPrayHcn: {}", currentPrayHcn);
        int counter = 0;
        int baseCycle = 0;
        //log.debug("intervalLapiDistanceFrom0: {}", intervalLapiDistanceFrom0);
        while (hunterFactor.isBiggerThan(prayFactor)) {

            prayCandidate = prayCandidate.getNextRecorder();
            log.debug("prayCandidate: {}", prayCandidate);
            prayFactor = prayCandidate.getHcnFactor(Prime.getPrimeByLapiDistance(prayCandidate.getIntervalLapiDistance() + baseCycle));

            if (previousPrayFactor != null) {
                if (previousPrayFactor.isBiggerThan(prayFactor)) {
                    baseCycle--;
                    log.debug("lapiDistance: {}", (prayCandidate.getIntervalLapiDistance() + baseCycle));
                    prayFactor = prayCandidate.getHcnFactor(Prime.getPrimeByLapiDistance(prayCandidate.getIntervalLapiDistance() + baseCycle));
                }
            }

            log.debug("prayFactor: {}", prayFactor);
            currentPrayHcn = prayCandidate.getCurrentHcn();
            log.debug("currentPrayHcn: {}", currentPrayHcn);
            previousPrayFactor = prayFactor;
            counter++;
            if (counter > 10) {
                log.debug("WARN");
                break;
            }
        }

        log.debug("hunterFactor: {}, prayFactor: {}", hunterFactor, prayFactor);


        hunter.setPrayBody(prayCandidate);
        log.debug("prayCandidate.getIntervalLapiDistance(): {}", prayCandidate.getIntervalLapiDistance());
        hunter.setPrayLapiDistance(hunterPrimeDistance - prayCandidate.getIntervalLapiDistance() - baseCycle);
        prayCandidate.getHunters().add(hunter);
    }


    public static void killCurrentRecorder() {
        //log.debug("  killBody bodyToDelete={}", bodyToDelete);
        //log.debug("deleted body {} has hunters: {}", bodyToDelete, bodyToDelete.getHunters());

        /*
        if (Lapi.getHighestLapi().getWalker().equals(bodyToDelete)) {
            //log.debug("    remove body={}: is highestLapiWlaker", bodyToDelete);
            Lapi.highestLapiWalkerDeleted();
        }



        if (Matrix.getLastProvedRecorder().equals(bodyToDelete)) {
            //Matrix.resetDeletedCurrentRecorder();
        }*/

        Body bodyToDelete = currentRecorder;

        Body prev = bodyToDelete.getPreviousRecorder();
        Body next = bodyToDelete.getNextRecorder();

        prev.setNextRecorder(next);
        next.setPreviousRecorder(prev);

        currentRecorder = prev;
        log.debug("killBody bodyToDelete={}", bodyToDelete);
        log.debug("  preve={}", prev);
        if (firstRecorder.equals(bodyToDelete)) {
            Body potentialFirstRecorder = firstRecorder.getNextRecorder();
            log.debug("  potentialFirstRecorder={}", potentialFirstRecorder);
            while (potentialFirstRecorder.getIntervalLapiDistance() > 0) {
                potentialFirstRecorder.setIntervalLapiDistance(potentialFirstRecorder.getIntervalLapiDistance() - 1);
                potentialFirstRecorder = potentialFirstRecorder.getNextRecorder();
                log.debug("  potentialFirstRecorder={}", potentialFirstRecorder);
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

        Body prayBody = newRecorderHcn.getBody().getPrayBody();
        Body referenceBody = prayBody.getPreviousRecorder();
        log.debug("  addNewRecord , referenceBody: {}, newRecorderHcn: {}, prayBody: {}", referenceBody, newRecorderHcn, prayBody);

        prayBody.setPreviousRecorder(newRecorderHcn.getBody());
        newRecorderHcn.getBody().setNextRecorder(prayBody);

        referenceBody.setNextRecorder(newRecorderHcn.getBody());
        newRecorderHcn.getBody().setPreviousRecorder(referenceBody);
        size++;

        newRecorderHcn.getBody().getPrayBody().getHunters().remove(newRecorderHcn.getBody());
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
        prayBody.getHunters().forEach(hunter -> {
            ScientificNumber hunterHcnFactor = hunter.getHcnFactor(Prime.getPrimeByLapiDistance(prayBody.getIntervalLapiDistance() + hunter.getPrayLapiDistance()));
            if (hunterHcnFactor.isNotBiggerThan(newRecorderHcn.getFactor())) {
                log.debug("  {} should be moved to newrecorder", hunter);
                toMove.add(hunter);
            } else {
                log.debug("  {} should not be moved", hunter);
            }
        });


        if (prayBody.equals(firstRecorder)) {
            toMove.forEach(hunter -> {
                prayBody.getHunters().remove(hunter);
                hunter.setPrayBody(newRecorderHcn.getBody());
                int distance = hunter.getPrayLapiDistance() + prayBody.getIntervalLapiDistance() - newRecorderHcn.getBody().getIntervalLapiDistance();
                hunter.setPrayLapiDistance(distance - 1);
                newRecorderHcn.getBody().getHunters().add(hunter);
            });
        } else {
            toMove.forEach(hunter -> {
                prayBody.getHunters().remove(hunter);
                hunter.setPrayBody(newRecorderHcn.getBody());
                int distance = hunter.getPrayLapiDistance() + prayBody.getIntervalLapiDistance() - newRecorderHcn.getBody().getIntervalLapiDistance();
                hunter.setPrayLapiDistance(distance);
                newRecorderHcn.getBody().getHunters().add(hunter);
            });
        }
    }

    public static void generateHcns() {
        currentRecorder = currentRecorder.getNextRecorder();
        log.debug("generateHcns {} ***************", currentRecorder);

        int requestedHighestLapiDiff  = currentRecorder.getIntervalLapiDistance();
        log.debug("requestedHighestLapiDiff {}", requestedHighestLapiDiff);

        Hcn recorderHcn = currentRecorder.generateHcn(Prime.getHcnProducerPrimes().get(requestedHighestLapiDiff));

        log.debug("hcn: {}", recorderHcn);

        ArrayList<Hcn> smallerHcns = new ArrayList<>();

        currentRecorder.getHunters().forEach(hunter -> {
            log.debug(" hunter: {}, getPrayLapiDistance: {}", hunter, hunter.getPrayLapiDistance());
            log.debug(" hcn producer prime: {}", Prime.getHcnProducerPrimes().get(requestedHighestLapiDiff + hunter.getPrayLapiDistance()));
            Hcn hunterHcn = hunter.generateHcn(Prime.getHcnProducerPrimes().get(requestedHighestLapiDiff + hunter.getPrayLapiDistance()));
            log.debug(" hunterHcn: {}", hunterHcn);
            if (hunter.getValue().isSmallerThan(recorderHcn.getValue())) {
                smallerHcns.add(hunterHcn);
            }
        });

        if (!smallerHcns.isEmpty()) {

            smallerHcns.sort(Comparator.comparing(Hcn::getValue));
            Interval.addRecorderHcn(smallerHcns.get(0));
            RecorderList.addNewRecord(smallerHcns.get(0));
            //Interval.getCurrentInterval().setLastBaseIntervalBody(smallerHcns.get(0).getBody());
            if (smallerHcns.size() > 1) {

                log.debug("smallerHcns: {}", smallerHcns);

                for (int i = 1; i < smallerHcns.size(); i++) {
                    Hcn candidate = smallerHcns.get(i);
                    Hcn referenceHcn = smallerHcns.get(i - 1);

                    log.debug(" referenceHcn: {}", referenceHcn);
                    log.debug(" candidate: {}", candidate);
                    if (candidate.getFactor().isBiggerThan(referenceHcn.getFactor())) {
                        Interval.addRecorderHcn(candidate);
                        RecorderList.addNewRecord(candidate);
                    } else {
                        if (candidate.getLapiIndex() > referenceHcn.getLapiIndex()) {
                            currentRecorder.getPreviousRecorder().getHunters().add(smallerHcns.get(i).getBody());
                            smallerHcns.get(i).getBody().setPrayBody(currentRecorder.getPreviousRecorder());
                        } else {
                            log.warn("NEEDS TO BE IMPLEMENTED, HERE candidate SHOULD BE DEACTIVATED");
                        }
                    }
                }
            }
            if (recorderHcn.getFactor().isNotBiggerThan(smallerHcns.get(smallerHcns.size() - 1).getFactor())) {
                killCurrentRecorder();
            } else {
                Interval.addRecorderHcn(recorderHcn);
            }
        } else {
            Interval.addRecorderHcn(recorderHcn);
            log.debug("recorderHcn added to hcnlist: {}", recorderHcn);
        }
        log.debug("");
    }


}
