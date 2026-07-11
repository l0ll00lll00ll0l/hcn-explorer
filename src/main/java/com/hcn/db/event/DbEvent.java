package com.hcn.db.event;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Getter;

@Getter
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = IntervalProcessedEvent.class,          name = "INTERVAL_PROCESSED"),
    @JsonSubTypes.Type(value = InsertBatchCreatedEvent.class,         name = "INSERT_BATCH_CREATED"),
    @JsonSubTypes.Type(value = InsertBatchExecutionStartedEvent.class, name = "INSERT_BATCH_EXECUTION_STARTED"),
    @JsonSubTypes.Type(value = InsertBatchExecutionFinishedEvent.class, name = "INSERT_BATCH_EXECUTION_FINISHED"),
    @JsonSubTypes.Type(value = FinalFlushEvent.class,                 name = "FINAL_FLUSH")
})
public abstract class DbEvent {

    public enum TableType { BODY, HCN, INTERVAL }

    private final long timestamp;
    private final String threadName;

    protected DbEvent() {
        this.timestamp = System.currentTimeMillis();
        this.threadName = Thread.currentThread().getName();
    }
}
