package com.hcn.newCore;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.TreeSet;

@Getter
@Setter
@SuperBuilder
@Slf4j
public class TransitionNode extends MatrixNode{

    public static TransitionNode lastTransition;
    public static TransitionNode getLastTransition() {return lastTransition;}
    public static void reset() { lastTransition = null; }
    private static Body matrixMaintainLimitBody;
    public static Body getMatrixMaintainLimitBody() {return matrixMaintainLimitBody;}

    public static TransitionNode initialize() {

        Body b1 = ApiNode.getLastApiNode().bodyList.getSmallestBody();
        lastTransition = TransitionNode.builder().transitionFrom(2).transitionTo(1).build();
        lastTransition.indexes.add(Prime.getFirstPrime().getNextPrime());
        BodyNode t1 = BodyNode.builder().parentNode(lastTransition).bodyNodeId(1)
                .value(new ScientificNumber(3, 0))
                .factor(new ScientificNumber(2, 0)).lastParentProvider(b1).build();
        lastTransition.smallestActiveBodyNode = t1;
        lastTransition.largestActiveBodyNode = t1;
        lastTransition.nextBodyNode = BodyNode.builder()
                .parentNode(lastTransition).bodyNodeId(2).value(new ScientificNumber(9, 0))
                .factor(new ScientificNumber(3, 0)).lastParentProvider(null).build();
        lastTransition.factorLimit = new ScientificNumber(3,0);

        ApiNode.getLastApiNode().setNextMatrixNode(lastTransition);
        lastTransition.setPrevMatrixNode(ApiNode.getLastApiNode());

        Body b11 = Body.builder().bodyNode(t1).parent(b1).smallerBody(null).largerBody(null)
                .value(new ScientificNumber(6, 0)).factor(new ScientificNumber(4, 0)).build();
        lastTransition.setBodyList(BodyList.builder().smallestBody(b11).largestBody(b11).size(1).build());
        matrixMaintainLimitBody = b11;
        return lastTransition;
    }

    private final int transitionFrom;
    private final int transitionTo;

    public static void matrixMaintainOnBufferBodies() {
        while (matrixMaintainLimitBody.getLargerBody() != null) {
            matrixMaintainLimitBody = matrixMaintainLimitBody.getLargerBody();
            matrixMaintainLimitBody.matrixMaintainCheck();
        }
    }

    @Override
    protected BodyNode provideNextBodyNode() {
        return BodyNode.builder().parentNode(this).bodyNodeId(largestActiveBodyNode.getBodyNodeId() + 1)
                .value(largestActiveBodyNode.getValue().multiply(indexes.get(indexes.size() - 1).getValue()))
                .factor(largestActiveBodyNode.getFactor().multiply(new ScientificNumber((double) (transitionFrom + 1) / (transitionTo + 1), 0)))
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
    public void generateBodies(ScientificNumber factorLimit) {

        nextBodyNodeActivationCheck(factorLimit);

        TreeSet<Body> generatedBodies = new TreeSet<>();
        BodyNode bodyProvider = smallestActiveBodyNode;
        while (bodyProvider != null) {
            generatedBodies.addAll(bodyProvider.generateBodies(factorLimit));
            bodyProvider = bodyProvider.getLargerBodyNode();
        }

        log.info("Generated {} bodies", generatedBodies);
        log.info("getLargestBody {}", bodyList.getLargestBody());

        ScientificNumber oldFactorLimit = bodyList.getLargestBody().getFactor();
        for (Body generatedBody : generatedBodies) {
            if (generatedBody.getFactor().isBiggerThan(oldFactorLimit)) {
                oldFactorLimit = generatedBody.getFactor();
                bodyList.getLargestBody().setLargerBody(generatedBody);
                generatedBody.setSmallerBody(bodyList.getLargestBody());
                bodyList.setLargestBody(generatedBody);
            }
        }
    }

    private void nextBodyNodeActivationCheck(ScientificNumber factorLimit) {
        if (nextBodyNode.getLastParentProvider() == null) {
            if (prevMatrixNode instanceof ApiNode) {
                Body parentCandidate = prevMatrixNode.bodyList.getSmallestBody();
                while (parentCandidate != null && parentCandidate.getBodyNode().getBodyNodeId() < transitionFrom) {
                    parentCandidate = parentCandidate.getLargerBody();
                }
                nextBodyNode.setLastParentProvider(parentCandidate);
            }
        }

        ScientificNumber smallestFactor = nextBodyNode.getLastParentProvider().getFactor().multiply(nextBodyNode.getFactor());
        if (smallestFactor.isNotBiggerThan(factorLimit)) {
            log.debug("YES smallestFactor: {}, factorLimit: {}", smallestFactor, factorLimit);
        } else {
            log.debug("NO smallestFactor: {}, factorLimit: {}", smallestFactor, factorLimit);
        }
    }

    /*
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

        HcnGenerator hcnGenerator = HcnGenerator.getLastHcnGenerator();
        while (hcnGenerator != null) {
            hcnGenerator.setValue(hcnGenerator.getValue().multiply(valueMultiplier));
            hcnGenerator.setFactor(hcnGenerator.getFactor().multiply(new ScientificNumber(transitionTo + 1, 0)));
            hcnGenerator = hcnGenerator.getPreviousHcnGenerator();
        }
    }

     */

    @Override
    public String toString() {
        return "TransitionNode{" +
                "transitionFrom=" + transitionFrom +
                ", transitionTo=" + transitionTo +
                ", indexes=" + indexes +
                '}';
    }

    public ScientificNumber recalculateAfterFirstIndexSteal() {
        ScientificNumber valueExcluded = new ScientificNumber(Math.pow(indexes.get(0).getIntValue(), transitionFrom), 0);
        ScientificNumber factorExcluded = new ScientificNumber(transitionFrom + 1, 0);

        BodyNode activeTransition = smallestActiveBodyNode;
        while (activeTransition != null) {
            activeTransition.setValue(activeTransition.getValue().divide(valueExcluded));
            activeTransition.setFactor(activeTransition.getFactor().divide(factorExcluded));
            activeTransition = activeTransition.getLargerBodyNode();
        }
        return factorExcluded;
    }
}
