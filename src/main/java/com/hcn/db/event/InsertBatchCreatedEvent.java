package com.hcn.db.event;

import lombok.Getter;

@Getter
public class InsertBatchCreatedEvent extends DbEvent {

    private final TableType table;
    private final int batchSize;

    public InsertBatchCreatedEvent(TableType table, int batchSize) {
        super();
        this.table = table;
        this.batchSize = batchSize;
    }
}
