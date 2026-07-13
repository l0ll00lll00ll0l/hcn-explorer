package com.hcn.event;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class ActivityCenter {

    private static long nanoReference;
    private static final List<MatrixMainActivity> matrixMainActivities = new ArrayList<>();
    private static final List<MatrixExtensionActivity> matrixExtensionActivities = new ArrayList<>();
    private static final List<ApiNodeCreationActivity> apiNodeCreationActivities = new ArrayList<>();
    private static final List<TransitionNodeCreationActivity> transitionNodeCreationActivities = new ArrayList<>();
    private static final List<HcnGenerationActivity> hcnGenerationActivities = new ArrayList<>();

    private static final List<SqlInsertActivity> sqlInsertActivities = new CopyOnWriteArrayList<>();
    private static final List<InsertBatchCreatedEvent> insertBatchCreatedEvents = new CopyOnWriteArrayList<>();

    private static volatile int currentLapi = 0;

    // Progress tracking
    private static volatile boolean proving = false;
    private static volatile int proveTarget = 0;
    private static volatile int proveProgress = 0;

    public static void initialize(int firstLapiParam) {
        nanoReference = System.nanoTime();
        matrixMainActivities.clear();
        matrixExtensionActivities.clear();
        apiNodeCreationActivities.clear();
        transitionNodeCreationActivities.clear();
        hcnGenerationActivities.clear();
        sqlInsertActivities.clear();
        insertBatchCreatedEvents.clear();
        new MatrixMainActivity(firstLapiParam);
        new HcnGenerationActivity(firstLapiParam);
    }

    public static void resume(int fromLapi) {
        new MatrixMainActivity(fromLapi);
        new HcnGenerationActivity(fromLapi);
    }

    public static List<HcnGenerationActivity> getHcnGenerationActivities() {
        return hcnGenerationActivities;
    }

    public static void interruptHcnGeneration(int lapi) {
        HcnGenerationActivity last = hcnGenerationActivities.get(hcnGenerationActivities.size() - 1);
        last.setEndLapi(lapi);
        last.finish();
        if (last.getStartLapi() == last.getEndLapi()) {
            hcnGenerationActivities.remove(hcnGenerationActivities.size() - 1);
        }
    }

    public static void interruptHcnGeneration() { interruptHcnGeneration(currentLapi); }

    public static void resumeHcnGeneration(int lapi) {
        new HcnGenerationActivity(lapi);
    }

    public static void resumeHcnGeneration() { resumeHcnGeneration(currentLapi); }

    public static List<TransitionNodeCreationActivity> getTransitionNodeCreationActivities() {
        return transitionNodeCreationActivities;
    }

    public static List<ApiNodeCreationActivity> getApiNodeCreationActivities() {
        return apiNodeCreationActivities;
    }

    public static void addExtensionActivity(MatrixExtensionActivity activity) {
        matrixExtensionActivities.add(activity);
    }

    public static List<InsertBatchCreatedEvent> getInsertBatchCreatedEvents() {
        return insertBatchCreatedEvents;
    }

    public static List<SqlInsertActivity> getSqlInsertActivities() {
        return sqlInsertActivities;
    }

    public static boolean isProving() { return proving; }
    public static int getProveTarget() { return proveTarget; }
    public static int getProveProgress() { return proveProgress; }
    public static void setProving(boolean v) { proving = v; }
    public static void setCurrentLapi(int lapi) { currentLapi = lapi; }
    public static void setProveTarget(int v) { proveTarget = v; }
    public static void setProveProgress(int v) { proveProgress = v; }

    public static List<MatrixMainActivity> getMatrixMainActivities() {
        return matrixMainActivities;
    }

    public static List<MatrixExtensionActivity> getMatrixExtensionActivities() {
        return matrixExtensionActivities;
    }

    public static long getNanos() {
        return System.nanoTime() - nanoReference;
    }

    public static void finishMatrixMainActivity(int lastLapi) {
        interruptHcnGeneration(lastLapi);
        matrixMainActivities.get(matrixMainActivities.size() - 1).finish();
        matrixMainActivities.get(matrixMainActivities.size() - 1).setLastLapi(lastLapi);
    }

    public static void finishMatrixExtensionActivity() {
        matrixExtensionActivities.get(matrixExtensionActivities.size() - 1).finish();
    }
}
