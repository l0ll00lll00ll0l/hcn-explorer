package com.hcn.newCore;

import lombok.experimental.SuperBuilder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

@Getter
@Setter
@SuperBuilder
@Slf4j
public class RecorderBody implements Comparable<RecorderBody> {

    private static RecorderBody firstRecorder;
    public static RecorderBody getFirstRecorder() {return firstRecorder;}

    public static void initialize() {
        firstRecorder = RecorderBody.builder().hcnGenerator(HcnGenerator.getFirstHcnGenerator()).intervalLapiDistance(0).order(1).build();
        RecorderBody secondRecorder = RecorderBody.builder().hcnGenerator(HcnGenerator.getFirstHcnGenerator().getNextHcnGenerator()).intervalLapiDistance(0).order(2).build();
        HcnGenerator.getFirstHcnGenerator().setRecorderBody(firstRecorder);
        HcnGenerator.getFirstHcnGenerator().getNextHcnGenerator().setRecorderBody(secondRecorder);
        firstRecorder.setNextRecorder(secondRecorder);
        secondRecorder.setPreviousRecorder(firstRecorder);
        firstRecorder.setPreviousRecorder(secondRecorder);
        secondRecorder.setNextRecorder(firstRecorder);

        firstRecorder.getRecorderHcn(Prime.getFirstPrime(), HcnStatus.FIRST_RECORDER);
        secondRecorder.getRecorderHcn(Prime.getFirstPrime(), HcnStatus.FIRST_RECORDER);

        firstRecorder.getRecorderHcn(Prime.getFirstPrime().getNextPrime().calculateNextPrimeIfNeeded(), HcnStatus.RECORDER_IN_PASSINGZONE);
        HcnGenerator thirdGenerator = secondRecorder.getHcnGenerator().getNextHcnGenerator();
        Hcn hcn31 = thirdGenerator.generateReferencedHcn(Prime.getFirstPrime().getNextPrime(), firstRecorder, 1);
        firstRecorder.setPassingHcn(hcn31);
        firstRecorder.getHunters().put(1, hcn31);

        secondRecorder.getRecorderHcn(Prime.getFirstPrime().getNextPrime().getNextPrime(), HcnStatus.RECORDER_IN_PASSINGZONE);
        HcnGenerator fourthGenerator = thirdGenerator.getNextHcnGenerator();
        Hcn hcn22 = fourthGenerator.generateReferencedHcn(Prime.getFirstPrime().getNextPrime(), secondRecorder, 1);
        secondRecorder.setPassingHcn(hcn22);
        secondRecorder.getHunters().put(1, hcn22);

        Passing.setLowestPassing(new Passing(null, null, firstRecorder));
        Passing secondPassing = new Passing(null, Passing.getLowestPassing(), secondRecorder);
        Passing.getLowestPassing().setNextPassing(secondPassing);

        firstRecorder.setPassing(Passing.getLowestPassing());
        secondRecorder.setPassing(secondPassing);
    }

    public static void printRecordlist() {
        //log.debug("RecorderList:");
        firstRecorder.print();
        RecorderBody walker = firstRecorder.nextRecorder;
        while (!walker.equals(firstRecorder)) {
            walker.print();
            walker = walker.nextRecorder;
        }
    }

    private int intervalLapiDistance;
    private Integer order;
    private RecorderBody previousRecorder;
    private RecorderBody nextRecorder;
    private final List<RecorderBody> recorderHunters = new ArrayList<>();
    private Hcn prayHcn;
    private final List<Hcn> generatedRecorderHcns = new ArrayList<>();
    private final HcnGenerator hcnGenerator;

