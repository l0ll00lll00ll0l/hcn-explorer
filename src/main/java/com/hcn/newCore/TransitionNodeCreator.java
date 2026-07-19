package com.hcn.newCore;

import com.hcn.event.ActivityCenter;
import com.hcn.event.TransitionNodeCreationActivity;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Slf4j
public class TransitionNodeCreator {

    private final ApiNode fixNode;
    private ApiNode apiNodeToTurnIntoTransition;
    private TransitionNode newTransitionNode;
    private BodyNode deactivatedTransition;
    private final List<ApiNode> apiNodesToMove = new ArrayList<>();
    private final List<Body> bodiesForBodyList = new ArrayList<>();

    public TransitionNodeCreator(ApiNode fixNode) {
        this.fixNode = fixNode;
    }

    public void create() {
        TransitionNodeCreationActivity activity = ActivityCenter.isDbMode() ? new TransitionNodeCreationActivity(fixNode.bodyNodes.firstEntry().getValue().getBodyNodeId() - 1) : null;
        collectApiNodesToMove();
        findDyingbodyChains();
        buildNewTransitionNode();
        buildTransitionBodyNodes();
        rebuildBodyList();
        checkForDeactivateBodyNodes();
        wireNewTransitionNode();
        cleanUpOldNodes();

        if (activity != null) activity.finish();
    }

    private void findDyingbodyChains() {
        fixNode.bodyList.forEach(body -> {
            if (body.isDeactivated()) {
                if (!offspringExistsInLastApiBodylist(body)) {
                    body.getParent().getDeactivatedOffsprings().clear();
                }
            }
        });
    }

    private boolean offspringExistsInLastApiBodylist(Body body) {
        Body offspring = body.getDeactivatedOffsprings().stream().findAny().orElse(null);
        if (offspring == null) return false;
        if (offspring.getBodyNode().getParentNode().equals(apiNodesToMove.get(apiNodesToMove.size() - 1))) return true;
        return offspringExistsInLastApiBodylist(offspring);
    }

    private void checkForDeactivateBodyNodes() {
        while (newTransitionNode.bodyNodes.firstEntry().getValue().getActiveBodies().isEmpty()) {
            newTransitionNode.deactivatedBodyNodes.put(newTransitionNode.bodyNodes.firstKey(), newTransitionNode.bodyNodes.remove(newTransitionNode.bodyNodes.firstKey()));
        }
    }

    private void collectApiNodesToMove() {

        apiNodeToTurnIntoTransition = fixNode;
        while (apiNodeToTurnIntoTransition.nextMatrixNode instanceof ApiNode) {
            apiNodeToTurnIntoTransition = (ApiNode) apiNodeToTurnIntoTransition.nextMatrixNode;
            apiNodesToMove.add(apiNodeToTurnIntoTransition);
        }
    }

    private void buildNewTransitionNode() {
        BodyNode fixPip = fixNode.bodyNodes.firstEntry().getValue();
        List<Prime> primeList = new ArrayList<>(fixNode.indexes);
        apiNodesToMove.forEach(n -> primeList.addAll(n.indexes));

        newTransitionNode = TransitionNode.builder()
                .transitionFrom(fixPip.getBodyNodeId())
                .transitionTo(fixPip.getBodyNodeId() - 1)
                .prevMatrixNode(fixNode.prevMatrixNode)
                .nextMatrixNode(apiNodeToTurnIntoTransition.getNextMatrixNode())
                .build();
        newTransitionNode.indexes.addAll(primeList);
    }

    private void buildTransitionBodyNodes() {
        BodyNode fixPip = fixNode.bodyNodes.firstEntry().getValue();
        ScientificNumber baseValue = fixPip.getValue();
        ScientificNumber baseFactor = fixPip.getFactor();

        for (ApiNode apiNode : apiNodesToMove) {
            BodyNode smallestBodyNodePresent = apiNode.bodyNodes.firstEntry().getValue();
            if (!apiNode.deactivatedBodyNodes.isEmpty()) {
                smallestBodyNodePresent = apiNode.deactivatedBodyNodes.firstEntry().getValue();
            }
            baseValue = baseValue.multiply(smallestBodyNodePresent.getValue());
            baseFactor = baseFactor.multiply((smallestBodyNodePresent).getFactor());
        }

        BodyNode newSmallestTransition = BodyNode.builder().parentNode(newTransitionNode)
                .bodyNodeId(fixNode.indexes.get(0).getIndex() + 1).value(baseValue).factor(baseFactor)
                .proved(fixNode.getBodyNodes().get(fixNode.getBodyNodes().lastKey()).isProved()).build();
        newTransitionNode.bodyNodes.put(newSmallestTransition.getBodyNodeId(), newSmallestTransition);

        for (ApiNode apiNode : apiNodesToMove) {
            baseValue = baseValue.multiply(apiNode.indexes.get(0).getValue());
            baseFactor = baseFactor.multiply(new ScientificNumber((double) (newTransitionNode.getTransitionFrom() + 1) / (newTransitionNode.getTransitionTo() + 1), 0));
            newTransitionNode.bodyNodes.put(apiNode.indexes.get(0).getIndex() + 1, BodyNode.builder().parentNode(newTransitionNode)
                    .bodyNodeId(apiNode.indexes.get(0).getIndex() + 1).value(baseValue).factor(baseFactor)
                    .proved(apiNode.getBodyNodes().get(apiNode.getBodyNodes().lastKey()).isProved()).build());
        }

        deactivatedTransition = BodyNode.builder().parentNode(newTransitionNode)
                .bodyNodeId(newSmallestTransition.getBodyNodeId() - 1)
                .value(newSmallestTransition.getValue().divide(fixNode.indexes.get(0).getValue()))
                .factor(newSmallestTransition.getFactor().divide(new ScientificNumber((double) (newTransitionNode.getTransitionFrom() + 1) / (newTransitionNode.getTransitionTo() + 1), 0)))
                .proved(true).build();
        newTransitionNode.bodyNodes.put(deactivatedTransition.getBodyNodeId(), deactivatedTransition);
    }

