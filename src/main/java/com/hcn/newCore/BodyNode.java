package com.hcn.newCore;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    private final Set<Body> activeBodies = new HashSet<>();
    @Builder.Default
    private Integer tempId = null;


    public void extensionCheck() {
        if (proved) {return;}
        proved = true;
        parentNode.extensionCheck();
    }

    public boolean isActive() {
        return parentNode.bodyNodes.containsValue(this);
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
