package com.hcn.v4;

public class Hcn implements Comparable<Hcn> {
    private HcnBody body;
    private int lastActivePrime;
    private ScientificNumber value;
    private ScientificNumber factor;
    
    public Hcn(HcnBody body, int lastActivePrime) {
        this.body = body;
        this.lastActivePrime = lastActivePrime;
    }
    
    public HcnBody getBody() {
        return body;
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

    public void setBody(HcnBody body) {
        this.body = body;
    }

    @Override
    public int compareTo(Hcn other) {
        return this.value.compareTo(other.value);
    }
    
    @Override
    public String toString() {
        return lastActivePrime + " v: " + value + " f: " + factor;
    }

    public String fullPrint() {
        return body.parentChainString() + "|" + lastActivePrime + " v: " + value + " f: " + factor;
    }

}
