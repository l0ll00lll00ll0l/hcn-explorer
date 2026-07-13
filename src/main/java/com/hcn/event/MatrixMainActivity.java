package com.hcn.event;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class MatrixMainActivity extends Activity {

    private final int firstLapi;
    private int lastLapi;

    public MatrixMainActivity(int firstLapi) {
        super();
        this.firstLapi = firstLapi;
        ActivityCenter.getMatrixMainActivities().add(this);
    }

    @Override
    public String getLabelName() { return "Main Activity"; }

    @Override
    public String getGuiLabel() {
        return "duration: " + formatDuration() + " | lapis: " + firstLapi + " - " + lastLapi;
    }
}
