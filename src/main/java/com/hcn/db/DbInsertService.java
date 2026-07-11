package com.hcn.db;

import com.hcn.db.event.*;
import com.hcn.newCore.Body;
import com.hcn.newCore.Hcn;
import com.hcn.newCore.Interval;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

@Service
@Slf4j
public class DbInsertService {

    private static final int THRESHOLD = 500;

    private record InsertBatch(String sql, int count) {}

    private final LinkedBlockingQueue<Interval> queue = new LinkedBlockingQueue<>();
    private final LinkedBlockingQueue<InsertBatch> bodyQueue     = new LinkedBlockingQueue<>();
    private final LinkedBlockingQueue<InsertBatch> hcnQueue      = new LinkedBlockingQueue<>();
    private final LinkedBlockingQueue<InsertBatch> intervalQueue = new LinkedBlockingQueue<>();

    private volatile boolean bodyRunning     = false;
    private volatile boolean hcnRunning      = false;
    private volatile boolean intervalRunning = false;
    private volatile int lastProcessedLapi = -1;

    private final List<DbEvent> events = new ArrayList<>();

    private void record(DbEvent event) { events.add(event); }

    public List<DbEvent> getEvents() { return events; }

    private StringBuilder bodyBuffer;
    private StringBuilder intervalBuffer;
    private StringBuilder hcnBuffer;
    private int bodyCount = 0;
    private int intervalCount = 0;
    private int hcnCount = 0;

    private static final String BODY_INSERT     = "INSERT INTO body (id, head, tail) VALUES ";
    private static final String INTERVAL_INSERT = "INSERT INTO interval (lapi, value_mantissa, value_exponent, factor_mantissa, factor_exponent, first_hcn, size, reference_interval) VALUES ";
    private static final String HCN_INSERT      = "INSERT INTO hcn (id, body, lapi) VALUES ";

    private int hcnIdCounter = 0;
    private int bodyIdCounter = 0;

    public int getHcnIdCounter() { return hcnIdCounter; }
    public int getBodyIdCounter() { return bodyIdCounter; }
    public void setHcnIdCounter(int v) { hcnIdCounter = v; }
    public void setBodyIdCounter(int v) { bodyIdCounter = v; }

    private JdbcTemplate dbTemplate;

    @Autowired
    private DatabaseService databaseService;

    public void setTargetDb(String dbName) {
        dbTemplate = databaseService.createTemplateForDb(dbName);
    }

    @PostConstruct
    public void start() {
        resetBuffers();
        new Thread(this::consumeIntervals, "db-interval-consumer").start();
        new Thread(() -> consumeInserts(bodyQueue,     () -> bodyRunning     = true, () -> bodyRunning     = false, DbEvent.TableType.BODY),     "db-body-insert").start();
        new Thread(() -> consumeInserts(hcnQueue,      () -> hcnRunning      = true, () -> hcnRunning      = false, DbEvent.TableType.HCN),      "db-hcn-insert").start();
        new Thread(() -> consumeInserts(intervalQueue, () -> intervalRunning = true, () -> intervalRunning = false, DbEvent.TableType.INTERVAL), "db-interval-insert").start();
    }

    private void resetBuffers() {
        bodyBuffer     = new StringBuilder(BODY_INSERT);
        intervalBuffer = new StringBuilder(INTERVAL_INSERT);
        hcnBuffer      = new StringBuilder(HCN_INSERT);
        bodyCount = 0;
        intervalCount = 0;
        hcnCount = 0;
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
        queue.add(interval);
    }

    private void consumeIntervals() {
        while (true) {
            try {
                Interval interval = queue.take();
                process(interval);
                record(new IntervalProcessedEvent(interval.getLapi()));
                if (bodyCount >= THRESHOLD)     flushBody();
                if (hcnCount >= THRESHOLD)      flushHcn();
                if (intervalCount >= THRESHOLD) flushInterval();
                synchronized (this) {
                    lastProcessedLapi = interval.getLapi();
                    notifyAll();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    private void consumeInserts(LinkedBlockingQueue<InsertBatch> q, Runnable onStart, Runnable onEnd, DbEvent.TableType table) {
        while (true) {
            try {
                InsertBatch batch = q.take();
                onStart.run();
                record(new InsertBatchExecutionStartedEvent(table, batch.count()));
                dbTemplate.execute(batch.sql());
                onEnd.run();
                record(new InsertBatchExecutionFinishedEvent(table, batch.count()));
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

    private void flushBody() {
        if (bodyCount > 0) { record(new InsertBatchCreatedEvent(DbEvent.TableType.BODY, bodyCount)); bodyQueue.add(new InsertBatch(bodyBuffer.toString(), bodyCount)); resetBodyBuffer(); }
    }

    private void flushHcn() {
        if (hcnCount > 0) { record(new InsertBatchCreatedEvent(DbEvent.TableType.HCN, hcnCount)); hcnQueue.add(new InsertBatch(hcnBuffer.toString(), hcnCount)); resetHcnBuffer(); }
    }

    private void flushInterval() {
        if (intervalCount > 0) { record(new InsertBatchCreatedEvent(DbEvent.TableType.INTERVAL, intervalCount)); intervalQueue.add(new InsertBatch(intervalBuffer.toString(), intervalCount)); resetIntervalBuffer(); }
    }

    public void finalFlush(int lapiId) throws InterruptedException {
        synchronized (this) {
            while (lastProcessedLapi < lapiId) wait();
        }
        record(new FinalFlushEvent(true));
        flushBody();
        flushHcn();
        flushInterval();
        while (!bodyQueue.isEmpty() || bodyRunning ||
               !hcnQueue.isEmpty() || hcnRunning ||
               !intervalQueue.isEmpty() || intervalRunning) {
            Thread.sleep(10);
        }
        record(new FinalFlushEvent(false));
    }
}
