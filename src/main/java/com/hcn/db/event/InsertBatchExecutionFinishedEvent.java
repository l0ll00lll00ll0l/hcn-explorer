package com.hcn.db.event;

import lombok.Getter;

@Getter
public class InsertBatchExecutionFinishedEvent extends DbEvent {

    private final TableType table;
    private final int batchSize;

    public InsertBatchExecutionFinishedEvent(TableType table, int batchSize) {
        super();
        this.table = table;
        this.batchSize = batchSize;
    }
}
