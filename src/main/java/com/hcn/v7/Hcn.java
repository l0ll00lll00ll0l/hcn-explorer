package com.hcn.v7;

import java.util.LinkedHashMap;

public class Hcn implements Comparable<Hcn> {
    private HcnBody body;
    private int lastActivePrime;
    private ScientificNumber value;
    private ScientificNumber factor;
    private LinkedHashMap<LastActivePrimeIndexGroup, Hcn> smallerHcns = new LinkedHashMap<>();
    private LinkedHashMap<LastActivePrimeIndexGroup, Hcn> largerHcns = new LinkedHashMap<>();
    
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
    public int getPowerAt(int primeIndex, ActivePrimeIndex lastActivePrimeIndex) {
        // first check the body chain directly — it has the correct power for this Hcn
        HcnBody b = body;
        while (b != null) {
            if (b.getPip().getActivePrimeIndex().getIndex() == primeIndex) {
                return b.getPip().getPower();
            }
            b = b.getParent();
        }

        // not in body chain — must be a FixedPowerGroup prime or lapi-only prime
        // walk the current matrix structure to find it
        Object current = lastActivePrimeIndex;
        while (current != null) {
            if (current instanceof ActivePrimeIndex) {
                ActivePrimeIndex api = (ActivePrimeIndex) current;
                if (api.getParentFixedPowerGroup() != null) {
                    current = api.getParentFixedPowerGroup();
                } else {
                    current = api.getParentActivePrimeIndex();
                }
            } else if (current instanceof FixedPowerGroup) {
                FixedPowerGroup fpg = (FixedPowerGroup) current;
                for (ActivePrimeIndex fixed : fpg.getFixedPowerGroup()) {
                    if (fixed.getIndex() == primeIndex) {
                        return fixed.getPips().firstEntry().getValue().getPower();
                    }
                }
                current = fpg.getParentPrimeIndex();
            } else {
                break;
            }
        }
        return 1;
    }

}
