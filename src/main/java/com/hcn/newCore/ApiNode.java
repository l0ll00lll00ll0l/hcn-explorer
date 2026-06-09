package com.hcn.newCore;

import com.hcn.core.HcnBody;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Getter @Setter @Builder
public class ApiNode implements MatrixNode {

    private int index;
    private ScientificNumber prime;
    private final TreeMap<Integer, Pip> pips = new TreeMap<>();
    private MatrixNode prevMatrixNode = null;
    private MatrixNode nextMatrixNode= null;
    private BodyList bodyList;

    @Override
    public void deactivatedMaintain() {
        System.out.println("deactivated body maintain for ApiNode yet to be implemented");
    }

    @Override
    public void generateNewBodies(List<Body> successfullyAddedLocalBodies) {
        Set<Body> createdBodies = successfullyAddedLocalBodies.stream()
                .flatMap(previousBody -> pips.values().stream()
                        .map(pip -> Body.builder().bodyNode(pip).parent(previousBody)
                                .value(pip.getValue().multiply(previousBody.getValue()))
                                .factor(pip.getFactor().multiply(previousBody.getFactor())).build()))
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

    public void extensionCheck(Pip pip) {
        // probably unnecessary check
        if (pips.get(pips.lastKey()) == pip) {
            if (transitionReleaseRequired()) {ApiNodeCreator.createNewApi(this, (TransitionNode) nextMatrixNode);}
            if (isLocalExtensionRequired()) {
                createNextPip();
            }
        }
    }

    private void createNextPip() {

        int newPipPower = pips.lastKey() + 1;
        Pip newPip = Pip.builder().api(this).power(newPipPower).value(pips.get(pips.lastKey()).getValue().multiply(prime))
                .factor(new ScientificNumber(newPipPower + 1, 0)).build();
        pips.put(newPipPower, newPip);

        Set<Body> createdBodies;

        // at p0, we initiate new body chain
        if (prevMatrixNode == null) {
            createdBodies = Set.of(Body.builder().bodyNode(newPip).parent(null).value(newPip.getValue()).factor(newPip.getFactor()).build());
        } else {
            createdBodies = ((ApiNode)prevMatrixNode).pips.values().stream()
                    .filter(pip -> pip.getPower() >= newPip.getPower())
                    .flatMap(pip -> pip.getActiveBodies().stream())
                    .map(parentBody ->  Body.builder().bodyNode(newPip).parent(parentBody)
                            .value(newPip.getValue().multiply(parentBody.getValue()))
                            .factor(newPip.getFactor().multiply(parentBody.getFactor())).build())
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

    private boolean transitionReleaseRequired() {return nextMatrixNode instanceof TransitionNode transitionNode && pips.lastKey() > transitionNode.getTransitionFrom();}

    private boolean isLocalExtensionRequired() {
        if (prevMatrixNode == null) {
            return true;
        } else {
            int pipToCreate = pips.lastKey() + 1;
            ApiNode prevAoiNode = (ApiNode) prevMatrixNode;
            int largestPreviousProvedPip = prevAoiNode.pips.lastKey() -1;
            if (largestPreviousProvedPip < pipToCreate) {
                return false;
            } else {
                return true;
            }
        }
    }
}
