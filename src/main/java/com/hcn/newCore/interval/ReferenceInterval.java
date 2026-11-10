package com.hcn.newCore.interval;

import com.hcn.newCore.Hcn;
import com.hcn.newCore.HcnStatus;
import com.hcn.newCore.Passing;
import com.hcn.newCore.RecorderBody;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

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
    }

    public static List<Hcn> getHcnProjections(int index) {

        if (!currentReferenceInterval.activeIntervals.get(index).getHcnProjections().isEmpty()) {
            return currentReferenceInterval.activeIntervals.get(index).getHcnProjections();
        }

        int firstMissingInterval = 0;
        for (int i = 0; i < currentReferenceInterval.activeIntervals.size(); i++) {
            if (currentReferenceInterval.activeIntervals.get(i).getHcnProjections().isEmpty()) {
                firstMissingInterval = i;
                break;
            }
        }

        for (int i = firstMissingInterval; i < index + 1; i++) {
            if (currentReferenceInterval.activeIntervals.get(i).getHcnProjections().isEmpty()) {
                currentReferenceInterval.activeIntervals.get(i).generateHcnProjections(currentReferenceInterval.activeIntervals.get(i - 1));
            }
        }
        return currentReferenceInterval.activeIntervals.get(index).getHcnProjections();
    }

    private final List<ActiveInterval> activeIntervals = new ArrayList<>();
    private int lastHcnOrder;
    private int lastActiveInterval;
    private RecorderBody lastActiveRecorder;

    public void preCalculatePassing(Passing passing) {

        int order = passing.getPreRecorderBodies().first().getHcnGenerator().getRecorderBody().getOrder();
        int hcnOrderAdvancement = order - lastActiveRecorder.getOrder();
        Hcn firstPrayHcn = passing.getPrayHcns().first();
        int interval = firstPrayHcn.getLapiIndex() + firstPrayHcn.getHcnGenerator().getRecorderBody().getIntervalLapiDistance();
        if (passing.prayHcnsContainFirstRecorder()) {
            interval--;
            hcnOrderAdvancement += RecorderBody.getRecorderListSize();
        }
        if (interval != lastActiveInterval) {
            createNextActiveInterval(interval);
            int extra = (interval - lastActiveInterval) * RecorderBody.getRecorderListSize();
            hcnOrderAdvancement = hcnOrderAdvancement + extra;
            lastActiveInterval = interval;
        }
        lastHcnOrder = lastHcnOrder + hcnOrderAdvancement;
        passing.setHcnOrder(lastHcnOrder);
        passing.setInterval(interval);
    }

    public void postCalculations(Passing currentPassing) {

        List<Hcn> activityHcns = new ArrayList<>();
        for (Hcn passingHcn : currentPassing.getPassingHcns()) {
            if (passingHcn.getHcnStatus().equals(HcnStatus.FIRST_RECORDER)) {
                passingHcn.setOrder(lastHcnOrder);
                activityHcns.add(passingHcn);
            }
            lastHcnOrder++;
            lastActiveRecorder = passingHcn.getHcnGenerator().getRecorderBody();
        }
        lastHcnOrder--;
        ActiveInterval.getCurrentActiveInterval().addActivityZone(activityHcns, currentPassing);
    }

    private void createNextActiveInterval(int interval) {
        ActiveInterval.getCurrentActiveInterval().setPostRecorderListSize(RecorderBody.getRecorderListSize());
        ActiveInterval.setCurrentActiveInterval(ActiveInterval.builder().interval(interval).firstRecorder(RecorderBody.getFirstRecorder()).build());
        activeIntervals.add(ActiveInterval.getCurrentActiveInterval());
    }
}
