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
    private Body lastBaseIntervalBody;
    private Interval previousInterval;
    private Interval nextInterval;
    private List<Body> postBaseBodyList = null;

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

    public static Interval getInterval(Prime passingPrayPrime) {

        Interval walker = globalReferenceInterval;
        Interval previousWalker = null;
        while (walker != null && walker.getPrime().getIndex() < passingPrayPrime.getIndex()) {
            previousWalker = walker;
            walker = walker.getNextInterval();
        }

        if (walker == null) {
            Interval newInterval = Interval.builder().prime(passingPrayPrime).hcnList(new ArrayList<>()).previousInterval(previousWalker).nextInterval(null).build();
            previousWalker.setNextInterval(newInterval);
            return newInterval;
        } else {
            if (walker.getPrime().getIndex() > passingPrayPrime.getIndex()) {
                Interval newInterval = Interval.builder().prime(passingPrayPrime).hcnList(new ArrayList<>()).previousInterval(previousWalker).nextInterval(walker).build();
                previousWalker.setNextInterval(newInterval);
                walker.setPreviousInterval(newInterval);
                return newInterval;
            } else {
                return walker;
            }
        }
    }

    public boolean isReferenced() {
        return referenceInterval != null && referenceInterval != this;
    }


    public void referenceCheck() {

        if (globalReferenceInterval.getHcnList().size() != hcnList.size()) {
            this.referenceInterval = this;
            setGlobalReferenceInterval(this);
        }
        for (int i = 0; i < hcnList.size(); i++) {
            if (!globalReferenceInterval.getHcnList().get(i).getHcnGenerator().equals(hcnList.get(i).getHcnGenerator())) {
                this.referenceInterval = this;
                setGlobalReferenceInterval(this);
            }
        }
        this.referenceInterval = globalReferenceInterval;
    }

}
