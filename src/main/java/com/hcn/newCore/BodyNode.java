package com.hcn.newCore;

import java.util.List;
import java.util.Set;

public interface BodyNode {

    int getBodyNodeId();

    ScientificNumber getValue();
    void setValue(ScientificNumber value);

    ScientificNumber getFactor();
    void setFactor(ScientificNumber factor);

    boolean isProved();
    void setProved(boolean proved);

    Set<Body> getActiveBodies();

    void extensionCheck(Body newProvedBody);
}
