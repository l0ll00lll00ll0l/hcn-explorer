package com.hcn.newCore;

import com.hcn.newCore.interval.ReferenceInterval;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

@Getter
@Setter
@Slf4j
public class Passing {

    private static Passing lowestPassing;

    public static Passing getLowestPassing() {return lowestPassing;}
    public static void setLowestPassing(Passing lowestPassing) {Passing.lowestPassing = lowestPassing;}

    public static void reset() { lowestPassing = null; }

    public static void processNextPassing() {

        Passing currentPassing = lowestPassing;
        currentPassing.prayHcns.first().getHcnGenerator().getRecorderBody().generatePassingHcns(currentPassing);

        currentPassing.passingHcns.forEach(hcn -> {
            Extension.extend(hcn);
            if (hcn.getHcnStatus().equals(HcnStatus.FIRST_RECORDER)) {
                new RecorderBody(hcn);
            } else {
                hcn.getHcnGenerator().getRecorderBody().resetReferencedHuntersFromPreviousRecorderBody(currentPassing.prayHcns.first());
            }
        });

        ScientificNumber factorRecord = currentPassing.passingHcns.last().getFactor();
        for (Hcn prayHcn : currentPassing.prayHcns) {
            if (factorRecord.isNotSmallerThan(prayHcn.getFactor())) {
                prayHcn.getHcnGenerator().getRecorderBody().deleteRecorderBody(currentPassing);
            } else {
                prayHcn.getHcnGenerator().getRecorderBody().createPassingZoneAfterSurvival(currentPassing.passingHcns.last());
                factorRecord = prayHcn.getFactor();
            }
        }

        ReferenceInterval.getCurrentReferenceInterval().postCalculations(currentPassing);
        removePassingFromStaticChain(currentPassing);
    }


    private static Passing placePassingInStaticChain(RecorderBody prayBody) {
        //log.debug("   :::::::::::::::::::  placePassingZoneInStaticChain {}  :::::::::::::::::::: ", prayBody.getPassingHcn());

        Passing firstNotSmallerPassing = lowestPassing;
        Passing previousPassingZone = null;
        //log.debug(" firstNotSmallerPassing {}", firstNotSmallerPassing.passingHcn);
        while (firstNotSmallerPassing != null
                && firstNotSmallerPassing.passingHcn.getValue().isSmallerThan(prayBody.getPassingHcn().getValue())) {
            previousPassingZone = firstNotSmallerPassing;
            firstNotSmallerPassing = firstNotSmallerPassing.nextPassing;
            //log.debug(" previousPassingZone: {}",  previousPassingZone.passingHcn);
        }

        if (firstNotSmallerPassing != null) {
            if (firstNotSmallerPassing.passingHcn.getValue().isNotBiggerThan(prayBody.getPassingHcn().getValue())) {
                firstNotSmallerPassing.prayHcns.add(prayBody.getPrayHcn());
                //log.debug(" firstNotSmallerPassing EXTENDED {}", firstNotSmallerPassing.prayHcns);
                return firstNotSmallerPassing;
            } else {

                Passing newPassing = new Passing(prayBody);
                if (previousPassingZone != null) {
                    previousPassingZone.nextPassing = newPassing;
                } else {
                    lowestPassing = newPassing;
                }
                newPassing.prevPassing = previousPassingZone;
                firstNotSmallerPassing.prevPassing = newPassing;
                newPassing.nextPassing = firstNotSmallerPassing;
                String prev = "null";
                if (newPassing.prevPassing != null) {
                    prev = previousPassingZone.passingHcn.toString();
                }
                String next = "null";
                if (newPassing.nextPassing != null) {
                    next = newPassing.nextPassing.passingHcn.toString();
                }
                //log.debug("   ::::::::::::::::::: {} - {} - {} :::::::::::::::::::: ", prev, prayBody.passingHcn, next);
                return newPassing;
            }
        } else {
            Passing newPassing = new Passing(prayBody);
            newPassing.prevPassing = previousPassingZone;
            previousPassingZone.nextPassing = newPassing;
            newPassing.nextPassing = firstNotSmallerPassing;
            return newPassing;
        }
    }

