package com.hcn.detailed;

public class GeneratorConfig {
    private static boolean extendedHcnBodyData = false;
    private static boolean locked = false;

    public static boolean isExtendedHcnBodyData() { return extendedHcnBodyData; }
    public static void setExtendedHcnBodyData(boolean value) {
        if (!locked) extendedHcnBodyData = value;
    }

    public static boolean isLocked() { return locked; }
    public static void lock() { locked = true; }
    public static void reset() { locked = false; extendedHcnBodyData = false; }
}
