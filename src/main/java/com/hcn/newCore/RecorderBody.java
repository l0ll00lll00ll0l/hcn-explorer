package com.hcn.newCore;

import lombok.experimental.SuperBuilder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

@Getter
@Setter
@SuperBuilder
@Slf4j
public class RecorderBody {

    private int intervalLapiDistance;
    private RecorderBody previousRecorder;
    private RecorderBody nextRecorder;
    private final List<RecorderBody> recorderHunters = new ArrayList<>();
    private Hcn prayHcn;
    private RecorderBody prayBody = null;
    private final List<Hcn> generatedRecorderHcns = new ArrayList<>();
    private final HcnGenerator hcnGenerator;

    public RecorderBody(Hcn triggerHcn) {
        hcnGenerator = triggerHcn.getHcnGenerator();
        nextRecorder = triggerHcn.getPrayBody();
        previousRecorder = nextRecorder.getPreviousRecorder();
        nextRecorder.setPreviousRecorder(this);
        previousRecorder.setNextRecorder(this);
        intervalLapiDistance = triggerHcn.getIntervalDistance() + (nextRecorder.equals(RecorderList.getFirstRecorder()) ? -1 : 0);
        hcnGenerator.setRecorderBody(this);
        //log.debug("creating new Passing Zone for {}", triggerHcn);
        getRecorderHcn(triggerHcn.getLastActivePrime(), HcnStatus.FIRST_RECORDER);
        createPassingZone(initiateHcnGeneratorsBasedOnPreviousPassingZone());
    }

    @Override
    public String toString() {
        return hcnGenerator.getBody().toString() + " intervalLapiDistance: " + intervalLapiDistance;
    }

    public void print() {
        //log.debug("  {}", this);
        //passingZone.print();
    }

    public Hcn getRecorderHcn(Prime prime, HcnStatus status) {
        prayHcn = hcnGenerator.getHcn(prime, status, null, intervalLapiDistance);
        generatedRecorderHcns.add(prayHcn);
        return prayHcn;
    }

    public void findPrayCheck(HcnGenerator hunter, int intervalDistance) {
        int distDiff = intervalDistance - this.intervalLapiDistance;
        //log.debug("prayHcn {}", prayHcn);
        Prime hunterPrime = prayHcn.getLastActivePrime().getPreviousPrime(distDiff);
        //log.debug("hunterPrime {}", hunterPrime);
        Hcn hunterHcn = hunter.generateHunterHcn(hunterPrime, HcnStatus.HUNTER_DURING_FINDPRAY, this, intervalDistance);
        //log.debug(" # findPrayCheck distDiff: {} hunterPrime: {} hunterHcn: {} ########################", distDiff, hunterPrime, hunterHcn);

        placeHunterIntoActivePassingZone(hunterHcn, intervalDistance);

        if (prayHcn.getFactor().isSmallerThan(hunterHcn.getFactor())) {
            if (nextRecorder.equals(RecorderList.getFirstRecorder())) {
                //log.debug("# intervalDistance raised to {}", intervalDistance);
                intervalDistance++;
            }
            //log.debug(" calling nextRecorder: {}", nextRecorder);
            nextRecorder.findPrayCheck(hunter, intervalDistance);
        }
    }

    public void deleteRecorderBody() {
        prayHcn.setHcnStatus(HcnStatus.RECORDER_DEACTIVATED);
        previousRecorder.setNextRecorder(nextRecorder);
        nextRecorder.setPreviousRecorder(previousRecorder);
        if (this.equals(RecorderList.getFirstRecorder())) {
            RecorderBody potentialFirstRecorder = nextRecorder;
            while (potentialFirstRecorder.getIntervalLapiDistance() > 0) {
                //potentialFirstRecorder.setIntervalLapiDistance(potentialFirstRecorder.getIntervalLapiDistance() - 1);
                potentialFirstRecorder.decreaseIntervalLapiDistances();
                potentialFirstRecorder = potentialFirstRecorder.getNextRecorder();
            }
            RecorderList.setFirstRecorder(potentialFirstRecorder);

        }
        previousRecorder = null;
        nextRecorder = null;
        hcnGenerator.getBody().deactivate();
        hunters.values().forEach(hunterHcn -> {
            hunterHcn.deactivateHunter();
            hunterHcn.setPrayBody(null);
        });
        hunters.clear();
        Passing.removePassingFromStaticChain(passing);
    }