    public RecorderBody(Hcn triggerHcn) {
        triggerHcn.setHcnStatus(HcnStatus.FIRST_RECORDER);
        hcnGenerator = triggerHcn.getHcnGenerator();
        nextRecorder = triggerHcn.getPrayBody();
        previousRecorder = nextRecorder.getPreviousRecorder();
        nextRecorder.setPreviousRecorder(this);
        previousRecorder.setNextRecorder(this);
        intervalLapiDistance = triggerHcn.getIntervalDistance() + (nextRecorder.equals(firstRecorder) ? -1 : 0);
        hcnGenerator.setRecorderBody(this);
        //log.debug("creating new Passing Zone for {}", triggerHcn);
        getRecorderHcn(triggerHcn.getLastActivePrime(), HcnStatus.FIRST_RECORDER);
        this.order = previousRecorder.order + 1;

        RecorderBody recorderToRaiseOrderNumber = nextRecorder;
        while (recorderToRaiseOrderNumber != firstRecorder) {
            recorderToRaiseOrderNumber.order++;
            recorderToRaiseOrderNumber = recorderToRaiseOrderNumber.nextRecorder;
        }


        createPassingZone(initiateHcnGeneratorsBasedOnPreviousPassingZone());
        //log.debug("RecorderBody NEW at {}: {}", Matrix.getPassingCounter(), this);
    }

    public void resetReferencedHuntersFromPreviousRecorderBody(Hcn hcn, Hcn prayHcn) {
        //log.debug("RecorderBody passing again at {}: {}", Matrix.getPassingCounter(), this);
        hcn.setHcnStatus(HcnStatus.RECORDER_HUNTER);
        RecorderBody.printRecordlist();
        RecorderBody oldPrev = previousRecorder;
        RecorderBody oldNext = nextRecorder;
        //log.debug("RecorderBody passing again: oldPrev: {} - this - {} - oldNext: {}", oldPrev, this, oldNext);
        oldPrev.setNextRecorder(oldNext);
        oldNext.setPreviousRecorder(oldPrev);

        RecorderBody prayBody = prayHcn.getHcnGenerator().getRecorderBody();
        RecorderBody newNext = prayBody;
        RecorderBody newPrev = prayBody.previousRecorder;

        //log.debug("RecorderBody passing again: newPrev: {} - this - {} - newNext: {}", newPrev, this, newNext);

        newNext.setPreviousRecorder(this);
        this.nextRecorder = newNext;
        newPrev.setNextRecorder(this);
        this.previousRecorder = newPrev;

        this.order = previousRecorder.order + 1;
        if (prayBody.equals(firstRecorder)) {
            decreaseIntervalLapiDistances();
            RecorderBody recorderJummpedOver = firstRecorder.nextRecorder;
            while (!recorderJummpedOver.equals(oldNext)) {
                recorderJummpedOver.order = recorderJummpedOver.previousRecorder.order + 1;
                recorderJummpedOver = recorderJummpedOver.nextRecorder;
            }
            while (recorderJummpedOver != firstRecorder) {
                recorderJummpedOver.order--;
                recorderJummpedOver = recorderJummpedOver.nextRecorder;
            }
            //log.debug("1");
            RecorderBody.printRecordlist();
        } else {
            RecorderBody recorderToShiftOrderNumber = nextRecorder;
            while (recorderToShiftOrderNumber != oldNext) {
                recorderToShiftOrderNumber.order = recorderToShiftOrderNumber.previousRecorder.order + 1;
                recorderToShiftOrderNumber = recorderToShiftOrderNumber.nextRecorder;
            }
            //log.debug("2");
            RecorderBody.printRecordlist();
        }

        //log.debug("@@: it is HAPPENING ");
        Passing.removePassingFromStaticChain(passing);
        createPassingZone(initiateHcnGeneratorsBasedOnPreviousPassingZone());
    }

    @Override
    public String toString() {
        return order + " - " + hcnGenerator.getBody().toString() + " intervalLapiDistance: " + intervalLapiDistance;
    }

    public void print() {
        //log.debug("{}", this);
        hunters.entrySet().forEach(entry -> {
            //log.debug("  [{}] - {}", entry.getKey(), entry.getValue());
        });
    }

    public Hcn getRecorderHcn(Prime prime, HcnStatus status) {
        prayHcn = hcnGenerator.getHcn(prime, status, null, intervalLapiDistance);
        generatedRecorderHcns.add(prayHcn);
        return prayHcn;
    }

