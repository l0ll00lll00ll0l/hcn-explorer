package com.hcn.newCore;

import com.hcn.event.ActivityCenter;
import com.hcn.event.ApiNodeCreationActivity;
import lombok.extern.slf4j.Slf4j;
import java.util.TreeMap;

@Slf4j
public class ApiNodeCreator {

    private final ApiNode parentApiNode;
    private final TransitionNode transitionNode;
    private ApiNode newApiNode;
    private BodyNode pipHigher;
    private BodyNode pipLower;
    private BodyNode nextLowerTransition;
    private final TreeMap<ScientificNumber, Body> distinctLocalBodies = new TreeMap<>();

    public ApiNodeCreator(ApiNode parentApiNode, TransitionNode transitionNode) {
        this.parentApiNode = parentApiNode;
        this.transitionNode = transitionNode;
    }

    public void create() {
        Prime newIndex = transitionNode.indexes.get(0);
        ApiNodeCreationActivity activity = ActivityCenter.isDbMode() ? new ApiNodeCreationActivity(newIndex.getIndex()) : null;

        createNewApiNode(newIndex);
        createNextLowerTransition();
        createHigherPip();
        createLowerPip();
        BodyNode transitionToRemove = createTransitionToRemove(newIndex);

        recalculateTransitions(transitionToRemove);
        newApiNode.setBodyList(rebuildBodyList());

        transitionNode.indexes.remove(0);
        parentApiNode.setNextMatrixNode(newApiNode);
        transitionNode.setPrevMatrixNode(newApiNode);
        if (activity != null) activity.finish();
    }

    private BodyNode createTransitionToRemove(Prime newIndex) {
        BodyNode transitionToRemove = null;
        if (transitionNode.getBodyNodes().containsKey(newIndex.getIndex())) {
            transitionToRemove = transitionNode.getBodyNodes().get(newIndex.getIndex());
            pipLower.setProved(transitionToRemove.isProved());
            newApiNode.getBodyNodes().put(pipLower.getBodyNodeId(), pipLower);
        }
        return transitionToRemove;
    }

    private void createLowerPip() {
        pipLower = BodyNode.builder().parentNode(newApiNode)
                .value(newApiNode.indexes.get(0).getValue().pow(transitionNode.getTransitionTo()))
                .factor(new ScientificNumber(transitionNode.getTransitionTo() + 1, 0))
                .bodyNodeId(transitionNode.getTransitionTo()).proved(true).build();
    }

    private void createHigherPip() {
        pipHigher = BodyNode.builder().parentNode(newApiNode)
                .value(newApiNode.indexes.get(0).getValue().pow(transitionNode.getTransitionFrom()))
                .factor(new ScientificNumber(transitionNode.getTransitionFrom() + 1, 0))
                .bodyNodeId(transitionNode.getTransitionFrom()).proved(nextLowerTransition.isProved()).build();
        newApiNode.getBodyNodes().put(pipHigher.getBodyNodeId(), pipHigher);
    }

    private void createNextLowerTransition() {
        if (transitionNode.getBodyNodes().containsKey(newApiNode.indexes.get(0).getIndex() + 1)) {
            nextLowerTransition = transitionNode.getBodyNodes().get(newApiNode.indexes.get(0).getIndex() + 1);
        } else {
            nextLowerTransition = transitionNode.getBodyNodes().get(transitionNode.bodyNodes.firstKey());
        }
    }

    private void createNewApiNode(Prime newIndex) {
        newApiNode = ApiNode.builder().prevMatrixNode(parentApiNode).nextMatrixNode(transitionNode).build();
        newApiNode.indexes.add(newIndex);
    }

    private void recalculateTransitions(BodyNode transitionToRemove) {
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

    private BodyList rebuildBodyList() {
        Body bodyToRebuild = transitionNode.getBodyList().getSmallestBody();

        do {
            BodyNode suitableNewPip = getSuitableNewPip(bodyToRebuild);
            Body localBody = findOrCreateLocalBody(bodyToRebuild, suitableNewPip);
            maintainOffspringTracking(bodyToRebuild, localBody);
            bodyToRebuild.setParent(localBody);
            bodyToRebuild = bodyToRebuild.getLargerBody();
        } while (bodyToRebuild != null);

        distinctParentsMaintain();

        return createBodyList();
    }

    private void distinctParentsMaintain() {
        distinctLocalBodies.values().forEach(localBody -> {
            if (!localBody.getOffsprings().isEmpty()) {
                localBody.getBodyNode().getActiveBodies().add(localBody);
            } else {
                localBody.getBodyNode().getDeactivatedBodies().add(localBody);
            }
        });
    }

    private void maintainOffspringTracking(Body bodyToRebuild, Body newBody) {
        if (!bodyToRebuild.isDeactivated()) {
            newBody.getParent().getOffsprings().remove(bodyToRebuild);
            newBody.getOffsprings().add(bodyToRebuild);
            newBody.setDeactivated(false);
            if (!newBody.getParent().getOffsprings().contains(newBody)) {
                newBody.getParent().getOffsprings().add(newBody);
            }
        } else {
            newBody.getDeactivatedOffsprings().add(bodyToRebuild);
            newBody.getParent().getDeactivatedOffsprings().remove(bodyToRebuild);
            newBody.getParent().getDeactivatedOffsprings().add(newBody);
        }

        if (newBody.getParent().getOffsprings().contains(newBody) && newBody.getParent().getDeactivatedOffsprings().contains(newBody)) {
            newBody.getParent().getDeactivatedOffsprings().remove(newBody);
        }
    }

    private Body findOrCreateLocalBody(Body bodyToRebuild, BodyNode suitableNewPip) {
        Body newBody = Body.builder().parent(bodyToRebuild.getParent()).proved(bodyToRebuild.isProved()).deactivated(bodyToRebuild.isDeactivated()).build();
        newBody.setValue(newBody.getParent().getValue().multiply(suitableNewPip.getValue()));

        if (distinctLocalBodies.containsKey(newBody.getValue())) {
            newBody = distinctLocalBodies.get(newBody.getValue());
            if (bodyToRebuild.isProved()) { newBody.setProved(true); }
        } else {
            if (suitableNewPip.equals(pipLower)) {
                newBody.setBodyNode(pipLower);
                bodyToRebuild.setBodyNode(nextLowerTransition);
            } else {
                newBody.setBodyNode(pipHigher);
            }
            distinctLocalBodies.put(newBody.getValue(), newBody);
            newBody.setFactor(newBody.getParent().getFactor().multiply(suitableNewPip.getFactor()));
        }
        return newBody;
    }

    private BodyNode getSuitableNewPip(Body bodyToRebuild) {
        if (bodyToRebuild.getBodyNode().getBodyNodeId() > transitionNode.indexes.get(0).getIndex()) {
            return pipHigher;
        } else {
            return pipLower;
        }
    }

    private BodyList createBodyList() {
        BodyList bodyList = BodyList.builder().smallestBody(distinctLocalBodies.firstEntry().getValue()).build();
        Body prevBody = null;
        for (Body body : distinctLocalBodies.values()) {
            body.setSmallerBody(prevBody);
            if (prevBody != null) { prevBody.setLargerBody(body); }
            prevBody = body;
        }
        return bodyList;
    }
}
