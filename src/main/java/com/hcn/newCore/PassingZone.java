package com.hcn.newCore;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

@Getter
@Setter
@Slf4j
public class PassingZone {

    private static PassingZone globalNextPassingZone;
    public static void setGlobalNextPassingZone(PassingZone globalNextPassingZone) {PassingZone.globalNextPassingZone = globalNextPassingZone;}
    public static PassingZone getGlobalNextPassingZone() {return globalNextPassingZone;}

    public static void processNextPassingZone() {
        //log.debug("[PROCESS PASSING ZONE]]");
        RecorderList.setCurrentRecorder(globalNextPassingZone.getPrayBody());
        ScientificNumber previousRecorderFactor = new ScientificNumber(1, 0);


        for (Hcn hcn : globalNextPassingZone.getPassingZone()) {
            //log.debug("processNextPassingZone - {}", hcn);
            if (hcn.getHcnStatus().equals(HcnStatus.HUNTER_IN_PASSINGZONE)) {

                //globalNextPassingZone.removeFromHunters(hcn.getHcnGenerator());
                if (hcn.getFactor().isBiggerThan(previousRecorderFactor)) {
                    //log.info(" matrixMaintainCheck ofr hcn: {}", hcn);
                    hcn.getHcnGenerator().getBody().matrixMaintainCheck();
                    RecorderBody newRecorderBody = new RecorderBody(hcn);
                    previousRecorderFactor = hcn.getFactor();
                } else {
                    //TODO hunter switching pray implementation required
                    //log.debug("hunter switching pray implementation required :{}", activityHcn);
                }
            } else if (hcn.getHcnStatus() == HcnStatus.RECORDER_IN_PASSINGZONE) {
                //PassingZone passingZone = PassingZone.getGlobalNextPassingZone();
                if (hcn.getFactor().isBiggerThan(previousRecorderFactor)) {
                    globalNextPassingZone.createPassingZoneAfterSurvival();
                    //globalNextPassingZone.prayBodySurvivedCreateNewPassingZone();
                    previousRecorderFactor = hcn.getFactor();
                } else {
                    //log.info(" deleteRecorderBody hcn: {}", hcn);
                    hcn.getHcnGenerator().getRecorderBody().deleteRecorderBody(hcn);
                    //RecorderList.recorderToDeactivate(hcn);
                    //globalNextPassingZone.prayBodyDeactivated();
                }
            }
        }
        //log.debug("");
    }

