package com.hcn.event;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class HcnGenerationActivity extends MatrixActivity {

    private final int startLapi;
    private int endLapi;

    public HcnGenerationActivity(int startLapi) {
        super();
        this.startLapi = startLapi;
        ActivityCenter.setLastHcnGenerationActivity(this);
    }

    public HcnGenerationActivity(int startLapi, int endLapi, long startNanos, long finishNanos) {
        super(startNanos, finishNanos);
        this.startLapi = startLapi;
        this.endLapi = endLapi;
    }

    @Override
    public String getLabelName() { return "HCN Generation"; }

    @Override
    public String getGuiLabel() {
        return "duration: " + formatDuration() + " | lapis: " + startLapi + " - " + endLapi;
    }
}
