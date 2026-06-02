package com.hcn.core.basicdata;

import com.hcn.core.*;

import java.util.ArrayList;
import java.util.List;

public class BasicDataMatrix extends Matrix {

    private Interval referenceInterval = null;
    private List<Interval> intervals = new ArrayList<>();
    private long dbNanos = 0;
    private com.hcn.db.DatabaseService databaseService;

    private static final int BASIC_DATA_TRIGGER = 100;

    public Interval getReferenceInterval() { return referenceInterval; }
    public List<Interval> getIntervals() { return intervals; }
    public long getDbMs() { return dbNanos / 1_000_000; }
    public long getDbNanos() { return dbNanos; }
    public void setDatabaseService(com.hcn.db.DatabaseService databaseService) { this.databaseService = databaseService; }

    @Override
    public boolean isBasicData() { return true; }

    @Override
    public void proveLapi(int count) {
        super.proveLapi(count);
        flushAllIntervals();
    }

    @Override
    public void proveNextLapi() {
        super.proveNextLapi();
        if (intervals.size() > BASIC_DATA_TRIGGER) {
            flushAllIntervals();
        }
    }

    public void flushAllIntervals() {
        if (!intervals.isEmpty()) {
            long tDb = System.nanoTime();
            // TODO: save intervals to DB
            dbNanos += System.nanoTime() - tDb;
            intervals.clear();
        }
    }

    @Override
    public void initialize() {
        super.initialize();
        Interval newInterval = new Interval(provedHcns, null);
        referenceInterval = newInterval.getReferenceInterval();
        intervals.add(newInterval);
        provedHcns.clear();
    }

    @Override
    protected void maintainProvedHcns() {
        highestLapiGroup.getHcnList().remove(0);
        lowestProvedLapiWithinInterval = Integer.MAX_VALUE;
        highestLapiGroup.getHcnList().forEach(hcn -> {
            if (hcn.getLastActivePrime() < lowestProvedLapiWithinInterval) {
                lowestProvedLapiWithinInterval = hcn.getLastActivePrime();
            }
            provedHcns.add(hcn);
            provedCount++;
        });

        Interval newInterval = new Interval(provedHcns, referenceInterval);
        referenceInterval = newInterval.getReferenceInterval();
        intervals.add(newInterval);
        provedHcns.clear();
    }

    public static BasicDataMatrix fromLoad(ActivePrimeIndex lastApi, LastActivePrimeIndexGroup lowestLapi,
                                            LastActivePrimeIndexGroup highestLapi, LastActivePrimeIndexGroup nextLapi,
                                            ScientificNumber provedLimit,
                                            int provedCount, int lastProvedPrimeIndex, int lowestProvedLapi) {
        BasicDataMatrix m = new BasicDataMatrix();
        m.lastActivePrimeIndex = lastApi;
        m.lowestLapiGroup = lowestLapi;
        m.highestLapiGroup = highestLapi;
        m.nextLapiGroup = nextLapi;
        m.provedLimit = provedLimit;
        m.provedCount = provedCount;
        m.lastProvedPrimeIndex = lastProvedPrimeIndex;
        m.lowestProvedLapiWithinInterval = lowestProvedLapi;
        return m;
    }
}
