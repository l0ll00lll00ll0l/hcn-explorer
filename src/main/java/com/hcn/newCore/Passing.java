package com.hcn.newCore;

import com.hcn.newCore.interval.ActiveInterval;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.TreeSet;

@Getter
@Setter
@Slf4j
public class Passing {

    private static Passing lowestPassing;

    public static Passing getLowestPassing() {return lowestPassing;}
    public static void setLowestPassing(Passing lowestPassing) {Passing.lowestPassing = lowestPassing;}

    public static void processNextPassing() {

        Passing currentPassing = lowestPassing;
        currentPassing.prayHcns.first().getHcnGenerator().getRecorderBody().generatePassingHcns(currentPassing);
        currentPassing.passingHcns.forEach(hcn -> {
            Extension.extend(hcn);
            if (hcn.getHcnGenerator().getRecorderBody() == null) {
                new RecorderBody(hcn);
            } else {
                hcn.getHcnGenerator().getRecorderBody().resetReferencedHuntersFromPreviousRecorderBody(hcn, currentPassing.prayHcns.first());
            }
        });

        ScientificNumber factorRecord = currentPassing.passingHcns.last().getFactor();
        for (Hcn prayHcn : currentPassing.prayHcns) {
            if (factorRecord.isNotSmallerThan(prayHcn.getFactor())) {
                prayHcn.getHcnGenerator().getRecorderBody().deleteRecorderBody();
            } else {
                prayHcn.getHcnGenerator().getRecorderBody().createPassingZoneAfterSurvival(currentPassing.passingHcns.last());
                factorRecord = prayHcn.getFactor();
            }
        }

        ActiveInterval.processPassing(currentPassing);
    }


    public static void placePassingInStaticChain(Passing passingZoneToPlace) {
        //log.debug("   :::::::::::::::::::  placePassingZoneInStaticChain {}  :::::::::::::::::::: ", passingZoneToPlace.passingHcn);

        Passing firstNotSmallerPassing = lowestPassing;
        Passing previousPassingZone = null;
        //log.debug(" firstNotSmallerPassing {}", firstNotSmallerPassing.passingHcn);
        while (firstNotSmallerPassing != null
                && firstNotSmallerPassing.passingHcn.getValue().isSmallerThan(passingZoneToPlace.passingHcn.getValue())) {
            previousPassingZone = firstNotSmallerPassing;
            firstNotSmallerPassing = firstNotSmallerPassing.nextPassing;
            //log.debug(" previousPassingZone: {}",  previousPassingZone.passingHcn);
        }

        if (firstNotSmallerPassing != null) {
            if (firstNotSmallerPassing.passingHcn.getValue().isNotBiggerThan(passingZoneToPlace.passingHcn.getValue())) {
                firstNotSmallerPassing.prayHcns.addAll(passingZoneToPlace.prayHcns);
                for (Hcn prayHcn : passingZoneToPlace.getPrayHcns()) {
                    prayHcn.getHcnGenerator().getRecorderBody().setPassing(firstNotSmallerPassing);
                }
                //log.debug(" firstNotSmallerPassing EXTENDED {}", firstNotSmallerPassing.recorderBodies);
            } else {
                if (previousPassingZone != null) {
                    previousPassingZone.nextPassing = passingZoneToPlace;
                } else {
                    lowestPassing = passingZoneToPlace;
                }
                passingZoneToPlace.prevPassing = previousPassingZone;
                firstNotSmallerPassing.prevPassing = passingZoneToPlace;
                passingZoneToPlace.nextPassing = firstNotSmallerPassing;
                String prev = "null";
                if (passingZoneToPlace.prevPassing != null) {
                    prev = previousPassingZone.passingHcn.toString();
                }
                String next = "null";
                if (passingZoneToPlace.nextPassing != null) {
                    next = passingZoneToPlace.nextPassing.passingHcn.toString();
                }
                //log.debug("   ::::::::::::::::::: {} - {} - {} :::::::::::::::::::: ", prev, passingZoneToPlace.passingHcn, next);
            }
            firstNotSmallerPassing.prevPassing = passingZoneToPlace;
        } else {
            passingZoneToPlace.prevPassing = previousPassingZone;
            previousPassingZone.nextPassing = passingZoneToPlace;
            passingZoneToPlace.nextPassing = firstNotSmallerPassing;
        }

    }

    public static void removePassingFromStaticChain(Passing passingZoneToRemove) {
        //log.debug("   -----_______------ removeFromStaticChain {}   ---------____________-------", prayHcn);
        if (passingZoneToRemove.equals(lowestPassing)) {
            setLowestPassing(passingZoneToRemove.nextPassing);
        }

        Passing prev = passingZoneToRemove.prevPassing;
        Passing next = passingZoneToRemove.nextPassing;

        passingZoneToRemove.setPrevPassing(null);
        passingZoneToRemove.setNextPassing(null);

        if (prev != null) {
            prev.nextPassing = next;
        }
        if (next != null) {
            next.prevPassing = prev;
        }
    }

    private final TreeSet<Hcn> prayHcns = new TreeSet<>();
    private final Hcn passingHcn;
    private final TreeSet<Hcn> passingHcns = new TreeSet<>();
    private Passing prevPassing;
    private Passing nextPassing;

    public Passing (RecorderBody prayBody) {
        this.prayHcns.add(prayBody.getPrayHcn());
        this.passingHcn = prayBody.getPassingHcn();
        Passing.placePassingInStaticChain(this);
    }

    public Passing(Passing nextPassing, Passing prevPassing, RecorderBody prayBody) {
        this.nextPassing = nextPassing;
        this.prevPassing = prevPassing;
        this.prayHcns.add(prayBody.getPrayHcn());
        this.passingHcn = prayBody.getPassingHcn();
    }
}
