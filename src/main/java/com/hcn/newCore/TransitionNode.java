package com.hcn.newCore;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
public class TransitionNode extends MatrixNode{
    private final int transitionFrom;
    private final int transitionTo;
    private int firstIndex;
    private int lastIndex;
    private PrimeCenter primeCenter;

    @Override
    public void deactivatedMaintain() {
        System.out.println("deactivated body maintain for TransitionNode yet to be implemented");
    }

    @Override
    protected BodyNode provideNextBodyNode() {
        return null;
    }

    @Override
    protected int determineBodyNodeIdLowLimit() {
        return transitionFrom;
    }

    @Override
    public void extensionCheck() {
            System.out.println("createNextTransition ");
            createNextTransition();

    }

    private void createNextTransition() {
    }

}
