package com.hcn.event;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class TransitionNodeCreationActivity extends MatrixActivity {

    private final int transitionTo;

    public TransitionNodeCreationActivity(int transitionTo) {
        super();
        ActivityCenter.interruptHcnGeneration();
        this.transitionTo = transitionTo;
    }

    public TransitionNodeCreationActivity(int transitionTo, long startNanos, long finishNanos) {
        super(startNanos, finishNanos);
        this.transitionTo = transitionTo;
    }

    @Override
    public String getLabelName() { return "Transition Node Creation"; }

    @Override
    public String getGuiLabel() {
        return "duration: " + formatDuration() + " | transitionTo: " + transitionTo;
    }

    public void finish() {
        super.finish();
        ActivityCenter.getDbInsertService().submitStructural(this);
        ActivityCenter.resumeHcnGeneration();
    }
}
