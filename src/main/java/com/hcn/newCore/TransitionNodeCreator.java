package com.hcn.newCore;

import com.hcn.event.ActivityCenter;
import com.hcn.event.TransitionNodeCreationActivity;
import java.util.ArrayList;
import java.util.List;

public class TransitionNodeCreator {

    public static void createNewTransitionNode(ApiNode fixNode) {
        TransitionNodeCreationActivity activity = ActivityCenter.isDbMode() ? new TransitionNodeCreationActivity(fixNode.bodyNodes.firstEntry().getValue().getBodyNodeId() - 1) : null;

        List<ApiNode> apiNodesToMove = new ArrayList<>();
        BodyNode fixPip = fixNode.bodyNodes.firstEntry().getValue();
        ApiNode apiNodeToTurnIntoTransition = fixNode;
        ScientificNumber baseValue = fixPip.getValue();
        ScientificNumber baseFactor = fixPip.getFactor();
        List<Prime> primeList = new ArrayList<>(fixNode.indexes);

        while (apiNodeToTurnIntoTransition.nextMatrixNode instanceof ApiNode) {
            apiNodeToTurnIntoTransition = (ApiNode) apiNodeToTurnIntoTransition.nextMatrixNode;
            baseValue = baseValue.multiply(apiNodeToTurnIntoTransition.bodyNodes.firstEntry().getValue().getValue());
            baseFactor = baseFactor.multiply(apiNodeToTurnIntoTransition.bodyNodes.firstEntry().getValue().getFactor());
            apiNodesToMove.add(apiNodeToTurnIntoTransition);
            primeList.addAll(apiNodeToTurnIntoTransition.indexes);
        }

        TransitionNode newTransitionNode = TransitionNode.builder().transitionFrom(fixPip.getBodyNodeId())
                .transitionTo(fixPip.getBodyNodeId() - 1).prevMatrixNode(fixNode.prevMatrixNode)
                .nextMatrixNode(apiNodeToTurnIntoTransition.getNextMatrixNode())
                .build();
        newTransitionNode.indexes.addAll(primeList);

        BodyNode newSmallestTransition = BodyNode.builder().parentNode(newTransitionNode)
                .bodyNodeId(fixNode.indexes.get(0).getIndex() + 1).value(baseValue).factor(baseFactor)
                .proved(fixNode.getBodyNodes().get(fixNode.getBodyNodes().lastKey()).isProved()).build();
        newTransitionNode.bodyNodes.put(fixNode.indexes.get(0).getIndex() + 1, newSmallestTransition);
        for (ApiNode apiNode : apiNodesToMove) {
            baseValue = baseValue.multiply(apiNode.indexes.get(0).getValue());
            baseFactor = baseFactor.multiply(new ScientificNumber((double) (newTransitionNode.getTransitionFrom() + 1) / (newTransitionNode.getTransitionTo() + 1), 0));
            newTransitionNode.bodyNodes.put(apiNode.indexes.get(0).getIndex() + 1, BodyNode.builder().parentNode(newTransitionNode)
                    .bodyNodeId(apiNode.indexes.get(0).getIndex() + 1).value(baseValue).factor(baseFactor)
                    .proved(apiNode.getBodyNodes().get(apiNode.getBodyNodes().lastKey()).isProved()).build());
        }

        BodyNode deactivatedTRansition = BodyNode.builder().parentNode(newTransitionNode)
                .bodyNodeId(newSmallestTransition.getBodyNodeId() - 1).value(newSmallestTransition.getValue().divide(fixNode.indexes.get(0).getValue()))
                .factor(newSmallestTransition.getFactor().divide(new ScientificNumber((double) (newTransitionNode.getTransitionFrom() + 1) / (newTransitionNode.getTransitionTo() + 1), 0)))
                .proved(true).build();

        newTransitionNode.bodyNodes.put(deactivatedTRansition.getBodyNodeId(), deactivatedTRansition);

        Body bodyToRebuild = apiNodeToTurnIntoTransition.bodyList.getSmallestBody();
        List<Body> bodiesForBodylist = new ArrayList<>();
        while (bodyToRebuild != null) {

            Body parentBody = getSuitableParentBody(bodyToRebuild, newTransitionNode.bodyNodes.size() - 1);
            int transitionId = getTransitioIdForBody(bodyToRebuild, newTransitionNode.getTransitionTo());
            BodyNode transitionToUse = newTransitionNode.bodyNodes.get(transitionId);

            Body newBody = Body.builder().bodyNode(transitionToUse).proved(bodyToRebuild.isProved()).deactivated(bodyToRebuild.isDeactivated())
                    .parent(parentBody).value(parentBody.getValue().multiply(transitionToUse.getValue()))
                    .factor(parentBody.getFactor().multiply(transitionToUse.getFactor())).build();

            if (!bodyToRebuild.isDeactivated()) {
                Body parentBodysOffspring = getSuitableParentBody(bodyToRebuild, newTransitionNode.bodyNodes.size() - 2);
                parentBody.getOffsprings().remove(parentBodysOffspring);
                parentBody.getOffsprings().add(newBody);
                newBody.getOffsprings().addAll(bodyToRebuild.getOffsprings());
                transitionToUse.getActiveBodies().add(newBody);
            } else {
                bodyToRebuild.getBodyNode().getDeactivatedBodies().remove(bodyToRebuild);
                transitionToUse.getDeactivatedBodies().add(newBody);
                parentBody.getDeactivatedOffsprings().remove(bodyToRebuild);
                parentBody.getDeactivatedOffsprings().add(newBody);
            }

            bodyToRebuild.getOffsprings().forEach(offspring -> offspring.setParent(newBody));

            bodyToRebuild = bodyToRebuild.getLargerBody();
            bodiesForBodylist.add(newBody);
        }

        newTransitionNode.bodyNodes.remove(deactivatedTRansition.getBodyNodeId());
        newTransitionNode.setBodyList(createBodyList(bodiesForBodylist));
        fixNode.prevMatrixNode.setNextMatrixNode(newTransitionNode);
        newTransitionNode.setPrevMatrixNode(fixNode.prevMatrixNode);
        newTransitionNode.setNextMatrixNode(apiNodeToTurnIntoTransition.getNextMatrixNode());
        apiNodeToTurnIntoTransition.getNextMatrixNode().setPrevMatrixNode(newTransitionNode);
        fixNode.setPrevMatrixNode(null);
        fixNode.setNextMatrixNode(null);
        fixNode.bodyNodes.clear();
        apiNodesToMove.forEach(apiNode -> {
            apiNode.setNextMatrixNode(null);
            apiNode.setPrevMatrixNode(null);
            apiNode.getBodyNodes().clear();
        });
        if (activity != null) activity.finish();
    }

    private static int getTransitioIdForBody(Body bodyToRebuild, int transitionTo) {
        int transitionId = ((ApiNode) bodyToRebuild.getBodyNode().getParentNode()).indexes.get(0).getIndex() + 1;
        Body walker = bodyToRebuild;
        while (walker.getBodyNode().getBodyNodeId() == transitionTo) {
            transitionId--;
            walker = walker.getParent();
        }
        return transitionId;
    }

    private static Body getSuitableParentBody(Body bodyToRebuild, int parentDepth) {
        Body parent = bodyToRebuild;
        for (int i = 0; i < parentDepth; i++) {
            parent = parent.getParent();
        }
        return parent;
    }

    private static BodyList createBodyList(List<Body>  bodies) {
        BodyList bodyList = BodyList.builder().smallestBody(bodies.get(0)).build();
        Body prevBody = null;
        for (Body body : bodies) {
            body.setSmallerBody(prevBody);
            if (prevBody != null) {prevBody.setLargerBody(body);}
            prevBody = body;
        }
        return bodyList;
    }
}
