package com.hcn.newCore;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public class TransitionNodeCreator {

    public static void createNewTransitionNode(ApiNode fixNode) {

        List<ApiNode> apiNodesToMove = new ArrayList<>();
        BodyNode fixPip = fixNode.bodyNodes.firstEntry().getValue();
        ApiNode apiNodeToTurnIntoTransition = fixNode;
        ScientificNumber baseValue = fixPip.getValue();
        ScientificNumber baseFactor = fixPip.getFactor();
        //System.out.println("baseValue " + baseValue);
        //System.out.println("baseFactor " + baseFactor);

        while (apiNodeToTurnIntoTransition.nextMatrixNode instanceof ApiNode) {
            apiNodeToTurnIntoTransition = (ApiNode) apiNodeToTurnIntoTransition.nextMatrixNode;
            baseValue = baseValue.multiply(apiNodeToTurnIntoTransition.bodyNodes.firstEntry().getValue().getValue());
            baseFactor = baseFactor.multiply(apiNodeToTurnIntoTransition.bodyNodes.firstEntry().getValue().getFactor());
            apiNodesToMove.add(apiNodeToTurnIntoTransition);
            //System.out.println("baseValue " + baseValue);
            //System.out.println("baseFactor " + baseFactor);
        }

        TransitionNode newTransitionNode = TransitionNode.builder().transitionFrom(fixPip.getBodyNodeId())
                .transitionTo(fixPip.getBodyNodeId() - 1).lastIndex(apiNodeToTurnIntoTransition.getIndex())
                .firstIndex(fixNode.getIndex()).prevMatrixNode(fixNode.prevMatrixNode).nextMatrixNode(apiNodeToTurnIntoTransition.getNextMatrixNode())
                .build();

        BodyNode newSmallestTransition = BodyNode.builder().parentNode(newTransitionNode)
                .bodyNodeId(fixNode.getIndex() + 1).value(baseValue).factor(baseFactor)
                .proved(fixNode.getBodyNodes().get(fixNode.getBodyNodes().lastKey()).isProved()).build();
        newTransitionNode.bodyNodes.put(fixNode.getIndex() + 1, newSmallestTransition);
        for (ApiNode apiNode : apiNodesToMove) {
            baseValue = baseValue.multiply(apiNode.getPrime());
            baseFactor = baseFactor.multiply(new ScientificNumber((double) (newTransitionNode.getTransitionFrom() + 1) / (newTransitionNode.getTransitionTo() + 1), 0));
            newTransitionNode.bodyNodes.put(apiNode.getIndex() + 1, BodyNode.builder().parentNode(newTransitionNode)
                    .bodyNodeId(apiNode.getIndex() + 1).value(baseValue).factor(baseFactor)
                    .proved(apiNode.getBodyNodes().get(apiNode.getBodyNodes().lastKey()).isProved()).build());
        }

        //System.out.println("newTransitionNode.bodyNodes " + newTransitionNode.bodyNodes);

        BodyNode deactivatedTRansition = BodyNode.builder().parentNode(newTransitionNode)
                .bodyNodeId(newSmallestTransition.getBodyNodeId() - 1).value(newSmallestTransition.getValue().divide(fixNode.getPrime()))
                .factor(newSmallestTransition.getFactor().divide(new ScientificNumber((double) (newTransitionNode.getTransitionFrom() + 1) / (newTransitionNode.getTransitionTo() + 1), 0)))
                .proved(true).build();

        //System.out.println("deactivatedTRansition " + deactivatedTRansition);
        newTransitionNode.bodyNodes.put(deactivatedTRansition.getBodyNodeId(), deactivatedTRansition);

        Body bodyToRebuild = apiNodeToTurnIntoTransition.bodyList.getSmallestBody();
        List<Body> bodiesForBodylist = new ArrayList<>();
        while (bodyToRebuild != null) {

            //System.out.println("bodyToRebuild " + bodyToRebuild);
            Body parentBody = getSuitableParentBody(bodyToRebuild, newTransitionNode.bodyNodes.size() - 1);
            //System.out.println("parentBody " + parentBody);
            int transitionId = getTransitioIdForBody(bodyToRebuild, newTransitionNode.getTransitionTo());
            //System.out.println("transitionId " + transitionId);
            BodyNode transitionToUse = newTransitionNode.bodyNodes.get(transitionId);

            Body newBody = Body.builder().bodyNode(transitionToUse).proved(bodyToRebuild.isProved()).deactivated(bodyToRebuild.isDeactivated())
                    .parent(parentBody).value(parentBody.getValue().multiply(transitionToUse.getValue()))
                    .factor(parentBody.getFactor().multiply(transitionToUse.getFactor())).build();

            //System.out.println("newBody " + newBody);


            if (!bodyToRebuild.isDeactivated()) {
                Body parentBodysOffspring = getSuitableParentBody(bodyToRebuild, newTransitionNode.bodyNodes.size() - 2);
                parentBody.getOffsprings().remove(parentBodysOffspring);
                parentBody.getOffsprings().add(newBody);
                newBody.getOffsprings().addAll(bodyToRebuild.getOffsprings());
                transitionToUse.getActiveBodies().add(newBody);
            }

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
    }

    private static int getTransitioIdForBody(Body bodyToRebuild, int transitionTo) {
        int transitionId = ((ApiNode) bodyToRebuild.getBodyNode().getParentNode()).getIndex() + 1;
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
