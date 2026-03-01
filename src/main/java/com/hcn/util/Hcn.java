package com.hcn.util;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class Hcn implements Comparable<Hcn> {
    private Hcn parentHcn;
    private PrimeIndexPower ownPower;
    private double referenceValue = 1.0;
    private double referenceFactor = 1.0;
    
    public Hcn(Hcn parentHcn, PrimeIndexPower ownPower) {
        this.parentHcn = parentHcn;
        this.ownPower = ownPower;
    }
    
    public Hcn getParentHcn() {
        return parentHcn;
    }
    
    public PrimeIndexPower getOwnPower() {
        return ownPower;
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
        List<PrimeIndexPower> result = new ArrayList<>();
        collectPowers(result);
        return result;
    }
    
    private void collectPowers(List<PrimeIndexPower> result) {
        if (parentHcn != null) {
            parentHcn.collectPowers(result);
        }
        result.add(ownPower);
    }
    
    public void setPowers(List<PrimeIndexPower> powers) {
        // Deprecated - kept for compatibility
    }
    
    @Override
    public int compareTo(Hcn other) {
        return Double.compare(this.referenceValue, other.referenceValue);
    }
    
    private int getPowerAtIndex(int primeIndex) {
        if (ownPower.getPrimeIndex() == primeIndex) {
            return ownPower.getPower();
        }
        return parentHcn != null ? parentHcn.getPowerAtIndex(primeIndex) : 0;
    }
    
    private int getMaxPrimeIndex() {
        return ownPower.getPrimeIndex();
    }
    
    @Override
    public String toString() {
        return toString(false, false);
    }
    
    public String toString(boolean showValue, boolean showFactor) {
        List<PrimeIndexPower> powers = getPowers();
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
        List<PrimeIndexPower> powers = getPowers();
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
        BigInteger parentValue = parentHcn != null ? parentHcn.calculateValue() : BigInteger.ONE;
        if (ownPower.getPower() > 0) {
            int prime = PrimeCenter.getPrime(ownPower.getPrimeIndex());
            return parentValue.multiply(BigInteger.valueOf(prime).pow(ownPower.getPower()));
        }
        return parentValue;
    }
    
    private long calculateDivisorCount() {
        long parentCount = parentHcn != null ? parentHcn.calculateDivisorCount() : 1;
        return parentCount * (ownPower.getPower() + 1);
    }

    public void setProved() {
        ownPower.setProved(true);
        if (parentHcn != null) {
            parentHcn.setProved();
        }
    }
    
    public void calculateReferences(Hcn referenceHcn) {
        int maxIndex = Math.max(this.getMaxPrimeIndex(), referenceHcn.getMaxPrimeIndex());
        
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