    public static void removePassingFromStaticChain(Passing passingZoneToRemove) {
        //log.debug("   -----_______------ removeFromStaticChain {}   ---------____________-------", passingZoneToRemove.passingHcn);
        //printFirstNpassings(3);

        if (passingZoneToRemove.equals(lowestPassing)) {
            //log.debug("lowerMovedTo: {}", passingZoneToRemove.nextPassing.passingHcn);
            setLowestPassing(passingZoneToRemove.nextPassing);
        }

        Passing prev = passingZoneToRemove.prevPassing;
        Passing next = passingZoneToRemove.nextPassing;

        passingZoneToRemove.setPrevPassing(null);
        passingZoneToRemove.setNextPassing(null);

        if (prev != null) {
            //log.debug("prev: {}", prev.passingHcn);
            prev.nextPassing = next;
        }
        if (next != null) {
            //log.debug("next: {}", next.passingHcn);
            next.prevPassing = prev;
        }

        //printFirstNpassings(3);
        //log.debug("");
    }

    private static void printFirstNpassings(int count) {
        Passing toPrint = lowestPassing;
        int order = 0;

        while (toPrint != null && order < count) {
            order++;
            log.debug("[{}] - {}", order, toPrint);
            toPrint = toPrint.nextPassing;
        }
    }

    private final TreeSet<Hcn> prayHcns = new TreeSet<>();
    private final Hcn passingHcn;
    private final TreeSet<Hcn> passingHcns = new TreeSet<>();
    private Passing prevPassing;
    private Passing nextPassing;
    private int hcnOrder;
    private int interval;
    private final TreeSet<RecorderBody> preRecorderBodies = new TreeSet<>();
    private int skippingCount = 0;

    public Passing (RecorderBody prayBody) {
        this.prayHcns.add(prayBody.getPrayHcn());
        this.passingHcn = prayBody.getPassingHcn();
    }

    //only in initialize
    public Passing(Passing nextPassing, Passing prevPassing, RecorderBody prayBody) {
        this.nextPassing = nextPassing;
        this.prevPassing = prevPassing;
        this.prayHcns.add(prayBody.getPrayHcn());
        this.passingHcn = prayBody.getPassingHcn();
    }

    public static Passing assignPassing(RecorderBody recorderBody) {
       Passing passing = Passing.placePassingInStaticChain(recorderBody);
       log.debug("Generated passing for {}, passing: {}", recorderBody, passing.getPrayHcns());
        return passing;
    }

    public boolean prayHcnsContainFirstRecorder() {
        return prayHcns.stream().anyMatch(hcn -> hcn.getHcnGenerator().getRecorderBody().equals(RecorderBody.getFirstRecorder()));
    }

    @Override
    public String toString() {
        return "passingHcn=" + passingHcn +
                ", prayHcns=" + prayHcns +
                ", passingHcns=" + passingHcns;
    }

    public void removeRecorderBody(RecorderBody prayBody) {
        if (prayHcns.size() > 1) {
            log.debug("HERE WE GO {}");
            Hcn prayHcnToRemove = prayHcns.stream().filter(prayHcn -> prayHcn.getHcnGenerator().equals(prayBody.getHcnGenerator())).findFirst().get();
            prayHcns.remove(prayHcnToRemove);
        } else {
            removePassingFromStaticChain(this);
        }
    }

    public void calculateSkipCount(ScientificNumber factorLimit) {
        for (Hcn prayHcn : prayHcns) {
            if (factorLimit.isNotSmallerThan(prayHcn.getFactor())) {
                prayHcn.setHcnStatus(HcnStatus.RECORDER_DEACTIVATED);
                preRecorderBodies.add(prayHcn.getHcnGenerator().getRecorderBody());
                skippingCount++;
            } else {
                prayHcn.setHcnStatus(HcnStatus.RECORDER_SURVIVED);
                preRecorderBodies.add(prayHcn.getHcnGenerator().getRecorderBody());
            }
        }
    }
}
