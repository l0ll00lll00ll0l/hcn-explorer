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
import java.util.TreeMap;
import java.util.TreeSet;

@Getter @Setter @Builder @Slf4j
public class Body implements Comparable<Body>{
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
    private Body smallerHcnGenerator;
    private Body largerHcnGenerator;
    private Body previousRecorder;
    private Body nextRecorder;
    private Body prayBody;
    private final List<Body> hunters = new ArrayList<>();
    private final List<Hcn> generatedHcns = new ArrayList<>();
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
    private DbBody dbBody;

    public boolean isDeleted() {
        return smallerBody == null && largerBody == null;
    }
    public boolean isNonDeactivated() {return smallerHcnGenerator != null || largerHcnGenerator != null;}

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

    public void deactivate() {

        deactivated = true;
        bodyNode.getActiveBodies().remove(this);
        bodyNode.getDeactivatedBodies().add(this);
        bodyNode.getParentNode().needsDeactivateMaintain = true;

        if (parent != null) {
            //log.debug("deactivate parent offspring remove: {}", parent);
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
                //log.debug("deletedDuringExtension parent deactivatedOffsprings remove: {}", parent);
                parent.deactivatedOffsprings.remove(this);
            }
        } else {
            bodyNode.getActiveBodies().remove(this);
            if (parent != null) {
                //log.debug("deletedDuringExtension parent offspring remove: {}", parent);
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
                //log.debug("deleteDuringBodyListMaintain parent deactivatedOffsprings remove: {}", parent);
                parent.deactivatedOffsprings.remove(this);
            }
        } else {
            bodyNode.getActiveBodies().remove(this);
            if (parent != null) {
                //log.debug("deleteDuringBodyListMaintain parent offsprings remove: {}", parent);
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

    public Hcn generateHcn(Lapi lapi) {

        lastGeneratedHcn = Hcn.builder().body(this).lapi(lapi).value(getValue().multiply(lapi.getValueMultiplier()))
                .factor(getFactor().multiply(lapi.getFactorMultiplier())).build();
        log.debug("Generating Hcn for Body {} with lapi {}", lastGeneratedHcn, lapi.getPrime().getIndex());

        if (firstHcn == null) {
            firstHcn = lastGeneratedHcn;
        }

        return lastGeneratedHcn;
    }

    public Hcn generateNextHcn() {
        Lapi nextLapi = lastGeneratedHcn.getLapi().getHigherLapi();
        if (nextLapi == null) {
            nextLapi = Lapi.createNextLapi();
        }
        lastGeneratedHcn = Hcn.builder().body(this).lapi(nextLapi).value(getValue().multiply(nextLapi.getValueMultiplier()))
                .factor(getFactor().multiply(nextLapi.getFactorMultiplier())).build();
        log.debug("Generating Hcn for Body {} with lapi {}", lastGeneratedHcn, nextLapi.getPrime().getIndex());
        if (firstHcn == null) {
            firstHcn = lastGeneratedHcn;
        }
        return lastGeneratedHcn;
    }

    public void hunt(Hcn generatedHcn) {

        if (firstHcn.equals(generatedHcn)) {
            log.debug("{} First activation", generatedHcn);
            RecorderList.findPrayForHunterBody(generatedHcn);
            //RecorderList.placeNewActiveBody(generatedHcn);

        }

            if (prayBody != null) {
                Hcn targetHcn = prayBody.getHcnForLapi(generatedHcn.getLapi().getHigherLapi());
                if (generatedHcn.getValue().isSmallerThan(targetHcn.getValue())) {
                    log.debug("{} moving ahead of pray: {}", generatedHcn, targetHcn);
                    RecorderList.prayHunted(generatedHcn);
                } else {
                    log.debug("{} staying behind pray: {}", generatedHcn, targetHcn);
                }
            } else {
                if (isRecorder()) {
                    log.debug("{} No action for recorder body with null pray", generatedHcn);
                } else {
                    if (deactivated) {
                        log.debug("{} No action for deactivated recorder body", generatedHcn);
                    } else {
                        RecorderList.placeBodyWithoutPray(generatedHcn);
                    }
                }
            }
        log.debug("");
    }

    public Hcn getHcnForLapi(Lapi lapi) {
        for (int i = generatedHcns.size() - 1; i >= 0; i--) {
            if (generatedHcns.get(i).getLapi().equals(lapi)) {
                return generatedHcns.get(i);
            }
        }
        return null;
    }

    public boolean isRecorder() {
        return nextRecorder != null || previousRecorder != null ;
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
}
