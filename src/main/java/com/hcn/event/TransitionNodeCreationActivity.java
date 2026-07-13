package com.hcn.event;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class TransitionNodeCreationActivity extends Activity {

    private final int transitionTo;

    public TransitionNodeCreationActivity(int transitionTo) {
        super();
        ActivityCenter.interruptHcnGeneration();
        this.transitionTo = transitionTo;
        ActivityCenter.getTransitionNodeCreationActivities().add(this);
    }

    @Override
    public String getLabelName() { return "Transition Node Creation"; }

    @Override
    public String getGuiLabel() {
        return "duration: " + formatDuration() + " | transitionTo: " + transitionTo;
    }

    public void finish() {
        super.finish();
        ActivityCenter.resumeHcnGeneration();
    }
}
