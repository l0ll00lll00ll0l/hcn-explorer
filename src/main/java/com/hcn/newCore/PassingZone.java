package com.hcn.newCore;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

@Getter
@Setter
@Slf4j
public class PassingZone {

    private static PassingZone globalNextPassingZone;
    private PassingZone previousPassingZone;
    private PassingZone nextPassingZone;
    private final TreeSet<Hcn> passingZone = new TreeSet<>();
    private final TreeSet<Hcn> nonPassingHcns = new TreeSet<>();
    private RecorderBody prayBody;
    private Hcn prayHcn;

    public static void sortPassingZone(PassingZone passingZone) {

    }

    public PassingZone(RecorderBody prayBody, HunterBody hunter) {

        //log.debug("------------------- PassingZone(RecorderBody {}, HunterBody {}) ------------------", prayBody, hunter);
        boolean found = false;
        Prime hunterWalker = Prime.getFirstPrime();
        Prime prayWalker = hunterWalker.calculateNextPrimeIfNeeded(hunter.getIntervalLapiDistance() - prayBody.getIntervalLapiDistance());
        ScientificNumber hunterValue = hunter.getHcnValue(hunterWalker);
        ScientificNumber prayValue = prayBody.getHcnValue(prayWalker);

        while (!found) {
            //log.debug("calculatePassingZone prayWalker: {} - prayValue: {}, hunterValue: {}", prayWalker.getIndex(), prayValue, hunterValue);
            if (hunterValue.isSmallerThan(prayValue)) {
                found = true;
            } else {
                hunterWalker = hunterWalker.calculateNextPrimeIfNeeded();
                prayWalker = prayWalker.calculateNextPrimeIfNeeded();
                hunterValue = hunter.getHcnValue(hunterWalker);
                prayValue = prayBody.getHcnValue(prayWalker);
            }
        }

        //log.debug(" New method: hunterValue: {} prayValue: {} ({}-{})", hunterValue, prayValue, hunterWalker.getIndex(), prayWalker.getIndex());

        prayHcn = prayBody.getHcn(prayWalker);
        prayHcn.setHcnStatus(HcnStatus.RECORDER_IN_PASSINGZONE);
        Hcn hunterHcn = hunter.getHcn(hunterWalker);
        this.passingZone.add(hunterHcn);
        this.passingZone.add(prayHcn);
        this.prayBody = prayBody;

        placePassingZoneInStaticChain(this);

        if (hunterHcn.getHcnGenerator() instanceof HunterBody) {
            hunterHcn.setHcnStatus(HcnStatus.HUNTER_IN_PASSINGZONE);
        } else {
            hunterHcn.setHcnStatus(HcnStatus.RECORDER_IN_PASSINGZONE);
        }

        hunter.setPassingZone(this);
        hunter.setHunterHcn(hunterHcn);
        //log.debug("------------------- -------------------------- ------------------");
    }

    public static void placePassingZoneInStaticChain(PassingZone passingZoneToPlace) {

        log.debug("   :::::::::::::::::::  placePassingZoneInStaticChain {}  :::::::::::::::::::: ", passingZoneToPlace.prayHcn);

        ScientificNumber pzKey = passingZoneToPlace.passingZone.first().getValue();

        PassingZone firstLargerPassingZone = globalNextPassingZone;
        PassingZone previousPassingZone = null;
        while (firstLargerPassingZone != null
                && firstLargerPassingZone.getPassingZone().first().getValue().isSmallerThan(pzKey)) {
            previousPassingZone = firstLargerPassingZone;
            firstLargerPassingZone = firstLargerPassingZone.nextPassingZone;
        }

        if (previousPassingZone != null) {
            ScientificNumber previousPassingZoneEnd = previousPassingZone.passingZone.last().getValue();
            //log.debug("Floor PZ check: new entry: {}, previousPassingZoneEnd: {}", pzKey, previousPassingZoneEnd);
            if (previousPassingZoneEnd.isBiggerThan(pzKey)) {
                //log.debug(" PassingZoneMerge Required implementation");
            } else {
                //log.debug(" check: OK");
            }
            previousPassingZone.setNextPassingZone(passingZoneToPlace);
        }

        if (firstLargerPassingZone != null) {
            ScientificNumber nextPassingZoneStart = firstLargerPassingZone.passingZone.first().getValue();
            //log.debug("Ceiling PZ check: new entry passingZoneToPlace.passingZone.last(): {}, nextPassingZoneStart: {}", passingZoneToPlace.passingZone.last(), nextPassingZoneStart);
            if (nextPassingZoneStart.isSmallerThan(passingZoneToPlace.passingZone.last().getValue())) {
                //log.debug(" PassingZoneMerge Required implementation");
            } else {
                //log.debug(" check: OK");
            }
            if (firstLargerPassingZone.equals(globalNextPassingZone)) {
                globalNextPassingZone = passingZoneToPlace;
            }
            firstLargerPassingZone.setPreviousPassingZone(passingZoneToPlace);
        }

        passingZoneToPlace.previousPassingZone = previousPassingZone;
        passingZoneToPlace.nextPassingZone = firstLargerPassingZone;
    }



