package com.hcn.core;

import com.hcn.db.IntervalStorageService;

public class GeneratorConfig {
    private static boolean locked = false;
    private static boolean basicData = true;
    private static IntervalStorageService intervalStorageService;
    private static String dbName;

    public static boolean isLocked() { return locked; }
    public static void lock() { locked = true; }
    public static void reset() { locked = false; basicData = true; }

    public static boolean isBasicData() { return basicData; }
    public static void setBasicData(boolean value) { basicData = value; }

    public static IntervalStorageService getIntervalStorageService() { return intervalStorageService; }
    public static void setIntervalStorageService(IntervalStorageService service) { intervalStorageService = service; }

    public static String getDbName() { return dbName; }
    public static void setDbName(String name) { dbName = name; }
}
