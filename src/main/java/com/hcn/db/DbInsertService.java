package com.hcn.db;

import com.hcn.event.*;
import com.hcn.newCore.Body;
import com.hcn.newCore.Hcn;
import com.hcn.newCore.Interval;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Slf4j
public class DbInsertService {

    private static final int THRESHOLD = 500;

    private record InsertBatch(String sql, int count) {}

    private final LinkedBlockingQueue<DbTask> mainQueue = new LinkedBlockingQueue<>();
    private final LinkedBlockingQueue<InsertBatch> bodyQueue          = new LinkedBlockingQueue<>();
    private final LinkedBlockingQueue<InsertBatch> hcnQueue           = new LinkedBlockingQueue<>();
    private final LinkedBlockingQueue<InsertBatch> intervalQueue      = new LinkedBlockingQueue<>();
    private final LinkedBlockingQueue<InsertBatch> structuralQueue    = new LinkedBlockingQueue<>();
    private final LinkedBlockingQueue<InsertBatch> extensionQueue     = new LinkedBlockingQueue<>();
    private final LinkedBlockingQueue<InsertBatch> hcnGenerationQueue = new LinkedBlockingQueue<>();
    private final LinkedBlockingQueue<InsertBatch> sqlInsertActQueue       = new LinkedBlockingQueue<>();
    private final LinkedBlockingQueue<InsertBatch> bodyDeletionEventQueue  = new LinkedBlockingQueue<>();
    private final LinkedBlockingQueue<InsertBatch> bodyLifecycleQueue      = new LinkedBlockingQueue<>();

    private volatile boolean bodyRunning          = false;
    private volatile boolean hcnRunning           = false;
    private volatile boolean intervalRunning      = false;
    private volatile boolean structuralRunning    = false;
    private volatile boolean extensionRunning     = false;
    private volatile boolean hcnGenerationRunning = false;
    private volatile boolean sqlInsertActRunning       = false;
    private volatile boolean bodyDeletionEventRunning  = false;
    private volatile boolean bodyLifecycleRunning       = false;
    private volatile int lastProcessedLapi = -1;

    private StringBuilder bodyBuffer;
    private StringBuilder intervalBuffer;
    private StringBuilder hcnBuffer;
    private StringBuilder structuralBuffer;
    private StringBuilder extensionBuffer;
    private StringBuilder hcnGenerationBuffer;
    private StringBuilder sqlInsertActBuffer;
    private StringBuilder bodyDeletionEventBuffer;
    private StringBuilder bodyLifecycleBuffer;
    private int bodyCount = 0;
    private int intervalCount = 0;
    private int hcnCount = 0;
    private int structuralCount    = 0;
    private int extensionCount     = 0;
    private int hcnGenerationCount = 0;
    private int sqlInsertActCount       = 0;
    private int bodyDeletionEventCount  = 0;
    private int bodyLifecycleCount      = 0;

    private static final java.util.Set<SqlTable> ACTIVITY_TABLES = java.util.Set.of(
            SqlTable.STRUCTURAL_ACTIVITY, SqlTable.EXTENSION_ACTIVITY, SqlTable.HCN_GENERATION_ACTIVITY, SqlTable.SQL_INSERT_ACTIVITY, SqlTable.BODY_DELETION_EVENT);

    private static final String BODY_INSERT           = "INSERT INTO body (id, head, tail) VALUES ";
    private static final String INTERVAL_INSERT       = "INSERT INTO interval (lapi, value_mantissa, value_exponent, factor_mantissa, factor_exponent, first_hcn, size, reference_interval) VALUES ";
    private static final String HCN_INSERT            = "INSERT INTO hcn (id, body, lapi) VALUES ";
    private static final String STRUCTURAL_INSERT     = "INSERT INTO structural_activity (id, type, start_nanos, finish_nanos, int_1, int_2) VALUES ";
    private static final String EXTENSION_INSERT      = "INSERT INTO extension_activity (id, start_nanos, finish_nanos, index, power, created_active_body_count, deactivated_body_count) VALUES ";
    private static final String HCN_GENERATION_INSERT = "INSERT INTO hcn_generation_activity (id, start_nanos, finish_nanos, start_lapi, end_lapi) VALUES ";
    private static final String SQL_INSERT_ACT_INSERT      = "INSERT INTO sql_insert_activity (id, start_nanos, finish_nanos, row_count, table_name) VALUES ";
    private static final String BODY_DELETION_EVENT_INSERT = "INSERT INTO body_deletion_event (id, nanos, deleted_body_count, non_proved_body_count) VALUES ";
    private static final String BODY_LIFECYCLE_INSERT      = "INSERT INTO body_lifecycle (body_id, first_hcn_lapi, first_superior_hcn_lapi, first_dominated_hcn_lapi) VALUES ";

