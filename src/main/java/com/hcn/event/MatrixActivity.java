package com.hcn.event;

public abstract class MatrixActivity extends Activity {

    protected MatrixActivity() {
        super(ActivityCenter.getMatrixNanos());
    }

    protected MatrixActivity(long startNanos, long finishNanos) {
        super(startNanos, finishNanos);
    }

    @Override
    public void finish() {
        super.finishAt(ActivityCenter.getMatrixNanos());
    }
}
