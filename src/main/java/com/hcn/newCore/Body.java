package com.hcn.newCore;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import com.hcn.db.DbBody;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter @Setter @Builder @Slf4j
public class Body implements Comparable<Body>{

    // TODO Future Body class
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
    private HunterBody hunterBody = null;
    private RecorderBody recorderBody = null;

    public boolean isDeleted() {
        return smallerBody == null && largerBody == null;
    }
    public DbBody getDbBody() {
        if (dbBody == null) {
            dbBody = new DbBody(this);
        }
        return dbBody;
    }
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
