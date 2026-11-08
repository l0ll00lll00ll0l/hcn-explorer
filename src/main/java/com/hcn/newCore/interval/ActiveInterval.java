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
        boolean referenceList;

        Iterator<Hcn> previousHcnBaseIterator = referenceInterval.getHcnProjections().iterator();
        Iterator<ActivityZone> activityIterator = activityZones.iterator();

        Hcn nextPrevHcnProjection = previousHcnBaseIterator.next();
        //log.debug(" nextPrevHcnProjection initial: {}", nextPrevHcnProjection);
        while (!nextPrevHcnProjection.getHcnGenerator().equals(firstRecorder.getHcnGenerator())) {
            nextPrevHcnProjection = previousHcnBaseIterator.next();
        }
        /*
        for (int i = 0; i < referenceInterval.offset; i++) {
            nextPrevHcnProjection = previousHcnBaseIterator.next();
        }

         */
        //log.debug(" with offset adjust: {}", nextPrevHcnProjection);
        ActivityZone nextActivityZone = activityIterator.next();
        //log.debug(" nextActivityZone initial: {}", nextActivityZone.getHcns());

        while (nextPrevHcnProjection != null) {

            if (nextActivityZone != null) {
                if (hcnOrder < nextActivityZone.getHcnOrder()) {
                    hcnProjections.add(generateTempHcnBasedOnReference(nextPrevHcnProjection, lapiDistance, hcnOrder));
                    //log.debug(" nextPrevHcnProjection: {} - {}", hcnProjections.get(hcnProjections.size() - 1), hcnOrder);
                    hcnOrder++;
                    nextPrevHcnProjection = previousHcnBaseIterator.hasNext() ? previousHcnBaseIterator.next() : null;
                    //log.debug(" nextPrevHcnProjection moves To 1: {}, hcnOrder: {}", nextPrevHcnProjection, hcnOrder);
                } else {
                    hcnProjections.addAll(nextActivityZone.getHcns());
                    //log.debug(" nextActivityZone: {}", nextActivityZone.getHcns());
                    hcnOrder = hcnOrder + nextActivityZone.getHcns().size();
                    for (int i = 0; i < nextActivityZone.getSkippingCount(); i++) {
                        nextPrevHcnProjection = previousHcnBaseIterator.hasNext() ? previousHcnBaseIterator.next() : null;
                    }

                    nextActivityZone = activityIterator.hasNext() ? activityIterator.next() : null;
                    //log.debug(" nextActivityZone moves To 1: {}, hcnOrder: {}", nextActivityZone == null ? nextActivityZone : nextActivityZone.getHcns() , hcnOrder);
                    /*
                    if (nextActivityZone.getHcnStatus().equals(HcnStatus.FIRST_RECORDER)) {
                        //hcnProjections.add(nextActivityZone);
                        //log.debug(" nextActivityZone: {}", nextActivityZone);
                        //hcnOrder++;

                        log.debug(" nextActivityZone moves To: {}", nextActivityZone);
                    } else {
                        nextPrevHcnProjection = previousHcnBaseIterator.hasNext() ? previousHcnBaseIterator.next() : null;
                        nextActivityZone = activityIterator.hasNext() ? activityIterator.next() : null;
                        log.debug(" nextPrevHcnProjection moves To 2: {}", nextPrevHcnProjection);
                    }

                     */
                }
            } else {
                hcnProjections.add(generateTempHcnBasedOnReference(nextPrevHcnProjection, lapiDistance, hcnOrder));
                //log.debug(" nextPrevHcnProjection: {} - {}", hcnProjections.get(hcnProjections.size() - 1), hcnOrder);
                hcnOrder++;
                nextPrevHcnProjection = previousHcnBaseIterator.hasNext() ? previousHcnBaseIterator.next() : null;
                //log.debug(" nextPrevHcnProjection moves To 2: {}, hcnOrder: {}", nextPrevHcnProjection, hcnOrder);
            }
        }

        if (nextActivityZone != null) {
            lapiDistance = 1;
            int shiftingHcnIndex = 0;

            while (nextActivityZone != null)  {

                if (hcnOrder == nextActivityZone.getHcnOrder()) {

                    hcnProjections.addAll(nextActivityZone.getHcns());
                    //log.debug(" nextActivityZone: {}", nextActivityZone.getHcns());
                    hcnOrder = hcnOrder + nextActivityZone.getHcns().size();
                    for (int i = 0; i < nextActivityZone.getSkippingCount(); i++) {
                        shiftingHcnIndex++;
                    }
                    nextActivityZone = activityIterator.hasNext() ? activityIterator.next() : null;

                    //log.debug(" nextActivityZone moves To 1: {}, hcnOrder: {}, shiftingHcnIndex: {}", nextActivityZone == null ? nextActivityZone : nextActivityZone.getHcns() , hcnOrder, shiftingHcnIndex);

                } else {
                    hcnProjections.add(generateTempHcnBasedOnReference(hcnProjections.get(shiftingHcnIndex), lapiDistance, hcnOrder));
                    //log.debug(" nextHcnProjection lower: {} - {}", hcnProjections.get(hcnProjections.size() - 1), hcnOrder);
                    hcnOrder++;
                    shiftingHcnIndex++;
                    //log.debug(" shiftingHcnIndex: {}, hcnOrder: {}", shiftingHcnIndex, hcnOrder);
                }
            }

            while (hcnProjections.get(shiftingHcnIndex).getLapiIndex() != interval) {
                hcnProjections.add(generateTempHcnBasedOnReference(hcnProjections.get(shiftingHcnIndex), lapiDistance, hcnOrder));
                //log.debug(" nextHcnProjection post: {} - {}", hcnProjections.get(hcnProjections.size() - 1), hcnOrder);
                hcnOrder++;
                shiftingHcnIndex++;
                //log.debug(" shiftingHcnIndex: {}, hcnOrder: {}", shiftingHcnIndex, hcnOrder);
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
