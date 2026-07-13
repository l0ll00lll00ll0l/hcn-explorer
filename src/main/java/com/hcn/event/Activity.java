package com.hcn.event;

import lombok.Getter;

@Getter
public abstract class Activity {

    private static final int DIGITS = 6;

    private final long startNanos;
    private long finishNanos;

    protected Activity() {
        this.startNanos = ActivityCenter.getNanos();
    }

    public void finish() {
        this.finishNanos = ActivityCenter.getNanos();
    }

    protected String formatDuration() {
        long nanos = finishNanos - startNanos;
        if (nanos >= 60_000_000_000L) {
            return String.format("%." + DIGITS + "g min", nanos / 60_000_000_000.0);
        } else if (nanos >= 1_000_000_000L) {
            return String.format("%." + DIGITS + "g s",   nanos / 1_000_000_000.0);
        } else if (nanos >= 1_000_000L) {
            return String.format("%." + DIGITS + "g ms",  nanos / 1_000_000.0);
        } else if (nanos >= 1_000L) {
            return String.format("%." + DIGITS + "g µs",  nanos / 1_000.0);
        } else {
            return nanos + " ns";
        }
    }

    public abstract String getLabelName();
    public abstract String getGuiLabel();
}
