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


    public void extensionCheck() {
        if (proved) {return;}
        parentNode.extensionCheck();
        proved = true;
    }
}
