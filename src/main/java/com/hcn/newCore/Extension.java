package com.hcn.newCore;

import com.hcn.event.ActivityCenter;
import com.hcn.event.MatrixExtensionActivity;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;

@Slf4j
public class Extension {

    private static Extension currentExtension = null;
    private static ArrayList<Extension> historicalExtensions = new ArrayList<>();

    public static void extend(Hcn passingHcns) {

        historicalExtensions.clear();
        passingHcns.getHcnGenerator().getBody().matrixMaintainCheck();

        /*
        passingHcns.forEach(passingHcn -> {
            passingHcn.getHcnGenerator().getBody().matrixMaintainCheck();

        });


        historicalExtensions.forEach(extension -> {
            //log.debug("Extension #{} completed", extension.newBodyNode);
            //log.debug(" successfullyAddedNewBodies {}", extension.successfullyAddedNewBodies);
            //log.debug(" dominatedSuperiorBodies {}", extension.dominatedSuperiorBodies);
        });

         */
        //historicalExtensions.forEach(Extension::maintainRecorderList);

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
        currentExtension = null;
    }

    private void gatherBodyListChanges() {
        successfullyAddedNewBodies = new ArrayList<>(TransitionNode.getLastTransition().bodyList.getSuccessfullyAddedNewBodies());
        dominatedSuperiorBodies = new ArrayList<>(TransitionNode.getLastTransition().bodyList.getDominatedSuperiorBodies());
        dominatedDeactivatedBodies = new ArrayList<>(TransitionNode.getLastTransition().bodyList.getDominatedDeactivatedBodies());
    }

    private void maintainRecorderList() {

        //log.debug("[CREATED NEW HCN GENERATORS]");
        //log.debug(" successfullyAddedNewBodies {}", successfullyAddedNewBodies);
        //log.debug(" dominatedSuperiorBodies {}", dominatedSuperiorBodies);
        //log.debug(" bodylist {}", TransitionNode.getLastTransition().getBodyList());
        //log.debug("");
        Iterator<Body> delIt = dominatedSuperiorBodies.iterator();
        Iterator<Body> addIt = successfullyAddedNewBodies.iterator();
        Body del = delIt.hasNext() ? delIt.next() : null;
        Body add = addIt.hasNext() ? addIt.next() : null;
        //log.debug(" [BEFORE:]");
        RecorderList.print();
        while (del != null || add != null) {
            if (add == null || (del != null && del.compareTo(add) <= 0)) {
                //log.debug("DELETING: {}", del);
                del.getHcnGenerator().deleteHcnGenerator();
                del = delIt.hasNext() ? delIt.next() : null;
            } else {
                new HcnGenerator(add);
                add = addIt.hasNext() ? addIt.next() : null;
            }
        }
        //log.debug(" [AFTER:]");
        RecorderList.print();
        if (ActivityCenter.isDbMode()) {
            MatrixExtensionActivity mea = ActivityCenter.getLastMatrixExtensionActivity();
            mea.setCreatedActiveBodyCount(successfullyAddedNewBodies.size());
            mea.setDeletedActiveBodyCount(dominatedSuperiorBodies.size());
            mea.setDeletedDeactivatedBodyCount(dominatedDeactivatedBodies.size());
        }
    }
}
