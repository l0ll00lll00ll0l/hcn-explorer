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

@Getter
@Setter
@Builder
@Slf4j
public class ActiveInterval {

    private static ActiveInterval currentActiveInterval;

    public static ActiveInterval getCurrentActiveInterval() {return currentActiveInterval;}

    public static void reset() {
        currentActiveInterval = null;
    }

    public static void initialize() {
        currentActiveInterval = ActiveInterval.builder().interval(0).build();
    }

    private int interval;
    private final List<Passing> passings = new ArrayList<>();
    private final List<Hcn> hcns = new ArrayList<>();

    public static void processPassing(Passing currentPassing) {
        log.debug("ActiveInterval starting");


        int interval = currentPassing.getPassingHcn().getLapiIndex() + currentPassing.getPassingHcn().getHcnGenerator().getRecorderBody().getIntervalLapiDistance();
        log.debug(" passingHcn: {} interval: {}", currentPassing.getPassingHcn(), interval);

        if (currentActiveInterval.interval != interval) {
            currentActiveInterval = ActiveInterval.builder().interval(interval).build();
            ReferenceInterval.getCurrentReferenceInterval().getActiveIntervals().add(currentActiveInterval);

            log.debug(" new currentInterval: {}", interval);
        }
        currentActiveInterval.passings.add(currentPassing);

        for (Hcn passingHcn : currentPassing.getPassingHcns()) {
            if (passingHcn.getHcnStatus().equals(HcnStatus.FIRST_RECORDER)) {
                currentActiveInterval.hcns.add(passingHcn);
            }
        }

        for (Hcn prayHcn : currentPassing.getPrayHcns()) {
            if (prayHcn.getHcnStatus().equals(HcnStatus.RECORDER_DEACTIVATED)) {
                currentActiveInterval.hcns.add(prayHcn);
            }
        }
    }
}
