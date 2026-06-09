package com.hcn.newCore;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Builder
public class Pip implements BodyNode{

    private final ApiNode api;
    private final int bodyNodeId;
    @Builder.Default
    private boolean proved = false;
    private ScientificNumber value;
    private ScientificNumber factor;
    private final Set<Body> activeBodies = new HashSet<>();

    @Override
    public void extensionCheck(Body newProvedBody) {
        if (proved) {return;}
        proved = true;
        api.extensionCheck(this);
    }
}
