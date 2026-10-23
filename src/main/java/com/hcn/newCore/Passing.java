package com.hcn.newCore;

import lombok.Builder;
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

    public static void processNextPassing() {
        //log.debug("[PROCESS PASSING ZONE]]");
        RecorderList.setCurrentRecorder(lowestPassing.getPrayBody());
        ScientificNumber previousRecorderFactor = new ScientificNumber(1, 0);

        TreeSet<Hcn> passingHcns = lowestPassing.prayBody.generatePassingHcns();

        //log.debug("[PROCESS PASSING ZONE]]");
        //log.debug("passingHcns: {}", passingHcns);


        passingHcns.forEach(hcn -> {
            Extension.extend(hcn);
            RecorderBody newRecorderBody = new RecorderBody(hcn);
        });

        if (passingHcns.last().getFactor().isNotSmallerThan(lowestPassing.prayBody.getPrayHcn().getFactor())) {
            lowestPassing.prayBody.deleteRecorderBody();
        } else {
            lowestPassing.prayBody.createPassingZoneAfterSurvival(passingHcns.last());
        }
        /*
        for (Hcn hcn : globalNextPassingZone.getPassingZone()) {
            //log.debug("processNextPassingZone - {}", hcn);
            if (hcn.getHcnStatus().equals(HcnStatus.HUNTER_IN_PASSINGZONE)) {

                //globalNextPassingZone.removeFromHunters(hcn.getHcnGenerator());
                if (hcn.getFactor().isBiggerThan(previousRecorderFactor)) {
                    //log.info(" matrixMaintainCheck ofr hcn: {}", hcn);
                    hcn.getHcnGenerator().getBody().matrixMaintainCheck();
                    RecorderBody newRecorderBody = new RecorderBody(hcn);
                    previousRecorderFactor = hcn.getFactor();
                } else {
                    //TODO hunter switching pray implementation required
                    //log.debug("hunter switching pray implementation required :{}", activityHcn);
                }
            } else if (hcn.getHcnStatus() == HcnStatus.RECORDER_IN_PASSINGZONE) {
                //PassingZone passingZone = PassingZone.getGlobalNextPassingZone();
                if (hcn.getFactor().isBiggerThan(previousRecorderFactor)) {
                    globalNextPassingZone.createPassingZoneAfterSurvival();
                    //globalNextPassingZone.prayBodySurvivedCreateNewPassingZone();
                    previousRecorderFactor = hcn.getFactor();
                } else {
                    //log.info(" deleteRecorderBody hcn: {}", hcn);
                    hcn.getHcnGenerator().getRecorderBody().deleteRecorderBody(hcn);
                    //RecorderList.recorderToDeactivate(hcn);
                    //globalNextPassingZone.prayBodyDeactivated();
                }
            }
        }

         */
        //log.debug("");
    }


    public static void placePassingInStaticChain(Passing passingZoneToPlace) {
        //log.debug("   :::::::::::::::::::  placePassingZoneInStaticChain {}  :::::::::::::::::::: ", passingZoneToPlace);

        ScientificNumber pzKey = passingZoneToPlace.lowestPassingHcn.getValue();

        Passing firstLargerPassingZone = lowestPassing;
        Passing previousPassingZone = null;
        while (firstLargerPassingZone != null
                && firstLargerPassingZone.lowestPassingHcn.getValue().isSmallerThan(pzKey)) {
            previousPassingZone = firstLargerPassingZone;
            firstLargerPassingZone = firstLargerPassingZone.nextPassing;
        }

        if (previousPassingZone != null) {
            ScientificNumber previousPassingZoneEnd = previousPassingZone.prayBody.getPrayHcn().getValue();
            //log.debug("Floor PZ check: new entry: {}, previousPassingZoneEnd: {}", pzKey, previousPassingZoneEnd);
            if (previousPassingZoneEnd.isBiggerThan(pzKey)) {
                //log.debug(" PassingZoneMerge Required implementation");
            } else {
                //log.debug(" check: OK");
            }
            previousPassingZone.nextPassing = passingZoneToPlace;
        }

        if (firstLargerPassingZone != null) {
            ScientificNumber nextPassingZoneStart = firstLargerPassingZone.lowestPassingHcn.getValue();
            //log.debug("Ceiling PZ check: new entry passingZoneToPlace.passingZone.last(): {}, nextPassingZoneStart: {}", passingZoneToPlace.passingZone.last(), nextPassingZoneStart);
            if (nextPassingZoneStart.isSmallerThan(passingZoneToPlace.lowestPassingHcn.getValue())) {
                //log.debug(" PassingZoneMerge Required implementation");
            } else {
                //log.debug(" check: OK");
            }
            if (firstLargerPassingZone.equals(lowestPassing)) {
                lowestPassing = passingZoneToPlace;
            }
            firstLargerPassingZone.prevPassing = passingZoneToPlace;
        }

        passingZoneToPlace.prevPassing = previousPassingZone;
        passingZoneToPlace.nextPassing = firstLargerPassingZone;
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

    private final RecorderBody prayBody;
    private Hcn lowestPassingHcn;
    private Passing prevPassing;
    private Passing nextPassing;

    public Passing (Hcn lowestPassingHcn, RecorderBody prayBody) {
        this.lowestPassingHcn = lowestPassingHcn;
        this.prayBody = prayBody;
        Passing.placePassingInStaticChain(this);
    }

    public Passing(Hcn lowestPassingHcn, Passing nextPassing, Passing prevPassing, RecorderBody prayBody) {
        this.lowestPassingHcn = lowestPassingHcn;
        this.nextPassing = nextPassing;
        this.prevPassing = prevPassing;
        this.prayBody = prayBody;
    }
}
