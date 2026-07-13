package com.hcn.event;

import lombok.Getter;

@Getter
public abstract class Event {

    private final long nanos;

    protected Event() {
        this.nanos = ActivityCenter.getNanos();
    }
}
