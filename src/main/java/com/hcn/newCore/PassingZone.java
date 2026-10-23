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

    private Hcn passingHcn;
    //private final TreeMap<Integer, TreeSet<HcnGenerator>> hunters = new TreeMap<>();
    private final TreeMap<Integer, Hcn> hunters = new TreeMap<>();
    private RecorderBody prayBody;
    private Passing passing;
    
    public PassingZone(RecorderBody recorderBody) {
        this.prayBody = recorderBody;
    }

    public PassingZone(Hcn triggerHcn, RecorderBody recorderBody) {
        this.prayBody = recorderBody;
        this.prayBody.getRecorderHcn(triggerHcn.getLastActivePrime(), HcnStatus.FIRST_RECORDER, this);
        createPassingZone(initiateHcnGeneratorsBasedOnPreviousPassingZone());
    }

    public void createPassingZoneAfterSurvival(Hcn factorReferenceHcn) {
        //log.debug("&&&&&&&&&&&&&&&&&&&&&&&& createPassingZoneAfterSurvival for prayHcn {} &&&&&&&&&&&&&&&&&&&&&&&&&", prayBody.getPrayHcn());
        prayBody.getPrayHcn().setHcnStatus(HcnStatus.RECORDER_SURVIVED);
        Passing.removePassingFromStaticChain(passing);
        //removeFromStaticChain();
        createPassingZone(initiateHcnGeneratorsBasedOnLocalPassingZone(factorReferenceHcn));
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

    private TreeMap<Integer, HcnGenerator> initiateHcnGeneratorsBasedOnLocalPassingZone(Hcn factorReferenceHcn) {
        TreeMap<Integer, HcnGenerator> hunterBodies = new TreeMap<>();
        hunters.keySet().forEach(intervalDistance -> {
            hunterBodies.put(intervalDistance, hunters.get(intervalDistance).getHcnGenerator());
        });
        //log.debug("  hunterBodies: {}", hunterBodies);


                //log.debug("  factorTrigger: {}", factorTrigger);
                Prime hunterPrimeWalker = prayBody.getPrayHcn().getLastActivePrime();
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
                    .generateHunterHcn(innerHunterWalker, HcnStatus.REFERENCED_HUNTER, this, intervalDistance).activateHunter(HcnStatus.REFERENCED_HUNTER));
            //log.debug("  hunterHcn: {}", hunters.get(intervalDistance));
        }

        passingHcn = hunters.get(hunterValuesByIntervalDistance.firstEntry().getValue());
        //log.debug("passingZone: {}", passingHcn);
        //log.debug("");
        passing = new Passing(passingHcn, prayBody);
        //Passing.placePassingInStaticChain(Passing.builder().lowestPassingHcn(passingHcn).build());
        //placePassingZoneInStaticChain(this);
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
        prayBody.getRecorderHcn(basePrayPrime, HcnStatus.RECORDER_IN_PASSINGZONE, this);
        //passingZone.add(prayBody.getRecorderHcn(basePrayPrime, HcnStatus.RECORDER_IN_PASSINGZONE, this));
        return basePrayPrime;
    }

    public void prayBodyDeactivated() {

        //log.debug("------------------- prayBodyDeactivated ------------------");
        hunters.values().forEach(hunterHcn -> {
            hunterHcn.deactivateHunter();
            hunterHcn.setPassingZone(null);
        });
        hunters.clear();
        prayBody = null;
        //passingZone.clear();
        Passing.removePassingFromStaticChain(passing);
        //removeFromStaticChain();
        //log.debug("------------------- -------------------------- ------------------");
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
                    .getPreviousPrime(hunterHcn.getIntervalDistance()), HcnStatus.REFERENCED_HUNTER, this, hunterHcn.getIntervalDistance()).activateHunter(HcnStatus.REFERENCED_HUNTER));
        }

    }

    public void print() {
        for(int intervalDistance : hunters.keySet()) {
            //log.debug("    - {} - {}", intervalDistance, hunters.get(intervalDistance));
        }
    }

    public TreeSet<Hcn> generatePassingHcns() {
        TreeSet<Hcn> passingHcns = new TreeSet<>();
        hunters.keySet().forEach(intervalDistance -> {
            Hcn hunterHcn = hunters.get(intervalDistance);
            ScientificNumber potentialPasserValue = hunterHcn.getValue();
            HcnGenerator potentialPasser = hunterHcn.getHcnGenerator();
            while (potentialPasserValue.isSmallerThan(getPrayBody().getPrayHcn().getValue())) {
                Hcn passingHcn = potentialPasser.generateHunterHcn(hunterHcn.getLastActivePrime(), HcnStatus.HUNTER_IN_PASSINGZONE, this, intervalDistance);
                passingHcns.add(passingHcn);
                potentialPasser = potentialPasser.getNextHcnGenerator();
                potentialPasserValue = potentialPasser.getHcnValue(hunterHcn.getLastActivePrime());
            }
        });
        return passingHcns;
    }
}
