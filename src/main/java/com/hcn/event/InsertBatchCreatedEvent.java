package com.hcn.event;

import lombok.Getter;

@Getter
public class InsertBatchCreatedEvent extends Event {

    private final SqlTable table;
    private final int batchSize;

    public InsertBatchCreatedEvent(SqlTable table, int batchSize) {
        super();
        this.table = table;
        this.batchSize = batchSize;
        ActivityCenter.getInsertBatchCreatedEvents().add(this);
    }
}
