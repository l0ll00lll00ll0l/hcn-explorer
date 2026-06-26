package com.hcn.newCore;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Getter @Setter @Builder @Slf4j
public class Body implements Comparable<Body>{
    private BodyNode bodyNode;
    private ScientificNumber value;
    private ScientificNumber factor;
    private Body parent;
    private final List<Body> offsprings = new ArrayList<>();
    @Builder.Default
    private boolean proved = false;
    @Builder.Default
    private Integer tempId = null;
    private Body smallerBody;
    private Body largerBody;
    private Body smallerActiveBody;
    private Body largerActiveBody;
    @Builder.Default
    private Hcn lastGeneratedHcn = null;
    @Builder.Default
    private Hcn firstHcn = null;
    @Builder.Default
    private Hcn firstSuperiorHcn = null;
    @Builder.Default
    private Hcn firstDominatedHcn = null;
    @Builder.Default
    private boolean deactivated = false;

    public Body getNextActiveBody() {
        Body candidate = largerBody;
        while (candidate != null && candidate.deactivated) {candidate = candidate.largerBody;}
        return candidate;
    }

    public Body getPrevActiveBody() {
        Body candidate = smallerBody;
        while (candidate != null && candidate.deactivated) {candidate = candidate.smallerBody;}
        return candidate;
    }

    public void removeFromActiveList() {
        if (smallerActiveBody != null) {
            smallerActiveBody.setLargerActiveBody(largerActiveBody);
        }
        if (largerActiveBody != null) {
            largerActiveBody.setSmallerActiveBody(smallerActiveBody);
        }
        smallerActiveBody = null;
        largerActiveBody = null;
    }

    public void addToActiveBodyList() {
        this.smallerActiveBody = getPrevActiveBody();
        this.largerActiveBody = getNextActiveBody();

        if (smallerActiveBody != null) {
            smallerActiveBody.setLargerActiveBody(this);
        }
        if (largerActiveBody != null) {
            largerActiveBody.setSmallerActiveBody(this);
        }
    }

    private Hcn findReferenceHcnForHcnGeneration(Lapi lapi) {
        Body referenceCandidate = smallerBody;
        while (!isReferenceCandidateSuitable(lapi, referenceCandidate)) {
            referenceCandidate = referenceCandidate.smallerBody;
        }
        return referenceCandidate.lastGeneratedHcn;
    }

    private boolean isReferenceCandidateSuitable(Lapi lapi, Body referenceCandidate) {
        return referenceCandidate != null && referenceCandidate.lastGeneratedHcn != null && referenceCandidate.lastGeneratedHcn.getLapi() == lapi.getPrime().getIndex();
    }

    public void gotDominated() {

        deactivated = true;
        bodyNode.getActiveBodies().remove(this);

        if (parent != null) {
            parent.offsprings.remove(this);
            if (parent.offsprings.isEmpty()) {
                parent.gotDominated();
            }
        }

        if (bodyNode.getActiveBodies().isEmpty()) {
            bodyNode.getParentNode().bodyNodes.remove(bodyNode.getBodyNodeId());
            if (bodyNode.getParentNode().bodyNodes.size() == 1) {
                TransitionNodeCreator.createNewTransitionNode((ApiNode) bodyNode.getParentNode());
            }
        }
    }

    public void matrixMaintainCheck() {
        if (proved) {return;}
        proved = true;
        bodyNode.extensionCheck();
        if (parent != null) {parent.matrixMaintainCheck();}
    }

    @Override
    public int compareTo(Body other) {
        return this.value.compareTo(other.value);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        buildChain(this, sb);
        sb.append(" v=").append(value).append(" f=").append(factor);
        if (!offsprings.isEmpty()) {
            sb.append(" offsprings: {");
            for (int i = 0; i < offsprings.size(); i++) {
                if (i > 0) sb.append(",");
                BodyNode node = offsprings.get(i).getBodyNode();
                if (node.getParentNode() instanceof ApiNode) {
                    sb.append(node.getBodyNodeId());
                } else {
                    sb.append("t").append(node.getBodyNodeId());
                }
            }
            sb.append("}");
        }
        return sb.toString();
    }

    private void buildChain(Body body, StringBuilder sb) {
        if (body.parent != null) {
            buildChain(body.parent, sb);
            sb.append(", ");
        }
        if (body.bodyNode.getParentNode() instanceof ApiNode apiNode) {
            sb.append("p").append(apiNode.indexes.get(0).getIndex()).append("^").append(body.bodyNode.getBodyNodeId());
        } else {
            sb.append("t").append(body.bodyNode.getBodyNodeId());
        }
    }
}
