package com.hcn.newCore;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import com.hcn.db.DbBody;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter @Setter @Builder @Slf4j
public class Body implements Comparable<Body>{

    public static void initialize() {

        MatrixNode p0 = TransitionNode.getLastTransition().getPrevMatrixNode();
        BodyNode pip01 = p0.bodyNodes.get(1);
        BodyNode pip02 = p0.bodyNodes.get(2);
        BodyNode pip03 = p0.bodyNodes.get(3);

        Body b01 = Body.builder().bodyNode(pip01).parent(null).value(new ScientificNumber(2, 0))
                .factor(new ScientificNumber(2, 0)).proved(true).build();
        Body b02 = Body.builder().bodyNode(pip02).parent(null).value(new ScientificNumber(4, 0))
                .factor(new ScientificNumber(3, 0)).proved(true).build();
        Body b03 = Body.builder().bodyNode(pip03).parent(null).value(new ScientificNumber(8, 0))
                .factor(new ScientificNumber(4, 0)).proved(false).build();

        pip01.getActiveBodies().add(b01);
        pip02.getActiveBodies().add(b02);
        pip03.getActiveBodies().add(b03);

        b01.setSmallerBody(null);
        b01.setLargerBody(b02);
        b02.setSmallerBody(b01);
        b02.setLargerBody(b03);
        b03.setSmallerBody(b02);
        b03.setLargerBody(null);

        p0.setBodyList(BodyList.builder().smallestBody(b01).largestBody(b03).size(3).build());

        BodyNode t1 = TransitionNode.getLastTransition().bodyNodes.get(1);
        BodyNode t2 = TransitionNode.getLastTransition().bodyNodes.get(2);

        Body b11 = Body.builder().bodyNode(t1).parent(b01).value(new ScientificNumber(6, 0))
                .factor(new ScientificNumber(4, 0)).proved(true).build();
        Body b21 = Body.builder().bodyNode(t1).parent(b02).value(new ScientificNumber(12, 0))
                .factor(new ScientificNumber(6, 0)).proved(true).build();
        Body b31 = Body.builder().bodyNode(t1).parent(b03).value(new ScientificNumber(24, 0))
                .factor(new ScientificNumber(8, 0)).build();
        Body b22 = Body.builder().bodyNode(t2).parent(b02).value(new ScientificNumber(36, 0))
                .factor(new ScientificNumber(9, 0)).build();
        Body b32 = Body.builder().bodyNode(t2).parent(b03).value(new ScientificNumber(72, 0))
                .factor(new ScientificNumber(12, 0)).build();

        t1.getActiveBodies().add(b11);
        t1.getActiveBodies().add(b21);
        t1.getActiveBodies().add(b31);
        t2.getActiveBodies().add(b22);
        t2.getActiveBodies().add(b32);

        b11.setSmallerBody(null);
        b11.setLargerBody(b21);
        b21.setSmallerBody(b11);
        b21.setLargerBody(b31);
        b31.setSmallerBody(b21);
        b31.setLargerBody(b22);
        b22.setSmallerBody(b31);
        b22.setLargerBody(b32);
        b32.setSmallerBody(b22);
        b32.setLargerBody(null);
        b01.getOffsprings().add(b11);
        b02.getOffsprings().add(b21);
        b03.getOffsprings().add(b31);
        b02.getOffsprings().add(b22);
        b03.getOffsprings().add(b32);

        TransitionNode.getLastTransition().setBodyList(BodyList.builder().smallestBody(b11).largestBody(b32).size(5).build());
    }

    private BodyNode bodyNode;
    private ScientificNumber value;
    private ScientificNumber factor;
    private Body parent;
    private final List<Body> offsprings = new ArrayList<>();
    private final Set<Body> deactivatedOffsprings = new HashSet<>();
    @Builder.Default
    private boolean proved = false;
    @Builder.Default
    private Integer tempId = null;
    private Integer dbId = null;
    private Body smallerBody;
    private Body largerBody;
    @Builder.Default
    private boolean deactivated = false;
    private DbBody dbBody;
    private HcnGenerator hcnGenerator = null;

