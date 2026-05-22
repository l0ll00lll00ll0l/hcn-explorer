package com.hcn.core;

import java.util.LinkedHashMap;

public class Hcn implements Comparable<Hcn> {
    private HcnGenerator hcnGenerator;
    private int lastActivePrime;
    private ScientificNumber value;
    private ScientificNumber factor;
    
    public Hcn(HcnGenerator hcnGenerator, int lastActivePrime) {
        this.hcnGenerator = hcnGenerator;
        this.lastActivePrime = lastActivePrime;
    }
    
    public HcnBody getBody() {
        return hcnGenerator != null ? hcnGenerator.getCurrentHcnBody() : null;
    }
    public HcnGenerator getHcnGenerator() {
        return hcnGenerator;
    }
    public void setHcnGenerator(HcnGenerator hcnGenerator) {
        this.hcnGenerator = hcnGenerator;
    }
    public int getLastActivePrime() {
        return lastActivePrime;
    }
    public ScientificNumber getValue() {
        return value;
    }
    public void setValue(ScientificNumber value) {
        this.value = value;
    }
    public ScientificNumber getFactor() {
        return factor;
    }
    public void setFactor(ScientificNumber factor) {
        this.factor = factor;
    }
    public void setLastActivePrime(int lastActivePrime) {this.lastActivePrime = lastActivePrime;}

    @Override
    public int compareTo(Hcn other) {
        return this.value.compareTo(other.value);
    }
    
    @Override
    public String toString() {
        if (getBody() == null) {
            return "nullbody " + lastActivePrime + " v: " + value + " f: " + factor;
        }
        return getBody().parentChainString() + " " + lastActivePrime + " v: " + value + " f: " + factor;
    }

    public String fullPrint() {
        return getBody().parentChainString() + "|" + lastActivePrime + " v: " + value + " f: " + factor;
    }
}
