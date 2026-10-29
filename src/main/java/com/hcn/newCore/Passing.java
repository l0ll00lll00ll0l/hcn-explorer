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
        log.debug("[PROCESS PASSING ZONE]]");

        Passing currentPassing = lowestPassing;
        TreeSet<Hcn> passingHcns = currentPassing.recorderBodies.first().generatePassingHcns();

        //log.debug("[PROCESS PASSING ZONE]]");
        log.debug("passingHcns: {}", passingHcns);


        passingHcns.forEach(hcn -> {
            Extension.extend(hcn);
            if (hcn.getHcnGenerator().getRecorderBody() == null) {
                log.debug("HCN creates new Body: {}", hcn);
                RecorderBody newRecorderBody = new RecorderBody(hcn);
            } else {
                log.debug("HCN belongs to an already recorderBody: {}", hcn);
                hcn.getHcnGenerator().getRecorderBody().resetReferencedHuntersFromPreviousRecorderBody(hcn, currentPassing.recorderBodies.first());
            }
        });

        ScientificNumber factorRecord = passingHcns.last().getFactor();

        for (RecorderBody passedRecorder : currentPassing.recorderBodies) {
            if (factorRecord.isNotSmallerThan(passedRecorder.getPrayHcn().getFactor())) {
                log.debug("deleting passedRecorder: {}", passedRecorder);
                passedRecorder.deleteRecorderBody();
            } else {
                log.debug("surviving passedRecorder: {}", passedRecorder);
                passedRecorder.createPassingZoneAfterSurvival(passingHcns.last());
                factorRecord = passedRecorder.getPrayHcn().getFactor();
            }
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
                firstNotSmallerPassing.recorderBodies.addAll(passingZoneToPlace.recorderBodies);
                for (RecorderBody recorderBody : passingZoneToPlace.getRecorderBodies()) {
                    recorderBody.setPassing(firstNotSmallerPassing);
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

    private final TreeSet<RecorderBody> recorderBodies = new TreeSet<>();
    private final Hcn passingHcn;
    private Passing prevPassing;
    private Passing nextPassing;

    public Passing (RecorderBody prayBody) {
        this.recorderBodies.add(prayBody);
        this.passingHcn = prayBody.getPassingHcn();
        Passing.placePassingInStaticChain(this);
    }

    public Passing(Passing nextPassing, Passing prevPassing, RecorderBody prayBody) {
        this.nextPassing = nextPassing;
        this.prevPassing = prevPassing;
        this.recorderBodies.add(prayBody);
        this.passingHcn = prayBody.getPassingHcn();
    }
}
