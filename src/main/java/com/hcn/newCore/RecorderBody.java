package com.hcn.newCore;

import com.hcn.newCore.interval.ActiveInterval;
import com.hcn.newCore.interval.ReferenceInterval;
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

@Getter
@Setter
@SuperBuilder
@Slf4j
public class RecorderBody implements Comparable<RecorderBody> {

    private static RecorderBody firstRecorder;
    public static RecorderBody getFirstRecorder() {return firstRecorder;}
    public static int getRecorderListSize() {
        return firstRecorder.previousRecorder.order;
    }

    public static void reset() { firstRecorder = null; }

    public static void initialize() {
        firstRecorder = RecorderBody.builder().hcnGenerator(HcnGenerator.getFirstHcnGenerator()).intervalLapiDistance(0).order(1).build();
        RecorderBody secondRecorder = RecorderBody.builder().hcnGenerator(HcnGenerator.getFirstHcnGenerator().getNextHcnGenerator()).intervalLapiDistance(0).order(2).build();
        HcnGenerator.getFirstHcnGenerator().setRecorderBody(firstRecorder);
        HcnGenerator.getFirstHcnGenerator().getNextHcnGenerator().setRecorderBody(secondRecorder);
        firstRecorder.setNextRecorder(secondRecorder);
        secondRecorder.setPreviousRecorder(firstRecorder);
        firstRecorder.setPreviousRecorder(secondRecorder);
        secondRecorder.setNextRecorder(firstRecorder);

        Hcn hcn1 = firstRecorder.getRecorderHcn(Prime.getFirstPrime(), HcnStatus.FIRST_RECORDER);
        hcn1.setOrder(1);
        ActiveInterval.getCurrentActiveInterval().getHcnProjections().add(hcn1);
        Hcn hcn2 = secondRecorder.getRecorderHcn(Prime.getFirstPrime(), HcnStatus.FIRST_RECORDER);
        hcn2.setOrder(2);
        ActiveInterval.getCurrentActiveInterval().getHcnProjections().add(hcn2);

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

        ReferenceInterval.getCurrentReferenceInterval().setLastHcnOrder(2);
        ReferenceInterval.getCurrentReferenceInterval().setLastActiveInterval(0);
        ReferenceInterval.getCurrentReferenceInterval().setLastActiveRecorder(secondRecorder);
        ActiveInterval.getCurrentActiveInterval().setFirstRecorder(firstRecorder);
    }

    private int intervalLapiDistance;
    private Integer order;
    private RecorderBody previousRecorder;
    private RecorderBody nextRecorder;
    private final List<RecorderBody> recorderHunters = new ArrayList<>();
    private Hcn prayHcn;
    private final List<Hcn> generatedRecorderHcns = new ArrayList<>();
    private final HcnGenerator hcnGenerator;
    private Hcn passingHcn;
    private final TreeMap<Integer, Hcn> hunters = new TreeMap<>();
    private Passing passing;

    @Override
    public int compareTo(RecorderBody other) {return order.compareTo(other.order);}
    @Override
    public String toString() {return order + " - " + hcnGenerator.getBody().toString() + " intervalLapiDistance: " + intervalLapiDistance;}

    public RecorderBody(Hcn triggerHcn) {

        hcnGenerator = triggerHcn.getHcnGenerator();
        hcnGenerator.setRecorderBody(this);
        hcnGenerator.getBody().setProvedRecursive();
        HcnGenerator.createHcnGenerators(this);
        intervalLapiDistance = triggerHcn.getIntervalDistance() + (triggerHcn.getPrayBody().equals(firstRecorder) ? -1 : 0);
        this.order = triggerHcn.getPrayBody().previousRecorder.order + 1;

        nextRecorder = triggerHcn.getPrayBody();
        previousRecorder = nextRecorder.getPreviousRecorder();
        nextRecorder.setPreviousRecorder(this);
        previousRecorder.setNextRecorder(this);
        getRecorderHcn(triggerHcn.getLastActivePrime(), HcnStatus.FIRST_RECORDER);

        RecorderBody recorderToRaiseOrderNumber = nextRecorder;
        while (recorderToRaiseOrderNumber != firstRecorder) {
            recorderToRaiseOrderNumber.order++;
            recorderToRaiseOrderNumber = recorderToRaiseOrderNumber.nextRecorder;
        }

        createPassingZone(initiateHcnGeneratorsBasedOnPreviousPassingZone());
    }