    private int hcnIdCounter = 0;
    private int bodyIdCounter = 0;
    private int structuralIdCounter    = 0;
    private int extensionIdCounter     = 0;
    private int hcnGenerationIdCounter     = 0;
    private int bodyDeletionEventIdCounter = 0;
    private final AtomicInteger sqlInsertActIdCounter = new AtomicInteger(0);

    public void setHcnIdCounter(int v) { hcnIdCounter = v; }
    public void setBodyIdCounter(int v) { bodyIdCounter = v; }
    public void setStructuralIdCounter(int v) { structuralIdCounter = v; }
    public void setExtensionIdCounter(int v) { extensionIdCounter = v; }
    public void setHcnGenerationIdCounter(int v) { hcnGenerationIdCounter = v; }
    public void setSqlInsertActIdCounter(int v) { sqlInsertActIdCounter.set(v); }
    public void setBodyDeletionEventIdCounter(int v) { bodyDeletionEventIdCounter = v; }

    private JdbcTemplate dbTemplate;

    @Autowired
    private DatabaseService databaseService;

    public void setTargetDb(String dbName) {
        dbTemplate = databaseService.createTemplateForDb(dbName);
    }

    @PostConstruct
    public void start() {
        resetBuffers();
        new Thread(this::consumeTasks, "db-main-consumer").start();
        new Thread(() -> consumeInserts(bodyQueue,          () -> bodyRunning          = true, () -> bodyRunning          = false, SqlTable.BODY),     "db-body-insert").start();
        new Thread(() -> consumeInserts(hcnQueue,           () -> hcnRunning           = true, () -> hcnRunning           = false, SqlTable.HCN),      "db-hcn-insert").start();
        new Thread(() -> consumeInserts(intervalQueue,      () -> intervalRunning      = true, () -> intervalRunning      = false, SqlTable.INTERVAL), "db-interval-insert").start();
        new Thread(() -> consumeInserts(structuralQueue,    () -> structuralRunning    = true, () -> structuralRunning    = false, SqlTable.STRUCTURAL_ACTIVITY),    "db-structural-insert").start();
        new Thread(() -> consumeInserts(extensionQueue,     () -> extensionRunning     = true, () -> extensionRunning     = false, SqlTable.EXTENSION_ACTIVITY),     "db-extension-insert").start();
        new Thread(() -> consumeInserts(hcnGenerationQueue, () -> hcnGenerationRunning = true, () -> hcnGenerationRunning = false, SqlTable.HCN_GENERATION_ACTIVITY), "db-hcngen-insert").start();
        new Thread(() -> consumeInserts(sqlInsertActQueue,      () -> sqlInsertActRunning      = true, () -> sqlInsertActRunning      = false, SqlTable.SQL_INSERT_ACTIVITY),  "db-sqlact-insert").start();
        new Thread(() -> consumeInserts(bodyDeletionEventQueue,  () -> bodyDeletionEventRunning = true, () -> bodyDeletionEventRunning = false, SqlTable.BODY_DELETION_EVENT), "db-bodydel-insert").start();
        new Thread(() -> consumeInserts(bodyLifecycleQueue,      () -> bodyLifecycleRunning     = true, () -> bodyLifecycleRunning     = false, SqlTable.BODY_LIFECYCLE),     "db-lifecycle-insert").start();
    }

    private void resetBuffers() {
        bodyBuffer          = new StringBuilder(BODY_INSERT);
        intervalBuffer      = new StringBuilder(INTERVAL_INSERT);
        hcnBuffer           = new StringBuilder(HCN_INSERT);
        structuralBuffer    = new StringBuilder(STRUCTURAL_INSERT);
        extensionBuffer     = new StringBuilder(EXTENSION_INSERT);
        hcnGenerationBuffer = new StringBuilder(HCN_GENERATION_INSERT);
        sqlInsertActBuffer  = new StringBuilder(SQL_INSERT_ACT_INSERT);
        bodyDeletionEventBuffer = new StringBuilder(BODY_DELETION_EVENT_INSERT);
        bodyLifecycleBuffer     = new StringBuilder(BODY_LIFECYCLE_INSERT);
        bodyCount = 0; intervalCount = 0; hcnCount = 0;
        structuralCount = 0; extensionCount = 0; hcnGenerationCount = 0; sqlInsertActCount = 0; bodyDeletionEventCount = 0; bodyLifecycleCount = 0;
    }

