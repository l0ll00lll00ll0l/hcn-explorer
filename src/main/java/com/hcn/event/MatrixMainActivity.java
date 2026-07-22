package com.hcn.event;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class MatrixMainActivity extends MainProcessActivity {

    private final int firstLapi;
    private int lastLapi;

    public MatrixMainActivity(int firstLapi) {
        super();
        this.firstLapi = firstLapi;
        ActivityCenter.setLastMatrixMainActivity(this);
    }

    public MatrixMainActivity(int firstLapi, long startNanos, long finishNanos) {
        super(startNanos, finishNanos);
        this.firstLapi = firstLapi;
    }

    public void finish() {
        super.finish();
        ActivityCenter.getDbInsertService().submitStructural(this);
    }

    @Override
    public String getLabelName() { return "Main Activity"; }

    @Override
    public String getGuiLabel() {
        return "duration: " + formatDuration() + " | lapis: " + firstLapi + " - " + lastLapi;
    }
}
