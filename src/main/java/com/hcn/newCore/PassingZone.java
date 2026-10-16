package com.hcn.newCore;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
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
    private final TreeSet<Hcn> nonPassingRecorderHcns = new TreeSet<>();
    private final TreeMap<Integer, TreeSet<Hcn>> hunters = new TreeMap<>();
    private RecorderBody prayBody;
    
    public PassingZone(RecorderBody recorderBody) {
        this.prayBody = recorderBody;
    }

    public void placeFirstHunter(HcnGenerator hunter) {
        log.info("------------------- placeFirstHunter(RecorderBody {}, HunterBody {}) ------------------", prayBody, hunter);
        //log.debug("prayHcn: {}", prayBody.getPrayHcn());
        boolean found = false;

        Prime prayWalker = prayBody.getPrayHcn().getLastActivePrime();
        //log.debug("prayWalker: {}", prayWalker.getIndex());

        //log.debug("dist: {}", hunter.getIntervalLapiDistance() - prayBody.getIntervalLapiDistance());
        Prime hunterWalker = prayWalker.getPreviousPrime(hunter.getIntervalLapiDistance() - prayBody.getIntervalLapiDistance());
        //log.debug("hunterWalker: {}", hunterWalker.getIndex());
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

        prayBody.setPrayHcn(prayBody.getHcn(prayWalker));
        prayBody.getPrayHcn().setHcnStatus(HcnStatus.RECORDER_IN_PASSINGZONE);
        Hcn hunterHcn = hunter.getHcn(hunterWalker);
        this.passingZone.add(hunterHcn);
        storeInHunters(hunterHcn);
        this.passingZone.add(prayBody.getPrayHcn());

        placePassingZoneInStaticChain(this);
        hunterHcn.setHcnStatus(HcnStatus.HUNTER_IN_PASSINGZONE);
        hunter.setHunterPassingZone(this);
        hunter.setHunterHcn(hunterHcn);
        //log.debug("------------------- {} ------------------", passingZone);
    }

    public static void placePassingZoneInStaticChain(PassingZone passingZoneToPlace) {

        //log.debug("   :::::::::::::::::::  placePassingZoneInStaticChain {}  :::::::::::::::::::: ", passingZoneToPlace.prayHcn);

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
    
    public void placeHunter2(HcnGenerator hunter) {
        if (!passingZone.isEmpty()) {
            placeHunterWithExistingPassingZone(hunter);
        } else {
            placeFirstHunter(hunter);
        }
    }

    public void placeHunterWithExistingPassingZone(HcnGenerator hunter) {
        log.debug("------------------- placeHunterWithExistingPassingZone ------------------");
        log.debug("passingZone: {}", passingZone);
        log.debug("recorderHcn: {}", prayBody.getPrayHcn());

            Prime hunterPrime = prayBody.getPrayHcn().getLastActivePrime().getPreviousPrime(hunter.getIntervalLapiDistance() - prayBody.getIntervalLapiDistance());
            Hcn hunterHcn = hunter.getHcn(hunterPrime);

            if (hunterHcn.getValue().isSmallerThan(prayBody.getPrayHcn().getValue())) {

                checkIfLowerHcnWouldHuntedSooner(hunterHcn);
                passingZone.add(hunterHcn);
                hunterHcn.setHcnStatus(HcnStatus.HUNTER_IN_PASSINGZONE);
                log.debug("calculatePassingZone hunterHcn: {}, passingZone: {}", hunterHcn, passingZone);
            } else {
                storeInHunters(hunterHcn);
                nonPassingHcns.add(hunterHcn);
                hunterHcn.setHcnStatus(HcnStatus.NONPASSING_HCN);
                log.debug("calculatePassingZone hunterHcn: {}, nonPassingHcns: {}", hunterHcn, nonPassingHcns);
            }
            hunter.setHunterPassingZone(this);
            hunter.setHunterHcn(hunterHcn);
            //log.info("hcn placed: {}", hunterHcn);
        //log.debug("------------------- -------------------------- ------------------");
    }

    private void checkIfLowerHcnWouldHuntedSooner(Hcn hunterHcn) {
        log.debug(" ------------------------------ checkIfLowerHcnWouldHuntedSooner prayHcn: {}, hunterHcn: {}  ------------------------------", prayBody.getPrayHcn(), hunterHcn);
        Prime hunterPrime = hunterHcn.getLastActivePrime().getPreviousPrime();
        Prime prayPrime = prayBody.getPrayHcn().getLastActivePrime().getPreviousPrime();
        ScientificNumber hunterValue = hunterHcn.getHcnGenerator().getHcnValue(hunterPrime);
        ScientificNumber prayValue = prayBody.getPrayHcn().getHcnGenerator().getHcnValue(prayPrime);
        boolean lowerPassingHunterHcnFound = false;

        while (prayValue.isSmallerThan(hunterValue)) {
            if (hunterPrime.getPreviousPrime() == null) {
                storeInHunters(hunterHcn);
                //log.debug(" ------------------------------ lowerPassingHunterHcnFound no previous ------------------------------");
                return;
            }
            lowerPassingHunterHcnFound = true;
            hunterPrime = hunterPrime.getPreviousPrime();
            prayPrime = prayPrime.getPreviousPrime();
            hunterValue = hunterHcn.getHcnGenerator().getHcnValue(hunterPrime);
            prayValue = prayBody.getPrayHcn().getHcnGenerator().getHcnValue(prayPrime);
        }

        if (lowerPassingHunterHcnFound) {
            passingZoneResetLowerPrayHcnPrime(prayBody.getPrayHcn().getHcnGenerator().getHcn(prayPrime.getNextPrime()), hunterHcn.getHcnGenerator().getHcn(hunterPrime));
        } else {
            storeInHunters(hunterHcn);
            log.debug(" ------------------------------ lowerPassingHunterHcnFound was not detected ------------------------------");
        }
    }

    private void passingZoneResetLowerPrayHcnPrime(Hcn newHunterHcn, Hcn newPrayHcn) {
        log.debug(" ---------------- passingZoneResetLowerPrayHcnPrime oldPrayHcn: {}, new prayHcn: {}, newHunterHcn: {} -------------------", prayBody.getPrayHcn(), newPrayHcn, newHunterHcn);
        storeInHunters(newPrayHcn);
        //log.debug(" ------------------------------ has to be implemented ------------------------------");
    }

    public static void processNextPassingZone() {
        //log.debug("------------------- processNextPassingZone ------------------");
        //PassingZone passingZone = globalNextPassingZone;

        RecorderList.setCurrentRecorder(globalNextPassingZone.getPrayBody());
        ScientificNumber previousRecorderFactor = new ScientificNumber(1, 0);
        for (Hcn hcn : globalNextPassingZone.getPassingZone()) {
            log.info("processNextPassingZone - {}", hcn);
            if (hcn.getHcnStatus().equals(HcnStatus.HUNTER_IN_PASSINGZONE)) {
                //log.info("act: {}", activityHcn);
                globalNextPassingZone.removeFromHunters(hcn);
                if (hcn.getFactor().isBiggerThan(previousRecorderFactor)) {
                    RecorderList.hunterToRecorder(hcn);
                    previousRecorderFactor = hcn.getFactor();
                } else {
                    //TODO hunter switching pray implementation required
                    //log.info("hunter switching pray implementation required :{}", activityHcn);
                }
            } else if (hcn.getHcnStatus() == HcnStatus.RECORDER_IN_PASSINGZONE) {
                //PassingZone passingZone = PassingZone.getGlobalNextPassingZone();
                if (hcn.getFactor().isBiggerThan(previousRecorderFactor)) {
                    hcn.setHcnStatus(HcnStatus.RECORDER_SURVIVED);
                    globalNextPassingZone.prayBodySurvivedCreateNewPassingZone();
                    previousRecorderFactor = hcn.getFactor();
                } else {
                    hcn.setHcnStatus(HcnStatus.RECORDER_DEACTIVATED);
                    RecorderList.recorderToDeactivate(hcn);
                    globalNextPassingZone.prayBodyDeactivated();
                }
            }
        }

        //log.debug("------------------- -------------------------- ------------------");
    }

    public void prayBodySurvivedCreateNewPassingZone() {
        //log.debug("------------------- prayBodySurvivedCreateNewPassingZone {} ------------------", prayBody.getPrayHcn());
        passingZone.clear();
        if (!nonPassingHcns.isEmpty()) {
            //log.debug("nonPassingHcns: {}", nonPassingHcns);
        } else {
            //log.debug("empty nonPassingHcns");
        }

        //Hcn potentialPassingHunterHcn = nonPassingHcns.first();
        //nonPassingHcns.remove(potentialPassingHunterHcn);
        //placeHunter((HunterBody) potentialPassingHunterHcn.getHcnGenerator());

        while (nonPassingHcns.first().getValue().isNotSmallerThan(prayBody.getPrayHcn().getValue())) {
            //log.debug(" 2prayHcn: {}", prayBody.getPrayHcn());
            Set<Hcn> newHcns = new HashSet<Hcn>();
            nonPassingHcns.stream().forEach(nonPassingHcn -> {
                newHcns.add(nonPassingHcn.getHcnGenerator().getHcn(nonPassingHcn.getLastActivePrime().calculateNextPrimeIfNeeded()));
            });
            //log.debug(" 2newHcns: {}", newHcns);
            nonPassingHcns.clear();
            nonPassingHcns.addAll(newHcns);
            prayBody.setPrayHcn(prayBody.getPrayHcn().getHcnGenerator().getHcn(prayBody.getPrayHcn().getLastActivePrime().calculateNextPrimeIfNeeded()));
            //prayHcn = prayHcn.getHcnGenerator().getHcn(prayHcn.getLastActivePrime().calculateNextPrimeIfNeeded());
            //log.debug(" prayHcn: {}, nonPassingHcns.first(): {}", prayBody.getPrayHcn(), nonPassingHcns.first());
        }

        while (!nonPassingHcns.isEmpty() && nonPassingHcns.first().getValue().isSmallerThan(prayBody.getPrayHcn().getValue())) {
            passingZone.add(nonPassingHcns.first());
            nonPassingHcns.first().setHcnStatus(HcnStatus.HUNTER_IN_PASSINGZONE);
            nonPassingHcns.first().getHcnGenerator().setHunterHcn(nonPassingHcns.first());
            nonPassingHcns.remove(nonPassingHcns.first());
        }

        nonPassingHcns.forEach(hcn -> {
            hcn.setHcnStatus(HcnStatus.NONPASSING_HCN);
            hcn.getHcnGenerator().setHunterHcn(hcn);
        });

        prayBody.getPrayHcn().setHcnStatus(HcnStatus.RECORDER_IN_PASSINGZONE);
        passingZone.add(prayBody.getPrayHcn());
        removeFromStaticChain();
        placePassingZoneInStaticChain(this);

        //log.debug("passingZone: {}", passingZone);
        passingZone.forEach(recorderHcn -> {
            //log.debug("recorderHcn: {} passingZone: {}", recorderHcn);
        });
        //log.debug("------------------- -------------------------- ------------------");
    }

    public void prayBodyDeactivated() {

        //log.info("------------------- prayBodyDeactivated ------------------");
        if (!nonPassingHcns.isEmpty()) {
            //log.debug("nonPassingHcns: {}", nonPassingHcns);
        } else {
            //log.debug("empty nonPassingHcns");
        }

        prayBody.setPassingZone(null);
        //log.info("passingZone 1: {}", prayBody);
        prayBody = null;
        //log.debug("passingZone: {}", passingZone);
        passingZone.forEach(recorderHcn -> {
            //log.debug("recorderHcn: {} passingZone: {}", recorderHcn, recorderHcn.getHcnGenerator().getBody().getRecorderBody().getPassingZone());
        });
        passingZone.clear();
        removeFromStaticChain();
        //log.debug("------------------- -------------------------- ------------------");
    }

    public void deleteHunterBodyFromBodyList(HcnGenerator hunterBody) {

        removeFromHunters(hunterBody.getHunterHcn());
        //log.info("------------------- deleteHunterBodyFromBodyList ------------------");
        if (hunterBody.getHunterHcn().getHcnStatus() == HcnStatus.NONPASSING_HCN) {
            //log.info("delete NONPASSING_HCN HunterBody hunterBody: {}", hunterBody.getHunterHcn());
            nonPassingHcns.remove(hunterBody.getHunterHcn());
        }

        if (hunterBody.getHunterHcn().getHcnStatus() == HcnStatus.HUNTER_IN_PASSINGZONE) {
            if (passingZone.size() < 3) {
                if (nonPassingHcns.isEmpty()) {
                    //log.info("delete passing zone required: {}", hunterBody.getHunterHcn());
                    removeFromStaticChain();
                    hunterBody.setHunterPassingZone(null);
                    passingZone.clear();
                    //prayBody.setPassingZone(null);
                    //log.info("passingZone 2: {}", prayBody);
                } else {
                    //log.info("delete active HunterBody PassingZone reset required: {}", hunterBody.getHunterHcn());
                }

            } else {
                //log.info("delete active HunterBody hunterBody: {}", hunterBody.getHunterHcn());
                passingZone.remove(hunterBody.getHunterHcn());
            }
        }
        hunterBody.getHunterHcn().setHcnStatus(HcnStatus.DELETION_FROM_BODYLIST);
        //log.info("------------------- -------------------------- ------------------");
    }

    private void removeFromStaticChain() {

        //log.debug("   -----_______------ removeFromStaticChain {}   ---------____________-------", prayHcn);
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
            HcnGenerator hunter = hcn.getHcnGenerator();
            hunter.setIntervalLapiDistance(hunter.getIntervalLapiDistance() - 1);
        });
        passingZone.forEach(hcn -> {
            if (hcn.getHcnStatus() == HcnStatus.HUNTER_IN_PASSINGZONE) {
                HcnGenerator hunter = hcn.getHcnGenerator();
                hunter.setIntervalLapiDistance(hunter.getIntervalLapiDistance() - 1);
            }
        });
        Set<Hcn> nonPassingRecorderHcnsToRemove = new HashSet<>();
        nonPassingRecorderHcns.forEach(nonPassingRecorderHcn -> {
            int relativeDistance = nonPassingRecorderHcn.getHcnGenerator().getIntervalLapiDistance() - prayBody.getIntervalLapiDistance();
            if (relativeDistance == 0) {
                nonPassingRecorderHcnsToRemove.add(nonPassingRecorderHcn);
            } else {
                log.debug("nonPassingRecorderHcn stays for pray: {} and nonPassingRecorderHcn: {}", prayBody.getPrayHcn(), nonPassingRecorderHcn);
            }
        });

        nonPassingRecorderHcnsToRemove.forEach(recorderHcn -> {
            recorderHcn.getHcnGenerator().setHunterHcn(null);
            nonPassingRecorderHcns.remove(recorderHcn);
            log.debug("relativeDistance is 0 for pray: {} and nonPassingRecorderHcn: {}", prayBody.getPrayHcn(), recorderHcn);
        });
    }

    public void recorderHunterCheck() {

        RecorderBody prevBody = prayBody.getPreviousRecorder();
        if (prevBody.getIntervalLapiDistance() < prayBody.getIntervalLapiDistance()) {

            if (prayBody.equals(RecorderList.getFirstRecorder())) {
                log.info(" PRAYBODY is firstRecorder");
            } else {
                log.debug(" $$$$$$$$$$$$$$$$ regular recorderHunterCheck {}", prayBody.getPrayHcn());
            }

            int relativeDistance = prayBody.getIntervalLapiDistance() - prevBody.getIntervalLapiDistance();
            Prime requiredPrime = prevBody.getPrayHcn().getLastActivePrime().getPreviousPrime(relativeDistance);
            Hcn recorderHunterHcn = prayBody.getHcn(requiredPrime);
            prayBody.setHunterHcn(recorderHunterHcn);

            if (recorderHunterHcn.getValue().isSmallerThan(prevBody.getPrayHcn().getValue())) {
                log.debug("  recorderHunterHcn beats pray: {}, recorderHunterHcn: {}", prevBody.getPrayHcn(), recorderHunterHcn);
            } else {
                log.debug("  prevRecord holds: {}, recorderHunterHcn: {}", prevBody.getPrayHcn(), recorderHunterHcn);
                nonPassingRecorderHcns.add(recorderHunterHcn);
            }

            log.debug("  recorderHunterHcn: {}", recorderHunterHcn);
        } else {
            log.debug(" negative recorderHunterCheck {}", prayBody.getPrayHcn());
        }
    }

    public void storeInHunters(Hcn hunterHcn) {
        int intervalDistance = hunterHcn.getHcnGenerator().getIntervalLapiDistance();
        if (!hunters.containsKey(intervalDistance)) {
            hunters.put(intervalDistance, new TreeSet<>());
        }
        hunters.get(intervalDistance).add(hunterHcn);
    }

    public void removeFromHunters(Hcn hunterHcn) {
        int intervalDistance = hunterHcn.getHcnGenerator().getIntervalLapiDistance();
        hunters.get(intervalDistance).remove(hunterHcn);
        if (hunters.get(intervalDistance).isEmpty()) {
            hunters.remove(intervalDistance);
        }
    }
}
