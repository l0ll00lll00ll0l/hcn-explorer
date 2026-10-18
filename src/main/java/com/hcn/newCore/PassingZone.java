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
    private final TreeMap<Integer, TreeSet<HcnGenerator>> hunters = new TreeMap<>();
    private RecorderBody prayBody;
    
    public PassingZone(RecorderBody recorderBody) {
        this.prayBody = recorderBody;
    }

    public void placeFirstHunter(HcnGenerator hunter) {
        log.debug("------------------- placeFirstHunter(RecorderBody {}, HunterBody {}) ------------------", prayBody, hunter);
        //log.info("prayHcn: {}", prayBody.getPrayHcn());
        boolean found = false;

        Prime prayWalker = prayBody.getPrayHcn().getLastActivePrime();
        //log.info("prayWalker: {}", prayWalker.getIndex());

        //log.info("dist: {}", hunter.getIntervalLapiDistance() - prayBody.getIntervalLapiDistance());
        Prime hunterWalker = prayWalker.getPreviousPrime(hunter.getIntervalLapiDistance() - prayBody.getIntervalLapiDistance());
        //log.info("hunterWalker: {}", hunterWalker.getIndex());
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

        //log.info(" New method: hunterValue: {} prayValue: {} ({}-{})", hunterValue, prayValue, hunterWalker.getIndex(), prayWalker.getIndex());

        this.passingZone.add(prayBody.getRecorderHcn(prayWalker, HcnStatus.RECORDER_IN_PASSINGZONE));
        this.passingZone.add(hunter.generateHunterHcn(hunterWalker, HcnStatus.HUNTER_IN_PASSINGZONE));
        hunter.setHunterPassingZone(this);
        storeInHunters(hunter);

        placePassingZoneInStaticChain(this);
        //log.info("------------------- {} ------------------", passingZone);
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
        //log.debug("------------------- placeHunterWithExistingPassingZone ------------------");
        //log.debug("passingZone: {}", passingZone);
        //log.debug("getPrayHcn: {}", prayBody.getPrayHcn());

            Prime hunterPrime = prayBody.getPrayHcn().getLastActivePrime().getPreviousPrime(hunter.getIntervalLapiDistance() - prayBody.getIntervalLapiDistance());
        //log.debug("hunterPrime: {}", hunterPrime);
            ScientificNumber hunterValue = hunter.getHcnValue(hunterPrime);
        //log.debug("hunterValue: {}, getPrayHcn().getValue(): {}", hunterValue, prayBody.getPrayHcn().getValue());
            if (hunterValue.isSmallerThan(prayBody.getPrayHcn().getValue())) {

                checkIfLowerHcnWouldHuntedSooner(hunter, hunterPrime);
                hunter.setHunterPassingZone(this);
                //passingZone.add(hunter.generateHunterHcn(hunterPrime, HcnStatus.HUNTER_IN_PASSINGZONE));
                //log.debug("calculatePassingZone hunterHcn: {}, passingZone: {}", hunterHcn, passingZone);
            } else {
                hunter.generateHunterHcn(hunterPrime, HcnStatus.NONPASSING_HCN);
                hunter.setHunterPassingZone(this);
                //log.debug("calculatePassingZone hunterHcn: {}, nonPassingHcns: {}", hunterHcn, nonPassingHcns);
            }
            storeInHunters(hunter);
            //log.info("hcn placed: {}", hunterHcn);
        //log.debug("------------------- -------------------------- ------------------");
    }

    private void checkIfLowerHcnWouldHuntedSooner(HcnGenerator hunter, Prime hunterPrime) {
        //log.debug(" ------------------------------ checkIfLowerHcnWouldHuntedSooner prayHcn: {}, hunterHcn: {}  ------------------------------", prayBody.getPrayHcn(), hunter.getHunterHcn());

        Prime prayPrime = prayBody.getPrayHcn().getLastActivePrime().getPreviousPrime();
        hunterPrime = hunterPrime.getPreviousPrime();
        //log.debug("hunterPrime: {}", hunterPrime);
        ScientificNumber prayValue = prayBody.getPrayHcn().getHcnGenerator().getHcnValue(prayPrime);
        ScientificNumber hunterValue = hunter.getHcnValue(hunterPrime);
        //log.debug("hunterValue: {}, prayValue: {}", hunterValue, prayValue);
        boolean lowerPassingHunterHcnFound = false;

        log.debug("prayPrime: {} hunterPrime: {}, prayValue: {}, hunterValue: {}", prayPrime.getIndex(), hunterPrime, prayValue, hunterValue);
        while (prayValue.isBiggerThan(hunterValue)) {
            if (hunterPrime.getPreviousPrime() == null) {
                log.debug(" ------------------------------ lowerPassingHunterHcnFound no previous ------------------------------");
                return;
            }
            lowerPassingHunterHcnFound = true;
            hunterPrime = hunterPrime.getPreviousPrime();
            prayPrime = prayPrime.getPreviousPrime();
            hunterValue = hunter.getHcnValue(hunterPrime);
            prayValue = prayBody.getPrayHcn().getHcnGenerator().getHcnValue(prayPrime);
        }

        if (lowerPassingHunterHcnFound) {
            removeFromStaticChain();
            passingZone.clear();
            passingZone.add(prayBody.getRecorderHcn(prayPrime.getNextPrime(), HcnStatus.RECORDER_IN_PASSINGZONE));
            passingZone.add(hunter.generateHunterHcn(hunterPrime.getNextPrime(), HcnStatus.HUNTER_IN_PASSINGZONE));
            passingZoneResetLowerPrayHcnPrime();
            placePassingZoneInStaticChain(this);
        } else {
            passingZone.add(hunter.generateHunterHcn(prayBody.getPrayHcn().getLastActivePrime().getPreviousPrime(hunter.getIntervalLapiDistance() - prayBody.getIntervalLapiDistance()), HcnStatus.HUNTER_IN_PASSINGZONE));
            log.debug(" ------------------------------ lowerPassingHunterHcnFound was not detected ------------------------------");
        }
    }

    private void passingZoneResetLowerPrayHcnPrime() {
        log.debug(" ---------------- passingZoneResetLowerPrayHcnPrime oldPrayHcn: {}, new prayHcn: {}, newHunterHcn: {} -------------------", prayBody.getPrayHcn());

        Prime prayPrime = prayBody.getPrayHcn().getLastActivePrime();
        for (int intervalDistance : hunters.keySet()) {
            hunters.get(intervalDistance).forEach(hcnGenerator -> {
                hcnGenerator.getHunterHcn().setHcnStatus(HcnStatus.DELETED_BY_PRAYPRIME_DECREASE);
                hcnGenerator.generateHunterHcn(prayPrime.getPreviousPrime(intervalDistance), HcnStatus.NONPASSING_HCN);
            });
        }
        log.debug(" ------------------------------ has to be implemented ------------------------------");
    }

    public static void processNextPassingZone() {
        //log.debug("------------------- processNextPassingZone ------------------");
        RecorderList.setCurrentRecorder(globalNextPassingZone.getPrayBody());
        ScientificNumber previousRecorderFactor = new ScientificNumber(1, 0);


        for (Hcn hcn : globalNextPassingZone.getPassingZone()) {
            //log.info("processNextPassingZone - {}", hcn);
            if (hcn.getHcnStatus().equals(HcnStatus.HUNTER_IN_PASSINGZONE)) {
                //log.info("act: {}", activityHcn);
                globalNextPassingZone.removeFromHunters(hcn.getHcnGenerator());
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
        removeFromStaticChain();
        passingZone.clear();
        Prime prayPrime = prayBody.getPrayHcn().getLastActivePrime();
        ScientificNumber prayValue;
        TreeMap<ScientificNumber, HcnGenerator> hunterValues = new TreeMap<>();

        do {
            hunterValues.clear();
            prayValue = prayBody.getHcnValue(prayPrime);
            //ScientificNumber smallestHunterHcn = hunters.firstEntry().getValue().first().getValue();
            for (int intervalDistance : hunters.keySet()) {
                HcnGenerator hcnGenerator = hunters.get(intervalDistance).first();
                ScientificNumber hunterValue = hcnGenerator.getHcnValue(prayPrime.getPreviousPrime(intervalDistance));
                hunterValues.put(hunterValue, hcnGenerator);
                //log.debug(" hunterValue: {}, hcnGenerator: {}", hunterValue, hcnGenerator);
            }
            //log.debug(" prayValue: {}, hunterValues.first: {}", prayValue, hunterValues.firstKey());
            prayPrime = prayPrime.calculateNextPrimeIfNeeded();
        } while (hunterValues.firstKey().isNotSmallerThan(prayValue));
        prayPrime = prayPrime.getPreviousPrime();
        passingZone.add(prayBody.getRecorderHcn(prayPrime, HcnStatus.RECORDER_IN_PASSINGZONE));
        //log.debug("prayBody.getPrayHcn(): {}", prayBody.getPrayHcn());

        for (int intervalDistance : hunters.keySet()) {
            for (HcnGenerator hcnGenerator : hunters.get(intervalDistance)) {
                Hcn hunterHcn = hcnGenerator.generateHunterHcn(prayPrime.getPreviousPrime(intervalDistance), HcnStatus.NONPASSING_HCN);
                //log.debug(" hcnGenerator.getHunterHcn(): {}, prayValue: {}", hcnGenerator.getHunterHcn(), prayValue);
                if (hunterHcn.getValue().isSmallerThan(prayValue)) {
                    hunterHcn.setHcnStatus(HcnStatus.HUNTER_IN_PASSINGZONE);
                    passingZone.add(hunterHcn);
                }
            }
        }
        placePassingZoneInStaticChain(this);
    }

    public void prayBodyDeactivated() {

        //log.info("------------------- prayBodyDeactivated ------------------");
        if (!hunters.isEmpty()) {
            //log.debug("nonPassingHcns: {}", hunters.keySet());
        } else {
            //log.debug("empty hunters");
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

        removeFromHunters(hunterBody);

        if (hunterBody.getHunterHcn().getHcnStatus() == HcnStatus.HUNTER_IN_PASSINGZONE) {
            if (passingZone.size() < 3) {
                if (hunters.isEmpty()) {
                    //log.info("delete passing zone required: {}", hunterBody.getHunterHcn());
                    removeFromStaticChain();
                    hunterBody.setHunterPassingZone(null);
                    passingZone.clear();
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

        Set<HcnGenerator> recorderHunterToRemove = new HashSet<>();
        for (int intervalDistance : hunters.keySet()) {
            hunters.get(intervalDistance).forEach(hcnGenerator -> {
                if (hcnGenerator.getHunterHcn().getHcnStatus() == HcnStatus.RECORDER_HUNTER) {
                    if (intervalDistance == 1) {
                        recorderHunterToRemove.add(hcnGenerator);
                    } else {
                        //log.debug("nonPassingRecorderHcn stays for pray: {} and nonPassingRecorderHcn: {}", prayBody.getPrayHcn(), hcnGenerator.getHunterHcn());
                    }
                } else {
                    hcnGenerator.setIntervalLapiDistance(intervalDistance - 1);
                }
            });
        }

        recorderHunterToRemove.forEach(hcnGenerator -> {
            hcnGenerator.setHunterHcn(null);
            removeFromHunters(hcnGenerator);
            //log.debug("relativeDistance is 0 for pray: {} and nonPassingRecorderHcn: {}", prayBody.getPrayHcn(), hcnGenerator);
        });
    }

    public void recorderHunterCheck() {

        RecorderBody prevBody = prayBody.getPreviousRecorder();
        if (prevBody.getIntervalLapiDistance() < prayBody.getIntervalLapiDistance()) {

            if (prayBody.equals(RecorderList.getFirstRecorder())) {
                //log.info(" PRAYBODY is firstRecorder");
            } else {
                //log.debug(" $$$$$$$$$$$$$$$$ regular recorderHunterCheck {}", prayBody.getPrayHcn());
            }

            int relativeDistance = prayBody.getIntervalLapiDistance() - prevBody.getIntervalLapiDistance();
            Prime requiredPrime = prevBody.getPrayHcn().getLastActivePrime().getPreviousPrime(relativeDistance);

            if (prayBody.generateHunterHcn(requiredPrime, HcnStatus.RECORDER_HUNTER).getValue().isSmallerThan(prevBody.getPrayHcn().getValue())) {
                //log.debug("  recorderHunterHcn beats pray: {}, recorderHunterHcn: {}", prevBody.getPrayHcn(), recorderHunterHcn);
            } else {
                //log.debug("  prevRecord holds: {}, recorderHunterHcn: {}", prevBody.getPrayHcn(), recorderHunterHcn);
                prevBody.getPassingZone().storeInHunters(prayBody);
            }

            //log.debug("  recorderHunterHcn: {}", recorderHunterHcn);
        } else {
            //log.debug(" negative recorderHunterCheck {}", prayBody.getPrayHcn());
        }
    }

    public void storeInHunters(HcnGenerator hcnGenerator) {
        int intervalDistance = hcnGenerator.getIntervalLapiDistance();
        if (!hunters.containsKey(intervalDistance)) {
            hunters.put(intervalDistance, new TreeSet<>());
        }
        hunters.get(intervalDistance).add(hcnGenerator);
    }

    public void removeFromHunters(HcnGenerator hcnGenerator) {
        int intervalDistance = hcnGenerator.getIntervalLapiDistance();
        hunters.get(intervalDistance).remove(hcnGenerator);
        if (hunters.get(intervalDistance).isEmpty()) {
            hunters.remove(intervalDistance);
        }
    }
}
