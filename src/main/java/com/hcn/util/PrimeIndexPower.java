package com.hcn.util;

public class PrimeIndexPower {
    private int primeIndex;
    private int power;
    private boolean proved = false;
    private int activeCount = 0;
    
    public PrimeIndexPower(int primeIndex, int power) {
        this.primeIndex = primeIndex;
        this.power = power;
    }
    
    public int getPrimeIndex() {
        return primeIndex;
    }
    
    public void setPrimeIndex(int primeIndex) {
        this.primeIndex = primeIndex;
    }
    
    public int getPower() {
        return power;
    }
    
    public void setPower(int power) {
        this.power = power;
    }

    public void setProved(boolean b) {
        this.proved = b;
    }

    public boolean isProved() {
        return proved;
    }
    
    public int getActiveCount() {
        return activeCount;
    }
    
    public void incrementActiveCount() {
        activeCount++;
    }
    
    public void decrementActiveCount() {
        activeCount--;
    }
}
