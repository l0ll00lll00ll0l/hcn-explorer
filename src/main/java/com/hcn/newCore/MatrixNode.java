package com.hcn.newCore;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@SuperBuilder
@Slf4j
public abstract class MatrixNode {

    protected MatrixNode prevMatrixNode;
    protected MatrixNode nextMatrixNode;
    protected Integer tempId;
    protected BodyList bodyList;
    protected BodyNode nextBodyNode;
    protected ScientificNumber factorLimit;
    protected BodyNode smallestActiveBodyNode;
    protected BodyNode largestActiveBodyNode;
    protected final List<Prime> indexes = new ArrayList<>();

    public List<Prime> getIndexes() { return indexes; }

    public BodyNode getBodyNode(int bodyNodeId) {
        if (bodyNodeId < smallestActiveBodyNode.getBodyNodeId()) {return null;}
        BodyNode walker = smallestActiveBodyNode;
        while (walker != null && walker.getBodyNodeId() < bodyNodeId) {walker = walker.getLargerBodyNode();}
        return walker;
    }

    protected abstract BodyNode provideNextBodyNode();
    public abstract void generateBodies(ScientificNumber factorLimit);
}