    public static void setGlobalNextPassingZone(PassingZone globalNextPassingZone) {
        PassingZone.globalNextPassingZone = globalNextPassingZone;
    }

    public static PassingZone getGlobalNextPassingZone() {
        return globalNextPassingZone;
    }

    public void placeHunter(HunterBody hunter) {
        //log.debug("------------------- placeHunter ------------------");
        //log.debug("passingZone: {}", passingZone);
        //log.debug("recorderHcn: {}", prayHcn);
        if (!passingZone.isEmpty()) {
            Prime hunterPrime = prayHcn.getLastActivePrime().getPreviousPrime(hunter.getIntervalLapiDistance() - prayBody.getIntervalLapiDistance());
            Hcn hunterHcn = hunter.getHcn(hunterPrime);

            if (hunterHcn.getValue().isSmallerThan(prayHcn.getValue())) {

                /*
                ScientificNumber hunterHcnValue = hunterHcn.getValue();
                ScientificNumber prayHcnValue = prayHcn.getValue();
                while (hunterHcnValue.isSmallerThan(prayHcnValue)) {
                }
                 */
                passingZone.add(hunterHcn);
                hunterHcn.setHcnStatus(HcnStatus.HUNTER_IN_PASSINGZONE);
                //log.debug("calculatePassingZone hunterHcn: {}, passingZone: {}", hunterHcn, passingZone);
            } else {
                nonPassingHcns.add(hunterHcn);
                hunterHcn.setHcnStatus(HcnStatus.NONPASSING_HCN);
                //log.debug("calculatePassingZone hunterHcn: {}, nonPassingHcns: {}", hunterHcn, nonPassingHcns);
            }
            hunter.setPassingZone(this);
            hunter.setHunterHcn(hunterHcn);
            //log.debug("hcn placed: {}", hunterHcn);
        } else {
            //log.debug("hunter not placed: {}", hunter);
        }


        //log.debug("------------------- -------------------------- ------------------");
    }

    public static void processNextPassingZone() {
        //log.debug("------------------- processNextPassingZone ------------------");
        PassingZone passingZone = globalNextPassingZone;

        RecorderList.setCurrentRecorder(passingZone.getPrayBody());
        ScientificNumber previousRecorderFactor = new ScientificNumber(1, 0);
        for (Hcn hcn : passingZone.getPassingZone()) {
            log.debug("processNextPassingZone - {}", hcn);
            previousRecorderFactor = RecorderList.processActivityHcn(hcn, previousRecorderFactor);
        }

        //log.debug("------------------- -------------------------- ------------------");
    }

    public void prayBodySurvivedCreateNewPassingZone() {
        //log.debug("------------------- prayBodySurvivedCreateNewPassingZone ------------------");
        passingZone.clear();
        if (!nonPassingHcns.isEmpty()) {
            //log.debug("nonPassingHcns: {}", nonPassingHcns);
        } else {
            //log.debug("empty nonPassingHcns");
        }

        //Hcn potentialPassingHunterHcn = nonPassingHcns.first();
        //nonPassingHcns.remove(potentialPassingHunterHcn);
        //placeHunter((HunterBody) potentialPassingHunterHcn.getHcnGenerator());

        while (nonPassingHcns.first().getValue().isNotSmallerThan(prayHcn.getValue())) {
            //log.debug(" 2prayHcn: {}", prayHcn);
            Set<Hcn> newHcns = new HashSet<Hcn>();
            nonPassingHcns.stream().forEach(nonPassingHcn -> {
                newHcns.add(nonPassingHcn.getHcnGenerator().getHcn(nonPassingHcn.getLastActivePrime().calculateNextPrimeIfNeeded()));
            });
            //log.debug(" 2newHcns: {}", newHcns);
            nonPassingHcns.clear();
            nonPassingHcns.addAll(newHcns);
            prayHcn = prayHcn.getHcnGenerator().getHcn(prayHcn.getLastActivePrime().calculateNextPrimeIfNeeded());
            //log.debug(" 2nonPassingHcns: {}", nonPassingHcns);
        }

        while (nonPassingHcns.first().getValue().isSmallerThan(prayHcn.getValue())) {
            passingZone.add(nonPassingHcns.first());
            nonPassingHcns.first().setHcnStatus(HcnStatus.HUNTER_IN_PASSINGZONE);
            nonPassingHcns.remove(nonPassingHcns.first());
        }

        nonPassingHcns.forEach(hcn -> hcn.setHcnStatus(HcnStatus.NONPASSING_HCN));

        prayHcn.setHcnStatus(HcnStatus.RECORDER_IN_PASSINGZONE);
        passingZone.add(prayHcn);
        removeFromStaticChain();
        placePassingZoneInStaticChain(this);

        //log.debug("passingZone: {}", passingZone);
        passingZone.forEach(recorderHcn -> {
            //log.debug("recorderHcn: {} passingZone: {}", recorderHcn, recorderHcn.getHcnGenerator().getBody().getRecorderBody().getPassingZone());
        });
        //log.debug("------------------- -------------------------- ------------------");
    }

