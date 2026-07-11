package com.hcn.db.event;

import lombok.Getter;

@Getter
public class InsertBatchExecutionStartedEvent extends DbEvent {

    private final TableType table;
    private final int batchSize;

    public InsertBatchExecutionStartedEvent(TableType table, int batchSize) {
        super();
        this.table = table;
        this.batchSize = batchSize;
    }
}
