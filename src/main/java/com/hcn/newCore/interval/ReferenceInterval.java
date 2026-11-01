package com.hcn.newCore.interval;

import com.hcn.newCore.Hcn;
import com.hcn.newCore.RecorderBody;
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
public class ReferenceInterval {

    private static ReferenceInterval currentReferenceInterval;

    public static ReferenceInterval getCurrentReferenceInterval() {return currentReferenceInterval;}

    public static void reset() { currentReferenceInterval = null; }

    public static void initialize() {
        currentReferenceInterval = ReferenceInterval.builder().build();
        currentReferenceInterval.getActiveIntervals().add(ActiveInterval.getCurrentActiveInterval());
        //hcns added in recorderList initialize
    }

    public  static Object getHcnProjections() {
        if (currentReferenceInterval.activeIntervals.get(0).getHcnProjections().isEmpty()) {
            return "generating hcn projections START";
        } else {
            return "generating hcn projections CARRY ON";
        }
    }

    private final List<Hcn> referenceRecorderList = new ArrayList<>();
    private final List<ActiveInterval> activeIntervals = new ArrayList<>();
    private int lastHcnOrder;
    private int lastActiveInterval;
    private RecorderBody lastActiveRecorder;

    public void setHcnOrderForNewRecorder(Hcn triggerHcn) {

        int interval = triggerHcn.getLapiIndex() + triggerHcn.getHcnGenerator().getRecorderBody().getIntervalLapiDistance();

        //log.debug(" interval: {}", interval);
        int order = triggerHcn.getHcnGenerator().getRecorderBody().getOrder();
        //log.debug(" order: {}", order);
        int hcnOrderAdvancement = order - lastActiveRecorder.getOrder();
        //log.debug(" hcnOrderAdvancement1: {}", hcnOrderAdvancement);

        if (interval != lastActiveInterval) {
            ActiveInterval.setCurrentActiveInterval(ActiveInterval.builder().interval(interval).build());
            activeIntervals.add(ActiveInterval.getCurrentActiveInterval());
            int extra = (interval - lastActiveInterval) * RecorderBody.getRecorderListSize();
            //log.debug(" extra: {}", extra);
            hcnOrderAdvancement = hcnOrderAdvancement + extra;
            //log.debug(" hcnOrderAdvancement3: {}", hcnOrderAdvancement);
        }

        lastHcnOrder = lastHcnOrder + hcnOrderAdvancement;
        //log.debug(" lastHcnOrder: {}", lastHcnOrder);
        triggerHcn.setOrder(lastHcnOrder);
        ActiveInterval.getCurrentActiveInterval().getActivityHcn().add(triggerHcn);

        //lastPassingRecorderOrder = order;
        lastActiveInterval = interval;
        lastActiveRecorder = triggerHcn.getHcnGenerator().getRecorderBody();
        //log.debug("setHcnOrderForNewRecorder {}, lastHcnOrder: {}, lastActiveRecorder: {}, lastActiveInterval: {}", triggerHcn, lastHcnOrder, lastActiveRecorder, lastActiveInterval);
    }

    public void addDeleteHcn(Hcn prayHcn) {

        int interval = prayHcn.getLapiIndex() + prayHcn.getHcnGenerator().getRecorderBody().getIntervalLapiDistance();
        int order = prayHcn.getHcnGenerator().getRecorderBody().getOrder();
        int hcnOrderAdvancement = order - lastActiveRecorder.getOrder();

        if (prayHcn.getHcnGenerator().getRecorderBody().equals(RecorderBody.getFirstRecorder())) {
            interval--;
            hcnOrderAdvancement += RecorderBody.getRecorderListSize();
        }

        if (interval != lastActiveInterval) {
            ActiveInterval.setCurrentActiveInterval(ActiveInterval.builder().interval(interval).build());
            activeIntervals.add(ActiveInterval.getCurrentActiveInterval());
            hcnOrderAdvancement += (interval - lastActiveInterval) * RecorderBody.getRecorderListSize();
        }

        ActiveInterval.getCurrentActiveInterval().getActivityHcn().add(prayHcn);
        lastHcnOrder += hcnOrderAdvancement -1;
        lastActiveRecorder = prayHcn.getHcnGenerator().getRecorderBody().getPreviousRecorder();
        lastActiveInterval = interval;
        //log.debug("addDeleteHcn {}, lastHcnOrder: {}, lastActiveRecorder: {}, lastActiveInterval: {}", prayHcn, lastHcnOrder, lastActiveRecorder, lastActiveInterval);
    }
}
