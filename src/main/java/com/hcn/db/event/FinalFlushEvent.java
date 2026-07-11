package com.hcn.db.event;

import lombok.Getter;

@Getter
public class FinalFlushEvent extends DbEvent {

    private final boolean started;

    public FinalFlushEvent(boolean started) {
        super();
        this.started = started;
    }
}
