package com.hcn.v6;

import java.util.HashSet;
import java.util.Set;

public class PrimeIndexPower {
    private ActivePrimeIndex primeIndex;
    private int power;
    private boolean proved = false;
    private Set<HcnBody> activeHcnBodies = new HashSet<>();
    
    public ActivePrimeIndex getActivePrimeIndex() {
        return primeIndex;
    }
    
    public int getPower() {
        return power;
    }

    public boolean isProved() {
        return proved;
    }

    public void setProved(boolean proved) {
        this.proved = proved;
    }

    public Set<HcnBody> getActiveHcnBodies() {
        return activeHcnBodies;
    }

    public void addActiveHcnBody(HcnBody body) {
        activeHcnBodies.add(body);
    }

    public void removeActiveHcnBody(HcnBody body) {
        activeHcnBodies.remove(body);
    }

    public PrimeIndexPower(ActivePrimeIndex primeIndex, int power) {
        this.primeIndex = primeIndex;
        this.power = power;
    }

    public String toString() {
        return "PIP{" + primeIndex.getIndex() +
                "^" + power +
                ", proved=" + proved +
                ", bodies=" + activeHcnBodies.size() +
                '}';
    }
}
