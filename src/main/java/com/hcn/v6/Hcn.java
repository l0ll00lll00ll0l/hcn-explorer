package com.hcn.v6;

import java.util.LinkedHashMap;

public class Hcn implements Comparable<Hcn> {
    private HcnBody body;
    private int lastActivePrime;
    private ScientificNumber value;
    private ScientificNumber factor;
    private LinkedHashMap<LastActivePrimeIndexGroup, Hcn> smallerHcns = new LinkedHashMap<>();
    private LinkedHashMap<LastActivePrimeIndexGroup, Hcn> largerHcns = new LinkedHashMap<>();
    private Hcn superiorHcn;
    
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

    public void setLastActivePrime(int lastActivePrime) {this.lastActivePrime = lastActivePrime;}

    public LinkedHashMap<LastActivePrimeIndexGroup, Hcn> getSmallerHcns() { return smallerHcns; }
    public LinkedHashMap<LastActivePrimeIndexGroup, Hcn> getLargerHcns() { return largerHcns; }
    public Hcn getSuperiorHcn() { return superiorHcn; }
    public void setSuperiorHcn(Hcn superiorHcn) { this.superiorHcn = superiorHcn; }
    public boolean isDominated() { return superiorHcn != null; }

    @Override
    public int compareTo(Hcn other) {
        return this.value.compareTo(other.value);
    }
    
    @Override
    public String toString() {
        return this.body.parentChainString() + " " + lastActivePrime + " v: " + value + " f: " + factor;
    }

    public String fullPrint() {
        return body.parentChainString() + "|" + lastActivePrime + " v: " + value + " f: " + factor;
    }

    public Hcn createHcnByReferenceOld(HcnBody hcnBody) {
        Hcn newHcn = new Hcn(hcnBody, lastActivePrime);

        newHcn.setValue(body.getValueMultiplier(hcnBody).multiply(value));
        newHcn.setFactor(this.body.getFactorMultiplier(hcnBody).multiply(factor));
        return newHcn;
    }

}
