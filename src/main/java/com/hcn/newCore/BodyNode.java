package com.hcn.newCore;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;
import java.util.TreeSet;

@Getter
@Setter
@Builder
public class BodyNode {

    private final MatrixNode parentNode;
    private final int bodyNodeId;
    @Builder.Default
    private boolean proved = false;
    @Builder.Default
    private boolean hasHunterBody = false;
    private ScientificNumber value;
    private ScientificNumber factor;
    private final TreeSet<Body> activeBodies = new TreeSet<>();
    private final Set<Body> deactivatedBodies = new HashSet<>();
    @Builder.Default
    private Integer tempId = null;


    public void extensionCheck() {
        if (hasHunterBody) { return; }
        hasHunterBody = true;
        parentNode.extensionCheck();
    }

    public void setProvedRecursive() {
        if (proved) { return; }
        proved = true;
    }

    public boolean isActive() {
        return parentNode.bodyNodes.containsValue(this);
    }

    public boolean isDeactivated() {
        return parentNode.deactivatedBodyNodes.containsValue(this);
    }

    @Override
    public String toString() {
        if (parentNode instanceof ApiNode) {
            return "p" + parentNode.indexes.get(0).getIndex() + "^" + bodyNodeId;
        } else {
            return "t" + bodyNodeId;
        }
    }
}
