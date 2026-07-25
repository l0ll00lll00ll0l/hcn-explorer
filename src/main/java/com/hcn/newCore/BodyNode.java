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
    private ScientificNumber value;
    private ScientificNumber factor;
    private final TreeSet<Body> activeBodies = new TreeSet<>();
    private final Set<Body> deactivatedBodies = new HashSet<>();
    @Builder.Default
    private Integer tempId = null;


    public void extensionCheck() {
        if (proved) { return; }
        proved = true;
        parentNode.extensionCheck();
    }

    public boolean isActive() {
        return parentNode.bodyNodes.containsValue(this);
    }

    public boolean isDeactivated() {
        return parentNode.deactivatedBodyNodes.containsValue(this);
    }

    @Override
    public String toString() {
        return "BodyNode{" +
                "parentNode=" + parentNode +
                ", bodyNodeId=" + bodyNodeId +
                ", proved=" + proved +
                ", value=" + value +
                ", factor=" + factor +
                ", activeBodies=" + activeBodies +
                '}';
    }
}
