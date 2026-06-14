package com.hcn.newCore;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Getter
@Setter
@SuperBuilder
@Slf4j
public abstract class MatrixNode {

    protected MatrixNode prevMatrixNode = null;
    protected MatrixNode nextMatrixNode= null;
    protected BodyList bodyList;
    protected final TreeMap<Integer, BodyNode> bodyNodes = new TreeMap<>();

    public void deactivatedMaintain() {
        bodyList.deactivatedMaintain();
        if (prevMatrixNode != null) {prevMatrixNode.deactivatedMaintain();}
    }

    protected BodyNode getLargestProvedBodyNode() {
        if (bodyNodes.get(bodyNodes.lastKey()).isProved()) {return bodyNodes.get(bodyNodes.lastKey());}
        return bodyNodes.get(bodyNodes.lastKey() - 1);
    }

    public void generateNewBodies(List<Body> incomingParents) {
        log.debug(" generateNewBodies");
        Set<Body> createdBodies = incomingParents.stream()
                .flatMap(previousBody -> bodyNodes.values().stream()
                        .map(bodyNode -> Body.builder().bodyNode(bodyNode).parent(previousBody)
                                .value(bodyNode.getValue().multiply(previousBody.getValue()))
                                .factor(bodyNode.getFactor().multiply(previousBody.getFactor())).build()))
                .collect(Collectors.toSet());

        log.debug("  createdBodies {}", createdBodies);

        List<Body> successfullyAddedBodies = bodyList.mergeBodies(createdBodies);
        log.debug("  successfullyAddedBodies {}", successfullyAddedBodies);

        if (nextMatrixNode != null) {nextMatrixNode.generateNewBodies(successfullyAddedBodies);}
    }

    public void createNextBodyNode() {
        log.debug("createNextBodyNode");

        int nextBodyNodeId = bodyNodes.lastKey() + 1;
        BodyNode nwxtBodyNode = provideNextBodyNode();
        bodyNodes.put(nextBodyNodeId, nwxtBodyNode);

        Set<Body> createdBodies;
        // at p0, we initiate new body chain
        if (prevMatrixNode == null) {
            createdBodies = Set.of(Body.builder().bodyNode(nwxtBodyNode).parent(null).value(nwxtBodyNode.getValue()).factor(nwxtBodyNode.getFactor()).build());
        } else {

            int bodyNodeIdLowLimit = determineBodyNodeIdLowLimit();

            log.debug(" parent bodies with bod {}: {}", bodyNodeIdLowLimit, prevMatrixNode.bodyNodes.values().stream()
                    .filter(pip -> pip.getBodyNodeId() >= bodyNodeIdLowLimit)
                    .flatMap(pip -> pip.getActiveBodies().stream()));

            createdBodies = prevMatrixNode.bodyNodes.values().stream()
                    .filter(pip -> pip.getBodyNodeId() >= bodyNodeIdLowLimit)
                    .flatMap(pip -> pip.getActiveBodies().stream())
                    .map(parentBody ->  Body.builder().bodyNode(nwxtBodyNode).parent(parentBody)
                            .value(nwxtBodyNode.getValue().multiply(parentBody.getValue()))
                            .factor(nwxtBodyNode.getFactor().multiply(parentBody.getFactor())).build())
                    .collect(Collectors.toSet());
        }
        log.debug(" original createdBodies {}", createdBodies);
        List<Body> successfullyAddedLocalBodies = bodyList.mergeBodies(createdBodies);
        log.debug("  successfullyAddedLocalBodies {}", successfullyAddedLocalBodies);

        if (nextMatrixNode != null) {nextMatrixNode.generateNewBodies(successfullyAddedLocalBodies);}
    }

    protected abstract BodyNode provideNextBodyNode();
    protected abstract int determineBodyNodeIdLowLimit();
    public abstract void extensionCheck();
}
