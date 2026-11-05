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
        //hcns added in recorderList initialize
    }

    public static List<Hcn> getHcnProjections(int index) {

        log.info("!activeInterval.getHcnProjections().isEmpty() for {}", !currentReferenceInterval.activeIntervals.get(index).getHcnProjections().isEmpty());
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

        log.info("firstMissingInterval {}", firstMissingInterval);

            for (int i = firstMissingInterval; i < index + 1; i++) {
                if (currentReferenceInterval.activeIntervals.get(i).getHcnProjections().isEmpty()) {
                    log.info(" generating for: {}", currentReferenceInterval.activeIntervals.get(i).getInterval());
                    currentReferenceInterval.activeIntervals.get(i).generateHcnProjections(currentReferenceInterval.activeIntervals.get(i - 1));
                }
            }

        return currentReferenceInterval.activeIntervals.get(index).getHcnProjections();
    }

    private final List<ActiveInterval> activeIntervals = new ArrayList<>();
    private int lastHcnOrder;
    private int lastActiveInterval;
    private RecorderBody lastActiveRecorder;


    public void preCalculations(Passing passing) {

        log.debug("preCalculations: lastHcnOrder: {}, lastActiveInterval: {}, lastActiveRecorder.order: {}", lastHcnOrder, lastActiveInterval, lastActiveRecorder.getOrder());

        TreeSet<RecorderBody> preRecorderBodies = new TreeSet<>();
        passing.getPassingHcns().stream().filter(hcn -> hcn.getHcnGenerator().getRecorderBody() != null)
                .forEach(recorderHunterHcn -> preRecorderBodies.add(recorderHunterHcn.getHcnGenerator().getRecorderBody()));
        passing.getPrayHcns().forEach(recorderHcn -> preRecorderBodies.add(recorderHcn.getHcnGenerator().getRecorderBody()));

        log.debug("preRecorderBodies: {}", preRecorderBodies);

        Hcn smallestOrderHcn = passing.getPrayHcns().first();
        Hcn largestOrderHcn = passing.getPrayHcns().first();

        for (Hcn hcn : passing.getPrayHcns()) {
            if (hcn.getHcnGenerator().getRecorderBody().getOrder() < smallestOrderHcn.getHcnGenerator().getRecorderBody().getOrder()) smallestOrderHcn = hcn;
            if (hcn.getHcnGenerator().getRecorderBody().getOrder() > largestOrderHcn.getHcnGenerator().getRecorderBody().getOrder()) largestOrderHcn = hcn;
        }

        for (Hcn hcn : passing.getPassingHcns()) {
            if (hcn.getHcnGenerator().getRecorderBody() != null) {
                if (hcn.getHcnGenerator().getRecorderBody().getOrder() > largestOrderHcn.getHcnGenerator().getRecorderBody().getOrder()) largestOrderHcn = hcn;
            }
        }

        log.debug("smallestOrderHcn: {}", smallestOrderHcn);
        log.debug("largestOrderHcn: {}", largestOrderHcn);

        int order = smallestOrderHcn.getHcnGenerator().getRecorderBody().getOrder();
        log.debug(" order {}", order);
        int hcnOrderAdvancement = order - lastActiveRecorder.getOrder();
        log.debug(" hcnOrderAdvancement: {}", hcnOrderAdvancement);

        Hcn firstPrayHcn = passing.getPrayHcns().first();
        int interval = firstPrayHcn.getLapiIndex() + firstPrayHcn.getHcnGenerator().getRecorderBody().getIntervalLapiDistance();
        if (passing.prayHcnsContainFirstRecorder()) {
            interval--;
            log.debug(" interval down {}", interval);
            hcnOrderAdvancement += RecorderBody.getRecorderListSize();
            log.debug(" hcnOrderAdvancement: {}", hcnOrderAdvancement);
        }
        log.debug(" interval {}", interval);

        if (interval != lastActiveInterval) {
            createNextActiveInterval(interval);
            log.debug(" interval changed to: {}", interval);
            int extra = (interval - lastActiveInterval) * RecorderBody.getRecorderListSize();
            hcnOrderAdvancement = hcnOrderAdvancement + extra;
            lastActiveInterval = interval;
            log.debug(" hcnOrderAdvancement: {}", hcnOrderAdvancement);
        }
        lastHcnOrder = lastHcnOrder + hcnOrderAdvancement;
        log.debug(" lastHcnOrder {}", lastHcnOrder);
        passing.setHcnOrder(lastHcnOrder);
        passing.setInterval(interval);
        //passing.setLasrgestOrderPrev(largestOrderHcn);
        log.debug("");
    }

    public void postCalculations(Passing currentPassing) {

        log.debug("postCalculations: lastHcnOrder: {}, lastActiveInterval: {}, lastActiveRecorder.order: {}", lastHcnOrder, lastActiveInterval, lastActiveRecorder.getOrder());

        List<Hcn> activityHcns = new ArrayList<>();
        for (Hcn passingHcn : currentPassing.getPassingHcns()) {
            //if (passingHcn.getHcnStatus().equals(HcnStatus.FIRST_RECORDER)) {
                passingHcn.setOrder(lastHcnOrder);
            log.debug("passingHcn: {}, lastHcnOrder: {}", passingHcn, lastHcnOrder);
                lastHcnOrder++;
                activityHcns.add(passingHcn);
            lastActiveRecorder = passingHcn.getHcnGenerator().getRecorderBody();
            //}
        }
        for (Hcn prayHcn : currentPassing.getPrayHcns()) {
            if (prayHcn.getHcnStatus().equals(HcnStatus.RECORDER_DEACTIVATED)) {
                activityHcns.add(prayHcn);
                log.debug("prayHcn: {}", prayHcn);
            } else {
                lastHcnOrder++;
                lastActiveRecorder = prayHcn.getHcnGenerator().getRecorderBody();
            }
        }

        log.debug("lastActiveRecorder: {}", lastActiveRecorder);
        ActiveInterval.getCurrentActiveInterval().getActivity().add(activityHcns);
        ActiveInterval.getCurrentActiveInterval().getHcnsIdsForActivity().add(currentPassing.getHcnOrder());
        log.debug("");
    }

