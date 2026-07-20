package com.hcn.newCore;

import com.hcn.event.ActivityCenter;
import com.hcn.event.MatrixExtensionActivity;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Getter
@Setter
@SuperBuilder
@Slf4j
public abstract class MatrixNode {

    protected MatrixNode prevMatrixNode = null;
    protected MatrixNode nextMatrixNode= null;
    protected Integer tempId = null;
    protected BodyList bodyList;
    protected final TreeMap<Integer, BodyNode> bodyNodes = new TreeMap<>();
    protected final TreeMap<Integer, BodyNode> deactivatedBodyNodes = new TreeMap<>();
    protected final List<Prime> indexes = new ArrayList<>();

    public List<Prime> getIndexes() { return indexes; }
    protected boolean needsDeactivateMaintain = false;

    public List<Body> deactivatedMaintain() {
        ArrayList<Body> deactivatedHcnGenerators = new ArrayList<>();
        if (needsDeactivateMaintain) {
            ScientificNumber smallestPossibleExtension = getSmallestPossibleExtension();
            List<Body> deletedBodies = bodyList.deactivatedMaintain(smallestPossibleExtension);
            if (nextMatrixNode == null) {
                deactivatedHcnGenerators.addAll(deletedBodies);
            }
            needsDeactivateMaintain = false;
        }

        if (prevMatrixNode != null) {
            prevMatrixNode.deactivatedMaintain();
        }
        return deactivatedHcnGenerators;
    }

    protected BodyNode getLargestProvedBodyNode() {
        if (bodyNodes.get(bodyNodes.lastKey()).isProved()) {return bodyNodes.get(bodyNodes.lastKey());}
        return bodyNodes.get(bodyNodes.lastKey() - 1);
    }

    public void generateNewBodies(List<Body> incomingParents) {
        needsDeactivateMaintain = false;
        Set<Body> createdBodies = incomingParents.stream()
                .flatMap(previousBody -> bodyNodes.values().stream()
                        .map(bodyNode -> Body.builder().bodyNode(bodyNode).parent(previousBody)
                                .value(bodyNode.getValue().multiply(previousBody.getValue()))
                                .factor(bodyNode.getFactor().multiply(previousBody.getFactor())).build()))
                .collect(Collectors.toSet());

        bodyList.mergeBodies(createdBodies);
        parentDeactivationCheck(incomingParents);

        if (nextMatrixNode != null) {
            nextMatrixNode.generateNewBodies(bodyList.getSuccessfullyAddedNewBodies());
        } else {
            bodyList.maintainHcnGeneratorList();
        }

    }

    public void createNextBodyNode() {
        needsDeactivateMaintain = true;
        int nextBodyNodeId = bodyNodes.lastKey() + 1;
        BodyNode nextBodyNode = provideNextBodyNode();
        if (ActivityCenter.isDbMode()) new MatrixExtensionActivity(nextBodyNode);
        bodyNodes.put(nextBodyNodeId, nextBodyNode);

        Set<Body> createdBodies;
        if (prevMatrixNode == null) {
            createdBodies = Set.of(Body.builder().bodyNode(nextBodyNode).parent(null).value(nextBodyNode.getValue()).factor(nextBodyNode.getFactor()).build());
            bodyList.mergeBodies(createdBodies);
        } else {
            int bodyNodeIdLowLimit = determineBodyNodeIdLowLimit();
            List<Body> parents = prevMatrixNode.bodyNodes.values().stream()
                    .filter(pip -> pip.getBodyNodeId() >= bodyNodeIdLowLimit)
                    .flatMap(pip -> pip.getActiveBodies().stream()).collect(Collectors.toList());
            createdBodies = parents.stream().map(parentBody -> Body.builder().bodyNode(nextBodyNode).parent(parentBody)
                            .value(nextBodyNode.getValue().multiply(parentBody.getValue()))
                            .factor(nextBodyNode.getFactor().multiply(parentBody.getFactor())).build())
                    .collect(Collectors.toSet());
            bodyList.mergeBodies(createdBodies);
            parentDeactivationCheck(parents);
        }

        if (nextMatrixNode != null) {
            nextMatrixNode.generateNewBodies(bodyList.getSuccessfullyAddedNewBodies());
        } else {
            bodyList.maintainHcnGeneratorList();
        }
        ActivityCenter.finishMatrixExtensionActivity();
    }

    private void parentDeactivationCheck(List<Body> parents) {
        parents.stream().filter(body -> body.getOffsprings().isEmpty() && body.getDeactivatedOffsprings().isEmpty()).forEach(Body::deactivate);
    }

    protected abstract BodyNode provideNextBodyNode();
    protected abstract int determineBodyNodeIdLowLimit();
    protected abstract ScientificNumber determineDeactivationLimitMultiplier();
    public abstract ScientificNumber getSmallestPossibleExtension();
    protected abstract ScientificNumber getValurForNextMatrixExtension(int bodyNodeId);
    public abstract void extensionCheck();
    public abstract void transitionNodeTriggerCheck();
}