    public void findPrayCheck(HcnGenerator hunter, int intervalDistance) {
        int distDiff = intervalDistance - this.intervalLapiDistance;
        Prime hunterPrime = prayHcn.getLastActivePrime().getPreviousPrime(distDiff);
        ScientificNumber hunterValue = hunter.getHcnValue(hunterPrime);
        ScientificNumber hunterFactor = hunter.getHcnFactor(hunterPrime);
        ScientificNumber prayFactor = prayHcn.getFactor();
        //log.debug(" #{} findPrayCheck at {} hunterValue: {} intervalDistance: {}", intervalDistance, prayHcn, hunterValue, intervalDistance);

        placeHunterIntoActivePassingZone(intervalDistance, hunterValue, hunter,  hunterPrime);


        //log.debug("  @ Passing? prayFactor: {}, hunterfactor: {}", prayFactor, hunterFactor);
        if (prayFactor.isSmallerThan(hunterFactor)) {
            if (nextRecorder.equals(firstRecorder)) {
                //log.debug("# intervalDistance raised to {}", intervalDistance);
                intervalDistance++;
            }
            //log.debug(" calling nextRecorder: {}", nextRecorder);
            nextRecorder.findPrayCheck(hunter, intervalDistance);
        }
    }

    public void placeHunterIntoActivePassingZone(int intervalDistance, ScientificNumber hunterValue, HcnGenerator hunter, Prime hunterPrime) {

        if (!hunters.containsKey(intervalDistance)) {
            hunters.put(intervalDistance, hunter.generateReferencedHcn(hunterPrime,this, intervalDistance));
            //log.debug("   NEW: {} for prayHcn: {}, hunterHcn: {}", intervalDistance, prayHcn, hunters.get(intervalDistance));
            if (hunterValue.isSmallerThan(passingHcn.getValue())) {
                findSmallestPassingHcn(intervalDistance, hunterPrime, hunterValue, hunter);
            }
        } else {
            Hcn currentHunterHcn = hunters.get(intervalDistance);
            if (hunterValue.isSmallerThan(currentHunterHcn.getValue())) {
                if (hunterValue.isSmallerThan(passingHcn.getValue())) {
                    findSmallestPassingHcn(intervalDistance, hunterPrime, hunterValue, hunter);
                } else {
                    hunters.get(intervalDistance).removeHunterReference();
                    hunters.put(intervalDistance, hunter.generateReferencedHcn(hunterPrime,this, intervalDistance));
                }
                //log.debug("   {} REPLACED hunterHcn: {}",currentHunterHcn, hunters.get(intervalDistance));
            } else {
                //log.debug("   {} NOT replaced hunterValue: {}", currentHunterHcn, hunterValue);
            }
        }
    }

    private void findSmallestPassingHcn(int intervalDistance, Prime hunterPrime, ScientificNumber hunterValue, HcnGenerator hunter) {
        Prime prayPrimeWalker = prayHcn.getLastActivePrime();
        Prime hunterPrimeWalker = hunterPrime;
        ScientificNumber prayHcnValue = prayHcn.getValue();
        ScientificNumber hunterHcnValue = hunterValue;

        while (hunterHcnValue.isSmallerThan(prayHcnValue)) {
            prayPrimeWalker = prayPrimeWalker.getPreviousPrime();
            hunterPrimeWalker = hunterPrimeWalker.getPreviousPrime();
            prayHcnValue = hcnGenerator.getHcnValue(prayPrimeWalker);
            hunterHcnValue = hunter.getHcnValue(hunterPrimeWalker);
        }

        if (prayPrimeWalker.getNextPrime().equals(prayHcn.getLastActivePrime())) {
            //log.debug(" NO findSmallestPassingHcn found for hunter: {} and pray: {}", passingHcn, prayHcn);
            hunters.get(intervalDistance).removeHunterReference();
            hunters.put(intervalDistance, hunter.generateReferencedHcn(hunterPrime,this, intervalDistance));
            passingHcn = hunters.get(intervalDistance);
            Passing.removePassingFromStaticChain(passing);
            Passing.placePassingInStaticChain(new Passing(this));
        } else {
            //log.debug(" YEEEEEEEEEEEEEES findSmallestPassingHcn found for hunter: {} and pray: {}", passingHcn, prayHcn);
            replaceHunterHcns(prayPrimeWalker.getNextPrime());
            hunters.get(intervalDistance).removeHunterReference();
            hunters.put(intervalDistance, hunter.generateReferencedHcn(hunterPrimeWalker.getNextPrime(),this, intervalDistance));
            passingHcn = hunters.get(intervalDistance);
            Passing.removePassingFromStaticChain(passing);
            Passing.placePassingInStaticChain(new Passing(this));
        }
    }