    public static void placePassingZoneInStaticChain(PassingZone passingZoneToPlace) {

        //log.debug("   :::::::::::::::::::  placePassingZoneInStaticChain {}  :::::::::::::::::::: ", passingZoneToPlace.passingZone);

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

    private PassingZone previousPassingZone;
    private PassingZone nextPassingZone;
    private final TreeSet<Hcn> passingZone = new TreeSet<>();
    //private final TreeMap<Integer, TreeSet<HcnGenerator>> hunters = new TreeMap<>();
    private final TreeMap<Integer, Hcn> hunters = new TreeMap<>();
    private RecorderBody prayBody;
    
    public PassingZone(RecorderBody recorderBody) {
        this.prayBody = recorderBody;
    }

    public PassingZone(Hcn triggerHcn, RecorderBody recorderBody) {
        this.prayBody = recorderBody;
        this.prayBody.getRecorderHcn(triggerHcn.getLastActivePrime(), HcnStatus.FIRST_RECORDER, this);
        createPassingZone(initiateHcnGeneratorsBasedOnPreviousPassingZone());
    }

    private void createPassingZoneAfterSurvival() {
        //log.debug("&&&&&&&&&&&&&&&&&&&&&&&& createPassingZoneAfterSurvival for prayHcn {} &&&&&&&&&&&&&&&&&&&&&&&&&", prayBody.getPrayHcn());
        prayBody.getPrayHcn().setHcnStatus(HcnStatus.RECORDER_SURVIVED);
        removeFromStaticChain();
        createPassingZone(initiateHcnGeneratorsBasedOnLocalPassingZone());
    }

    private TreeMap<Integer, HcnGenerator> initiateHcnGeneratorsBasedOnPreviousPassingZone() {

        //log.debug("&&&&&&&&&&&&&&&&&&&&&&&& createFirstPassingZone for prayHcn {} &&&&&&&&&&&&&&&&&&&&&&&&&", prayBody.getPrayHcn());
        TreeMap<Integer, HcnGenerator> hunterBodies = new TreeMap<>();

        PassingZone previousRecorderPassingZone = prayBody.getPreviousRecorder().getPassingZone();
        previousRecorderPassingZone.hunters.keySet().forEach(intervalDistance -> {
            //log.debug("  previousRecorderPassingZone - intervalDistance: {}, - prayBody.getIntervalLapiDistance(): {}", intervalDistance, prayBody.getIntervalLapiDistance());
            if (intervalDistance > prayBody.getIntervalLapiDistance()) {
                //log.info("  {} exists for prayBody {}, hunterHcn: {}", intervalDistance, previousRecorderPassingZone.prayBody.getPrayHcn(),  previousRecorderPassingZone.hunters.get(intervalDistance));
                HcnGenerator hunterWalker = previousRecorderPassingZone.hunters.get(intervalDistance).getHcnGenerator();
                Prime hunterPrime = previousRecorderPassingZone.hunters.get(intervalDistance).getLastActivePrime();
                ScientificNumber hunterHcnFactor = hunterWalker.getHcnFactor(hunterPrime);
                ScientificNumber prayFactor = previousRecorderPassingZone.getPrayBody().getPrayHcn().getFactor();

                //log.info("    * hunterWalker {} hunterPrime: {}, hunterHcnFactor: {}, prayFactor: {}", hunterWalker, hunterPrime, hunterHcnFactor, prayFactor);
                while (hunterHcnFactor.isNotBiggerThan(prayFactor)) {
                    hunterWalker = hunterWalker.getNextHcnGenerator();
                    if (hunterWalker == null) {
                        //log.info("hunterWalker is null interval Not added and returned: {} ", intervalDistance);
                        //createFirstPassingZone(hunterBodies);
                        return;
                    }

                    hunterHcnFactor = hunterWalker.getHcnFactor(hunterPrime);
                    //log.info("    - hunterWalker {} hunterPrime: {}, hunterHcnFactor: {}, prayFactor: {}", hunterWalker, hunterPrime, hunterHcnFactor, prayFactor);
                }
                hunterBodies.put(intervalDistance, hunterWalker);
                //log.debug("  suitable hunterHcn generator found for intervaldistance {}:  {}", intervalDistance, hunterWalker);
            }
        });
        //log.debug("  hunterBodies: {}", hunterBodies);
        return hunterBodies;
    }

    private TreeMap<Integer, HcnGenerator> initiateHcnGeneratorsBasedOnLocalPassingZone() {
        ScientificNumber factorTrigger = new ScientificNumber(1, 0);
        TreeMap<Integer, HcnGenerator> hunterBodies = new TreeMap<>();
        hunters.keySet().forEach(intervalDistance -> {
            hunterBodies.put(intervalDistance, hunters.get(intervalDistance).getHcnGenerator());
        });
        //log.debug("  hunterBodies: {}", hunterBodies);
        while (!passingZone.isEmpty()) {
            Hcn bufferHcn = passingZone.first();
            passingZone.remove(bufferHcn);
            if (bufferHcn.getHcnStatus().equals(HcnStatus.HUNTER_IN_PASSINGZONE)) {
                if (bufferHcn.getFactor().isBiggerThan(factorTrigger)) {
                    factorTrigger = bufferHcn.getFactor();
                }
                //log.debug("  factorTrigger: {}", factorTrigger);
                Prime hunterPrimeWalker = prayBody.getPrayHcn().getLastActivePrime();
                //log.debug("  hunterPrimeWalker: {}", hunterPrimeWalker);
                for (Map.Entry<Integer, HcnGenerator> entry : hunterBodies.entrySet()) {
                    hunterPrimeWalker = hunterPrimeWalker.getPreviousPrime();
                    //log.debug("  hunterPrimeWalker: {}", hunterPrimeWalker);
                    //log.info("  {} exists for prayBody {}, hunterHcn: {}", entry.getKey(), prayBody.getPrayHcn(), entry.getValue().getHunterHcn());
                    HcnGenerator hunterWalker = entry.getValue();
                    ScientificNumber hunterHcnFactor = hunterWalker.getHcnFactor(hunterPrimeWalker);
                    while (hunterWalker != null && hunterHcnFactor.isNotBiggerThan(factorTrigger)) {
                        hunterWalker = hunterWalker.getNextHcnGenerator();
                        if (hunterWalker != null) {
                            hunterHcnFactor = hunterWalker.getHcnFactor(hunterPrimeWalker);
                        }
                        //log.info("    - hunterWalker {} hunterPrimeWalker: {}, hunterHcnFactor: {}, prayFactor: {}", hunterWalker, hunterPrimeWalker, hunterHcnFactor, prayBody.getPrayHcn().getFactor());
                    }
                    //log.debug(" {} + hunterWalker: {}", entry.getKey(), hunterWalker);
                    entry.setValue(hunterWalker);
                }

                if (hunterBodies.lastEntry().getValue() == null) {
                    //log.debug("  *@& removed: {}",hunterBodies.lastKey());
                    hunterBodies.remove(hunterBodies.lastKey());
                }
            }
        }
        return hunterBodies;
    }

    private void createPassingZone(TreeMap<Integer, HcnGenerator> hunterBodies) {
        //log.debug(" && createPassingZone with hunterBodies {} --------------------------", hunterBodies);

        TreeMap<ScientificNumber, Integer> hunterValuesByIntervalDistance = new TreeMap<>();
        Prime basePrayPrime = prayBody.getPrayHcn().getLastActivePrime();

        basePrayPrime = identifyNextPassing(hunterBodies, basePrayPrime, hunterValuesByIntervalDistance);

        //log.debug("  hunterBodies: {}, hunters: {}", hunterBodies.keySet(), hunters.keySet());

        hunters.values().forEach(Hcn::deactivateHunter);

        if (hunterBodies.size() < hunters.size()) {
            //log.debug("  hunters.lastkey toRemove {}", hunters.lastEntry().getValue());
            hunters.remove(hunters.lastKey());
        }

        Prime innerHunterWalker = basePrayPrime;
        for (Iterator<Integer> iterator = hunterBodies.keySet().iterator(); iterator.hasNext(); ) {
            int intervalDistance = iterator.next();
            innerHunterWalker = innerHunterWalker.getPreviousPrime();
            hunters.put(intervalDistance, hunterBodies.get(intervalDistance)
                    .generateHunterHcn(innerHunterWalker, null, this, intervalDistance).activateHunter(HcnStatus.NONPASSING_HCN));
            //log.debug("  hunterHcn: {}", hunterHcn);
        }

        passingZone.add(hunters.get(hunterValuesByIntervalDistance.firstEntry().getValue()).activateHunter(HcnStatus.HUNTER_IN_PASSINGZONE));
        //log.debug("passingZone: {}", passingZone);
        //log.debug("");
        placePassingZoneInStaticChain(this);
    }

    private Prime identifyNextPassing(TreeMap<Integer, HcnGenerator> hunterBodies, Prime basePrayPrime, TreeMap<ScientificNumber, Integer> hunterValuesByIntervalDistance) {
        ScientificNumber prayValue;
        do {
            prayValue = prayBody.getHcnGenerator().getHcnValue(basePrayPrime);
            //log.debug("  prayValue: {} with : {}", prayValue, basePrayPrime);
            hunterValuesByIntervalDistance.clear();

            Prime innerHunterWalker = basePrayPrime;
            for (Iterator<Integer> iterator = hunterBodies.keySet().iterator(); iterator.hasNext(); ) {
                int intervalDistance = iterator.next();
                innerHunterWalker = innerHunterWalker.getPreviousPrime();
                ScientificNumber hunterValue = hunterBodies.get(intervalDistance).getHcnValue(innerHunterWalker);
                hunterValuesByIntervalDistance.put(hunterValue, intervalDistance);
            }

            //log.debug("  prayValue: {}, hunterValuesByIntervalDistance.firstKey(): {}", prayValue, hunterValuesByIntervalDistance.firstKey());

            basePrayPrime = basePrayPrime.calculateNextPrimeIfNeeded();
        } while (prayValue.isSmallerThan(hunterValuesByIntervalDistance.firstKey()));

        basePrayPrime = basePrayPrime.getPreviousPrime();
        passingZone.add(prayBody.getRecorderHcn(basePrayPrime, HcnStatus.RECORDER_IN_PASSINGZONE, this));
        return basePrayPrime;
    }

    public void prayBodyDeactivated() {

        //log.debug("------------------- prayBodyDeactivated ------------------");
        hunters.values().forEach(hunterHcn -> {
            hunterHcn.setPassingZone(null);
        });
        hunters.clear();
        prayBody = null;
        passingZone.clear();
        removeFromStaticChain();
        //log.debug("------------------- -------------------------- ------------------");
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
        List<Integer> intervalLapiDistances = hunters.keySet().stream().toList();
        intervalLapiDistances.forEach(intervalLapiDistance -> {
            Hcn hunterHcn = hunters.remove(intervalLapiDistance);
            hunters.put(intervalLapiDistance - 1, hunterHcn);
        });
    }

    public void placeHunterIntoActivePassingZone(Hcn hunterHcn, int intervalDistance) {

        if (!hunters.containsKey(intervalDistance)) {
            //log.debug("placeHunterIntoActivePassingZone befor:");
            //printHunters();
            hunters.put(intervalDistance, hunterHcn.activateHunter(HcnStatus.NONPASSING_HCN));
            //log.debug("  placeHunterIntoActivePassingZone new intervalDistance: {} for prayHcn: {}, hunterHcn: {} ------------------- ", intervalDistance, prayBody.getPrayHcn(), hunterHcn);
            //printHunters();
        } else {
            Hcn currentHunterHcn = hunters.get(intervalDistance);
            //log.debug("  currentHunterHcn: {}, hunterHcn: {}", currentHunterHcn, hunterHcn);
            //printHunters();
            if (hunterHcn.getValue().isSmallerThan(currentHunterHcn.getValue())) {
                //log.debug(" currentHunterHcn  for prayHcn: {}, REPLACED prev: {}, hunter: {}", prayBody.getPrayHcn(), currentHunterHcn, hunterHcn);
                hunters.get(intervalDistance).deactivateHunter();
                hunters.put(intervalDistance, hunterHcn.activateHunter(HcnStatus.NONPASSING_HCN));
            } else {
                //log.debug("  currentHunterHcn  for prayHcn: {}, NOT replaced prev: {}, hunter: {}", prayBody.getPrayHcn(), currentHunterHcn, hunterHcn);
            }
            //log.debug("currentHunterHcn after: {}", hunters.get(intervalDistance));
            //printHunters();
        }

        if (hunterHcn.getValue().isSmallerThan(prayBody.getPrayHcn().getValue())) {
            //TODO checkIfLowerHcnWouldGenerateNewPassingZone
            passingZone.add(hunterHcn.activateHunter(HcnStatus.HUNTER_IN_PASSINGZONE));
            //log.debug("  CHECK IMPL NEEDED hunterHcn: {}", hunterHcn);
        }
    }

    public void deleteHunterHcn(Hcn hunterHcn) {

        //log.debug(" hunterHcn: {}", hunters.get(hunterHcn.getIntervalDistance()));
        //hunters.put(hunterHcn.getIntervalDistance(), hunterHcn.getHcnGenerator().getNextHcnGenerator().generateHunterHcn(hunterHcn.getLastActivePrime(), HcnStatus.HUNTER_IN_PASSINGZONE, this, hunterHcn.getIntervalDistance()));

        if (hunterHcn.getHcnStatus() == HcnStatus.HUNTER_IN_PASSINGZONE) {
            if (passingZone.size() < 3) {
                log.error("THIS SHOULD NOT OCCURR I RECKON");
            }
            //log.debug(" deleted huntehHcn {} removed", hunterHcn);
            passingZone.remove(hunterHcn);
        }

        if (hunters.get(hunterHcn.getIntervalDistance()).equals(hunterHcn)) {

            PassingZone previousPassingZone = prayBody.getPreviousRecorder().getPassingZone();
            int intervalDistance = hunterHcn.getIntervalDistance() + (prayBody.equals(RecorderList.getFirstRecorder()) ? -1 : 0);
            ScientificNumber hunterFactor = previousPassingZone.hunters.get(intervalDistance).getFactor();
            Prime hunterPrime = previousPassingZone.hunters.get(intervalDistance).getLastActivePrime();
            HcnGenerator hunterWalker = previousPassingZone.hunters.get(intervalDistance).getHcnGenerator();
            //log.debug(" find new hunterHcn hunterFactor:  {}, hunterWalker: {} limit: {}", hunterFactor, hunterWalker, previousPassingZone.prayBody.getPrayHcn().getFactor());
            while (hunterFactor.isNotBiggerThan(previousPassingZone.prayBody.getPrayHcn().getFactor())) {
                hunterWalker = hunterWalker.getNextHcnGenerator();
                hunterFactor = hunterWalker.getHcnFactor(hunterPrime);
                //log.debug(" deleted hunterFactor {} hunterWalker {}", hunterFactor, hunterWalker);
            }

            //log.debug("  hunterhcn: {}, lapiDist: {}", hunterHcn, hunterHcn.getIntervalDistance());
            // deactivate happens after this call
            hunters.put(hunterHcn.getIntervalDistance(), hunterWalker.generateHunterHcn(prayBody.getPrayHcn().getLastActivePrime()
                    .getPreviousPrime(hunterHcn.getIntervalDistance()), null, this, hunterHcn.getIntervalDistance()).activateHunter(HcnStatus.HUNTER_IN_PASSINGZONE));
        }

    }

    public void print() {
        for(int intervalDistance : hunters.keySet()) {
            //log.debug("    - {} - {}", intervalDistance, hunters.get(intervalDistance));
        }
    }
}