    // EX-PASSING_ZONE
    private Hcn passingHcn;
    private final TreeMap<Integer, Hcn> hunters = new TreeMap<>();
    private Passing passing;

    public void createPassingZoneAfterSurvival(Hcn factorReferenceHcn) {
        //log.debug("&&&&&&&&&&&&&&&&&&&&&&&& createPassingZoneAfterSurvival for prayHcn {} &&&&&&&&&&&&&&&&&&&&&&&&&", prayBody.getPrayHcn());
        prayHcn.setHcnStatus(HcnStatus.RECORDER_SURVIVED);
        Passing.removePassingFromStaticChain(passing);
        //removeFromStaticChain();
        createPassingZone(initiateHcnGeneratorsBasedOnLocalPassingZone(factorReferenceHcn));
    }

    private TreeMap<Integer, HcnGenerator> initiateHcnGeneratorsBasedOnPreviousPassingZone() {

        //log.debug("&&&&&&&&&&&&&&&&&&&&&&&& createFirstPassingZone for prayHcn {} &&&&&&&&&&&&&&&&&&&&&&&&&", prayBody.getPrayHcn());
        TreeMap<Integer, HcnGenerator> hunterBodies = new TreeMap<>();

        previousRecorder.hunters.keySet().forEach(prevIntervalDistance -> {
            //log.debug("  previousRecorder - prevIntervalDistance: {}, - prayBody.getIntervalLapiDistance(): {}", prevIntervalDistance, prayBody.getIntervalLapiDistance());
            if (prevIntervalDistance > intervalLapiDistance) {
                //log.info("  {} exists for prayBody {}, hunterHcn: {}", prevIntervalDistance, previousRecorder.prayBody.getPrayHcn(),  previousRecorder.hunters.get(prevIntervalDistance));
                HcnGenerator hunterWalker = previousRecorder.hunters.get(prevIntervalDistance).getHcnGenerator();
                Prime hunterPrime = previousRecorder.hunters.get(prevIntervalDistance).getLastActivePrime();
                ScientificNumber hunterHcnFactor = hunterWalker.getHcnFactor(hunterPrime);
                ScientificNumber prayFactor = previousRecorder.getPrayHcn().getFactor();

                //log.info("    * hunterWalker {} hunterPrime: {}, hunterHcnFactor: {}, prayFactor: {}", hunterWalker, hunterPrime, hunterHcnFactor, prayFactor);
                while (hunterHcnFactor.isNotBiggerThan(prayFactor)) {
                    hunterWalker = hunterWalker.getNextHcnGenerator();
                    if (hunterWalker == null) {
                        //log.info("hunterWalker is null interval Not added and returned: {} ", prevIntervalDistance);
                        //createFirstPassingZone(hunterBodies);
                        return;
                    }

                    hunterHcnFactor = hunterWalker.getHcnFactor(hunterPrime);
                    //log.info("    - hunterWalker {} hunterPrime: {}, hunterHcnFactor: {}, prayFactor: {}", hunterWalker, hunterPrime, hunterHcnFactor, prayFactor);
                }
                hunterBodies.put(prevIntervalDistance, hunterWalker);
                //log.debug("  suitable hunterHcn generator found for intervaldistance {}:  {}", prevIntervalDistance, hunterWalker);
            }
        });
        //log.debug("  hunterBodies: {}", hunterBodies);
        return hunterBodies;
    }