    public boolean isDeleted() {
        return smallerBody == null && largerBody == null;
    }
    public DbBody getDbBody() {
        if (dbBody == null) {
            dbBody = new DbBody(this);
        }
        return dbBody;
    }
    public HcnGenerator getPrevActiveBody() {
        Body candidate = smallerBody;
        while (candidate != null && candidate.getHcnGenerator() == null) {candidate = candidate.smallerBody;}
        if (candidate == null) {
            return null;
        } else {
            return candidate.hcnGenerator;
        }
    }

    @Override
    public int compareTo(Body other) {
        return this.value.compareTo(other.value);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        buildChain(sb);
        sb.append(" v=").append(value).append(" f=").append(factor);
        return sb.toString();
    }

    public void buildChain(StringBuilder sb) {
        if (parent != null) {
            parent.buildChain(sb);
            sb.append(", ");
        }
        if (bodyNode.getParentNode() instanceof ApiNode apiNode) {
            sb.append("p").append(apiNode.indexes.get(0).getIndex()).append("^").append(bodyNode.getBodyNodeId());
        } else {
            sb.append("t").append(bodyNode.getBodyNodeId());
        }
    }
    public void deactivate() {

        deactivated = true;
        bodyNode.getActiveBodies().remove(this);
        bodyNode.getDeactivatedBodies().add(this);
        bodyNode.getParentNode().needsDeactivateMaintain = true;

        if (parent != null) {
            parent.offsprings.remove(this);
            if (parent.offsprings.isEmpty()) {
                parent.deactivate();
            }
            parent.deactivatedOffsprings.add(this);
        }

        bodyNodeActivityMaintain();
    }

    public void deletedDuringExtension() {
        if (deactivated) {
            bodyNode.getDeactivatedBodies().remove(this);
            if (parent != null) {
                parent.deactivatedOffsprings.remove(this);
            }
        } else {
            bodyNode.getActiveBodies().remove(this);
            if (parent != null) {
                parent.offsprings.remove(this);
                if (!parent.isDeleted() && parent.offsprings.isEmpty()) {
                    parent.deactivate();
                }
            }
        }
        bodyNodeActivityMaintain();
    }

    public void deleteDuringBodyListMaintain() {

        if (deactivated) {
            bodyNode.getDeactivatedBodies().remove(this);
            if (parent != null) {
                parent.deactivatedOffsprings.remove(this);
            }
        } else {
            bodyNode.getActiveBodies().remove(this);
            if (parent != null) {
                parent.offsprings.remove(this);
                if (!parent.isDeleted() && parent.offsprings.isEmpty()) {
                    parent.deactivate();
                }
            }
        }
        bodyNodeActivityMaintain();
    }
    private void bodyNodeActivityMaintain() {
        if (bodyNode.getActiveBodies().isEmpty()) {
            MatrixNode parentNode = bodyNode.getParentNode();
            parentNode.bodyNodes.remove(bodyNode.getBodyNodeId());
            parentNode.deactivatedBodyNodes.put(bodyNode.getBodyNodeId(), bodyNode);
        }

        if (bodyNode.getDeactivatedBodies().isEmpty()) {
            MatrixNode parentNode = bodyNode.getParentNode();
            parentNode.deactivatedBodyNodes.remove(bodyNode.getBodyNodeId());
        }
    }

    public void matrixMaintainCheck() {
        if (proved) { return; }
        proved = true;
        bodyNode.extensionCheck();
        if (parent != null) { parent.matrixMaintainCheck(); }
    }

    public int getActiveHcnGeneratorCount() {
        if (bodyNode == null) {
            log.error("getActiveHcnGeneratorCount called on body with null bodyNode: value={} factor={} deactivated={} parent={}", value, factor, deactivated, parent);
            return 0;
        }
        if (bodyNode.getParentNode() instanceof TransitionNode transitionNode && transitionNode.getTransitionTo() == 1) {
            return 1;
        } else {
            int activeHcnGeneratorCount = 0;
            for (Body offspring : offsprings) {
                activeHcnGeneratorCount += offspring.getActiveHcnGeneratorCount();
            }
            return activeHcnGeneratorCount;
        }
    }

}
