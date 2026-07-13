package com.hcn.event;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class HcnGenerationActivity extends Activity {

    private final int startLapi;
    private int endLapi;

    public HcnGenerationActivity(int startLapi) {
        super();
        this.startLapi = startLapi;
        ActivityCenter.getHcnGenerationActivities().add(this);
    }

    @Override
    public String getLabelName() { return "HCN Generation"; }

    @Override
    public String getGuiLabel() {
        return "duration: " + formatDuration() + " | lapis: " + startLapi + " - " + endLapi;
    }
}
