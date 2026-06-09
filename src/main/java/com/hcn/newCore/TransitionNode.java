package com.hcn.newCore;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Getter
@Setter
@Builder
public class TransitionNode implements MatrixNode{
    private final int transitionFrom;
    private final int transitionTo;
    private MatrixNode prevMatrixNode;
    private MatrixNode nextMatrixNode;
    private int firstIndex;
    private int lastIndex;
    private final TreeMap<Integer, Transition> transitions = new TreeMap<>();
    private BodyList bodyList;
    private PrimeCenter primeCenter;

    @Override
    public void deactivatedMaintain() {
        System.out.println("deactivated body maintain for TransitionNode yet to be implemented");
    }

    @Override
    public void generateNewBodies(List<Body> successfullyAddedLocalBodies) {
        Set<Body> createdBodies = successfullyAddedLocalBodies.stream()
                .flatMap(previousBody -> transitions.values().stream()
                        .map(transition -> Body.builder().bodyNode(transition).parent(previousBody)
                                .value(transition.getValue().multiply(previousBody.getValue()))
                                .factor(transition.getFactor().multiply(previousBody.getFactor())).build()))
                .collect(Collectors.toSet());

        //System.out.println("created bodies: " + createdBodies);

        List<Body> successfullyAddedBodies = bodyList.mergeBodies(createdBodies);
        successfullyAddedBodies.forEach(body -> body.getBodyNode().getActiveBodies().add(body));
        successfullyAddedBodies.forEach(body -> {
            body.getBodyNode().getActiveBodies().add(body);
            if (body.getParent() != null) {
                body.getParent().getOffsprings().add(body);
                //System.out.println("afterOffspringset: " + body.getParent());
            }
        });
        //System.out.println("successfullyAddedBodies: " + successfullyAddedBodies);

        if (nextMatrixNode != null) {nextMatrixNode.generateNewBodies(successfullyAddedBodies);}
    }

    public void extensionCheck(Transition provedTransition) {
        if (transitions.get(transitions.lastKey()) == provedTransition) {
            // probably unnecessary check
            System.out.println("createNextTransition " + provedTransition.getValue());
            createNextTransition();
        }
    }

    private void createNextTransition() {
    }

}