/*
    public void setHcnOrderForNewRecorder(Hcn triggerHcn) {
        log.debug("TriggerHcn: {}", triggerHcn);
        int interval = triggerHcn.getLapiIndex() + triggerHcn.getHcnGenerator().getRecorderBody().getIntervalLapiDistance();

        int order = triggerHcn.getHcnGenerator().getRecorderBody().getOrder();
        int hcnOrderAdvancement = order - lastActiveRecorder.getOrder();
        log.debug(" interval {}, order: {}, hcnOrderAdvancement: {}", interval, order, hcnOrderAdvancement);

        if (interval != lastActiveInterval) {
            createNextActiveInterval(interval);
            int extra = (interval - lastActiveInterval) * RecorderBody.getRecorderListSize();
            hcnOrderAdvancement = hcnOrderAdvancement + extra;
            log.debug(" hcnOrderAdvancement: {}", hcnOrderAdvancement);
        }

        lastHcnOrder = lastHcnOrder + hcnOrderAdvancement;
        triggerHcn.setOrder(lastHcnOrder);
        //ActiveInterval.getCurrentActiveInterval().getActivityHcns().add(triggerHcn);
        //ActiveInterval.getCurrentActiveInterval().addHcnToActivityMap(triggerHcn);

        lastActiveInterval = interval;
        lastActiveRecorder = triggerHcn.getHcnGenerator().getRecorderBody();
        log.debug("setHcnOrderForNewRecorder {}, lastHcnOrder: {}, lastActiveRecorder: {}, lastActiveInterval: {}", triggerHcn, lastHcnOrder, lastActiveRecorder, lastActiveInterval);
    }

    public void addDeleteHcn(Hcn prayHcn, Passing currentPassing) {
        //log.debug("prayHcn {}, getAdvancingRecorderHunterCount: {}, getNewRecorderCountCount: {}", prayHcn, currentPassing.getAdvancingRecorderHunterCount(), currentPassing.getNewRecorderCountCount());
        int interval = prayHcn.getLapiIndex() + prayHcn.getHcnGenerator().getRecorderBody().getIntervalLapiDistance();
        int order = prayHcn.getHcnGenerator().getRecorderBody().getOrder();
        int hcnOrderAdvancement = order - lastActiveRecorder.getOrder();
        log.debug(" interval {}, order: {}, hcnOrderAdvancement: {}", interval, order, hcnOrderAdvancement);
        if (prayHcn.getHcnGenerator().getRecorderBody().equals(RecorderBody.getFirstRecorder())) {
            interval--;
            hcnOrderAdvancement += RecorderBody.getRecorderListSize();
            log.debug(" interval {}, hcnOrderAdvancement: {}", interval, hcnOrderAdvancement);
        }

        if (interval != lastActiveInterval) {
            createNextActiveInterval(interval);
            hcnOrderAdvancement += (interval - lastActiveInterval) * RecorderBody.getRecorderListSize();
            log.debug(" hcnOrderAdvancement {}", hcnOrderAdvancement);
        }

        //if (killerHcn.getHcnStatus().equals(H))
        lastHcnOrder += hcnOrderAdvancement -1;
        log.debug(" lastHcnOrder {}", lastHcnOrder);
        //TODO whatever
        int orderToSet = lastHcnOrder + 1;
        prayHcn.setOrder(orderToSet);

        //ActiveInterval.getCurrentActiveInterval().getActivityHcns().add(prayHcn);
        //ActiveInterval.getCurrentActiveInterval().addHcnToActivityMap(prayHcn);
        lastActiveRecorder = prayHcn.getHcnGenerator().getRecorderBody().getPreviousRecorder();
        lastActiveInterval = interval;
        log.debug(" addDeleteHcn {}, lastHcnOrder: {}, lastActiveRecorder: {}, lastActiveInterval: {}, orderToSet: {}", prayHcn, lastHcnOrder, lastActiveRecorder, lastActiveInterval, orderToSet);
    }

 */


    private void createNextActiveInterval(int interval) {
        ActiveInterval.getCurrentActiveInterval().setPostRecorderListSize(RecorderBody.getRecorderListSize());
        ActiveInterval.setCurrentActiveInterval(ActiveInterval.builder().interval(interval).build());
        activeIntervals.add(ActiveInterval.getCurrentActiveInterval());
    }

}
