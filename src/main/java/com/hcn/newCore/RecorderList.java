package com.hcn.newCore;

import lombok.Builder;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Builder
public class RecorderList {
    private static RecorderBody firstRecorder;
    private static RecorderBody lastRecorder;
    private static RecorderBody currentRecorder;
    private static int size;

    public static RecorderBody getFirstRecorder() {
        return firstRecorder;
    }

    public static int getSize() {
        return size;
    }

    public static RecorderBody getCurrentRecorder() {
        return currentRecorder;
    }

    public static void setCurrentRecorder(RecorderBody RecorderBody) {
        currentRecorder = RecorderBody;
    }

    public static void setSize(int size) {
        RecorderList.size = size;
    }

    public static void setFirstRecorder(RecorderBody firstRecorder) {
        RecorderList.firstRecorder = firstRecorder;
    }

    public static void initialize(RecorderBody first, int count) {
        firstRecorder = first;
        size = count;
    }

    public static void print() {
        //log.info("recorderList");
        firstRecorder.print();
        RecorderBody walker = firstRecorder.getNextRecorder();
        while (!walker.equals(firstRecorder))  {
            walker.print();
            walker = walker.getNextRecorder();
        }
        //log.info("");
    }



    public static Hcn addNewRecord(Hcn hunterHcn) {
            //TODO intervaldistance
        /*
            RecorderBody passingRecorderBody = hunterHcn.getHcnGenerator().getRecorderBody();
            RecorderBody prayBody = passingRecorderBody.getPrayBody();
            RecorderBody referenceBody = prayBody.getPreviousRecorder();

            if (prayBody.equals(firstRecorder)) {
                passingRecorderBody.getHcnGenerator().setIntervalLapiDistance(passingRecorderBody.getHcnGenerator().getIntervalLapiDistance() - 1);
                //passingRecorderBody.getHunters().forEach(hunter -> hunter.setIntervalLapiDistance(hunter.getIntervalLapiDistance() - 1));
            }

            prayBody.setNextRecorder(passingRecorderBody.getNextRecorder());
            passingRecorderBody.getNextRecorder().setPreviousRecorder(prayBody);
            prayBody.setPreviousRecorder(passingRecorderBody);
            passingRecorderBody.setNextRecorder(prayBody);
            passingRecorderBody.setPreviousRecorder(referenceBody);
            referenceBody.setNextRecorder(passingRecorderBody);

            moveHuntersFromPrayBody(prayBody, hunterHcn);
            prayBody.getRecorderHunters().remove(passingRecorderBody);
            passingRecorderBody.setPrayBody(null);


            if (passingRecorderBody.getHcnGenerator().getIntervalLapiDistance() > 0) {
                RecorderBody prayCandidate = passingRecorderBody.getPreviousRecorder();
                while (prayCandidate.getHcnGenerator().getIntervalLapiDistance() > passingRecorderBody.getHcnGenerator().getIntervalLapiDistance() - 1) {
                    prayCandidate = prayCandidate.getPreviousRecorder();
                }
                passingRecorderBody.setPrayBody(prayCandidate);
                prayCandidate.getRecorderHunters().add(passingRecorderBody);
                //log.debug("RecorderBody {} added to recorderHunters of {}", newRecorderBody, prayCandidate);
            }

         */

        return null;
    }
}
