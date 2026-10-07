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
            RecorderBody previousRecorder = RecorderList.getCurrentRecorder();
            RecorderBody currentRecorder = previousRecorder.getNextRecorder();
            RecorderList.setCurrentRecorder(currentRecorder);
            Hcn recorderHcn = currentRecorder.getProcessingHcn();
            List<Hcn> passingHcns = currentRecorder.getPassingHcns(recorderHcn);
            Hcn previouslyAddedHcn = null;

            if (!passingHcns.isEmpty()) {
                log.debug("passingHcns: {}", passingHcns);

                for (Hcn passingHcn : passingHcns) {
                    if (previouslyAddedHcn == null) {
                        addRecorderHcn(RecorderList.addNewRecord(passingHcn));
                        previouslyAddedHcn = passingHcn;
                    } else {
                        if (passingHcn.getFactor().isBiggerThan(previouslyAddedHcn.getFactor())) {
                            log.debug("FACTOR IS BIGGER - adding new recorder");
                            addRecorderHcn(RecorderList.addNewRecord(passingHcn));
                            previouslyAddedHcn = passingHcn;
                        } else {
                            log.debug("FACTOR IS SMALLER - setting pray body");
                            HunterBody hunterBody = passingHcn.getHcnGenerator().getBody().getHunterBody();
                            hunterBody.setPrayBody(previouslyAddedHcn.getHcnGenerator().getBody().getRecorderBody());
                            if (RecorderList.isCurrentRecorderFirstRecorder()) {
                                hunterBody.setIntervalLapiDistance(hunterBody.getIntervalLapiDistance() - 1);
                            }
                            previouslyAddedHcn.getHcnGenerator().getBody().getRecorderBody().getHunters().add(hunterBody);
                        }
                    }
                }
                if (recorderHcn.getFactor().isNotBiggerThan(previouslyAddedHcn.getFactor())) {
                    RecorderList.killCurrentRecorder();
                } else {
                    addRecorderHcn(recorderHcn);
                }
            } else {
                addRecorderHcn(recorderHcn);
            }
            //processCurrentRecorder();
        }
    }

    public void referenceCheck() {

        if (globalReferenceInterval.getHcnList().size() != hcnList.size()) {
            this.referenceInterval = this;
            globalReferenceInterval = this;
        }
        for (int i = 0; i < hcnList.size(); i++) {
            if (!globalReferenceInterval.getHcnList().get(i).getHcnGenerator().equals(hcnList.get(i).getHcnGenerator())) {
                this.referenceInterval = this;
                globalReferenceInterval = this;
            }
        }
        this.referenceInterval = globalReferenceInterval;
    }

    public static void addRecorderHcn(Hcn recorderHcn) {
        //Hcn recorderHcn = recorderBody.generateRecorderHcn();
        if (recorderHcn.getHcnGenerator().equals(RecorderList.getFirstRecorder())) {
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
