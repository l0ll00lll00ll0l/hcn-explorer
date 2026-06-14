package com.hcn.newCore;

import lombok.extern.slf4j.Slf4j;
import java.util.TreeMap;

@Slf4j
public class ApiNodeCreator {

    public static void createNewApi(ApiNode oldApiNode, TransitionNode transitionNode) {

        log.debug("TransitionRelease between API-{} and transition {}", oldApiNode.getIndex(), transitionNode.getFirstIndex());
        int newIndex = transitionNode.getFirstIndex();
        ScientificNumber newValue = new ScientificNumber(transitionNode.getPrimeCenter().getPrime(newIndex), 0);
        ApiNode newApiNode = ApiNode.builder().index(newIndex)
                .prime(newValue)
                .prevMatrixNode(oldApiNode).nextMatrixNode(transitionNode).build();

        BodyNode pipLower = null;
        BodyNode transitionToRemove = null;
        if (transitionNode.getBodyNodes().containsKey(newIndex)) {
            int transitionTo = transitionNode.getTransitionTo();
            transitionToRemove = transitionNode.getBodyNodes().get(newIndex);
            pipLower = BodyNode.builder().parentNode(newApiNode)
                    .value(newValue.pow(transitionTo))
                    .factor(new ScientificNumber(transitionTo + 1, 0))
                    .bodyNodeId(transitionTo).proved(transitionToRemove.isProved()).build();
            newApiNode.getBodyNodes().put(pipLower.getBodyNodeId(), pipLower);
        }

        int transitionFrom = transitionNode.getTransitionFrom();
        BodyNode nextLowerTransition;
        if (transitionNode.getBodyNodes().containsKey(newIndex + 1)) {
            nextLowerTransition = transitionNode.getBodyNodes().get(newIndex + 1);
        } else {
            nextLowerTransition = transitionNode.getBodyNodes().get(transitionNode.bodyNodes.firstKey());
        }
        BodyNode pip = BodyNode.builder().parentNode(newApiNode)
                .value(newValue.pow(transitionFrom))
                .factor(new ScientificNumber(transitionFrom + 1, 0))
                .bodyNodeId(transitionFrom).proved(nextLowerTransition.isProved()).build();
        newApiNode.getBodyNodes().put(pip.getBodyNodeId(), pip);

        recalculateTransitions(transitionNode);
        newApiNode.setBodyList(rebuildBodyNodesDualPip(pipLower, pip, transitionNode, transitionToRemove, nextLowerTransition));

        oldApiNode.setNextMatrixNode(newApiNode);
        transitionNode.setPrevMatrixNode(newApiNode);
    }

    private static void recalculateTransitions(TransitionNode transitionNode) {
        ScientificNumber valueExcluded = new ScientificNumber(Math.pow(transitionNode.getPrimeCenter().getPrime(transitionNode.getFirstIndex()), transitionNode.getTransitionFrom()), 0);
        ScientificNumber factorExcluded = new ScientificNumber(transitionNode.getTransitionFrom() + 1, 0);
        transitionNode.setFirstIndex(transitionNode.getFirstIndex() + 1);
        transitionNode.getBodyNodes().remove(transitionNode.getBodyNodes().firstKey());
        transitionNode.getBodyNodes().forEach((key, transition) -> {
                    transition.setValue(transition.getValue().divide(valueExcluded));
                    transition.setFactor(transition.getFactor().divide(factorExcluded));
                });

    }

    private static BodyList rebuildBodyNodesDualPip(BodyNode newLowerPip, BodyNode newLargerPip, TransitionNode transitionNode, BodyNode transitionToRemove, BodyNode nextLowerTransition) {

        log.debug(" TransitionRelease rebuildBodyNodesDualPip");
        Body bodyToRebuild = transitionNode.getBodyList().getSmallestBody();
        TreeMap<ScientificNumber, Body> distinctParents = new TreeMap<>();

        do {
            Body newBodyNode = Body.builder().parent(bodyToRebuild.getParent()).proved(bodyToRebuild.isProved()).deactivated(bodyToRebuild.isDeactivated()).build();
            BodyNode suitableNewPipForBody = getSuitableNewPipForBody(bodyToRebuild, transitionNode, newLowerPip, newLargerPip);
            newBodyNode.setValue(newBodyNode.getParent().getValue().multiply(suitableNewPipForBody.getValue()));
            log.debug("   bodyToRebuild {}", bodyToRebuild);

            if (distinctParents.containsKey(newBodyNode.getValue())) {
                newBodyNode = distinctParents.get(newBodyNode.getValue());
                // in case stored body is not proved but new offspring is
                if (bodyToRebuild.isProved()) {newBodyNode.setProved(true);}
                log.debug("   newBodyNode found in distinctParents {}", newBodyNode);
            } else {
                if (suitableNewPipForBody.equals(newLowerPip)) {
                    newBodyNode.setBodyNode(newLowerPip);
                    bodyToRebuild.setBodyNode(nextLowerTransition);
                } else {
                    newBodyNode.setBodyNode(newLargerPip);
                }
                distinctParents.put(newBodyNode.getValue(), newBodyNode);
                newBodyNode.setFactor(newBodyNode.getParent().getFactor().multiply(suitableNewPipForBody.getFactor()));
                log.debug("   newBodyNode updated {}", newBodyNode);
            }

            if (!bodyToRebuild.isDeactivated()) {
                newBodyNode.getParent().getOffsprings().remove(bodyToRebuild);
                suitableNewPipForBody.getActiveBodies().add(newBodyNode);
                newBodyNode.getOffsprings().add(bodyToRebuild);
                if (!newBodyNode.getParent().getOffsprings().contains(newBodyNode)) {
                    newBodyNode.getParent().getOffsprings().add(newBodyNode);
                }
                log.debug("   newBodyNode activity update {}", newBodyNode);
            }
            bodyToRebuild.setParent(newBodyNode);
            bodyToRebuild = bodyToRebuild.getLargerBody();
        } while (bodyToRebuild != null);

        return createBodyList(distinctParents);
    }


    private static BodyList createBodyList(TreeMap<ScientificNumber, Body> distinctParents) {
        BodyList bodyList = BodyList.builder().smallestBody(distinctParents.firstEntry().getValue()).build();
        Body prevBody = null;
        for (Body body : distinctParents.values()) {
            body.setSmallerBody(prevBody);
            if (prevBody != null) {prevBody.setLargerBody(body);}
            prevBody = body;
        }
        return bodyList;
    }

    private static BodyNode getSuitableNewPipForBody(Body bodyToRebuild, TransitionNode transitionNode, BodyNode newLowerPip, BodyNode newLargerPip) {
        if (bodyToRebuild.getBodyNode().getBodyNodeId() == transitionNode.getFirstIndex() - 1) {return newLowerPip;} else {return newLargerPip;}
    }
}