    public void resetReferencedHuntersFromPreviousRecorderBody(Hcn prayHcn) {

        RecorderBody oldPrev = previousRecorder;
        RecorderBody oldNext = nextRecorder;
        oldPrev.setNextRecorder(oldNext);
        oldNext.setPreviousRecorder(oldPrev);
        RecorderBody prayBody = prayHcn.getHcnGenerator().getRecorderBody();
        RecorderBody newPrev = prayBody.previousRecorder;

        prayBody.setPreviousRecorder(this);
        this.nextRecorder = prayBody;
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
        } else {
            RecorderBody recorderToShiftOrderNumber = nextRecorder;
            while (recorderToShiftOrderNumber != oldNext) {
                recorderToShiftOrderNumber.order = recorderToShiftOrderNumber.previousRecorder.order + 1;
                recorderToShiftOrderNumber = recorderToShiftOrderNumber.nextRecorder;
            }
        }

        passing.removeRecorderBody(this);
        createPassingZone(initiateHcnGeneratorsBasedOnPreviousPassingZone());
    }

    public Hcn getRecorderHcn(Prime prime, HcnStatus status) {
        prayHcn = hcnGenerator.getHcn(prime, status, null, intervalLapiDistance);
        generatedRecorderHcns.add(prayHcn);
        return prayHcn;
    }

    public void findPrayCheck(HcnGenerator hunter, int intervalDistance) {
        if (Passing.getLowestPassing().getPreRecorderBodies().contains(this)) {
            return;
        }

        log.debug("this: {}, hunter: {}", this, hunter);
        int distDiff = intervalDistance - this.intervalLapiDistance;
        Prime hunterPrime = prayHcn.getLastActivePrime().getPreviousPrime(distDiff);
        ScientificNumber hunterValue = hunter.getHcnValue(hunterPrime);
        ScientificNumber hunterFactor = hunter.getHcnFactor(hunterPrime);
        ScientificNumber prayFactor = prayHcn.getFactor();
        placeHunterIntoActivePassingZone(intervalDistance, hunterValue, hunter,  hunterPrime);

        if (prayFactor.isSmallerThan(hunterFactor)) {
            if (nextRecorder.equals(firstRecorder)) {
                intervalDistance++;
            }
            nextRecorder.findPrayCheck(hunter, intervalDistance);
        }
    }

    public void placeHunterIntoActivePassingZone(int intervalDistance, ScientificNumber hunterValue, HcnGenerator hunter, Prime hunterPrime) {

        if (!hunters.containsKey(intervalDistance)) {
            hunters.put(intervalDistance, hunter.generateReferencedHcn(hunterPrime,this, intervalDistance));
            if (hunterValue.isSmallerThan(passingHcn.getValue())) {
                findSmallestPassingHcn(intervalDistance, hunterPrime, hunterValue, hunter);
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
            hunters.get(intervalDistance).removeHunterReference();
            hunters.put(intervalDistance, hunter.generateReferencedHcn(hunterPrime,this, intervalDistance));
        } else {
            replaceHunterHcns(prayPrimeWalker.getNextPrime());
            hunters.get(intervalDistance).removeHunterReference();
            hunters.put(intervalDistance, hunter.generateReferencedHcn(hunterPrimeWalker.getNextPrime(),this, intervalDistance));
        }

        passingHcn = hunters.get(intervalDistance);
        passing.removeRecorderBody(this);
        passing = Passing.placePassingInStaticChain(this);
    }

    private void replaceHunterHcns(Prime prayHcnPrime) {
        getRecorderHcn(prayHcnPrime, HcnStatus.RECORDER_IN_PASSINGZONE);
        for (Map.Entry<Integer, Hcn> entry : hunters.entrySet()) {
            prayHcnPrime = prayHcnPrime.getPreviousPrime();
            entry.getValue().removeHunterReference();
            entry.setValue(entry.getValue().getHcnGenerator().generateReferencedHcn(prayHcnPrime, this, entry.getKey()));
        }
    }

    public void deleteRecorderBody(Passing currentPassing) {

        lowerLargerOrderNumbers();
        previousRecorder.setNextRecorder(nextRecorder);
        nextRecorder.setPreviousRecorder(previousRecorder);
        HcnGenerator smallestHcnGeneratorForDeactivateCheck = null;
        boolean isFirstRecorderDeleted = this.equals(firstRecorder);

        if (isFirstRecorderDeleted) {
            firstRecorder = nextRecorder;
            currentPassing.setInterval(currentPassing.getInterval() - 1);
            smallestHcnGeneratorForDeactivateCheck = hunters.get(1).getHcnGenerator();

            while (firstRecorder.getIntervalLapiDistance() > 0) {
                firstRecorder.decreaseIntervalLapiDistances();
                firstRecorder.order = firstRecorder.previousRecorder.order;
                firstRecorder.lowerLargerOrderNumbers();
                firstRecorder = firstRecorder.getNextRecorder();
            }
        }

        if (smallestHcnGeneratorForDeactivateCheck != null) {
            smallestHcnGeneratorForDeactivateCheck.deactivateCheck();
        }

        previousRecorder = null;
        nextRecorder = null;
        hcnGenerator.getBody().deactivate();
        if (isFirstRecorderDeleted) {
            TransitionNode.deleteMaintainStatic();
        }
        hunters.values().forEach(hunterHcn -> {
            hunterHcn.removeHunterReference();
            hunterHcn.setPrayBody(null);
        });
        hunters.clear();
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

    private void lowerLargerOrderNumbers() {
        RecorderBody recorderToLowerOrderNumber = nextRecorder;
        while (recorderToLowerOrderNumber != firstRecorder) {
            recorderToLowerOrderNumber.order--;
            recorderToLowerOrderNumber = recorderToLowerOrderNumber.nextRecorder;
        }
    }

    public void createPassingZoneAfterSurvival(Hcn factorReferenceHcn) {
        HcnGenerator smallestHcnGeneratorForDeactivateCheck = null;
        if (this.equals(firstRecorder)) {
            smallestHcnGeneratorForDeactivateCheck = hunters.get(1).getHcnGenerator();
        }
        createPassingZone(initiateHcnGeneratorsBasedOnLocalPassingZone(factorReferenceHcn));
        if (smallestHcnGeneratorForDeactivateCheck != null) {
            smallestHcnGeneratorForDeactivateCheck.deactivateCheck();
        }
    }

    private TreeMap<Integer, HcnGenerator> initiateHcnGeneratorsBasedOnLocalPassingZone(Hcn factorReferenceHcn) {
        TreeMap<Integer, HcnGenerator> hunterBodies = new TreeMap<>();
        hunters.keySet().forEach(intervalDistance -> {
            hunterBodies.put(intervalDistance, hunters.get(intervalDistance).getHcnGenerator());
        });

        Prime hunterPrimeWalker = prayHcn.getLastActivePrime();
        for (Map.Entry<Integer, HcnGenerator> entry : hunterBodies.entrySet()) {
            hunterPrimeWalker = hunterPrimeWalker.getPreviousPrime();
            HcnGenerator hunterWalker = entry.getValue();
            ScientificNumber hunterHcnFactor = hunterWalker.getHcnFactor(hunterPrimeWalker);
            while (hunterWalker != null && hunterHcnFactor.isNotBiggerThan(factorReferenceHcn.getFactor())) {
                hunterWalker = hunterWalker.getNextHcnGenerator();
                if (hunterWalker != null) {
                    hunterHcnFactor = hunterWalker.getHcnFactor(hunterPrimeWalker);
                }
            }
            entry.setValue(hunterWalker);
        }

        if (hunterBodies.lastEntry().getValue() == null) {
            hunterBodies.remove(hunterBodies.lastKey());
        }
        return hunterBodies;
    }

    private TreeMap<Integer, HcnGenerator> initiateHcnGeneratorsBasedOnPreviousPassingZone() {

        //log.debug("previousRecorder.prayHcn: {}", previousRecorder.prayHcn);
        Prime previousBasePrime = previousRecorder.prayHcn.getLastActivePrime();
        TreeMap<Integer, HcnGenerator> hunterBodies = new TreeMap<>();

        for (int intervalDistanceKey = intervalLapiDistance + 1; intervalDistanceKey < previousRecorder.hunters.lastKey() + 1; intervalDistanceKey++) {

            HcnGenerator hunterCandidate = getHunterCandidateForIntervalDistance(intervalDistanceKey);
            //log.debug("previousRecorder.prayHcn.getFactor(): {}", previousRecorder.prayHcn.getFactor());
            Prime hunterPrime = previousBasePrime.getPreviousPrime(intervalDistanceKey - previousRecorder.intervalLapiDistance);
            ScientificNumber hunterFactor = hunterCandidate.getHcnFactor(hunterPrime);
            log.debug("hunterFactor: {}", hunterFactor);

            while (hunterFactor.isNotBiggerThan(previousRecorder.prayHcn.getFactor())) {
                hunterCandidate = hunterCandidate.getNextHcnGenerator();
                //log.debug("hunterCandidate: {}", hunterCandidate);
                hunterFactor = hunterCandidate.getHcnFactor(hunterPrime);
                //log.debug("hunterFactor: {}", hunterFactor);
            }
            //log.debug("hunterCandidate final: {}", hunterCandidate);
            if(hunterCandidate != null) {
                hunterBodies.put(intervalDistanceKey, hunterCandidate);
            }
        }

        return hunterBodies;
    }

    private HcnGenerator getHunterCandidateForIntervalDistance(int intervalDistanceKey) {
        RecorderBody hcnGeneratorProvider = previousRecorder;
        while (hcnGeneratorProvider.intervalLapiDistance > intervalDistanceKey) {
            hcnGeneratorProvider = hcnGeneratorProvider.previousRecorder;
        }
        if (intervalDistanceKey == hcnGeneratorProvider.intervalLapiDistance) {
            return hcnGeneratorProvider.getHcnGenerator();
        } else {
            return hcnGeneratorProvider.hunters.get(intervalDistanceKey).getHcnGenerator();
        }
    }

    private void createPassingZone(TreeMap<Integer, HcnGenerator> hunterBodies) {

        TreeMap<ScientificNumber, Integer> hunterValuesByIntervalDistance = new TreeMap<>();
        Prime basePrayPrimeBefore = prayHcn.getLastActivePrime();
        //log.debug("hunterBodies: {}", hunterBodies);
        Prime basePrayPrimeAfter = identifyNextPassing(hunterBodies, basePrayPrimeBefore, hunterValuesByIntervalDistance);

        hunters.values().forEach(Hcn::removeHunterReference);

        if (hunterBodies.size() < hunters.size()) {
            hunters.remove(hunters.lastKey());
        }

        Prime innerHunterWalker = basePrayPrimeAfter;
        for (Iterator<Integer> iterator = hunterBodies.keySet().iterator(); iterator.hasNext(); ) {
            int intervalDistance = iterator.next();
            innerHunterWalker = innerHunterWalker.getPreviousPrime();
            hunters.put(intervalDistance, hunterBodies.get(intervalDistance).generateReferencedHcn(innerHunterWalker,this, intervalDistance));
        }

        passingHcn = hunters.get(hunterValuesByIntervalDistance.firstEntry().getValue());
        passing = Passing.placePassingInStaticChain(this);
    }

    private Prime identifyNextPassing(TreeMap<Integer, HcnGenerator> hunterBodies, Prime basePrayPrime, TreeMap<ScientificNumber, Integer> hunterValuesByIntervalDistance) {
        ScientificNumber prayValue;
        do {
            prayValue = hcnGenerator.getHcnValue(basePrayPrime);
            hunterValuesByIntervalDistance.clear();
            Prime innerHunterWalker = basePrayPrime;
            for (Iterator<Integer> iterator = hunterBodies.keySet().iterator(); iterator.hasNext(); ) {
                int intervalDistance = iterator.next();
                innerHunterWalker = innerHunterWalker.getPreviousPrime();
                ScientificNumber hunterValue = hunterBodies.get(intervalDistance).getHcnValue(innerHunterWalker);
                hunterValuesByIntervalDistance.put(hunterValue, intervalDistance);
            }
            basePrayPrime = basePrayPrime.calculateNextPrimeIfNeeded();
        } while (prayValue.isSmallerThan(hunterValuesByIntervalDistance.firstKey()));

        basePrayPrime = basePrayPrime.getPreviousPrime();

        if (basePrayPrime.equals(prayHcn.getLastActivePrime())) {
            basePrayPrime = checkThis(basePrayPrime.getPreviousPrime(hunterValuesByIntervalDistance.firstEntry().getValue() - intervalLapiDistance),
                    hunterValuesByIntervalDistance.firstKey(), hunterBodies.get(hunterValuesByIntervalDistance.firstEntry().getValue()));
        }

        getRecorderHcn(basePrayPrime, HcnStatus.RECORDER_IN_PASSINGZONE);
        return basePrayPrime;
    }

    private Prime checkThis(Prime hunterPrime, ScientificNumber hunterValue, HcnGenerator hunter) {

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
        return prayPrimeWalker.getNextPrime();
    }

    public void deleteHunterHcn(Hcn hunterHcn) {

        if (hunters.get(hunterHcn.getIntervalDistance()).equals(hunterHcn)) {

            int intervalDistance = hunterHcn.getIntervalDistance() + (this.equals(firstRecorder) ? -1 : 0);
            ScientificNumber hunterFactor = previousRecorder.hunters.get(intervalDistance).getFactor();
            Prime hunterPrime = previousRecorder.hunters.get(intervalDistance).getLastActivePrime();
            HcnGenerator hunterWalker = previousRecorder.hunters.get(intervalDistance).getHcnGenerator();

            while (hunterFactor.isNotBiggerThan(previousRecorder.getPrayHcn().getFactor())) {
                hunterWalker = hunterWalker.getNextHcnGenerator();
                hunterFactor = hunterWalker.getHcnFactor(hunterPrime);
            }

            hunters.put(hunterHcn.getIntervalDistance(), hunterWalker.generateReferencedHcn(prayHcn.getLastActivePrime()
                    .getPreviousPrime(hunterHcn.getIntervalDistance()),this, hunterHcn.getIntervalDistance()));
        }
    }

    public void generatePassingHcns(Passing currentPassing) {
        identifyPassingHcns(currentPassing);
        ScientificNumber factorLimit = filterPassingHcns(currentPassing);
        currentPassing.calculateSkipCount(factorLimit);
        ReferenceInterval.getCurrentReferenceInterval().preCalculatePassing(currentPassing);
    }

    private void identifyPassingHcns(Passing currentPassing) {
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
    }

    private static ScientificNumber filterPassingHcns(Passing currentPassing) {
        ScientificNumber factorLimit = new ScientificNumber(1, 0);
        Set<Hcn> notSuperiorHcns = new HashSet<>();
        for (Hcn passingCandidate : currentPassing.getPassingHcns()) {
            if (passingCandidate.getFactor().isBiggerThan(factorLimit)) {
                if (passingCandidate.getHcnGenerator().getRecorderBody() != null) {
                    passingCandidate.setHcnStatus(HcnStatus.RECORDER_HUNTER);
                    currentPassing.getPreRecorderBodies().add(passingCandidate.getHcnGenerator().getRecorderBody());
                } else {
                    passingCandidate.setHcnStatus(HcnStatus.FIRST_RECORDER);
                }
                factorLimit = passingCandidate.getFactor();
            } else {
                notSuperiorHcns.add(passingCandidate);
            }
        }
        notSuperiorHcns.forEach(currentPassing.getPassingHcns()::remove);
        return factorLimit;
    }
}
