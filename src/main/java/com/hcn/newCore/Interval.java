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
    private ScientificNumber value;
    private ScientificNumber factor;
    private List<Hcn> hcnList;
    private Interval referenceInterval;
    private int activeBodyCount;
    private int lowestRecorderLapi;

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


    public boolean postHcnGenerateMaintainFoundNextLapi() {

        Hcn targetHcn = RecorderList.getFirstRecorder().getLastGeneratedHcn();
        //log.debug("preHcnGenerateMaintain 1 {}", hcnList);
        Body bodyToProcess;

        if (hcnList.size() == 1) {
            if (hcnList.get(hcnList.size() - 1).getBody().equals(HcnGeneratorList.getSmallestBody())) {
                bodyToProcess = hcnList.get(hcnList.size() - 1).getBody().getNextRecorder();
            } else {
                bodyToProcess = RecorderList.getFirstRecorder();
            }
        } else {
            bodyToProcess = hcnList.get(hcnList.size() - 1).getBody().getNextRecorder();
        }

        finalizeLastGeneratedHcn(bodyToProcess);
        bodyToProcess = bodyToProcess.getNextRecorder();

        while (!bodyToProcess.equals(RecorderList.getFirstRecorder())) {
            if (finalizeLastGeneratedHcn(bodyToProcess)) {
                //log.debug("postHcnGenerateMaintain 1, NextLapiFound: {}", hcnList);
                if (!RecorderList.getFirstRecorder().equals(HcnGeneratorList.getSmallestBody())) {
                    //log.debug("NEED TO ADJUST FIRSTRECORDER 1 ({}) SMALLESTGEN: {}", RecorderList.getFirstRecorder(), HcnGeneratorList.getSmallestBody());
                    RecorderList.setFirstRecorder(HcnGeneratorList.getSmallestBody());
                    RecorderList.setLastRecorder(HcnGeneratorList.getSmallestBody().getPreviousRecorder());
                    //log.debug(RecorderList.print());
                }
                return true;
            }
            bodyToProcess = bodyToProcess.getNextRecorder();
        }

        if (targetHcn.getFactor().isBiggerThan(hcnList.get(hcnList.size() - 1).getFactor())) {
            hcnList.add(targetHcn);
            //log.debug("postHcnGenerateMaintain 2, NextLapiFound: {}", hcnList);
            return true;
        } else {
            //log.debug("postHcnGenerateMaintain, NextLapi NOT Found: {}", hcnList);
            if (!RecorderList.getFirstRecorder().equals(HcnGeneratorList.getSmallestBody())) {
                //log.debug("NEED TO ADJUST FIRSTRECORDER 2 ({}) SMALLESTGEN: {}", RecorderList.getFirstRecorder(), HcnGeneratorList.getSmallestBody());
            }
            return false;
        }
    }

    private boolean finalizeLastGeneratedHcn(Body currentRecorder) {
        Hcn recorder = currentRecorder.getLastGeneratedHcn();
        //log.debug(" recorder {}", recorder);
        hcnList.add(recorder);
        int recorderLapi = recorder.getLapi().getPrime().getIndex();
        if (recorderLapi < lowestRecorderLapi) {
            lowestRecorderLapi = recorderLapi;
        }
        if (recorderLapi == lapi + 1) {
            return true;
        }
        return false;
    }

    public static void populateHcnList() {
        //log.debug("populate: currentRecorder= {} {}", Matrix.getCurrentRecorder(), RecorderList.print());
        Body walker = Matrix.getCurrentRecorder().getNextRecorder();
        addWalkerHcnToHcnList(walker);
        //log.debug(" populateHcnList mandatory add {}", walker.getLastGeneratedHcn());
        walker = walker.getNextRecorder();
        while (walker.getLastGeneratedHcn().getFactor().isBiggerThan(walker.getPreviousRecorder().getLastGeneratedHcn().getFactor())) {

            addWalkerHcnToHcnList(walker);
            //log.debug(" populateHcnList conditional  add {}", walker.getLastGeneratedHcn());
            walker = walker.getNextRecorder();
        }
        Matrix.setCurrentRecorder(walker.getPreviousRecorder());
        //log.debug(" setCurrentRecorder after populate {}", Matrix.getCurrentRecorder());
    }

    private static void addWalkerHcnToHcnList(Body walker) {
        int recorderLapi = walker.getLastGeneratedHcn().getLapiIndex();
        if (recorderLapi > currentInterval.lapi) {
            initializeNewCurrentLapi();
        }

        if (recorderLapi < currentInterval.lowestRecorderLapi) {
            currentInterval.lowestRecorderLapi = recorderLapi;
        }
        currentInterval.hcnList.add(walker.getLastGeneratedHcn());
    }

    private static void initializeNewCurrentLapi() {
        //log.debug("initializeNewCurrentLapi");
        int prevLapi = currentInterval.lapi;
        currentInterval.referenceCheck();
        Lapi.deleteLapisUnder();
        currentInterval = Interval.builder().lapi(prevLapi + 1).hcnList(new ArrayList<>()).lowestRecorderLapi(prevLapi + 1).build();
    }
}
