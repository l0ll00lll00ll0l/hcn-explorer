package com.hcn.event;

import lombok.Getter;
import lombok.Setter;

@Getter
public class ApiNodeCreationActivity extends Activity {

    private int index;

    public ApiNodeCreationActivity(int index) {
        super();
        ActivityCenter.interruptHcnGeneration();
        this.index = index;
        ActivityCenter.getApiNodeCreationActivities().add(this);
    }

    @Override
    public String getLabelName() { return "API Node Creation"; }

    @Override
    public String getGuiLabel() {
        return "duration: " + formatDuration() + " | index: " + index;
    }

    public void finish() {
        super.finish();
        ActivityCenter.resumeHcnGeneration();
    }
}
