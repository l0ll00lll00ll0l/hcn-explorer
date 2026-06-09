package com.hcn.newCore;

public class ApiNodeCreator {

    public static void createNewApi(ApiNode oldApiNode, TransitionNode transitionNode) {

        int newIndex = transitionNode.getFirstIndex();
        ScientificNumber newValue = new ScientificNumber(transitionNode.getPrimeCenter().getPrime(newIndex), 0);
        ApiNode newApiNode = ApiNode.builder().index(newIndex)
                .prime(newValue)
                .prevMatrixNode(oldApiNode).nextMatrixNode(transitionNode).build();

        Pip pipLower = null;
        BodyNode transitionToRemove = null;
        if (transitionNode.getBodyNodes().containsKey(newIndex)) {
            int transitionTo = transitionNode.getTransitionTo();
            transitionToRemove = transitionNode.getBodyNodes().get(newIndex);
            pipLower = Pip.builder().api(newApiNode)
                    .value(newValue.pow(transitionTo))
                    .factor(new ScientificNumber(transitionTo + 1, 0))
                    .bodyNodeId(transitionTo).proved(transitionToRemove.isProved()).build();
            newApiNode.getBodyNodes().put(pipLower.getBodyNodeId(), pipLower);
        }

        int transitionFrom = transitionNode.getTransitionFrom();
        BodyNode nextLowerTransition = transitionNode.getBodyNodes().get(newIndex + 1);
        Pip pip = Pip.builder().api(newApiNode)
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

    private static BodyList rebuildBodyNodesDualPip(Pip pipLower, Pip pip, TransitionNode transitionNode, BodyNode transitionToRemove, BodyNode nextLowerTransition) {

        Body bodyToRebuild = transitionNode.getBodyList().getSmallestBody();
        Body newBodyNode = null;
        do {
            Body smallerBody = newBodyNode;
            newBodyNode = Body.builder().parent(bodyToRebuild.getParent()).deactivated(bodyToRebuild.isDeactivated()).smallerBody(smallerBody).build();
            if (smallerBody != null) {smallerBody.setLargerBody(newBodyNode);}
            bodyToRebuild.setParent(newBodyNode);

            if (bodyToRebuild.getBodyNode().equals(transitionToRemove)) {
                bodyToRebuild.setBodyNode(nextLowerTransition);
                newBodyNode.setBodyNode(pipLower);
                newBodyNode.setValue(newBodyNode.getParent().getValue().multiply(pipLower.getValue()));
                newBodyNode.setFactor(newBodyNode.getParent().getFactor().multiply(pipLower.getFactor()));

                // Deactivated bodies don't hold connections in offsprings, and not present in activeBodies
                if (!bodyToRebuild.isDeactivated()) {
                    newBodyNode.getParent().getOffsprings().remove(bodyToRebuild);
                    newBodyNode.getParent().getOffsprings().add(newBodyNode);
                    pipLower.getActiveBodies().add(newBodyNode);
                    newBodyNode.getOffsprings().add(bodyToRebuild);
                }

            } else {
                newBodyNode.setBodyNode(pip);
                newBodyNode.setValue(newBodyNode.getParent().getValue().multiply(pip.getValue()));
                newBodyNode.setFactor(newBodyNode.getParent().getFactor().multiply(pip.getFactor()));

                // Deactivated bodies don't hold connections in offsprings, and not present in activeBodies
                if (!bodyToRebuild.isDeactivated()) {
                    newBodyNode.getParent().getOffsprings().remove(bodyToRebuild);
                    newBodyNode.getParent().getOffsprings().add(newBodyNode);
                    pip.getActiveBodies().add(newBodyNode);
                    newBodyNode.getOffsprings().add(bodyToRebuild);
                }
            }
            bodyToRebuild = bodyToRebuild.getLargerBody();
        } while (bodyToRebuild != null);
        newBodyNode.setLargerBody(null);

        return BodyList.builder().smallestBody(transitionNode.getBodyList().getSmallestBody().getParent()).build();
    }
}
