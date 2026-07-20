package com.hcn.db;

import com.hcn.event.BodyDeletionEvent;
import com.hcn.newCore.Interval;

public sealed interface DbTask permits DbTask.IntervalTask, DbTask.BodyDeletionTask {
    record IntervalTask(Interval interval) implements DbTask {}
    record BodyDeletionTask(BodyDeletionEvent event) implements DbTask {}
}
