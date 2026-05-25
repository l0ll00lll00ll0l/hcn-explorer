package com.hcn.db;

import org.springframework.stereotype.Component;

@Component
public class SaveProgress {

    private volatile boolean active = false;
    private volatile String phase = "";
    private volatile int phaseNumber = 0;
    private volatile int totalPhases = 10;
    private volatile int itemsDone = 0;
    private volatile int itemsTotal = 0;

    private volatile String error = null;

    public void start() {
        active = true;
        error = null;
        phaseNumber = 0;
        phase = "Starting...";
        itemsDone = 0;
        itemsTotal = 0;
    }

    public void startPhase(int number, String name, int total) {
        phaseNumber = number;
        phase = name;
        itemsDone = 0;
        itemsTotal = total;
    }

    public void increment() {
        itemsDone++;
    }

    public void fail(String errorMessage) {
        active = false;
        error = errorMessage;
        phase = "Failed";
    }

    public void done() {
        active = false;
        phase = "Done";
        phaseNumber = totalPhases;
    }

    public String getError() { return error; }

    public boolean isActive() { return active; }
    public String getPhase() { return phase; }
    public int getPhaseNumber() { return phaseNumber; }
    public int getTotalPhases() { return totalPhases; }
    public int getItemsDone() { return itemsDone; }
    public int getItemsTotal() { return itemsTotal; }

    public int getPercentage() {
        if (!active) return 100;
        double phaseWeight = 100.0 / totalPhases;
        double completedPhases = (phaseNumber - 1) * phaseWeight;
        double currentPhaseProgress = itemsTotal > 0 ? (double) itemsDone / itemsTotal * phaseWeight : 0;
        return (int) (completedPhases + currentPhaseProgress);
    }
}