    private void resetBodyBuffer() {
        bodyBuffer = new StringBuilder(BODY_INSERT);
        bodyCount = 0;
    }

    private void resetHcnBuffer() {
        hcnBuffer = new StringBuilder(HCN_INSERT);
        hcnCount = 0;
    }

    private void resetIntervalBuffer() {
        intervalBuffer = new StringBuilder(INTERVAL_INSERT);
        intervalCount = 0;
    }

    public void submit(Interval interval) {
        mainQueue.add(new DbTask.IntervalTask(interval));
    }

    public void submitBodyDeletionEvent(BodyDeletionEvent event) {
        mainQueue.add(new DbTask.BodyDeletionTask(event));
    }

    private void consumeTasks() {
        while (true) {
            try {
                DbTask task = mainQueue.take();
                if (task instanceof DbTask.IntervalTask t) {
                    Interval interval = t.interval();
                    process(interval);
                    if (bodyCount >= THRESHOLD)     flushBody();
                    if (hcnCount >= THRESHOLD)      flushHcn();
                    if (intervalCount >= THRESHOLD) flushInterval();
                    synchronized (this) {
                        lastProcessedLapi = interval.getLapi();
                        notifyAll();
                    }
                } else if (task instanceof DbTask.BodyDeletionTask t) {
                    processBodyDeletionEvent(t.event());
                    if (bodyCount >= THRESHOLD) flushBody();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private void consumeInserts(LinkedBlockingQueue<InsertBatch> q, Runnable onStart, Runnable onEnd, SqlTable table) {
        while (true) {
            try {
                InsertBatch batch = q.take();
                onStart.run();
                if (table != null) {
                    if (ACTIVITY_TABLES.contains(table)) {
                        long startNanos = ActivityCenter.getNanos();
                        int stubId = sqlInsertActIdCounter.getAndIncrement();
                        dbTemplate.execute("INSERT INTO sql_insert_activity (id, start_nanos, row_count, table_name) VALUES (" + stubId + "," + startNanos + "," + batch.count() + ",'" + table + "')");
                        dbTemplate.execute(batch.sql());
                        dbTemplate.execute("UPDATE sql_insert_activity SET finish_nanos = " + ActivityCenter.getNanos() + " WHERE id = " + stubId);
                    } else {
                        SqlInsertActivity activity = new SqlInsertActivity(table, batch.count());
                        dbTemplate.execute(batch.sql());
                        activity.finish();
                    }
                } else {
                    dbTemplate.execute(batch.sql());
                }
                onEnd.run();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private void process(Interval interval) {
        int firstHcnId = hcnIdCounter + 1;
        if (!interval.isReferenced()) {
            interval.getHcnList().forEach(hcn -> {
                if (hcn.getBody().getDbId() == null) appendBody(hcn.getBody());
                appendHcn(hcn);
            });
        } else {
            hcnIdCounter += interval.getHcnList().size();
        }
        appendInterval(interval, firstHcnId);
    }

    private void appendBody(Body body) {
        if (bodyCount > 0) bodyBuffer.append(",");
        body.setDbId(bodyIdCounter++);
        bodyBuffer.append("(").append(body.getDbId())
                .append(",'").append(toArrayLiteral(body.getDbBody().getHead()))
                .append("','").append(toArrayLiteral(body.getDbBody().getTail()))
                .append("')");
        bodyCount++;
    }

    private void appendHcn(Hcn hcn) {
        if (hcnCount > 0) hcnBuffer.append(",");
        hcnBuffer.append("(").append(++hcnIdCounter)
                .append(",").append(hcn.getBody().getDbId())
                .append(",").append(hcn.getLapi())
                .append(")");
        hcnCount++;
    }

    private void appendInterval(Interval interval, int firstHcnId) {
        if (intervalCount > 0) intervalBuffer.append(",");
        intervalBuffer.append("(").append(interval.getLapi())
                .append(",").append(interval.getValue().getMantissa())
                .append(",").append(interval.getValue().getExponent())
                .append(",").append(interval.getFactor().getMantissa())
                .append(",").append(interval.getFactor().getExponent())
                .append(",").append(firstHcnId)
                .append(",").append(interval.getHcnList().size())
                .append(",").append(interval.getReferenceInterval().getLapi())
                .append(")");
        intervalCount++;
    }

    private String toArrayLiteral(int[] arr) {
        StringBuilder sb = new StringBuilder("{");
        for (int i = 0; i < arr.length; i++) {
            if (i > 0) sb.append(",");
            sb.append(arr[i]);
        }
        return sb.append("}").toString();
    }

    public void submitStructural(MatrixMainActivity a) {
        if (structuralCount > 0) structuralBuffer.append(",");
        structuralBuffer.append("(").append(structuralIdCounter++).append(",'MATRIX_MAIN',").append(a.getStartNanos()).append(",").append(a.getFinishNanos()).append(",").append(a.getFirstLapi()).append(",").append(a.getLastLapi()).append(")");
        if (++structuralCount >= THRESHOLD) flushStructural();
    }

    public void submitStructural(ApiNodeCreationActivity a) {
        if (structuralCount > 0) structuralBuffer.append(",");
        structuralBuffer.append("(").append(structuralIdCounter++).append(",'API_NODE',").append(a.getStartNanos()).append(",").append(a.getFinishNanos()).append(",").append(a.getIndex()).append(",NULL)");
        if (++structuralCount >= THRESHOLD) flushStructural();
    }

    public void submitStructural(TransitionNodeCreationActivity a) {
        if (structuralCount > 0) structuralBuffer.append(",");
        structuralBuffer.append("(").append(structuralIdCounter++).append(",'TRANSITION_NODE',").append(a.getStartNanos()).append(",").append(a.getFinishNanos()).append(",").append(a.getTransitionTo()).append(",NULL)");
        if (++structuralCount >= THRESHOLD) flushStructural();
    }

    public void submitExtension(MatrixExtensionActivity a) {
        if (extensionCount > 0) extensionBuffer.append(",");
        extensionBuffer.append("(").append(extensionIdCounter++).append(",").append(a.getStartNanos()).append(",").append(a.getFinishNanos()).append(",").append(a.getIndex()).append(",").append(a.getPower()).append(",").append(a.getCreatedActiveBodyCount()).append(",").append(a.getDeactivatedBodyCount()).append(")");
        if (++extensionCount >= THRESHOLD) flushExtension();
    }

    public void submitHcnGeneration(HcnGenerationActivity a) {
        if (hcnGenerationCount > 0) hcnGenerationBuffer.append(",");
        hcnGenerationBuffer.append("(").append(hcnGenerationIdCounter++).append(",").append(a.getStartNanos()).append(",").append(a.getFinishNanos()).append(",").append(a.getStartLapi()).append(",").append(a.getEndLapi()).append(")");
        if (++hcnGenerationCount >= THRESHOLD) flushHcnGeneration();
    }

    public synchronized void submitSqlInsertActivity(SqlInsertActivity a) {
        if (sqlInsertActCount > 0) sqlInsertActBuffer.append(",");
        sqlInsertActBuffer.append("(").append(sqlInsertActIdCounter.getAndIncrement()).append(",").append(a.getStartNanos()).append(",").append(a.getFinishNanos()).append(",").append(a.getRowCount()).append(",'").append(a.getTable()).append("')");
        if (++sqlInsertActCount >= THRESHOLD) flushSqlInsertActivity();
    }

    private void flushStructural() {
        if (structuralCount > 0) { structuralQueue.add(new InsertBatch(structuralBuffer.toString(), structuralCount)); structuralBuffer = new StringBuilder(STRUCTURAL_INSERT); structuralCount = 0; }
    }

    private void flushExtension() {
        if (extensionCount > 0) { extensionQueue.add(new InsertBatch(extensionBuffer.toString(), extensionCount)); extensionBuffer = new StringBuilder(EXTENSION_INSERT); extensionCount = 0; }
    }

    private void flushHcnGeneration() {
        if (hcnGenerationCount > 0) { hcnGenerationQueue.add(new InsertBatch(hcnGenerationBuffer.toString(), hcnGenerationCount)); hcnGenerationBuffer = new StringBuilder(HCN_GENERATION_INSERT); hcnGenerationCount = 0; }
    }

    private void flushSqlInsertActivity() {
        if (sqlInsertActCount > 0) { sqlInsertActQueue.add(new InsertBatch(sqlInsertActBuffer.toString(), sqlInsertActCount)); sqlInsertActBuffer = new StringBuilder(SQL_INSERT_ACT_INSERT); sqlInsertActCount = 0; }
    }

    private void processBodyDeletionEvent(BodyDeletionEvent e) {
        int nonProvedCount = 0;
        for (Body body : e.getDeletedBodies()) {
            if (body.getDbId() == null) {
                appendBody(body);
                nonProvedCount++;
            }
        }
        for (Body body : e.getDeletedBodies()) {
            appendBodyLifecycle(body);
        }
        if (bodyDeletionEventCount > 0) bodyDeletionEventBuffer.append(",");
        bodyDeletionEventBuffer.append("(").append(bodyDeletionEventIdCounter++).append(",").append(e.getNanos()).append(",").append(e.getDeletedBodies().size()).append(",").append(nonProvedCount).append(")");
        if (++bodyDeletionEventCount >= THRESHOLD) flushBodyDeletionEvent();
        if (bodyLifecycleCount >= THRESHOLD) flushBodyLifecycle();
    }

    private void flushBodyDeletionEvent() {
        if (bodyDeletionEventCount > 0) { bodyDeletionEventQueue.add(new InsertBatch(bodyDeletionEventBuffer.toString(), bodyDeletionEventCount)); bodyDeletionEventBuffer = new StringBuilder(BODY_DELETION_EVENT_INSERT); bodyDeletionEventCount = 0; }
    }

    private void appendBodyLifecycle(Body body) {
        if (bodyLifecycleCount > 0) bodyLifecycleBuffer.append(",");
        Integer firstHcnLapi           = body.getFirstHcn()           != null ? body.getFirstHcn().getLapi()           : null;
        Integer firstSuperiorHcnLapi   = body.getFirstSuperiorHcn()   != null ? body.getFirstSuperiorHcn().getLapi()   : null;
        Integer firstDominatedHcnLapi  = body.getFirstDominatedHcn()  != null ? body.getFirstDominatedHcn().getLapi()  : null;
        bodyLifecycleBuffer.append("(").append(body.getDbId())
                .append(",").append(firstHcnLapi          != null ? firstHcnLapi          : "NULL")
                .append(",").append(firstSuperiorHcnLapi  != null ? firstSuperiorHcnLapi  : "NULL")
                .append(",").append(firstDominatedHcnLapi != null ? firstDominatedHcnLapi : "NULL")
                .append(")");
        bodyLifecycleCount++;
    }

    private void flushBodyLifecycle() {
        if (bodyLifecycleCount > 0) { bodyLifecycleQueue.add(new InsertBatch(bodyLifecycleBuffer.toString(), bodyLifecycleCount)); bodyLifecycleBuffer = new StringBuilder(BODY_LIFECYCLE_INSERT); bodyLifecycleCount = 0; }
    }

    private void flushBody() {
        if (bodyCount > 0) { bodyQueue.add(new InsertBatch(bodyBuffer.toString(), bodyCount)); resetBodyBuffer(); }
    }

    private void flushHcn() {
        if (hcnCount > 0) { hcnQueue.add(new InsertBatch(hcnBuffer.toString(), hcnCount)); resetHcnBuffer(); }
    }

    private void flushInterval() {
        if (intervalCount > 0) { intervalQueue.add(new InsertBatch(intervalBuffer.toString(), intervalCount)); resetIntervalBuffer(); }
    }

    private static final int PAUSE_BATCHES  = 10;
    private static final int RESUME_BATCHES = 3;

    public boolean isQueueAbovePauseLimit() {
        return bodyQueue.size() + hcnQueue.size() + intervalQueue.size() > PAUSE_BATCHES;
    }

    public boolean isQueueBelowResumeLimit() {
        return bodyQueue.size() + hcnQueue.size() + intervalQueue.size() <= RESUME_BATCHES;
    }

    public void finalFlush(int lapiId) throws InterruptedException {
        synchronized (this) {
            while (lastProcessedLapi < lapiId) wait();
        }
        flushBody(); flushHcn(); flushInterval();
        flushStructural(); flushExtension(); flushHcnGeneration(); flushSqlInsertActivity(); flushBodyDeletionEvent(); flushBodyLifecycle();
        while (!bodyQueue.isEmpty()          || bodyRunning          ||
               !hcnQueue.isEmpty()           || hcnRunning           ||
               !intervalQueue.isEmpty()      || intervalRunning      ||
               !structuralQueue.isEmpty()    || structuralRunning    ||
               !extensionQueue.isEmpty()     || extensionRunning     ||
               !hcnGenerationQueue.isEmpty() || hcnGenerationRunning ||
               !sqlInsertActQueue.isEmpty()  || sqlInsertActRunning ||
               !bodyDeletionEventQueue.isEmpty() || bodyDeletionEventRunning ||
               !bodyLifecycleQueue.isEmpty()      || bodyLifecycleRunning) {
            Thread.sleep(10);
        }
        flushSqlInsertActivity();
        while (!sqlInsertActQueue.isEmpty() || sqlInsertActRunning) {
            Thread.sleep(10);
        }
    }
}
