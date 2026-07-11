package com.hcn.db.event;

import lombok.Getter;

@Getter
public class IntervalProcessedEvent extends DbEvent {

    private final int lapi;

    public IntervalProcessedEvent(int lapi) {
        super();
        this.lapi = lapi;
    }
}
