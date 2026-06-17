package com.hcn.newCore;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;


@Getter @Setter @SuperBuilder @Slf4j
public class ApiNode extends MatrixNode {

    @Override
    protected BodyNode provideNextBodyNode() {
        return BodyNode.builder().parentNode(this).bodyNodeId(bodyNodes.lastKey() + 1).value(bodyNodes.get(bodyNodes.lastKey()).getValue().multiply(indexes.get(0).getValue()))
                .factor(new ScientificNumber(bodyNodes.lastKey() + 2, 0)).build();
    }

    @Override
    protected int determineBodyNodeIdLowLimit() {
        return bodyNodes.lastKey();
    }

    @Override
    public void extensionCheck() {
        //log.debug("LocalExtension trigger at {}", indexes.get(0));
        if (transitionReleaseRequired()) {
            ApiNodeCreator.createNewApi(this, (TransitionNode) nextMatrixNode);
        }
        if (isLocalExtensionRequired()) {
            //log.debug("  LocalExtension required at {}", indexes.get(0));
            createNextBodyNode();}
        if (nextMatrixNodeExtensionRequired()) {
            //log.debug("  NextMatrixNodeExtension required at {}", indexes.get(0));
            nextMatrixNode.createNextBodyNode();
        }
    }

    private boolean nextMatrixNodeExtensionRequired() {
        if (nextMatrixNode instanceof ApiNode apinode) {
            BodyNode lastNode = apinode.getBodyNodes().get(apinode.bodyNodes.lastKey());
            //log.debug("  NextMatrixNodeExtension required at {}, this: {}, last: {}", indexes.get(0), getLargestProvedBodyNode(), nextMatrixNode.getLargestProvedBodyNode());
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
            //log.debug("isLocalExtensionRequired, pipToCreate: {}, largestPreviousProvedPip: {}", pipToCreate, largestPreviousProvedPip);
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
