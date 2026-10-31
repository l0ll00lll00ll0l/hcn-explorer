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
    public static TransitionNode lastTransition;

    private final int transitionFrom;
    private final int transitionTo;

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

    public static TransitionNode getLastTransition() {
        return lastTransition;
    }

    public static void reset() { lastTransition = null; }

    public static TransitionNode initialize() {

        ApiNode p0 = ApiNode.builder().prevMatrixNode(null).build();
        p0.getIndexes().add(Prime.getFirstPrime());
        BodyNode pip01 = BodyNode.builder().parentNode(p0).bodyNodeId(1).proved(true)
                .value(new ScientificNumber(2,0))
                .factor(new ScientificNumber(2, 0)).build();
        BodyNode pip02 = BodyNode.builder().parentNode(p0).bodyNodeId(2).proved(true)
                .value(new ScientificNumber(4,0))
                .factor(new ScientificNumber(3, 0)).build();
        BodyNode pip03 = BodyNode.builder().parentNode(p0).bodyNodeId(3)
                .value(new ScientificNumber(8,0))
                .factor(new ScientificNumber(4, 0)).build();
        p0.getBodyNodes().put(1, pip01);
        p0.getBodyNodes().put(2, pip02);
        p0.getBodyNodes().put(3, pip03);

        lastTransition = TransitionNode.builder().transitionFrom(2).transitionTo(1).build();
        lastTransition.indexes.add(Prime.getFirstPrime().getNextPrime());
        BodyNode t1 = BodyNode.builder().parentNode(lastTransition).bodyNodeId(1)
                .value(new ScientificNumber(1, 0))
                .factor(new ScientificNumber(1, 0)).proved(true).build();
        lastTransition.getBodyNodes().put(1, t1);
        BodyNode t2 = BodyNode.builder()
                .parentNode(lastTransition).bodyNodeId(2).value(new ScientificNumber(9, 0))
                .factor(new ScientificNumber(3, 0)).build();
        lastTransition.getBodyNodes().put(2, t2);

        p0.setNextMatrixNode(lastTransition);
        lastTransition.setPrevMatrixNode(p0);
        return lastTransition;
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
            nextprime = Prime.getPrime(bodyNodes.lastKey() + 1);
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
            newPrime = Prime.getPrime(getLastPrime().getIndex() + 1);
            Prime.recalculateAllMultipliers(newPrime);
        } else {
            newPrime = ((TransitionNode) nextMatrixNode).releaseFirstIndex();
        }
        prepareNodeForBodyNodeCreation(newPrime);
        createNextBodyNode();
    }

    @Override
    public void transitionNodeTriggerCheck() {}

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
