package com.hcn.newCore;

import lombok.experimental.SuperBuilder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

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
    private PassingZone passingZone = null;
    private final List<Hcn> generatedRecorderHcns = new ArrayList<>();
    private final HcnGenerator hcnGenerator;

    public RecorderBody(Hcn triggerHcn) {
        hcnGenerator = triggerHcn.getHcnGenerator();
        nextRecorder = triggerHcn.getPassingZone().getPrayBody();
        previousRecorder = nextRecorder.getPreviousRecorder();
        nextRecorder.setPreviousRecorder(this);
        previousRecorder.setNextRecorder(this);
        intervalLapiDistance = triggerHcn.getIntervalDistance() + (nextRecorder.equals(RecorderList.getFirstRecorder()) ? -1 : 0);
        hcnGenerator.setRecorderBody(this);
        //log.debug("creating new Passing Zone for {}", triggerHcn);
        passingZone = new PassingZone(triggerHcn, this);
    }

    @Override
    public String toString() {
        return hcnGenerator.getBody().toString() + " intervalLapiDistance: " + intervalLapiDistance;
    }

    public void print() {
        //log.debug("  {}", this);
        passingZone.print();
    }

    public Hcn getRecorderHcn(Prime prime, HcnStatus status, PassingZone passingZone) {
        prayHcn = hcnGenerator.getHcn(prime, status, passingZone, intervalLapiDistance);
        generatedRecorderHcns.add(prayHcn);
        return prayHcn;
    }

    public void findPrayCheck(HcnGenerator hunter, int intervalDistance) {
        int distDiff = intervalDistance - this.intervalLapiDistance;
        //log.debug("prayHcn {}", prayHcn);
        Prime hunterPrime = prayHcn.getLastActivePrime().getPreviousPrime(distDiff);
        //log.debug("hunterPrime {}", hunterPrime);
        Hcn hunterHcn = hunter.generateHunterHcn(hunterPrime, HcnStatus.HUNTER_DURING_FINDPRAY, passingZone, intervalDistance);
        //log.debug(" # findPrayCheck distDiff: {} hunterPrime: {} hunterHcn: {} ########################", distDiff, hunterPrime, hunterHcn);

        passingZone.placeHunterIntoActivePassingZone(hunterHcn, intervalDistance);

        if (prayHcn.getFactor().isSmallerThan(hunterHcn.getFactor())) {
            if (nextRecorder.equals(RecorderList.getFirstRecorder())) {
                //log.debug("# intervalDistance raised to {}", intervalDistance);
                intervalDistance++;
            }
            //log.debug(" calling nextRecorder: {}", nextRecorder);
            nextRecorder.findPrayCheck(hunter, intervalDistance);
        }
    }

    public void deleteRecorderBody(Hcn hcn) {
        hcn.setHcnStatus(HcnStatus.RECORDER_DEACTIVATED);
        previousRecorder.setNextRecorder(nextRecorder);
        nextRecorder.setPreviousRecorder(previousRecorder);
        if (this.equals(RecorderList.getFirstRecorder())) {
            RecorderBody potentialFirstRecorder = nextRecorder;
            while (potentialFirstRecorder.getIntervalLapiDistance() > 0) {
                potentialFirstRecorder.setIntervalLapiDistance(potentialFirstRecorder.getIntervalLapiDistance() - 1);
                potentialFirstRecorder.getPassingZone().decreaseIntervalLapiDistances();
                potentialFirstRecorder = potentialFirstRecorder.getNextRecorder();
            }
            RecorderList.setFirstRecorder(potentialFirstRecorder);

        }
        previousRecorder = null;
        nextRecorder = null;
        hcnGenerator.getBody().deactivate();
        passingZone.prayBodyDeactivated();
        passingZone = null;
    }
}
