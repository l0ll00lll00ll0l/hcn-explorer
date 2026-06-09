package com.hcn.newCore;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;


@Getter @Setter @SuperBuilder
public class ApiNode extends MatrixNode {

    private int index;
    private ScientificNumber prime;

    @Override
    public void deactivatedMaintain() {
        System.out.println("deactivated body maintain for ApiNode yet to be implemented");
    }

    @Override
    protected BodyNode provideNextBodyNode() {
        return Pip.builder().api(this).bodyNodeId(bodyNodes.lastKey() + 1).value(bodyNodes.get(bodyNodes.lastKey()).getValue().multiply(prime))
                .factor(new ScientificNumber(bodyNodes.lastKey() + 2, 0)).build();
    }

    @Override
    protected int determineBodyNodeIdLowLimit() {
        return bodyNodes.lastKey();
    }


    public void extensionCheck(Pip pip) {
        if (transitionReleaseRequired()) {ApiNodeCreator.createNewApi(this, (TransitionNode) nextMatrixNode);}
        if (isLocalExtensionRequired()) {createNextBodyNode();}
    }

    private boolean transitionReleaseRequired() {return nextMatrixNode instanceof TransitionNode transitionNode && bodyNodes.lastKey() > transitionNode.getTransitionFrom();}

    private boolean isLocalExtensionRequired() {
        if (prevMatrixNode == null) {
            return true;
        } else {
            int pipToCreate = bodyNodes.lastKey() + 1;
            ApiNode prevAoiNode = (ApiNode) prevMatrixNode;
            int largestPreviousProvedPip = prevAoiNode.bodyNodes.lastKey() -1;
            if (largestPreviousProvedPip < pipToCreate) {
                return false;
            } else {
                return true;
            }
        }
    }
}