    private void replaceHunterHcns(Prime prayHcnPrime) {
        getRecorderHcn(prayHcnPrime, HcnStatus.RECORDER_IN_PASSINGZONE);
        for (Map.Entry<Integer, Hcn> entry : hunters.entrySet()) {
            prayHcnPrime = prayHcnPrime.getPreviousPrime();
            entry.getValue().removeHunterReference();
            entry.setValue(entry.getValue().getHcnGenerator().generateReferencedHcn(prayHcnPrime, this, entry.getKey()));
        }
    }

    public void deleteRecorderBody() {
        prayHcn.setHcnStatus(HcnStatus.RECORDER_DEACTIVATED);
        lowerLargerOrderNumbers();
        previousRecorder.setNextRecorder(nextRecorder);
        nextRecorder.setPreviousRecorder(previousRecorder);

        if (this.equals(firstRecorder)) {
            int recorderListSize = previousRecorder.order;
            int movingRecorderCount = 0;
            firstRecorder = nextRecorder;
            while (firstRecorder.getIntervalLapiDistance() > 0) {
                //potentialFirstRecorder.setIntervalLapiDistance(potentialFirstRecorder.getIntervalLapiDistance() - 1);
                //log.debug("Decreasing for deleteRecorderBody: {}", this.getHcnGenerator());
                firstRecorder.decreaseIntervalLapiDistances();
                firstRecorder.order = firstRecorder.previousRecorder.order;
                firstRecorder.lowerLargerOrderNumbers();
                firstRecorder = firstRecorder.getNextRecorder();
                movingRecorderCount++;
                //log.debug("delete");
                RecorderBody.printRecordlist();
            }
            if (movingRecorderCount > 0) {
                //resetOrderNumbersAfterMovingRecorders(recorderListSize, movingRecorderCount);
            }
        }


        previousRecorder = null;
        nextRecorder = null;
        hcnGenerator.getBody().deactivate();
        hunters.values().forEach(hunterHcn -> {
            hunterHcn.removeHunterReference();
            hunterHcn.setPrayBody(null);
        });
        hunters.clear();
        Passing.removePassingFromStaticChain(passing);
    }

    private void resetOrderNumbersAfterMovingRecorders(int recorderListSize, int movingRecorderCount) {
        firstRecorder.order = 1;
        RecorderBody recorderToShiftOrderNumber = firstRecorder.nextRecorder;
        while (recorderToShiftOrderNumber != firstRecorder) {
            recorderToShiftOrderNumber.order = recorderToShiftOrderNumber.order - movingRecorderCount;
            if (recorderToShiftOrderNumber.order > 1) {
                recorderToShiftOrderNumber.order = recorderToShiftOrderNumber.order + recorderListSize;
            }
            recorderToShiftOrderNumber = recorderToShiftOrderNumber.nextRecorder;
        }
    }

