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
    private Body smallerBody;
    private Body largerBody;
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

    private boolean isSelfReferredGenerationPossible(Lapi lapi) {return lastGeneratedHcn != null && lastGeneratedHcn.getLapi() + 1 == lapi.getLapi();}

    public Body getNextActiveBody() {
        Body candidate = largerBody;
        while (candidate != null && candidate.deactivated) {candidate = candidate.largerBody;}
        return candidate;
    }

    public Hcn generateNextHcn(Lapi lapi) {
        boolean firstHcnGenerated = lastGeneratedHcn == null;
        if (isSelfReferredGenerationPossible(lapi)) {
            lastGeneratedHcn = Hcn.builder().body(this).lapi(lapi.getLapi()).value(lastGeneratedHcn.getValue().multiply(lapi.getPrime()))
                    .factor(lastGeneratedHcn.getFactor().multiply(new ScientificNumber(2, 0))).build();
        } else {
            Hcn referenceHcn = findReferenceHcnForHcnGeneration(lapi);
            lastGeneratedHcn = Hcn.builder().body(this).lapi(lapi.getLapi()).value(this.value.divide(referenceHcn.getBody().value).multiply(referenceHcn.getValue()))
                    .factor(this.factor.divide(referenceHcn.getBody().factor).multiply(referenceHcn.getFactor())).build();
        }
        if (firstHcnGenerated) {firstHcn = lastGeneratedHcn;}
        return lastGeneratedHcn;
    }

    private Hcn findReferenceHcnForHcnGeneration(Lapi lapi) {
        Body referenceCandidate = smallerBody;
        while (!isReferenceCandidateSuitable(lapi, referenceCandidate)) {
            referenceCandidate = referenceCandidate.smallerBody;
        }
        return referenceCandidate.lastGeneratedHcn;
    }

    private boolean isReferenceCandidateSuitable(Lapi lapi, Body referenceCandidate) {
        return referenceCandidate != null && referenceCandidate.lastGeneratedHcn != null && referenceCandidate.lastGeneratedHcn.getLapi() == lapi.getLapi();
    }

    public void gotDominated() {

        log.debug("dominated body: {}", this);;

        deactivated = true;
        bodyNode.getActiveBodies().remove(this);

        if (bodyNode.getActiveBodies().isEmpty()) {
            bodyNode.getParentNode().bodyNodes.remove(bodyNode.getBodyNodeId());
            if (bodyNode.getParentNode().bodyNodes.size() == 1) {
                System.out.println("used to be fixpowergroup trigger");
            }
        }

        if (parent != null) {
            parent.offsprings.remove(this);
            if (parent.offsprings.isEmpty()) {
                parent.gotDominated();
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
            sb.append("p").append(apiNode.getIndex()).append("^").append(body.bodyNode.getBodyNodeId());
        } else {
            sb.append("t").append(body.bodyNode.getBodyNodeId());
        }
    }
}
