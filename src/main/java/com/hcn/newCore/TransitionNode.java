package com.hcn.newCore;

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
    private PrimeCenter primeCenter;

    @Override
    protected BodyNode provideNextBodyNode() {
        return BodyNode.builder().parentNode(this).bodyNodeId(bodyNodes.lastKey() + 1)
                .value(bodyNodes.get(bodyNodes.lastKey()).getValue()
                        .multiply(indexes.get(indexes.size() - 1).getValue()))
                .factor(bodyNodes.get(bodyNodes.lastKey()).getFactor()
                        .multiply(new ScientificNumber((double) (transitionFrom + 1) / (transitionTo + 1), 0)))
                .build();
    }

    public Prime getLastPrime() {
        if (indexes.isEmpty()) {
            return prevMatrixNode.getIndexes().get(prevMatrixNode.getIndexes().size() - 1);
        } else {
            return indexes.get(indexes.size() - 1);
        }
    }

    @Override
    protected int determineBodyNodeIdLowLimit() {
        return transitionFrom;
    }

    @Override
    public void extensionCheck() {
        //log.debug("LocalExtension required at TransitionNode {}-{}", transitionFrom, transitionTo);
        Prime newPrime;
        if (nextMatrixNode == null) {
            newPrime = primeCenter.getPrime(getLastPrime().getIndex() + 1);
        } else {
            newPrime = ((TransitionNode) nextMatrixNode).releaseFirstIndex();
        }
        prepareNodeForBodyNodeCreation(newPrime);
        createNextBodyNode();
    }

    private Prime releaseFirstIndex() {
        ScientificNumber valueExcluded = new ScientificNumber(Math.pow(indexes.get(0).getIntValue(), transitionFrom), 0);
        ScientificNumber factorExcluded = new ScientificNumber(transitionFrom + 1, 0);
        bodyNodes.forEach((key, transition) -> {
            transition.setValue(transition.getValue().divide(valueExcluded));
            transition.setFactor(transition.getFactor().divide(factorExcluded));
            //log.debug("transition after update: {}", transition);
        });
        return indexes.remove(0);
    }

    private void prepareNodeForBodyNodeCreation(Prime newPrime) {
        //log.debug(" prepareNodeForBodyNodeCreation");
        indexes.add(newPrime);
        ScientificNumber valueMultiplier = new ScientificNumber(Math.pow(indexes.get(indexes.size() - 1).getIntValue(), transitionTo), 0);
        //log.debug("  prepareNodeForBodyNodeCreation: {}", valueMultiplier);
        bodyNodes.values().forEach(bodyNode -> {
            bodyNode.setValue(bodyNode.getValue().multiply(valueMultiplier));
            bodyNode.setFactor(bodyNode.getFactor().multiply(new ScientificNumber(transitionTo + 1, 0)));
            //log.debug("  prepareNodeForBodyNodeCreation bodyNode {}: ", bodyNode);
        });
        bodyList.forEach(body -> {
            body.setValue(body.getValue().multiply(valueMultiplier));
            body.setFactor(body.getFactor().multiply(new ScientificNumber(transitionTo + 1, 0)));
            //log.debug("  prepareNodeForBodyNodeCreation body {}: ", body);
        });
    }

    @Override
    public String toString() {
        return "TransitionNode{" +
                "transitionFrom=" + transitionFrom +
                ", transitionTo=" + transitionTo +
                ", indexes=" + indexes +
                '}';
    }
}
