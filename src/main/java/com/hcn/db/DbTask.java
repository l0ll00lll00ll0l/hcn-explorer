package com.hcn.db;

import com.hcn.event.BodyDeletionEvent;
import com.hcn.newCore.interval.ActiveInterval;

public sealed interface DbTask permits DbTask.IntervalTask, DbTask.BodyDeletionTask {
    record IntervalTask(ActiveInterval activeInterval) implements DbTask {}
    record BodyDeletionTask(BodyDeletionEvent event) implements DbTask {}
}
