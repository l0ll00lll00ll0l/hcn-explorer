package com.hcn.newCore;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@SuperBuilder
@Slf4j
public class TransitionNode extends MatrixNode{
    private final int transitionFrom;
    private final int transitionTo;
    private int firstIndex;
    private int lastIndex;
    private PrimeCenter primeCenter;

    @Override
    protected BodyNode provideNextBodyNode() {
        return BodyNode.builder().parentNode(this).bodyNodeId(bodyNodes.lastKey() + 1)
                .value(bodyNodes.get(bodyNodes.lastKey()).getValue()
                        .multiply(new ScientificNumber(primeCenter.getPrime(bodyNodes.lastKey()), 0)))
                .factor(bodyNodes.get(bodyNodes.lastKey()).getFactor().multiply(new ScientificNumber((double) (transitionFrom + 1) / (transitionTo + 1), 0))).build();
    }

    @Override
    protected int determineBodyNodeIdLowLimit() {
        return transitionFrom;
    }

    @Override
    public void extensionCheck() {
        log.debug("LocalExtension required at TransitionNode {}-{}", transitionFrom, transitionTo);
        if (nextMatrixNode == null) {
            prepareNodeForBodyNodeCreation();
        }
        createNextBodyNode();
    }

    private void prepareNodeForBodyNodeCreation() {
        log.debug(" prepareNodeForBodyNodeCreation");
        lastIndex ++;
        ScientificNumber valueMultiplier = new ScientificNumber(primeCenter.getPrime(lastIndex - 1), 0);
        bodyNodes.values().forEach(bodyNode -> {
            bodyNode.setValue(bodyNode.getValue().multiply(valueMultiplier));
            bodyNode.setFactor(bodyNode.getFactor().multiply(new ScientificNumber(transitionTo + 1, 0)));
        });
        bodyList.forEach(body -> {
            body.setValue(body.getValue().multiply(valueMultiplier));
            body.setFactor(body.getFactor().multiply(new ScientificNumber(transitionTo + 1, 0)));
        });
    }
}
