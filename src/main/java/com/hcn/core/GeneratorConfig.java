package com.hcn.core;

public class GeneratorConfig {
    private static boolean locked = false;
    private static boolean basicData = true;

    public static boolean isLocked() { return locked; }
    public static void lock() { locked = true; }
    public static void reset() { locked = false; basicData = true; }

    public static boolean isBasicData() { return basicData; }
    public static void setBasicData(boolean value) { basicData = value; }
}
