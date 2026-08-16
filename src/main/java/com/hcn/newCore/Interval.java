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

    private int lapi;
    private ScientificNumber value;
    private ScientificNumber factor;
    private List<Hcn> hcnList;
    private Interval referenceInterval;
    private int activeBodyCount;
    private int lowestRecorderLapi;
    private ScientificNumber targetValue;

    public boolean isReferenced() {
        return referenceInterval != null && referenceInterval != this;
    }

    public Interval referenceCheck(Interval referenceInterval) {
        if (referenceInterval.getHcnList().size() != hcnList.size()) {
            this.referenceInterval = this;
            return this;
        }
        for (int i = 0; i < hcnList.size(); i++) {
            if (!referenceInterval.getHcnList().get(i).getBody().equals(hcnList.get(i).getBody())) {
                this.referenceInterval = this;
                return this;
            }
        }
        this.referenceInterval = referenceInterval;
        return referenceInterval;
    }


    public boolean postHcnGenerateMaintainFoundNextLapi() {

        Hcn targetHcn = RecorderList.getFirstRecorder().getLastGeneratedHcn();
        log.debug("preHcnGenerateMaintain 1 {}", hcnList);
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
                log.debug("postHcnGenerateMaintain 1, NextLapiFound: {}", hcnList);
                if (!RecorderList.getFirstRecorder().equals(HcnGeneratorList.getSmallestBody())) {
                    log.debug("NEED TO ADJUST FIRSTRECORDER 1 ({}) SMALLESTGEN: {}", RecorderList.getFirstRecorder(), HcnGeneratorList.getSmallestBody());
                    RecorderList.setFirstRecorder(HcnGeneratorList.getSmallestBody());
                    RecorderList.setLastRecorder(HcnGeneratorList.getSmallestBody().getPreviousRecorder());
                    log.debug(RecorderList.print());
                }
                return true;
            }
            bodyToProcess = bodyToProcess.getNextRecorder();
        }

        if (targetHcn.getFactor().isBiggerThan(hcnList.get(hcnList.size() - 1).getFactor())) {
            hcnList.add(targetHcn);
            log.debug("postHcnGenerateMaintain 2, NextLapiFound: {}", hcnList);
            return true;
        } else {
            log.debug("postHcnGenerateMaintain, NextLapi NOT Found: {}", hcnList);
            if (!RecorderList.getFirstRecorder().equals(HcnGeneratorList.getSmallestBody())) {
                log.debug("NEED TO ADJUST FIRSTRECORDER 2 ({}) SMALLESTGEN: {}", RecorderList.getFirstRecorder(), HcnGeneratorList.getSmallestBody());
            }
            return false;
        }
    }

    private boolean finalizeLastGeneratedHcn(Body currentRecorder) {
        Hcn recorder = currentRecorder.getLastGeneratedHcn();
        log.debug(" recorder {}", recorder);
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
}
