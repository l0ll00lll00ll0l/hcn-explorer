package com.hcn.newCore;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

@Getter
@Setter
@Builder
@Slf4j
public class BodyNode {

    private final MatrixNode parentNode;
    private final int bodyNodeId;
    @Builder.Default
    private boolean hasHunterBody = false;
    private ScientificNumber value;
    private ScientificNumber factor;
    private final TreeSet<Body> activeBodies = new TreeSet<>();
    private final Set<Body> deactivatedBodies = new HashSet<>();
    @Builder.Default
    private Integer tempId = null;

    public boolean isDeactivated() {
        return parentNode.deactivatedBodyNodes.containsValue(this);
    }

    public void extensionCheck() {
        if (hasHunterBody) { return; }
        hasHunterBody = true;
        parentNode.extensionCheck();
    }

    public boolean isActive() {
        return parentNode.bodyNodes.containsValue(this);
    }

    @Override
    public String toString() {
        if (parentNode instanceof ApiNode) {return "p" + parentNode.indexes.get(0).getIndex() + "^" + bodyNodeId;}
        else {return "t" + bodyNodeId;}
    }

    public boolean isBodyNodeLastActive() {
        return parentNode.bodyNodes.lastEntry().getValue() == this;
    }

    public void deleteActiveBody(Body bodyToRemove) {
        log.info("Removing body {}", bodyToRemove);
        activeBodies.remove(bodyToRemove);
        if (activeBodies.isEmpty()) {
            log.info("No more active bodies for {}", this);
            parentNode.bodyNodes.remove(bodyNodeId);
            if (!deactivatedBodies.isEmpty()) {
                log.info("But there are deactivated ones");
                parentNode.deactivatedBodyNodes.put(bodyNodeId, this);
            }
        }
    }

    public void deleteDeactivatedBody(Body deactivatedBodyToRemove) {
        log.info("Removing deactivated body {}", deactivatedBodyToRemove);
        deactivatedBodies.remove(deactivatedBodyToRemove);
        if (deactivatedBodies.isEmpty()) {
            log.info("No more deactivated bodies for {}", this);
            parentNode.deactivatedBodyNodes.remove(bodyNodeId);
        }
    }

    public void deactivateBody(Body bodyToDeactivate) {
        log.info("Deactivating body {}", bodyToDeactivate);
        deactivatedBodies.add(bodyToDeactivate);
    }
}
