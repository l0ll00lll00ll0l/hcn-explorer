package com.hcn.newCore;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
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
    protected Integer tempId = null;
    protected BodyList bodyList;
    protected final TreeMap<Integer, BodyNode> bodyNodes = new TreeMap<>();
    protected final List<Prime> indexes = new ArrayList<>();

    public void deactivatedMaintain() {
        ScientificNumber smallestPossibleExtension = getSmallestPossibleExtension();
        bodyList.deactivatedMaintain(smallestPossibleExtension);
        if (prevMatrixNode != null) {
            prevMatrixNode.deactivatedMaintain();
        }
    }

    protected BodyNode getLargestProvedBodyNode() {
        if (bodyNodes.get(bodyNodes.lastKey()).isProved()) {return bodyNodes.get(bodyNodes.lastKey());}
        return bodyNodes.get(bodyNodes.lastKey() - 1);
    }

    public void generateNewBodies(List<Body> incomingParents) {
        //log.debug(" generateNewBodies");
        Set<Body> createdBodies = incomingParents.stream()
                .flatMap(previousBody -> bodyNodes.values().stream()
                        .map(bodyNode -> Body.builder().bodyNode(bodyNode).parent(previousBody)
                                .value(bodyNode.getValue().multiply(previousBody.getValue()))
                                .factor(bodyNode.getFactor().multiply(previousBody.getFactor())).build()))
                .collect(Collectors.toSet());

        //log.debug("  createdBodies {}", createdBodies);

        bodyList.mergeBodies(createdBodies);
        //log.debug("  bodyList {}", bodyList);

        parentDeactivationCheck(incomingParents);
        if (nextMatrixNode != null) {nextMatrixNode.generateNewBodies(bodyList.getSuccessfullyAddedNewBodies());} else {
            bodyList.resetActiveBodyChain();
        }
    }

    public void createNextBodyNode() {
        //log.debug("createNextBodyNode for indexes: {}", indexes);

        int nextBodyNodeId = bodyNodes.lastKey() + 1;
        BodyNode nwxtBodyNode = provideNextBodyNode();
        //log.debug("nwxtBodyNode: {}", nwxtBodyNode);
        bodyNodes.put(nextBodyNodeId, nwxtBodyNode);

        Set<Body> createdBodies;
        // at p0, we initiate new body chain
        if (prevMatrixNode == null) {

            createdBodies = Set.of(Body.builder().bodyNode(nwxtBodyNode).parent(null).value(nwxtBodyNode.getValue()).factor(nwxtBodyNode.getFactor()).build());
            //log.debug(" original createdBodies {}", createdBodies);
            bodyList.mergeBodies(createdBodies);
            //log.debug("  successfullyAddedLocalBodies {}", successfullyAddedLocalBodies);
        } else {

            int bodyNodeIdLowLimit = determineBodyNodeIdLowLimit();
            List<Body> parents = prevMatrixNode.bodyNodes.values().stream()
                    .filter(pip -> pip.getBodyNodeId() >= bodyNodeIdLowLimit)
                    .flatMap(pip -> pip.getActiveBodies().stream()).collect(Collectors.toList());

            //log.debug(" parent bodies with bod {}: {}", bodyNodeIdLowLimit, parents);

            createdBodies = parents.stream().map(parentBody ->  Body.builder().bodyNode(nwxtBodyNode).parent(parentBody)
                            .value(nwxtBodyNode.getValue().multiply(parentBody.getValue()))
                            .factor(nwxtBodyNode.getFactor().multiply(parentBody.getFactor())).build())
                    .collect(Collectors.toSet());
            //log.debug(" original createdBodies {}", createdBodies);
            bodyList.mergeBodies(createdBodies);
            //log.debug("  bodyList {}", bodyList);

            parentDeactivationCheck(parents);
        }

        if (nextMatrixNode != null) {nextMatrixNode.generateNewBodies(bodyList.getSuccessfullyAddedNewBodies());} else {
            bodyList.resetActiveBodyChain();
        }
    }

    private void parentDeactivationCheck(List<Body> parents) {
        parents.stream().filter(body -> body.getOffsprings().isEmpty()).forEach(Body::gotDominated);
    }

    protected abstract BodyNode provideNextBodyNode();
    protected abstract int determineBodyNodeIdLowLimit();
    protected abstract ScientificNumber determineDeactivationLimitMultiplier();
    public abstract ScientificNumber getSmallestPossibleExtension();
    protected abstract ScientificNumber getValurForNextMatrixExtension(int bodyNodeId);
    public abstract void extensionCheck();
}
