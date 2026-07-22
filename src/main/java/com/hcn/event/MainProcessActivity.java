package com.hcn.event;

public abstract class MainProcessActivity extends Activity {

    protected MainProcessActivity() {
        super();
    }

    protected MainProcessActivity(long startNanos, long finishNanos) {
        super(startNanos, finishNanos);
    }
}
