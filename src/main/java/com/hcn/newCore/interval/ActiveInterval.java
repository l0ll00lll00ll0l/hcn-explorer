package com.hcn.newCore.interval;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hcn.newCore.Hcn;
import com.hcn.newCore.RecorderBody;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Getter
@Setter
@Builder
@Slf4j
public class ActiveInterval {

    private static ActiveInterval currentActiveInterval;

    public static ActiveInterval getCurrentActiveInterval() {return currentActiveInterval;}
    public static void setCurrentActiveInterval(ActiveInterval activeInterval) {ActiveInterval.currentActiveInterval = activeInterval;}

    public static void reset() {
        currentActiveInterval = null;
    }

    public static void initialize() {
        currentActiveInterval = ActiveInterval.builder().interval(0).build();
    }



    private final int interval;
    private RecorderBody firstRecorder;
    @JsonIgnore
    private final List<ActivityZone> activityZones = new ArrayList<>();
    @JsonIgnore
    private final List<Hcn> hcnProjections = new ArrayList<>();
    @Builder.Default
    private Integer postRecorderListSize = null;

    public void generateHcnProjections(ActiveInterval referenceInterval) {

        int missingIntervalCount = (interval - referenceInterval.interval - 1) * referenceInterval.postRecorderListSize;
        int hcnOrder = referenceInterval.hcnProjections.get(referenceInterval.hcnProjections.size() - 1).getOrder() + 1 + missingIntervalCount;
        int lapiDistance = interval - referenceInterval.interval;

        Iterator<Hcn> previousHcnBaseIterator = referenceInterval.getHcnProjections().iterator();
        Iterator<ActivityZone> activityIterator = activityZones.iterator();

        Hcn nextPrevHcnProjection = previousHcnBaseIterator.next();
        while (!nextPrevHcnProjection.getHcnGenerator().equals(firstRecorder.getHcnGenerator())) {
            nextPrevHcnProjection = previousHcnBaseIterator.next();
        }
        ActivityZone nextActivityZone = activityIterator.next();

        while (nextPrevHcnProjection != null) {

            if (nextActivityZone != null) {
                if (hcnOrder < nextActivityZone.getHcnOrder()) {
                    hcnProjections.add(generateTempHcnBasedOnReference(nextPrevHcnProjection, lapiDistance, hcnOrder));
                    hcnOrder++;
                    nextPrevHcnProjection = previousHcnBaseIterator.hasNext() ? previousHcnBaseIterator.next() : null;
                } else {
                    hcnProjections.addAll(nextActivityZone.getHcns());
                    hcnOrder = hcnOrder + nextActivityZone.getHcns().size();
                    for (int i = 0; i < nextActivityZone.getSkippingCount(); i++) {
                        nextPrevHcnProjection = previousHcnBaseIterator.hasNext() ? previousHcnBaseIterator.next() : null;
                    }
                    nextActivityZone = activityIterator.hasNext() ? activityIterator.next() : null;
                }
            } else {
                hcnProjections.add(generateTempHcnBasedOnReference(nextPrevHcnProjection, lapiDistance, hcnOrder));
                hcnOrder++;
                nextPrevHcnProjection = previousHcnBaseIterator.hasNext() ? previousHcnBaseIterator.next() : null;
            }
        }

        if (nextActivityZone != null) {
            lapiDistance = 1;
            int shiftingHcnIndex = 0;
            while (nextActivityZone != null)  {
                if (hcnOrder == nextActivityZone.getHcnOrder()) {
                    hcnProjections.addAll(nextActivityZone.getHcns());
                    hcnOrder = hcnOrder + nextActivityZone.getHcns().size();
                    for (int i = 0; i < nextActivityZone.getSkippingCount(); i++) {
                        shiftingHcnIndex++;
                    }
                    nextActivityZone = activityIterator.hasNext() ? activityIterator.next() : null;
                } else {
                    hcnProjections.add(generateTempHcnBasedOnReference(hcnProjections.get(shiftingHcnIndex), lapiDistance, hcnOrder));
                    hcnOrder++;
                    shiftingHcnIndex++;
                }
            }

            while (hcnProjections.get(shiftingHcnIndex).getLapiIndex() != interval) {
                hcnProjections.add(generateTempHcnBasedOnReference(hcnProjections.get(shiftingHcnIndex), lapiDistance, hcnOrder));
                hcnOrder++;
                shiftingHcnIndex++;
            }
        }
    }

    private Hcn generateTempHcnBasedOnReference(Hcn referencehcn, int lapiDistance, int hcnOrder) {
        return referencehcn.getHcnGenerator().generateHcnProjection(referencehcn.getLastActivePrime().getPreviousPrime(-lapiDistance), hcnOrder);
    }

    @Override
    public String toString() {
        return "[" + interval + "] - " + activityZones.size();
    }

    public void addActivityZone(List<Hcn> activityHcns, int hcnOrder, int skippingCount) {
        activityZones.add(ActivityZone.builder().hcns(activityHcns).hcnOrder(hcnOrder).skippingCount(skippingCount).build());
    }
}