    private void lowerLargerOrderNumbers() {
        RecorderBody recorderToLowerOrderNumber = nextRecorder;
        while (recorderToLowerOrderNumber != firstRecorder) {
            recorderToLowerOrderNumber.order--;
            recorderToLowerOrderNumber = recorderToLowerOrderNumber.nextRecorder;
        }
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

    private TreeMap<Integer, HcnGenerator> initiateHcnGeneratorsBasedOnPreviousPassingZone() {

        //log.debug("&&&&&&&&&&&&&&&&&&&&&&&& createFirstPassingZone for prayHcn {} &&&&&&&&&&&&&&&&&&&&&&&&&", prayHcn);


        int intervalDistanceDiff = this == firstRecorder ? 1 : 0;
        //log.debug(" intervalDistanceDiff: {} previousRecorder.hunters.lastKey(): {}", intervalDistanceDiff, previousRecorder.hunters.lastKey());
        int requiredMaximalIntervalDistance = previousRecorder.hunters.lastKey() + intervalDistanceDiff;
        //log.debug(" previousRecorder: {}", previousRecorder);
        Prime previousBasePrime = previousRecorder.prayHcn.getLastActivePrime();
        TreeMap<Integer, HcnGenerator> hunterBodies2 = new TreeMap<>();

        //log.debug("  @ intervalLapiDistance: {} reviousRecorder.hunters.lastKey(): {}", intervalLapiDistance, previousRecorder.hunters.lastKey());

        for (int intervalDistanceKey = intervalLapiDistance + 1; intervalDistanceKey < previousRecorder.hunters.lastKey() + 1; intervalDistanceKey++) {
            //log.debug("  + intervalDistanceKey: {}", intervalDistanceKey);
            HcnGenerator hunterCandidate = getHunterCandidateForIntervalDistance(intervalDistanceKey);
            Prime hunterPrime = previousBasePrime.getPreviousPrime(intervalDistanceKey - previousRecorder.intervalLapiDistance);
            //log.debug("    hunterPrime: {}, previousBasePrime: {}", hunterPrime, previousBasePrime);
            ScientificNumber hunterFactor = hunterCandidate.getHcnFactor(hunterPrime);
            //log.debug("    prayfactor: {} hunterFactor: {}", previousRecorder.prayHcn.getFactor(), hunterFactor);

            while (hunterFactor.isNotBiggerThan(previousRecorder.prayHcn.getFactor())) {
                hunterCandidate = hunterCandidate.getNextHcnGenerator();
                if (hunterCandidate == null) {
                    break;
                } else {
                    hunterFactor = hunterCandidate.getHcnFactor(hunterPrime);
                    //log.debug("    hunterFactor: {}, hunterCandidate: {}", hunterFactor, hunterCandidate);
                }

            }
            if(hunterCandidate != null) {
                hunterBodies2.put(intervalDistanceKey, hunterCandidate);
            }
        }



        TreeMap<Integer, HcnGenerator> hunterBodies = new TreeMap<>();

        previousRecorder.hunters.keySet().forEach(prevIntervalDistance -> {
            //log.debug("  previousRecorder - prevIntervalDistance: {}, - prayBody.getIntervalLapiDistance(): {}", prevIntervalDistance, intervalLapiDistance);
            if (prevIntervalDistance > intervalLapiDistance) {
                //log.debug("  {} exists for prayBody {}, hunterHcn: {}", prevIntervalDistance, previousRecorder.getPrayHcn(),  previousRecorder.hunters.get(prevIntervalDistance));
                HcnGenerator hunterWalker = previousRecorder.hunters.get(prevIntervalDistance).getHcnGenerator();
                Prime hunterPrime = previousRecorder.hunters.get(prevIntervalDistance).getLastActivePrime();
                ScientificNumber hunterHcnFactor = hunterWalker.getHcnFactor(hunterPrime);
                ScientificNumber prayFactor = previousRecorder.getPrayHcn().getFactor();

                //log.debug("    * hunterWalker {} hunterPrime: {}, hunterHcnFactor: {}, prayFactor: {}", hunterWalker, hunterPrime, hunterHcnFactor, prayFactor);
                while (hunterHcnFactor.isNotBiggerThan(prayFactor)) {
                    hunterWalker = hunterWalker.getNextHcnGenerator();
                    if (hunterWalker == null) {
                        //log.info("hunterWalker is null interval Not added and returned: {} ", prevIntervalDistance);
                        //createFirstPassingZone(hunterBodies);
                        return;
                    }

                    hunterHcnFactor = hunterWalker.getHcnFactor(hunterPrime);
                    //log.debug("    - hunterWalker {} hunterPrime: {}, hunterHcnFactor: {}, prayFactor: {}", hunterWalker, hunterPrime, hunterHcnFactor, prayFactor);
                }
                hunterBodies.put(prevIntervalDistance, hunterWalker);
                //log.debug("  suitable hunterHcn generator found for intervaldistance {}:  {}", prevIntervalDistance, hunterWalker);
            }
        });
        //log.debug("  ");
        //log.debug("  hunterBodies: {}", hunterBodies);
        //log.debug(" hunterBodies2: {}", hunterBodies2);
        //log.debug("  ");
        return hunterBodies;
    }

    private HcnGenerator getHunterCandidateForIntervalDistance(int intervalDistanceKey) {
        RecorderBody hcnGeneratorProvider = previousRecorder;
        //log.debug("  hcnGeneratorProvider: {}", hcnGeneratorProvider);
        while (hcnGeneratorProvider.intervalLapiDistance > intervalDistanceKey) {
            hcnGeneratorProvider = hcnGeneratorProvider.previousRecorder;
            //log.debug("   provider had to be skipped: {}", hcnGeneratorProvider);
        }

        if (intervalDistanceKey == hcnGeneratorProvider.intervalLapiDistance) {
            //log.debug("   provider return itself: {}", hcnGeneratorProvider);
            return hcnGeneratorProvider.getHcnGenerator();
        } else {
            //log.debug("   provider returning for intervalDistanceKey {}, Hcn reference: {}", intervalDistanceKey, hcnGeneratorProvider.hunters.get(intervalDistanceKey).getHcnGenerator());
            return hcnGeneratorProvider.hunters.get(intervalDistanceKey).getHcnGenerator();
        }
    }

    private void createPassingZone(TreeMap<Integer, HcnGenerator> hunterBodies) {
        //log.debug(" && createPassingZone with hunterBodies {} --------------------------", hunterBodies);

        TreeMap<ScientificNumber, Integer> hunterValuesByIntervalDistance = new TreeMap<>();
        Prime basePrayPrimeBefore = prayHcn.getLastActivePrime();

        Prime basePrayPrimeAfter = identifyNextPassing(hunterBodies, basePrayPrimeBefore, hunterValuesByIntervalDistance);

        //log.debug("  hunterBodies: {}, hunters: {}", hunterBodies.keySet(), hunters.keySet());

        hunters.values().forEach(Hcn::removeHunterReference);

        if (hunterBodies.size() < hunters.size()) {
            //log.debug("  hunters.lastkey toRemove {}", hunters.lastEntry().getValue());
            hunters.remove(hunters.lastKey());
        }

        Prime innerHunterWalker = basePrayPrimeAfter;
        for (Iterator<Integer> iterator = hunterBodies.keySet().iterator(); iterator.hasNext(); ) {
            int intervalDistance = iterator.next();
            innerHunterWalker = innerHunterWalker.getPreviousPrime();
            hunters.put(intervalDistance, hunterBodies.get(intervalDistance).generateReferencedHcn(innerHunterWalker,this, intervalDistance));
            //log.debug("  hunterHcn: {}", hunters.get(intervalDistance));
        }

        passingHcn = hunters.get(hunterValuesByIntervalDistance.firstEntry().getValue());
        //log.debug("passingHcn: {}", passingHcn);
        //log.debug("");

        passing = new Passing(this);
        //Passing.placePassingInStaticChain(Passing.builder().lowestPassingHcn(passingHcn).build());
        //placePassingZoneInStaticChain(this);
    }

    private Prime checkThis(Prime hunterPrime, ScientificNumber hunterValue, HcnGenerator hunter) {

        Prime prayPrimeWalker = prayHcn.getLastActivePrime();
        Prime hunterPrimeWalker = hunterPrime;
        //log.debug("    % prayPrimeWalker: {} hunterPrimeWalker : {}", prayPrimeWalker, hunterPrimeWalker);
        ScientificNumber prayHcnValue = prayHcn.getValue();
        ScientificNumber hunterHcnValue = hunterValue;
        //log.debug("      prayHcnValue: {} hunterHcnValue : {}", prayHcnValue, hunterHcnValue);

        while (hunterHcnValue.isSmallerThan(prayHcnValue)) {
            prayPrimeWalker = prayPrimeWalker.getPreviousPrime();
            hunterPrimeWalker = hunterPrimeWalker.getPreviousPrime();
            //log.debug("      % prayPrimeWalker: {} hunterPrimeWalker : {}", prayPrimeWalker, hunterPrimeWalker);
            prayHcnValue = hcnGenerator.getHcnValue(prayPrimeWalker);
            hunterHcnValue = hunter.getHcnValue(hunterPrimeWalker);
            //log.debug("        prayHcnValue: {} hunterHcnValue : {}", prayHcnValue, hunterHcnValue);
        }
        return prayPrimeWalker.getNextPrime();
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

            //log.debug("  prayValue: {}, hunterValuesByIntervalDistance): {}", prayValue, hunterValuesByIntervalDistance);

            basePrayPrime = basePrayPrime.calculateNextPrimeIfNeeded();
        } while (prayValue.isSmallerThan(hunterValuesByIntervalDistance.firstKey()));

        basePrayPrime = basePrayPrime.getPreviousPrime();

        if (basePrayPrime.equals(prayHcn.getLastActivePrime())) {
            basePrayPrime = checkThis(basePrayPrime.getPreviousPrime(hunterValuesByIntervalDistance.firstEntry().getValue() - intervalLapiDistance),
                    hunterValuesByIntervalDistance.firstKey(), hunterBodies.get(hunterValuesByIntervalDistance.firstEntry().getValue()));
        }

        getRecorderHcn(basePrayPrime, HcnStatus.RECORDER_IN_PASSINGZONE);
        //passingZone.add(prayBody.getRecorderHcn(basePrayPrime, HcnStatus.RECORDER_IN_PASSINGZONE, this));
        return basePrayPrime;
    }