    private void createPassingZone(TreeMap<Integer, HcnGenerator> hunterBodies) {
        //log.debug(" && createPassingZone with hunterBodies {} --------------------------", hunterBodies);

        TreeMap<ScientificNumber, Integer> hunterValuesByIntervalDistance = new TreeMap<>();
        Prime basePrayPrime = prayHcn.getLastActivePrime();

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
                    .generateHunterHcn(innerHunterWalker, HcnStatus.REFERENCED_HUNTER, this, intervalDistance).activateHunter(HcnStatus.REFERENCED_HUNTER));
            //log.debug("  hunterHcn: {}", hunters.get(intervalDistance));
        }

        passingHcn = hunters.get(hunterValuesByIntervalDistance.firstEntry().getValue());
        //log.debug("passingZone: {}", passingHcn);
        //log.debug("");
        passing = new Passing(passingHcn, this);
        //Passing.placePassingInStaticChain(Passing.builder().lowestPassingHcn(passingHcn).build());
        //placePassingZoneInStaticChain(this);
    }

    private Prime identifyNextPassing(TreeMap<Integer, HcnGenerator> hunterBodies, Prime basePrayPrime, TreeMap<ScientificNumber, Integer> hunterValuesByIntervalDistance) {
        ScientificNumber prayValue;
        do {
            prayValue = hcnGenerator.getHcnValue(basePrayPrime);
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
        getRecorderHcn(basePrayPrime, HcnStatus.RECORDER_IN_PASSINGZONE);
        //passingZone.add(prayBody.getRecorderHcn(basePrayPrime, HcnStatus.RECORDER_IN_PASSINGZONE, this));
        return basePrayPrime;
    }

    private TreeMap<Integer, HcnGenerator> initiateHcnGeneratorsBasedOnLocalPassingZone(Hcn factorReferenceHcn) {
        TreeMap<Integer, HcnGenerator> hunterBodies = new TreeMap<>();
        hunters.keySet().forEach(intervalDistance -> {
            hunterBodies.put(intervalDistance, hunters.get(intervalDistance).getHcnGenerator());
        });
        //log.debug("  hunterBodies: {}", hunterBodies);


        //log.debug("  factorTrigger: {}", factorTrigger);
        Prime hunterPrimeWalker = prayHcn.getLastActivePrime();
        //log.debug("  hunterPrimeWalker: {}", hunterPrimeWalker);
        for (Map.Entry<Integer, HcnGenerator> entry : hunterBodies.entrySet()) {
            hunterPrimeWalker = hunterPrimeWalker.getPreviousPrime();
            //log.debug("  hunterPrimeWalker: {}", hunterPrimeWalker);
            //log.info("  {} exists for prayBody {}, hunterHcn: {}", entry.getKey(), prayBody.getPrayHcn(), entry.getValue().getHunterHcn());
            HcnGenerator hunterWalker = entry.getValue();
            ScientificNumber hunterHcnFactor = hunterWalker.getHcnFactor(hunterPrimeWalker);
            while (hunterWalker != null && hunterHcnFactor.isNotBiggerThan(factorReferenceHcn.getFactor())) {
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
        return hunterBodies;
    }

    public void decreaseIntervalLapiDistances() {
        intervalLapiDistance--;
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
            hunters.put(intervalDistance, hunterHcn.activateHunter(HcnStatus.REFERENCED_HUNTER));
            if (hunterHcn.getValue().isSmallerThan(passingHcn.getValue())) {
                passingHcn = hunterHcn;
            }
            //log.debug("  placeHunterIntoActivePassingZone new intervalDistance: {} for prayHcn: {}, hunterHcn: {} ------------------- ", intervalDistance, prayBody.getPrayHcn(), hunterHcn);
            //printHunters();
        } else {
            Hcn currentHunterHcn = hunters.get(intervalDistance);
            //log.debug("  currentHunterHcn: {}, hunterHcn: {}", currentHunterHcn, hunterHcn);
            //printHunters();
            if (hunterHcn.getValue().isSmallerThan(currentHunterHcn.getValue())) {
                //log.debug(" currentHunterHcn  for prayHcn: {}, REPLACED prev: {}, hunter: {}", prayBody.getPrayHcn(), currentHunterHcn, hunterHcn);
                hunters.get(intervalDistance).deactivateHunter();
                hunters.put(intervalDistance, hunterHcn.activateHunter(HcnStatus.REFERENCED_HUNTER));
                if (hunterHcn.getValue().isSmallerThan(passingHcn.getValue())) {
                    passingHcn = hunterHcn;
                }
            } else {
                //log.debug("  currentHunterHcn  for prayHcn: {}, NOT replaced prev: {}, hunter: {}", prayBody.getPrayHcn(), currentHunterHcn, hunterHcn);
            }
            //log.debug("currentHunterHcn after: {}", hunters.get(intervalDistance));
            //printHunters();
        }

        /*
        if (hunterHcn.getValue().isSmallerThan(prayBody.getPrayHcn().getValue())) {
            //TODO checkIfLowerHcnWouldGenerateNewPassingZone
            passingZone.add(hunterHcn.activateHunter(HcnStatus.HUNTER_IN_PASSINGZONE));
            //log.debug("  CHECK IMPL NEEDED hunterHcn: {}", hunterHcn);
        }

         */
    }

    public void deleteHunterHcn(Hcn hunterHcn) {

        //log.debug(" hunterHcn: {}", hunters.get(hunterHcn.getIntervalDistance()));
        //hunters.put(hunterHcn.getIntervalDistance(), hunterHcn.getHcnGenerator().getNextHcnGenerator().generateHunterHcn(hunterHcn.getLastActivePrime(), HcnStatus.HUNTER_IN_PASSINGZONE, this, hunterHcn.getIntervalDistance()));

        if (hunterHcn.equals(passingHcn)) {
            //if (passingZone.size() < 3) {
            log.error("THIS SHOULD NOT OCCURR I RECKON");
            //}
            //log.debug(" deleted huntehHcn {} removed", hunterHcn);
            //passingZone.remove(hunterHcn);
        }

        if (hunters.get(hunterHcn.getIntervalDistance()).equals(hunterHcn)) {

            int intervalDistance = hunterHcn.getIntervalDistance() + (this.equals(RecorderList.getFirstRecorder()) ? -1 : 0);
            ScientificNumber hunterFactor = previousRecorder.hunters.get(intervalDistance).getFactor();
            Prime hunterPrime = previousRecorder.hunters.get(intervalDistance).getLastActivePrime();
            HcnGenerator hunterWalker = previousRecorder.hunters.get(intervalDistance).getHcnGenerator();
            //log.debug(" find new hunterHcn hunterFactor:  {}, hunterWalker: {} limit: {}", hunterFactor, hunterWalker, previousPassingZone.prayBody.getPrayHcn().getFactor());
            while (hunterFactor.isNotBiggerThan(previousRecorder.getPrayHcn().getFactor())) {
                hunterWalker = hunterWalker.getNextHcnGenerator();
                hunterFactor = hunterWalker.getHcnFactor(hunterPrime);
                //log.debug(" deleted hunterFactor {} hunterWalker {}", hunterFactor, hunterWalker);
            }

            //log.debug("  hunterhcn: {}, lapiDist: {}", hunterHcn, hunterHcn.getIntervalDistance());
            // deactivate happens after this call
            hunters.put(hunterHcn.getIntervalDistance(), hunterWalker.generateHunterHcn(prayHcn.getLastActivePrime()
                    .getPreviousPrime(hunterHcn.getIntervalDistance()), HcnStatus.REFERENCED_HUNTER, this, hunterHcn.getIntervalDistance()).activateHunter(HcnStatus.REFERENCED_HUNTER));
        }

    }

    public TreeSet<Hcn> generatePassingHcns() {
        TreeSet<Hcn> passingHcns = new TreeSet<>();
        hunters.keySet().forEach(intervalDistance -> {
            Hcn hunterHcn = hunters.get(intervalDistance);
            ScientificNumber potentialPasserValue = hunterHcn.getValue();
            HcnGenerator potentialPasser = hunterHcn.getHcnGenerator();
            while (potentialPasserValue.isSmallerThan(prayHcn.getValue())) {
                Hcn passingHcn = potentialPasser.generateHunterHcn(hunterHcn.getLastActivePrime(), HcnStatus.HUNTER_IN_PASSINGZONE, this, intervalDistance);
                passingHcns.add(passingHcn);
                potentialPasser = potentialPasser.getNextHcnGenerator();
                potentialPasserValue = potentialPasser.getHcnValue(hunterHcn.getLastActivePrime());
            }
        });
        return passingHcns;
    }
}
