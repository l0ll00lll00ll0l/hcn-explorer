package com.hcn.newCore;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@Slf4j
public class Interval {

    private static Interval currentInterval;
    private static Interval globalReferenceInterval;
    private int lapi;
    private Prime prime;
    private List<Hcn> hcnList;
    private Interval referenceInterval;
    private int activeBodyCount;
    private int lowestRecorderLapi;
    private Body lastBaseIntervalBody;
    private List<Body> postBaseBodyList = null;

    public static Interval getCurrentInterval() {
        return currentInterval;
    }

    public static void setCurrentInterval(Interval currentInterval) {
        Interval.currentInterval = currentInterval;
    }

    public static Interval getGlobalReferenceInterval() {
        return globalReferenceInterval;
    }

    public static void setGlobalReferenceInterval(Interval globalReferenceInterval) {
        Interval.globalReferenceInterval = globalReferenceInterval;
    }

    public boolean isReferenced() {
        return referenceInterval != null && referenceInterval != this;
    }


    public static void processCurrentInterval() {
        Interval currentInterval = Interval.getCurrentInterval();
        while (currentInterval.equals(Interval.getCurrentInterval())) {
            RecorderList.setCurrentRecorder(RecorderList.getCurrentRecorder().getNextRecorder());
            processCurrentRecorder();
        }
    }

    private static void processCurrentRecorder() {

        Hcn recorderHcn = RecorderList.getCurrentRecorder().generateRecorderHcn();
        List<HunterHcn> passingHunters = RecorderList.getCurrentRecorder().generateSmallerHunterHcns(recorderHcn);
        log.debug("passingHunters: {}", passingHunters);

        if (!passingHunters.isEmpty()) {
            RecorderBody newRecorder = RecorderList.addNewRecord(passingHunters.get(0));
            Hcn newHcn = Hcn.builder().recorderBody(newRecorder).value(passingHunters.get(0).getValue())
                    .factor(passingHunters.get(0).getFactor()).lastActivePrime(passingHunters.get(0).getLastActivePrime()).build();
            addRecorderHcn(newHcn);

            for (int i = 1; i < passingHunters.size(); i++) {
                HunterHcn candidateHcn = passingHunters.get(i);
                HunterHcn referenceHcn = passingHunters.get(i - 1);

                log.debug("candidateHcn: {}, referenceHcn: {}", candidateHcn.getFactor(), referenceHcn.getFactor());
                if (candidateHcn.getFactor().isBiggerThan(referenceHcn.getFactor())) {
                    RecorderBody recorderBody = RecorderList.addNewRecord(passingHunters.get(i));
                    Hcn hcn = Hcn.builder().recorderBody(recorderBody).value(passingHunters.get(i).getValue())
                            .factor(passingHunters.get(i).getFactor()).lastActivePrime(passingHunters.get(i).getLastActivePrime()).build();
                    addRecorderHcn(hcn);
                } else {
                    if (passingHunters.get(i).getHunterBody().getPrayLapiDistance() > passingHunters.get(i - 1).getHunterBody().getPrayLapiDistance()) {
                        //referenceHcn.getBody().getHunters().add(candidate.getBody());
                        //candidate.getBody().setPrayBody(referenceHcn.getBody());
                        //TODO set prayLapiDistance is missing
                        log.warn("NEEDS TO BE IMPLEMENTED, HERE candidate SHOULD be pray for referencebody");
                    } else {
                        log.warn("NEEDS TO BE IMPLEMENTED, HERE candidate SHOULD BE DEACTIVATED");
                    }
                }
            }

            //log.debug("RecorderList.getCurrentRecorder().getRecorderHcnValue(): {}, passingHunters.get(passingHunters.size() - 1).getProcessingHcnFactor()): {}",
              //      RecorderList.getCurrentRecorder().getRecorderHcnValue(), passingHunters.get(passingHunters.size() - 1).getValue());
            if (recorderHcn.getFactor().isNotBiggerThan(passingHunters.get(passingHunters.size() - 1).getFactor())) {
                RecorderList.killCurrentRecorder();
            } else {
                addRecorderHcn(recorderHcn);
            }
        }
        else {
            addRecorderHcn(recorderHcn);
        }
    }

    public void referenceCheck() {

        if (globalReferenceInterval.getHcnList().size() != hcnList.size()) {
            this.referenceInterval = this;
            globalReferenceInterval = this;
        }
        for (int i = 0; i < hcnList.size(); i++) {
            if (!globalReferenceInterval.getHcnList().get(i).getRecorderBody().equals(hcnList.get(i).getRecorderBody())) {
                this.referenceInterval = this;
                globalReferenceInterval = this;
            }
        }
        this.referenceInterval = globalReferenceInterval;
    }

    public static void addRecorderHcn(Hcn recorderHcn) {
        //Hcn recorderHcn = recorderBody.generateRecorderHcn();
        if (recorderHcn.getRecorderBody().equals(RecorderList.getFirstRecorder())) {
            initializeNewCurrentLapi();
        }
        currentInterval.hcnList.add(recorderHcn);
    }

    private static void initializeNewCurrentLapi() {
        int prevLapi = currentInterval.lapi;
        Prime.addNextHcnProducer();
        currentInterval.referenceCheck();
        currentInterval = Interval.builder().prime(Prime.getHighestHcnProducerPrime())
                .hcnList(new ArrayList<>()).lowestRecorderLapi(prevLapi + 1).build();
    }

}
