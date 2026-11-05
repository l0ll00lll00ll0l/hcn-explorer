package com.hcn.newCore.interval;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.hcn.newCore.Hcn;
import com.hcn.newCore.HcnStatus;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

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
        currentActiveInterval = ActiveInterval.builder().interval(0).offset(0).build();
    }

    public static void increaseOffset() {
        currentActiveInterval.offset++;
    }


    private int interval;
    @JsonIgnore
    private final List<Hcn> activityHcns = new ArrayList<>();
    private final List<List<Hcn>> activity = new ArrayList<>();
    private final List<Integer> hcnsIdsForActivity = new ArrayList<>();
    //private final Map<Integer, List<Hcn>> activityMap = new LinkedHashMap<>();
    @JsonIgnore
    private final List<Hcn> hcnProjections = new ArrayList<>();
    private int offset;
    @Builder.Default
    private Integer postRecorderListSize = null;

    public void generateHcnProjections(ActiveInterval referenceInterval) {

        int hcnOrder = referenceInterval.hcnProjections.get(referenceInterval.hcnProjections.size() - 1).getOrder() + 1;
        int lapiDistance = interval - referenceInterval.interval;
        boolean referenceList;

        Iterator<Hcn> previousHcnBaseIterator = referenceInterval.getHcnProjections().iterator();
        Iterator<Hcn> activityIterator = activityHcns.iterator();

        Hcn nextPrevHcnProjection = previousHcnBaseIterator.next();
        log.debug(" nextPrevHcnProjection initial: {}", nextPrevHcnProjection);
        for (int i = 0; i < referenceInterval.offset; i++) {
            nextPrevHcnProjection = previousHcnBaseIterator.next();
        }
        log.debug(" with offset adjust: {}", nextPrevHcnProjection);
        Hcn nextActivityHcn = activityIterator.hasNext() ? activityIterator.next() : null;
        log.debug(" nextActivityHcn initial: {}", nextActivityHcn);

        while (nextPrevHcnProjection != null) {

            if (nextActivityHcn != null) {
                if (hcnOrder < nextActivityHcn.getOrder()) {
                    hcnProjections.add(generateTempHcnBasedOnReference(nextPrevHcnProjection, lapiDistance, hcnOrder));
                    log.debug(" nextPrevHcnProjection: {} - {}", hcnProjections.get(hcnProjections.size() - 1), hcnOrder);
                    hcnOrder++;
                    nextPrevHcnProjection = previousHcnBaseIterator.hasNext() ? previousHcnBaseIterator.next() : null;
                    log.debug(" nextPrevHcnProjection moves To 1: {}", nextPrevHcnProjection);
                } else {
                    if (nextActivityHcn.getHcnStatus().equals(HcnStatus.FIRST_RECORDER)) {
                        hcnProjections.add(nextActivityHcn);
                        log.debug(" nextActivityHcn: {}", nextActivityHcn);
                        hcnOrder++;
                        nextActivityHcn = activityIterator.hasNext() ? activityIterator.next() : null;
                        log.debug(" nextActivityHcn moves To: {}", nextActivityHcn);
                    } else {
                        nextPrevHcnProjection = previousHcnBaseIterator.hasNext() ? previousHcnBaseIterator.next() : null;
                        nextActivityHcn = activityIterator.hasNext() ? activityIterator.next() : null;
                        log.debug(" nextPrevHcnProjection moves To 2: {}", nextPrevHcnProjection);
                    }
                }
            } else {
                hcnProjections.add(generateTempHcnBasedOnReference(nextPrevHcnProjection, lapiDistance, hcnOrder));
                log.debug(" nextPrevHcnProjection: {} - {}", hcnProjections.get(hcnProjections.size() - 1), hcnOrder);
                hcnOrder++;
                nextPrevHcnProjection = previousHcnBaseIterator.hasNext() ? previousHcnBaseIterator.next() : null;
                log.debug(" nextPrevHcnProjection moves To: {}", nextPrevHcnProjection);
            }
        }

        if (nextActivityHcn != null) {
            lapiDistance = 1;
            int shiftingHcnIndex = 0;

            while (nextActivityHcn != null)  {

                if (hcnOrder == nextActivityHcn.getOrder()) {
                    if (nextActivityHcn.getHcnStatus().equals(HcnStatus.FIRST_RECORDER)) {
                        hcnProjections.add(nextActivityHcn);
                        log.debug(" nextActivityHcn lower: {}", nextActivityHcn);
                        hcnOrder++;
                        nextActivityHcn = activityIterator.hasNext() ? activityIterator.next() : null;
                        log.debug(" nextActivityHcn lower moves To: {}", nextActivityHcn);
                    } else {
                        shiftingHcnIndex++;
                        nextActivityHcn = activityIterator.hasNext() ? activityIterator.next() : null;
                        log.debug(" nextActivityHcn lower moves To 2: {}", nextActivityHcn);
                        log.debug(" nshiftingHcnIndex: {}", shiftingHcnIndex);
                    }

                } else {
                    hcnProjections.add(generateTempHcnBasedOnReference(hcnProjections.get(shiftingHcnIndex), lapiDistance, hcnOrder));
                    log.debug(" nextHcnProjection lower: {} - {}", hcnProjections.get(hcnProjections.size() - 1), hcnOrder);
                    hcnOrder++;
                    shiftingHcnIndex++;
                    log.debug(" shiftingHcnIndex: {}", shiftingHcnIndex);
                }
            }
        }
    }

    private Hcn generateTempHcnBasedOnReference(Hcn referencehcn, int lapiDistance, int hcnOrder) {
        return referencehcn.getHcnGenerator().generateHcnProjection(referencehcn.getLastActivePrime().getPreviousPrime(-lapiDistance), hcnOrder);
    }

    @Override
    public String toString() {
        return "[" + interval + "] - " + activityHcns.size();
    }

}