    public void prayBodyDeactivated() {

        //log.debug("------------------- prayBodyDeactivated ------------------");
        if (!nonPassingHcns.isEmpty()) {
            //log.debug("nonPassingHcns: {}", nonPassingHcns);
        } else {
            //log.debug("empty nonPassingHcns");
        }

        prayBody.setPassingZone(null);
        prayBody = null;
        //log.debug("passingZone: {}", passingZone);
        passingZone.forEach(recorderHcn -> {
            //log.debug("recorderHcn: {} passingZone: {}", recorderHcn, recorderHcn.getHcnGenerator().getBody().getRecorderBody().getPassingZone());
        });
        passingZone.clear();
        removeFromStaticChain();
        //log.debug("------------------- -------------------------- ------------------");
    }

    public void deleteHunterBodyFromBodyList(HunterBody hunterBody) {
        //log.debug("------------------- deleteHunterBodyFromBodyList ------------------");
        if (hunterBody.getHunterHcn().getHcnStatus() == HcnStatus.NONPASSING_HCN) {
            //log.debug("delete NONPASSING_HCN HunterBody hunterBody: {}", hunterBody.getHunterHcn());
            nonPassingHcns.remove(hunterBody.getHunterHcn());
        }

        if (hunterBody.getHunterHcn().getHcnStatus() == HcnStatus.HUNTER_IN_PASSINGZONE) {
            if (passingZone.size() < 3) {
                if (nonPassingHcns.isEmpty()) {
                    //log.debug("delete passing zone required: {}", hunterBody.getHunterHcn());
                    removeFromStaticChain();
                    hunterBody.setPassingZone(null);
                    prayBody.setPassingZone(null);
                } else {
                    //log.debug("delete active HunterBody PassingZone reset required: {}", hunterBody.getHunterHcn());
                }

            } else {
                //log.debug("delete active HunterBody hunterBody: {}", hunterBody.getHunterHcn());
                passingZone.remove(hunterBody.getHunterHcn());
            }
        }
        hunterBody.getHunterHcn().setHcnStatus(HcnStatus.DELETION_FROM_BODYLIST);
        //log.debug("------------------- -------------------------- ------------------");
    }

    private void removeFromStaticChain() {

        log.debug("   -----_______------ removeFromStaticChain {}   ---------____________-------", prayHcn);
        if (this.equals(globalNextPassingZone)) {
            setGlobalNextPassingZone(nextPassingZone);
        }

        PassingZone prev = previousPassingZone;
        PassingZone next = nextPassingZone;

        previousPassingZone = null;
        nextPassingZone = null;

        if (prev != null) {
            prev.nextPassingZone = next;
        }
        if (next != null) {
            next.previousPassingZone = prev;
        }
    }

    public void decreaseIntervalLapiDistances() {
        nonPassingHcns.forEach(hcn -> {
            HunterBody hunter = (HunterBody) hcn.getHcnGenerator();
            hunter.setIntervalLapiDistance(hunter.getIntervalLapiDistance() - 1);
        });
        passingZone.forEach(hcn -> {
            if (hcn.getHcnStatus() == HcnStatus.HUNTER_IN_PASSINGZONE) {
                HunterBody hunter = (HunterBody) hcn.getHcnGenerator();
                hunter.setIntervalLapiDistance(hunter.getIntervalLapiDistance() - 1);
            }
        });
    }
}
