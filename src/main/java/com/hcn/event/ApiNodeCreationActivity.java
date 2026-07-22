package com.hcn.event;

import lombok.Getter;
import lombok.Setter;

@Getter
public class ApiNodeCreationActivity extends MatrixActivity {

    private int index;

    public ApiNodeCreationActivity(int index) {
        super();
        ActivityCenter.interruptHcnGeneration();
        this.index = index;
    }

    public ApiNodeCreationActivity(int index, long startNanos, long finishNanos) {
        super(startNanos, finishNanos);
        this.index = index;
    }

    @Override
    public String getLabelName() { return "API Node Creation"; }

    @Override
    public String getGuiLabel() {
        return "duration: " + formatDuration() + " | index: " + index;
    }

    public void finish() {
        super.finish();
        ActivityCenter.getDbInsertService().submitStructural(this);
        ActivityCenter.resumeHcnGeneration();
    }
}
