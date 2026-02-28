package com.hcn.util;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Hcn implements Comparable<Hcn> {
    private List<PrimeIndexPower> powers;
    private double referenceValue = 1.0;
    private double referenceFactor = 1.0;
    
    public Hcn() {
        this.powers = new ArrayList<>();
    }
    
    public double getReferenceValue() {
        return referenceValue;
    }
    
    public void setReferenceValue(double referenceValue) {
        this.referenceValue = referenceValue;
    }
    
    public double getReferenceFactor() {
        return referenceFactor;
    }
    
    public void setReferenceFactor(double referenceFactor) {
        this.referenceFactor = referenceFactor;
    }
    
    public List<PrimeIndexPower> getPowers() {
        return powers;
    }
    
    public void setPowers(List<PrimeIndexPower> powers) {
        this.powers = powers;
    }
    
    @Override
    public int compareTo(Hcn other) {
        return Double.compare(this.referenceValue, other.referenceValue);
    }
    
    private int getPowerAtIndex(int primeIndex) {
        for (PrimeIndexPower power : powers) {
            if (power.getPrimeIndex() == primeIndex) {
                return power.getPower();
            }
        }
        return 0;
    }
    
    @Override
    public String toString() {
        return toString(false, false);
    }
    
    public String toString(boolean showValue, boolean showFactor) {
        String base = powers.stream()
            .map(p -> "p" + p.getPrimeIndex() + "^" + p.getPower())
            .collect(Collectors.joining(" × "));
        
        if (showValue || showFactor) {
            base += " (";
            if (showValue) {
                base += "v=" + calculateValue();
            }
            if (showValue && showFactor) {
                base += ", ";
            }
            if (showFactor) {
                base += "d=" + calculateDivisorCount();
            }
            base += ")";
        }
        
        return base;
    }
    
    public String toShortString() {
        return toShortString(false, false);
    }
    
    public String toShortString(boolean showValue, boolean showFactor) {
        if (powers.isEmpty()) return "";
        
        StringBuilder sb = new StringBuilder();
        int lastPower = -1;
        
        for (PrimeIndexPower p : powers) {
            int currentPower = p.getPower();
            if (lastPower != -1 && lastPower != currentPower) {
                sb.append("|");
            }
            sb.append(currentPower);
            lastPower = currentPower;
        }
        
        if (showValue || showFactor) {
            sb.append(" (");
            if (showValue) {
                sb.append("v=").append(calculateValue());
            }
            if (showValue && showFactor) {
                sb.append(", ");
            }
            if (showFactor) {
                sb.append("d=").append(calculateDivisorCount());
            }
            sb.append(")");
        }
        
        return sb.toString();
    }
    
    private BigInteger calculateValue() {
        BigInteger value = BigInteger.ONE;
        for (PrimeIndexPower p : powers) {
            if (p.getPower() > 0) {
                int prime = PrimeCenter.getPrime(p.getPrimeIndex());
                value = value.multiply(BigInteger.valueOf(prime).pow(p.getPower()));
            }
        }
        return value;
    }
    
    private long calculateDivisorCount() {
        long count = 1;
        for (PrimeIndexPower p : powers) {
            count *= (p.getPower() + 1);
        }
        return count;
    }

    public void setProved() {
        this.powers.stream().forEach(p -> p.setProved(true));
    }
    
    public void calculateReferences(Hcn referenceHcn) {
        int maxIndex = Math.max(
            this.powers.isEmpty() ? -1 : this.powers.get(this.powers.size() - 1).getPrimeIndex(),
            referenceHcn.powers.isEmpty() ? -1 : referenceHcn.powers.get(referenceHcn.powers.size() - 1).getPrimeIndex()
        );
        
        double valueRatio = 1.0;
        double factorRatio = 1.0;
        
        for (int i = 0; i <= maxIndex; i++) {
            int thisPower = getPowerAtIndex(i);
            int refPower = referenceHcn.getPowerAtIndex(i);
            
            if (thisPower != refPower) {
                int prime = PrimeCenter.getPrime(i);
                valueRatio *= Math.pow(prime, thisPower - refPower);
                factorRatio *= (double)(thisPower + 1) / (refPower + 1);
            }
        }
        
        this.referenceValue = valueRatio;
        this.referenceFactor = factorRatio;
    }
    
    public static class DivisorComparator implements java.util.Comparator<Hcn> {
        @Override
        public int compare(Hcn hcn1, Hcn hcn2) {
            return Double.compare(hcn1.referenceFactor, hcn2.referenceFactor);
        }
    }
}
