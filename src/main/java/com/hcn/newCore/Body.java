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

    private static Body smallestBufferBody;
    public static Body getSmallestBufferBody() {return smallestBufferBody;}
    public static void setSmallestBufferBody(Body smallestBufferBody) {Body.smallestBufferBody = smallestBufferBody;}

    public static void shiftNextHcnGeneratorBody() {
        smallestBufferBody.matrixMaintainCheck();
        smallestBufferBody = smallestBufferBody.largerBody;
    }

    public static void initialize() {
        smallestBufferBody = TransitionNode.getLastTransition().getBodyList().getSmallestBody();
    }

    private BodyNode bodyNode;
    private ScientificNumber value;
    private ScientificNumber factor;
    private Body parent;
    private final List<Body> offsprings = new ArrayList<>();
    private OffspringState offspringState;
    @Builder.Default
    private Integer tempId = null;
    private Integer dbId = null;
    private Body smallerBody;
    private Body largerBody;
    private DbBody dbBody;

    public DbBody getDbBody() {
        if (dbBody == null) {
            dbBody = new DbBody(this);
        }
        return dbBody;
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

    public String getBodyChain() {
        StringBuilder sb = new StringBuilder();
        buildChain(sb);
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

    public void deletedDeactivatedBodyDuringExtension() {

        log.info("TBH this should not really happen {}", this);
        //bodyNode.deleteActiveBody(this);
    }

    /*
    public void deleteActiveBodyDuringExtension() {

        log.debug("deleteActiveBodyDuringExtension body {}", this);
        if (parent != null) {
            parent.offsprings.remove(this);
            parent.deletedOffsprings.add(this);
            this.setOffspringState(OffspringState.DOMINATED_SUPERIOR_BODY);
            log.debug("deleteActiveBodyDuringExtension parent {} body {}", parent, this);
            if (!parent.deactivated && parent.offsprings.isEmpty()) {
                parent.deactivate();
            }
        }

        //bodyNode.deleteActiveBody(this);
    }

    public void deactivate() {

        if (deactivated) {
            log.info("silent deactivating body {}", this);
            return;
        }
        log.info("deactivating body {}", this);
        deactivated = true;
        //bodyNode.deleteActiveBody(this);
        //bodyNode.deactivateBody(this);

        if (parent != null) {
            parent.offsprings.remove(this);
            parent.deletedOffsprings.add(this);
            offspringState = OffspringState.UNEXTENDABLE;
            log.debug("deactivate parent {} body {}", parent, this);
            if (parent.offsprings.isEmpty()) {
                parent.deactivate();
            }
        }
    }

     */

    public void matrixMaintainCheck() {

        if (parent != null) { parent.matrixMaintainCheck(); }

        if (bodyNode.getParentNode() instanceof ApiNode apiNode) {
            if (bodyNode.equals(apiNode.getLargestActiveBodyNode())) {
                if (apiNode.equals(ApiNode.getLastApiNode())) {
                    TransitionNode transitionNode = (TransitionNode) apiNode.getNextMatrixNode();
                    if (bodyNode.getBodyNodeId() == transitionNode.getTransitionFrom()) {
                        ApiNode.builder().prevMatrixNode(apiNode).nextMatrixNode(transitionNode).build().create();
                    }
                }
                apiNode.createNextBodyNode();
            }
        }
    }

    public int getActiveHcnGeneratorCount() {
        if (bodyNode == null) {
            log.error("getActiveHcnGeneratorCount called on body with null bodyNode: value={} factor={}  parent={}", value, factor, parent);
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

    public void createPowerGroups(HcnGenerator hcnGenerator) {
        if (parent != null) {
            parent.createPowerGroups(hcnGenerator);
            int currentPower = hcnGenerator.getPowerGroupPowers().get(hcnGenerator.getPowerGroupPowers().size() - 1);
            if (bodyNode.getParentNode() instanceof ApiNode apiNode) {
                if (bodyNode.getBodyNodeId() < currentPower) {
                    hcnGenerator.getPowerGroupPowers().add(bodyNode.getBodyNodeId());
                    hcnGenerator.getPowerGroupIndexes().add(apiNode.indexes.get(0).getIndex());
                }
            } else {
                TransitionNode transitionNode = (TransitionNode) bodyNode.getParentNode();
                if (transitionNode.getTransitionFrom() < currentPower) {
                    if (transitionNode.getIndexes().get(0).getIndex() < bodyNode.getBodyNodeId()) {
                        hcnGenerator.getPowerGroupPowers().add(transitionNode.getTransitionFrom());
                        hcnGenerator.getPowerGroupIndexes().add(transitionNode.indexes.get(0).getIndex());
                    }
                }
                hcnGenerator.getPowerGroupPowers().add(transitionNode.getTransitionTo());
                hcnGenerator.getPowerGroupIndexes().add(bodyNode.getBodyNodeId());
            }
        } else {
            hcnGenerator.getPowerGroupPowers().add(bodyNode.getBodyNodeId());
            hcnGenerator.getPowerGroupIndexes().add(0);
        }
    }

    /*
    public void deletePolledBody() {

        bodyNode.getParentNode().getBodyList().deleteMaintain(this);
        if (parent != null) {
            parent.getOffsprings().remove(this);
            setOffspringState(OffspringState.POLLED_OFFSPRING);
            parent.getPolledOffsprings().add(this);
            if (parent.getOffsprings().isEmpty()) {
                parent.deletePolledBody(); }
            }
    }

     */
}
