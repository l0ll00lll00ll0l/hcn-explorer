package com.hcn.newCore;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;


@Getter @Setter @SuperBuilder @Slf4j
public class ApiNode extends MatrixNode {

    private static ApiNode lastApiNode;

    public static ApiNode getLastApiNode() {return lastApiNode;}

    public static void initialize() {
        lastApiNode = ApiNode.builder().prevMatrixNode(null).build();
        lastApiNode.getIndexes().add(Prime.getFirstPrime());
        BodyNode pip01 = BodyNode.builder().parentNode(lastApiNode).bodyNodeId(1).lastParentProvider(null)
                .value(new ScientificNumber(2,0))
                .factor(new ScientificNumber(2, 0)).build();
        BodyNode pip02 = BodyNode.builder().parentNode(lastApiNode).bodyNodeId(2).lastParentProvider(null)
                .value(new ScientificNumber(4,0))
                .factor(new ScientificNumber(3, 0)).build();
        lastApiNode.smallestActiveBodyNode = pip01;
        lastApiNode.largestActiveBodyNode = pip01;
        lastApiNode.nextBodyNode = pip02;
        lastApiNode.factorLimit = new ScientificNumber(2,0);

        Body b01 = Body.builder().bodyNode(pip01).parent(null).value(new ScientificNumber(2, 0))
                .factor(new ScientificNumber(2, 0)).smallerBody(null).largerBody(null).build();
        lastApiNode.setBodyList(BodyList.builder().smallestBody(b01).largestBody(b01).size(1).build());
    }

    @Override
    protected BodyNode provideNextBodyNode() {
        return BodyNode.builder().parentNode(this).bodyNodeId(largestActiveBodyNode.getBodyNodeId() + 1)
                .value(largestActiveBodyNode.getValue().multiply(indexes.get(0).getValue()))
                .factor(new ScientificNumber(largestActiveBodyNode.getBodyNodeId() + 2, 0)).build();
    }

    @Override
    public void generateBodies(ScientificNumber factorLimit) {

    }

    @Override
    public String toString() {
        return "ApiNode{" +
                "index=" + indexes.get(0) +
                ", prime=" + indexes.get(0).getIntValue() +
                '}';
    }

    public void createNextBodyNode() {
        largestActiveBodyNode.setLargerBodyNode(nextBodyNode);
        nextBodyNode.setSmallerBodyNode(largestActiveBodyNode);
        largestActiveBodyNode = nextBodyNode;
        if (prevMatrixNode == null) {
            bodyList.getLargestBody().setLargerBody(Body.builder().bodyNode(nextBodyNode).parent(null).value(nextBodyNode.getValue())
                    .factor(nextBodyNode.getFactor()).smallerBody(bodyList.getLargestBody()).largerBody(null).build());
            bodyList.setLargestBody(bodyList.getLargestBody().getLargerBody());
        } else {
            //TODO newBodyNodeShould align factorlimit
        }

        nextBodyNode = provideNextBodyNode();

    }

    public void create() {
        log.info("API Node is creating");
        TransitionNode transitionNode = (TransitionNode) nextMatrixNode;
        Prime newIndex = transitionNode.indexes.get(0);
        indexes.add(newIndex);
        prevMatrixNode.setNextMatrixNode(this);
        nextMatrixNode.setPrevMatrixNode(this);

        if (transitionNode.getSmallestActiveBodyNode().getBodyNodeId() == newIndex.getIndex()) {
            smallestActiveBodyNode = transitionNode.getSmallestActiveBodyNode();
            largestActiveBodyNode = smallestActiveBodyNode;
            nextBodyNode = provideNextBodyNode();
            log.info("smallestActiveBodyNode moved");
            if (transitionNode.getLargestActiveBodyNode().equals(transitionNode.getSmallestActiveBodyNode())) {
                log.info("transitionNode needs care");
            }
        }

        factorLimit = prevMatrixNode.getFactorLimit().multiply(transitionNode.recalculateAfterFirstIndexSteal());
        log.info("factorLimit recalculated: {}", factorLimit);
    }
}