    public void decreaseIntervalLapiDistances() {
        intervalLapiDistance--;
        List<Integer> intervalLapiDistances = hunters.keySet().stream().toList();
        intervalLapiDistances.forEach(intervalLapiDistance -> {
            Hcn hunterHcn = hunters.remove(intervalLapiDistance);
            hunterHcn.setIntervalDistance(intervalLapiDistance - 1);
            hunters.put(intervalLapiDistance - 1, hunterHcn);
        });
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

            int intervalDistance = hunterHcn.getIntervalDistance() + (this.equals(firstRecorder) ? -1 : 0);
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
            hunters.put(hunterHcn.getIntervalDistance(), hunterWalker.generateReferencedHcn(prayHcn.getLastActivePrime()
                    .getPreviousPrime(hunterHcn.getIntervalDistance()),this, hunterHcn.getIntervalDistance()));
        }

    }

    public void generatePassingHcns(Passing currentPassing) {

        hunters.keySet().forEach(intervalDistance -> {
            Hcn hunterHcn = hunters.get(intervalDistance);
            ScientificNumber potentialPasserValue = hunterHcn.getValue();
            HcnGenerator potentialPasser = hunterHcn.getHcnGenerator();
            while (potentialPasserValue.isSmallerThan(prayHcn.getValue())) {
                Hcn passingCandidate = potentialPasser.generateTempHcn(hunterHcn.getLastActivePrime(), this, intervalDistance);
                currentPassing.getPassingHcns().add(passingCandidate);
                potentialPasser = potentialPasser.getNextHcnGenerator();
                potentialPasserValue = potentialPasser.getHcnValue(hunterHcn.getLastActivePrime());
            }
        });

        ScientificNumber factorLimit = new ScientificNumber(1, 0);
        Set<Hcn> notSuperiorHcns = new HashSet<>();
        for (Hcn passingCandidate : currentPassing.getPassingHcns()) {
            if (passingCandidate.getFactor().isBiggerThan(factorLimit)) {
                factorLimit = passingCandidate.getFactor();
            } else {
                //passingCandidate.setHcnStatus(HcnStatus.NONPASSING_HCN);
                notSuperiorHcns.add(passingCandidate);
            }
        }
        notSuperiorHcns.forEach(currentPassing.getPassingHcns()::remove);
    }

    @Override
    public int compareTo(RecorderBody other) {
        return order.compareTo(other.order);
    }
}
