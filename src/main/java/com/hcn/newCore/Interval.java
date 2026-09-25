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
        if (currentInterval.prime.getIndex() < recorderHcn.getLastActivePrime().getIndex()) {
            initializeNewCurrentLapi();
        }
        if (recorderHcn.getLastActivePrime().getIndex() < currentInterval.lowestRecorderLapi) {
            currentInterval.lowestRecorderLapi = recorderHcn.getLastActivePrime().getIndex();
        }
        currentInterval.hcnList.add(recorderHcn);
    }

    private static void initializeNewCurrentLapi() {
        //log.debug("initializeNewCurrentLapi");
        int prevLapi = currentInterval.lapi;
        Prime newPrime = currentInterval.prime.getNextPrime();
        currentInterval.referenceCheck();
        //Lapi.deleteLapisUnder();
        //Prime.deleteHcnProducerPrimeUnder();
        currentInterval = Interval.builder().prime(newPrime).hcnList(new ArrayList<>()).lowestRecorderLapi(prevLapi + 1).potentialNextIntervalStarter(HcnGeneratorList.getSmallestBody()).build();
    }

}
