package com.hcn.newCore;

import com.hcn.event.ActivityCenter;
import com.hcn.event.MatrixExtensionActivity;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Slf4j
public class Extension {

    private static Extension currentExtension = null;
    private static ArrayList<Extension> historicalExtensions = new ArrayList<>();

    public static void reset() {
        currentExtension = null;
        historicalExtensions = new ArrayList<>();
    }

    public static void extend(Hcn passingHcns) {
        historicalExtensions.clear();
        passingHcns.getHcnGenerator().getBody().matrixMaintainCheck();
    }

    public static void reportExtension(BodyNode newBodyNode) {
        currentExtension = new Extension(newBodyNode);
    }

    private final BodyNode newBodyNode;
    private List<Body> successfullyAddedNewBodies = new ArrayList<>();
    private List<Body> dominatedSuperiorBodies = new ArrayList<>();
    private List<Body> dominatedDeactivatedBodies = new ArrayList<>();

    public Extension(BodyNode newBodyNode) {
        this.newBodyNode = newBodyNode;
    }

    public static void finished() {
        currentExtension.gatherBodyListChanges();
        historicalExtensions.add(currentExtension);
        currentExtension.maintainRecorderList();
        bodyListDeletionMaintain();
        currentExtension = null;
    }

    private static void bodyListDeletionMaintain() {
        List<Body> deletedHcnGenerators = TransitionNode.getLastTransition().deactivatedMaintain();
        ActivityCenter.submitBodyDeletionEvent(deletedHcnGenerators);
    }

    private void gatherBodyListChanges() {
        successfullyAddedNewBodies = new ArrayList<>(TransitionNode.getLastTransition().bodyList.getSuccessfullyAddedNewBodies());
        dominatedSuperiorBodies = new ArrayList<>(TransitionNode.getLastTransition().bodyList.getDominatedSuperiorBodies());
        dominatedDeactivatedBodies = new ArrayList<>(TransitionNode.getLastTransition().bodyList.getDominatedDeactivatedBodies());
    }

    private void maintainRecorderList() {

        Iterator<Body> delIt = dominatedSuperiorBodies.iterator();
        Iterator<Body> addIt = successfullyAddedNewBodies.iterator();
        Body del = delIt.hasNext() ? delIt.next() : null;
        Body add = addIt.hasNext() ? addIt.next() : null;

        while (del != null || add != null) {
            if (add == null || (del != null && del.compareTo(add) <= 0)) {
                del.getHcnGenerator().deleteHcnGenerator();
                del = delIt.hasNext() ? delIt.next() : null;
            } else {
                new HcnGenerator(add);
                add = addIt.hasNext() ? addIt.next() : null;
            }
        }

        if (ActivityCenter.isDbMode()) {
            MatrixExtensionActivity mea = ActivityCenter.getLastMatrixExtensionActivity();
            mea.setCreatedActiveBodyCount(successfullyAddedNewBodies.size());
            mea.setDeletedActiveBodyCount(dominatedSuperiorBodies.size());
            mea.setDeletedDeactivatedBodyCount(dominatedDeactivatedBodies.size());
        }
    }
}
