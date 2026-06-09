package com.hcn.newCore;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Getter
@Setter
@SuperBuilder
public abstract class MatrixNode {

    protected MatrixNode prevMatrixNode = null;
    protected MatrixNode nextMatrixNode= null;
    protected BodyList bodyList;
    protected final TreeMap<Integer, BodyNode> bodyNodes = new TreeMap<>();

    public void deactivatedMaintain() {

    }

    public void generateNewBodies(List<Body> incomingParents) {
        Set<Body> createdBodies = incomingParents.stream()
                .flatMap(previousBody -> bodyNodes.values().stream()
                        .map(bodyNode -> Body.builder().bodyNode(bodyNode).parent(previousBody)
                                .value(bodyNode.getValue().multiply(previousBody.getValue()))
                                .factor(bodyNode.getFactor().multiply(previousBody.getFactor())).build()))
                .collect(Collectors.toSet());

        //System.out.println("created bodies: " + createdBodies);

        List<Body> successfullyAddedBodies = bodyList.mergeBodies(createdBodies);
        successfullyAddedBodies.forEach(body -> body.getBodyNode().getActiveBodies().add(body));
        successfullyAddedBodies.forEach(body -> {
            body.getBodyNode().getActiveBodies().add(body);
            if (body.getParent() != null) {
                body.getParent().getOffsprings().add(body);
                //System.out.println("afterOffspringset: " + body.getParent());
            }
        });
        //System.out.println("successfullyAddedBodies: " + successfullyAddedBodies);

        if (nextMatrixNode != null) {nextMatrixNode.generateNewBodies(successfullyAddedBodies);}
    }

    public void createNextBodyNode() {

        int nextBodyNodeId = bodyNodes.lastKey() + 1;
        BodyNode nwxtBodyNode = provideNextBodyNode();
        bodyNodes.put(nextBodyNodeId, nwxtBodyNode);

        Set<Body> createdBodies;
        // at p0, we initiate new body chain
        if (prevMatrixNode == null) {
            createdBodies = Set.of(Body.builder().bodyNode(nwxtBodyNode).parent(null).value(nwxtBodyNode.getValue()).factor(nwxtBodyNode.getFactor()).build());
        } else {

            int bodyNodeIdLowLimit = determineBodyNodeIdLowLimit();

            createdBodies = prevMatrixNode.bodyNodes.values().stream()
                    .filter(pip -> pip.getBodyNodeId() >= bodyNodeIdLowLimit)
                    .flatMap(pip -> pip.getActiveBodies().stream())
                    .map(parentBody ->  Body.builder().bodyNode(nwxtBodyNode).parent(parentBody)
                            .value(nwxtBodyNode.getValue().multiply(parentBody.getValue()))
                            .factor(nwxtBodyNode.getFactor().multiply(parentBody.getFactor())).build())
                    .collect(Collectors.toSet());
        }

        //System.out.println("created bodies: " + createdBodies);
        List<Body> successfullyAddedLocalBodies = bodyList.mergeBodies(createdBodies);
        successfullyAddedLocalBodies.forEach(body -> {
            body.getBodyNode().getActiveBodies().add(body);
            if (body.getParent() != null) {
                body.getParent().getOffsprings().add(body);
                //System.out.println("afterOffspringset: " + body.getParent());
            }
        });
        //System.out.println("successfullyAddedLocalBodies: " + successfullyAddedLocalBodies);

        if (nextMatrixNode != null) {nextMatrixNode.generateNewBodies(successfullyAddedLocalBodies);}
    }

    protected abstract BodyNode provideNextBodyNode();

    protected abstract int determineBodyNodeIdLowLimit();
}
