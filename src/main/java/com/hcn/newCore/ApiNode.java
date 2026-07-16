package com.hcn.newCore;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;


@Getter @Setter @SuperBuilder @Slf4j
public class ApiNode extends MatrixNode {

    @Override
    protected BodyNode provideNextBodyNode() {
        return BodyNode.builder().parentNode(this).bodyNodeId(bodyNodes.lastKey() + 1)
                .value(bodyNodes.get(bodyNodes.lastKey()).getValue().multiply(indexes.get(0).getValue()))
                .factor(new ScientificNumber(bodyNodes.lastKey() + 2, 0)).build();
    }

    @Override
    protected int determineBodyNodeIdLowLimit() {
        return bodyNodes.lastKey();
    }

    @Override
    protected ScientificNumber determineDeactivationLimitMultiplier() {
        if (prevMatrixNode == null) {return new ScientificNumber(1, 0);}
        return indexes.get(0).getValue();
    }

    @Override
    public ScientificNumber getSmallestPossibleExtension() {

        ScientificNumber localextensionSmallest = bodyNodes.lastEntry().getValue().getValue().multiply(indexes.get(0).getValue());

        if (prevMatrixNode != null) {
            ScientificNumber valueForLocalExtension = prevMatrixNode.getValurForNextMatrixExtension(bodyNodes.lastEntry().getValue().getBodyNodeId());
            localextensionSmallest = localextensionSmallest.multiply(valueForLocalExtension);
            ScientificNumber prevExtensionSmallest = prevMatrixNode.getSmallestPossibleExtension().multiply(bodyNodes.firstEntry().getValue().getValue());
            if (prevExtensionSmallest.isSmallerThan(localextensionSmallest)) {
                return prevExtensionSmallest;
            } else {
                return localextensionSmallest;
            }
        }
        return localextensionSmallest;
    }

    @Override
    protected ScientificNumber getValurForNextMatrixExtension(int bodyNodeIdTrigger) {

        ScientificNumber localSuitableMultiplier;
        if (bodyNodes.firstKey() < bodyNodeIdTrigger) {
            localSuitableMultiplier = bodyNodes.get(bodyNodeIdTrigger).getValue();
        } else {
            localSuitableMultiplier = bodyNodes.firstEntry().getValue().getValue();
        }

        if (prevMatrixNode != null) {
            return localSuitableMultiplier.multiply(prevMatrixNode.getValurForNextMatrixExtension(bodyNodeIdTrigger));
        } else {
            return localSuitableMultiplier;
        }
    }

    @Override
    public void extensionCheck() {
        if (transitionReleaseRequired()) {
            new ApiNodeCreator(this, (TransitionNode) nextMatrixNode).create();
        }
        if (isLocalExtensionRequired()) {
            createNextBodyNode();
        }
        if (nextMatrixNodeExtensionRequired()) {
            nextMatrixNode.createNextBodyNode();
        }
    }

    private boolean nextMatrixNodeExtensionRequired() {
        if (nextMatrixNode instanceof ApiNode apinode) {
            BodyNode lastNode = apinode.getBodyNodes().get(apinode.bodyNodes.lastKey());
            if (lastNode.isProved() && lastNode.getBodyNodeId() < getLargestProvedBodyNode().getBodyNodeId()) {
                return true;
            }
        }
        return false;
    }

    private boolean transitionReleaseRequired() {return nextMatrixNode instanceof TransitionNode transitionNode && bodyNodes.lastKey() > transitionNode.getTransitionFrom();}

    private boolean isLocalExtensionRequired() {
        if (prevMatrixNode == null) {
            return true;
        } else {
            int pipToCreate = bodyNodes.lastKey() + 1;
            int largestPreviousProvedPip = prevMatrixNode.getLargestProvedBodyNode().getBodyNodeId();
            if (largestPreviousProvedPip < pipToCreate) {return false;} else {return true;}
        }
    }

    @Override
    public String toString() {
        return "ApiNode{" +
                "index=" + indexes.get(0) +
                ", prime=" + indexes.get(0).getIntValue() +
                '}';
    }
}
