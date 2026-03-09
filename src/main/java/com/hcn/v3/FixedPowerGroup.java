package com.hcn.v3;

public class FixedPowerGroup {
    private int startIndex;
    private int endIndex;
    private int fixedPower;
    
    public FixedPowerGroup(int startIndex, int endIndex, int fixedPower) {
        this.startIndex = startIndex;
        this.endIndex = endIndex;
        this.fixedPower = fixedPower;
    }
    
    public int getStartIndex() {
        return startIndex;
    }
    
    public int getEndIndex() {
        return endIndex;
    }
    
    public void setEndIndex(int endIndex) {
        this.endIndex = endIndex;
    }
    
    public int getFixedPower() {
        return fixedPower;
    }
    
    public boolean contains(int primeIndex) {
        return primeIndex >= startIndex && primeIndex <= endIndex;
    }
}