    private void rebuildBodyList() {
        Body bodyToRebuild = apiNodeToTurnIntoTransition.bodyList.getSmallestBody();
        while (bodyToRebuild != null) {
            Body parentBody = getSuitableParentBody(bodyToRebuild, newTransitionNode.bodyNodes.size() - 1);
            int transitionId = getTransitionIdForBody(bodyToRebuild);
            BodyNode transitionToUse = newTransitionNode.bodyNodes.get(transitionId);

            Body newBody = Body.builder().bodyNode(transitionToUse).proved(bodyToRebuild.isProved()).deactivated(bodyToRebuild.isDeactivated())
                    .parent(parentBody).value(parentBody.getValue().multiply(transitionToUse.getValue()))
                    .factor(parentBody.getFactor().multiply(transitionToUse.getFactor())).build();

            Body parentBodysOffspring = getSuitableParentBody(bodyToRebuild, newTransitionNode.bodyNodes.size() - 2);
            if (!bodyToRebuild.isDeactivated()) {
                parentBody.getOffsprings().add(newBody);
                transitionToUse.getActiveBodies().add(newBody);
            } else {
                parentBody.getDeactivatedOffsprings().add(newBody);
                transitionToUse.getDeactivatedBodies().add(newBody);
            }

            parentBody.getOffsprings().remove(parentBodysOffspring);
            parentBody.getDeactivatedOffsprings().remove(parentBodysOffspring);

            newBody.getOffsprings().addAll(bodyToRebuild.getOffsprings());
            newBody.getDeactivatedOffsprings().addAll(bodyToRebuild.getDeactivatedOffsprings());

            bodyToRebuild.getOffsprings().forEach(offspring -> offspring.setParent(newBody));
            bodyToRebuild.getDeactivatedOffsprings().forEach(offspring -> offspring.setParent(newBody));
            bodyToRebuild = bodyToRebuild.getLargerBody();
            bodiesForBodyList.add(newBody);
        }

        newTransitionNode.bodyNodes.remove(deactivatedTransition.getBodyNodeId());
        newTransitionNode.setBodyList(createBodyList());
    }

    private void wireNewTransitionNode() {
        fixNode.prevMatrixNode.setNextMatrixNode(newTransitionNode);
        newTransitionNode.setPrevMatrixNode(fixNode.prevMatrixNode);
        newTransitionNode.setNextMatrixNode(apiNodeToTurnIntoTransition.getNextMatrixNode());
        apiNodeToTurnIntoTransition.getNextMatrixNode().setPrevMatrixNode(newTransitionNode);
    }

    private void cleanUpOldNodes() {
        fixNode.setPrevMatrixNode(null);
        fixNode.setNextMatrixNode(null);
        fixNode.bodyNodes.clear();
        apiNodesToMove.forEach(apiNode -> {
            apiNode.setNextMatrixNode(null);
            apiNode.setPrevMatrixNode(null);
            apiNode.getBodyNodes().clear();
        });
    }

    private int getTransitionIdForBody(Body bodyToRebuild) {
        int transitionId = ((ApiNode) bodyToRebuild.getBodyNode().getParentNode()).indexes.get(0).getIndex() + 1;
        Body walker = bodyToRebuild;
        while (walker.getBodyNode().getBodyNodeId() == newTransitionNode.getTransitionTo()) {
            transitionId--;
            walker = walker.getParent();
        }
        return transitionId;
    }

    private Body getSuitableParentBody(Body bodyToRebuild, int parentDepth) {
        Body parent = bodyToRebuild;
        for (int i = 0; i < parentDepth; i++) {
            parent = parent.getParent();
        }
        return parent;
    }

    private BodyList createBodyList() {
        BodyList bodyList = BodyList.builder().smallestBody(bodiesForBodyList.get(0)).build();
        Body prevBody = null;
        for (Body body : bodiesForBodyList) {
            body.setSmallerBody(prevBody);
            if (prevBody != null) { prevBody.setLargerBody(body); }
            prevBody = body;
        }
        return bodyList;
    }
}
