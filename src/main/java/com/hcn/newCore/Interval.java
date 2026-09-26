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
    private Body potentialNextIntervalStarter;
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
        List<Hcn> smallerHcns = RecorderList.getCurrentRecorder().generateSmallerHunterHcns(recorderHcn);

        if (!smallerHcns.isEmpty()) {
            RecorderList.addNewRecord(smallerHcns.get(0));

            for (int i = 1; i < smallerHcns.size(); i++) {
                Hcn candidate = smallerHcns.get(i);
                Hcn referenceHcn = smallerHcns.get(i - 1);

                if (candidate.getFactor().isBiggerThan(referenceHcn.getFactor())) {
                    RecorderList.addNewRecord(candidate);
                } else {
                    if (candidate.getLapiIndex() > referenceHcn.getLapiIndex()) {
                        referenceHcn.getBody().getHunters().add(candidate.getBody());
                        candidate.getBody().setPrayBody(referenceHcn.getBody());
                        //TODO set prayLapiDistance is missing
                        log.warn("NEEDS TO BE IMPLEMENTED, HERE candidate SHOULD be pray for referencebody");
                    } else {
                        log.warn("NEEDS TO BE IMPLEMENTED, HERE candidate SHOULD BE DEACTIVATED");
                    }
                }
            }
            if (recorderHcn.getFactor().isNotBiggerThan(smallerHcns.get(smallerHcns.size() - 1).getFactor())) {
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
            if (!globalReferenceInterval.getHcnList().get(i).getBody().equals(hcnList.get(i).getBody())) {
                this.referenceInterval = this;
                globalReferenceInterval = this;
            }
        }
        this.referenceInterval = globalReferenceInterval;
    }

    public static void addRecorderHcn(Hcn recorderHcn) {
        if (recorderHcn.getBody().equals(RecorderList.getFirstRecorder())) {
            initializeNewCurrentLapi();
        }
        currentInterval.hcnList.add(recorderHcn);
    }

    private static void initializeNewCurrentLapi() {
        int prevLapi = currentInterval.lapi;
        Prime.addNextHcnProducer();
        currentInterval.referenceCheck();
        currentInterval = Interval.builder().prime(Prime.getHighestHcnProducerPrime())
                .hcnList(new ArrayList<>()).lowestRecorderLapi(prevLapi + 1)
                .potentialNextIntervalStarter(HcnGeneratorList.getSmallestBody()).build();
    }

}
