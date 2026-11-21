package com.hcn.newCore;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

@Getter
@Setter
@Builder
@Slf4j
public class BodyNode {

    private final MatrixNode parentNode;
    private int bodyNodeId;
    private BodyNode smallerBodyNode;
    private BodyNode largerBodyNode;
    private ScientificNumber value;
    private ScientificNumber factor;
    private Body lastParentProvider;
    @Builder.Default
    private Integer tempId = null;

    @Override
    public String toString() {
        if (parentNode instanceof ApiNode) {return "p" + parentNode.indexes.get(0).getIndex() + "^" + bodyNodeId;}
        else {return "t" + bodyNodeId;}
    }

    public List<Body> generateBodies(ScientificNumber factorLimit) {
        
        List<Body> factorLimitBodies = new ArrayList<>();
        log.info("generateBodies: factorLimit = {}, factor: {}", factorLimit, factor);
        ScientificNumber parentFactorLimit = factorLimit.divide(factor);
        log.info("generateBodies: parentFactorLimit = {}", parentFactorLimit);
        while (lastParentProvider.getLargerBody() != null && lastParentProvider.getLargerBody().getFactor().isNotBiggerThan(parentFactorLimit)) {
            lastParentProvider = lastParentProvider.getLargerBody();
            factorLimitBodies.add(createBody());
        }
        return factorLimitBodies;
    }

    private Body createBody() {
        Body newBody = Body.builder().bodyNode(this).parent(lastParentProvider)
                .value(this.getValue().multiply(lastParentProvider.getValue()))
                .factor(this.getFactor().multiply(lastParentProvider.getFactor())).build();
        lastParentProvider.getOffsprings().add(newBody);
        return newBody;
    }
}
