package com.hcn.newCore;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter @Setter @Builder
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
    private boolean deactivated = false;

    private boolean isSelfReferredGenerationPossible(Lapi lapi) {return lastGeneratedHcn != null && lastGeneratedHcn.getLapi() + 1 == lapi.getLapi();}

    public Body getNextActiveBody() {
        Body candidate = largerBody;
        while (candidate != null && candidate.deactivated) {candidate = candidate.largerBody;}
        return candidate;
    }

    public Hcn generateNextHcn(Lapi lapi) {
        if (isSelfReferredGenerationPossible(lapi)) {
            lastGeneratedHcn = Hcn.builder().body(this).lapi(lapi.getLapi()).value(lastGeneratedHcn.getValue().multiply(lapi.getPrime()))
                    .factor(lastGeneratedHcn.getFactor().multiply(new ScientificNumber(2, 0))).build();
        } else {
            Hcn referenceHcn = findReferenceHcnForHcnGeneration(lapi);
            lastGeneratedHcn = Hcn.builder().body(this).lapi(lapi.getLapi()).value(this.value.divide(referenceHcn.getBody().value).multiply(referenceHcn.getValue()))
                    .factor(this.factor.divide(referenceHcn.getBody().factor).multiply(referenceHcn.getFactor())).build();
        }
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
        System.out.println("dominated yet to implement");
    }

    public void matrixMaintainCheck() {
        if (proved) {return;}
        proved = true;
        bodyNode.extensionCheck(this);
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
                if (node instanceof Pip pip) {
                    sb.append(pip.getPower());
                } else if (node instanceof Transition transition) {
                    sb.append("t").append(transition.getFirstLowerIndex());
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
        if (body.bodyNode instanceof Pip pip) {
            sb.append("p").append(pip.getApi().getIndex()).append("^").append(pip.getPower());
        } else if (body.bodyNode instanceof Transition transition) {
            sb.append("t").append(transition.getFirstLowerIndex());
        }
    }
}
