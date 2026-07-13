package com.hcn.newCore;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

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
                .value(bodyNodes.get(bodyNodes.lastKey()).getValue().multiply(indexes.get(indexes.size() - 1).getValue()))
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
    protected ScientificNumber determineDeactivationLimitMultiplier() {
        return  indexes.stream().filter(prime -> prime.getIndex() == bodyNodes.firstKey() + 1).findFirst().get().getValue();
    }

    @Override
    public ScientificNumber getSmallestPossibleExtension() {
        Prime nextprime;
        if (nextMatrixNode == null) {
            nextprime = primeCenter.getPrime(bodyNodes.lastKey() + 1);
        } else {
            nextprime = nextMatrixNode.indexes.get(0);
        }
        ScientificNumber localextensionSmallest = bodyNodes.lastEntry().getValue().getValue().multiply(nextprime.getValue());

        ScientificNumber valueForLocalExtension = prevMatrixNode.getValurForNextMatrixExtension(transitionFrom);
        localextensionSmallest = localextensionSmallest.multiply(valueForLocalExtension);
        ScientificNumber prevExtensionSmallest = prevMatrixNode.getSmallestPossibleExtension().multiply(bodyNodes.firstEntry().getValue().getValue());
        if (prevExtensionSmallest.isSmallerThan(localextensionSmallest)) {
            return prevExtensionSmallest;
        } else {
            return localextensionSmallest;
        }
    }

    @Override
    protected ScientificNumber getValurForNextMatrixExtension(int bodyNodeId) {
        return bodyNodes.firstEntry().getValue().getValue().multiply(prevMatrixNode.getValurForNextMatrixExtension(transitionFrom));
    }

    @Override
    public void extensionCheck() {
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
        });
        return indexes.remove(0);
    }

    private void prepareNodeForBodyNodeCreation(Prime newPrime) {
        indexes.add(newPrime);
        ScientificNumber valueMultiplier = new ScientificNumber(Math.pow(indexes.get(indexes.size() - 1).getIntValue(), transitionTo), 0);
        bodyNodes.values().forEach(bodyNode -> {
            bodyNode.setValue(bodyNode.getValue().multiply(valueMultiplier));
            bodyNode.setFactor(bodyNode.getFactor().multiply(new ScientificNumber(transitionTo + 1, 0)));
        });
        bodyList.forEach(body -> {
            body.setValue(body.getValue().multiply(valueMultiplier));
            body.setFactor(body.getFactor().multiply(new ScientificNumber(transitionTo + 1, 0)));
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
