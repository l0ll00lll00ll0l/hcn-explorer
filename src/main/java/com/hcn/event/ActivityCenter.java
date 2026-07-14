package com.hcn.event;

import com.hcn.db.DbInsertService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class ActivityCenter {

    private static long nanoReference;

    private static MatrixMainActivity lastMatrixMainActivity;
    private static MatrixExtensionActivity lastMatrixExtensionActivity;
    private static HcnGenerationActivity lastHcnGenerationActivity;

    private static volatile int currentLapi = 1;
    private static boolean dbMode = false;
    private static DbInsertService dbInsertService;

    @Autowired
    public void setDbInsertService(DbInsertService s) { dbInsertService = s; }

    public static DbInsertService getDbInsertService() { return dbInsertService; }
    public static boolean isDbMode() { return dbMode; }

    // Progress tracking
    private static volatile boolean proving = false;
    private static volatile int proveTarget = 0;
    private static volatile int proveProgress = 0;

    public static void initialize(int firstLapiParam, boolean dbModeParam) {
        dbMode = dbModeParam;
        if (!dbMode) return;
        nanoReference = System.nanoTime();
        new MatrixMainActivity(firstLapiParam);
        new HcnGenerationActivity(firstLapiParam);
    }

    public static void resume(int fromLapi) {
        if (!dbMode) return;
        new MatrixMainActivity(fromLapi);
        new HcnGenerationActivity(fromLapi);
    }

    public static void setLastMatrixMainActivity(MatrixMainActivity a)           { lastMatrixMainActivity = a; }
    public static void setLastMatrixExtensionActivity(MatrixExtensionActivity a) { lastMatrixExtensionActivity = a; }
    public static void setLastHcnGenerationActivity(HcnGenerationActivity a)     { lastHcnGenerationActivity = a; }
    public static MatrixExtensionActivity getLastMatrixExtensionActivity()       { return lastMatrixExtensionActivity; }

    public static void interruptHcnGeneration(int lapi) {
        if (!dbMode) return;
        lastHcnGenerationActivity.setEndLapi(lapi);
        lastHcnGenerationActivity.finish();
        if (lastHcnGenerationActivity.getStartLapi() != lastHcnGenerationActivity.getEndLapi()) {
            dbInsertService.submitHcnGeneration(lastHcnGenerationActivity);
        }
    }

    public static void interruptHcnGeneration() { interruptHcnGeneration(currentLapi); }

    public static void resumeHcnGeneration(int lapi) {
        if (!dbMode) return;
        new HcnGenerationActivity(lapi);
    }

    public static void resumeHcnGeneration() { resumeHcnGeneration(currentLapi); }

    public static void addExtensionActivity(MatrixExtensionActivity a) {
        if (!dbMode) return;
        setLastMatrixExtensionActivity(a);
    }

    public static boolean isProving() { return proving; }
    public static int getProveTarget() { return proveTarget; }
    public static int getProveProgress() { return proveProgress; }
    public static void setProving(boolean v) { proving = v; }
    public static void setCurrentLapi(int lapi) { currentLapi = lapi; }
    public static void setProveTarget(int v) { proveTarget = v; }
    public static void setProveProgress(int v) { proveProgress = v; }

    private static long totalNanos = 0;
    public static long getTotalNanos() { return totalNanos; }
    public static void setTotalNanos(long v) { totalNanos = v; }

    public static long getNanos() {
        return totalNanos + System.nanoTime() - nanoReference;
    }

    public static void completeRun() {
        totalNanos += System.nanoTime() - nanoReference;
    }

    public static void finishMatrixMainActivity(int lastLapi) {
        if (!dbMode) return;
        interruptHcnGeneration(lastLapi);
        lastMatrixMainActivity.finish();
        lastMatrixMainActivity.setLastLapi(lastLapi);
        dbInsertService.submitStructural(lastMatrixMainActivity);
    }

    public static void finishMatrixExtensionActivity() {
        if (!dbMode) return;
        lastMatrixExtensionActivity.finish();
    }
}
