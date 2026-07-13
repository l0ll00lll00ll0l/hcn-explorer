package com.hcn.newCore;

import com.hcn.event.ApiNodeCreationActivity;
import lombok.extern.slf4j.Slf4j;
import java.util.TreeMap;

@Slf4j
public class ApiNodeCreator {

    public static void createNewApi(ApiNode parentApiNode, TransitionNode transitionNode) {

        Prime newIndex = transitionNode.indexes.get(0);
        ApiNodeCreationActivity activity = new ApiNodeCreationActivity(newIndex.getIndex());
        ApiNode newApiNode = ApiNode.builder().prevMatrixNode(parentApiNode).nextMatrixNode(transitionNode).build();
        newApiNode.indexes.add(newIndex);

        BodyNode nextLowerTransition;
        if (transitionNode.getBodyNodes().containsKey(newApiNode.indexes.get(0).getIndex() + 1)) {
            nextLowerTransition = transitionNode.getBodyNodes().get(newApiNode.indexes.get(0).getIndex() + 1);
        } else {
            nextLowerTransition = transitionNode.getBodyNodes().get(transitionNode.bodyNodes.firstKey());
        }
        BodyNode pip = BodyNode.builder().parentNode(newApiNode)
                .value(newApiNode.indexes.get(0).getValue().pow(transitionNode.getTransitionFrom()))
                .factor(new ScientificNumber(transitionNode.getTransitionFrom() + 1, 0))
                .bodyNodeId(transitionNode.getTransitionFrom()).proved(nextLowerTransition.isProved()).build();
        newApiNode.getBodyNodes().put(pip.getBodyNodeId(), pip);

        BodyNode pipLower = BodyNode.builder().parentNode(newApiNode)
                .value(newApiNode.indexes.get(0).getValue().pow(transitionNode.getTransitionTo()))
                .factor(new ScientificNumber(transitionNode.getTransitionTo() + 1, 0))
                .bodyNodeId(transitionNode.getTransitionTo()).proved(true).build();

        BodyNode transitionToRemove = null;
        if (transitionNode.getBodyNodes().containsKey(newIndex.getIndex())) {
            transitionToRemove = transitionNode.getBodyNodes().get(newIndex.getIndex());
            pipLower.setProved(transitionToRemove.isProved());
            newApiNode.getBodyNodes().put(pipLower.getBodyNodeId(), pipLower);
        }

        recalculateTransitions(transitionNode, transitionToRemove);
        newApiNode.setBodyList(rebuildBodyNodesDualPip(pipLower, pip, transitionNode, nextLowerTransition));

        transitionNode.indexes.remove(0);
        parentApiNode.setNextMatrixNode(newApiNode);
        transitionNode.setPrevMatrixNode(newApiNode);
        activity.finish();
    }

    private static void recalculateTransitions(TransitionNode transitionNode, BodyNode transitionToRemove) {
        ScientificNumber valueExcluded = new ScientificNumber(Math.pow(transitionNode.indexes.get(0).getIntValue(), transitionNode.getTransitionFrom()), 0);
        ScientificNumber factorExcluded = new ScientificNumber(transitionNode.getTransitionFrom() + 1, 0);
        if (transitionToRemove != null) {
            transitionNode.getBodyNodes().remove(transitionNode.getBodyNodes().firstKey());
        }
        transitionNode.getBodyNodes().forEach((key, transition) -> {
                    transition.setValue(transition.getValue().divide(valueExcluded));
                    transition.setFactor(transition.getFactor().divide(factorExcluded));
        });

    }

    private static BodyList rebuildBodyNodesDualPip(BodyNode newLowerPip, BodyNode newLargerPip, TransitionNode transitionNode, BodyNode nextLowerTransition) {

        Body bodyToRebuild = transitionNode.getBodyList().getSmallestBody();
        TreeMap<ScientificNumber, Body> distinctParents = new TreeMap<>();

        do {
            Body newBodyNode = Body.builder().parent(bodyToRebuild.getParent()).proved(bodyToRebuild.isProved()).deactivated(bodyToRebuild.isDeactivated()).build();
            BodyNode suitableNewPipForBody = getSuitableNewPipForBody(bodyToRebuild, transitionNode, newLowerPip, newLargerPip);
            newBodyNode.setValue(newBodyNode.getParent().getValue().multiply(suitableNewPipForBody.getValue()));

            if (distinctParents.containsKey(newBodyNode.getValue())) {
                newBodyNode = distinctParents.get(newBodyNode.getValue());
                // in case stored body is not proved but new offspring is
                if (bodyToRebuild.isProved()) {newBodyNode.setProved(true);}
            } else {
                if (suitableNewPipForBody.equals(newLowerPip)) {
                    newBodyNode.setBodyNode(newLowerPip);
                    bodyToRebuild.setBodyNode(nextLowerTransition);
                } else {
                    newBodyNode.setBodyNode(newLargerPip);
                }
                distinctParents.put(newBodyNode.getValue(), newBodyNode);
                newBodyNode.setFactor(newBodyNode.getParent().getFactor().multiply(suitableNewPipForBody.getFactor()));
            }

            if (!bodyToRebuild.isDeactivated()) {
                newBodyNode.getParent().getOffsprings().remove(bodyToRebuild);
                suitableNewPipForBody.getActiveBodies().add(newBodyNode);
                newBodyNode.getOffsprings().add(bodyToRebuild);
                newBodyNode.setDeactivated(false);
                if (!newBodyNode.getParent().getOffsprings().contains(newBodyNode)) {
                    newBodyNode.getParent().getOffsprings().add(newBodyNode);
                }
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
        if (bodyToRebuild.getBodyNode().getBodyNodeId() > transitionNode.indexes.get(0).getIndex()) {
            return newLargerPip;
        } else {
            return newLowerPip;
        }
    }
}

