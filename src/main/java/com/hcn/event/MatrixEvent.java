package com.hcn.event;

public abstract class MatrixEvent extends Event {

    protected MatrixEvent() {
        super(ActivityCenter.getMatrixNanos());
    }
}
