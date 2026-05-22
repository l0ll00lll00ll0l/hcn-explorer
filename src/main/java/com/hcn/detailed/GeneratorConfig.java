package com.hcn.detailed;

public class GeneratorConfig {
    private static boolean locked = false;

    public static boolean isLocked() { return locked; }
    public static void lock() { locked = true; }
    public static void reset() { locked = false; }
}
