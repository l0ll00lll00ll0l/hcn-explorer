package com.hcn.util;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

public class PrimeIndex {
    private int index;
    private TreeMap<Integer, PrimeIndexPower> powers = new TreeMap<>();
    private PrimeIndexHcnSet hcnList = new PrimeIndexHcnSet();
    
    public PrimeIndex(int index) {
        this.index = index;
        PrimeIndexPower power0 = new PrimeIndexPower(index, 0);
        powers.put(0, power0);
    }
    
    public static PrimeIndex initializePrimeIndex() {
        PrimeIndex primeIndex = new PrimeIndex(0);
        Hcn hcn = new Hcn(null, primeIndex.powers.get(0));
        primeIndex.hcnList.add(hcn);
        
        return primeIndex;
    }
    
    public int getIndex() {
        return index;
    }
    
    public void setIndex(int index) {
        this.index = index;
    }
    
    public List<PrimeIndexPower> getPowers() {
        return new ArrayList<>(powers.values());
    }
    
    public void setPowers(List<PrimeIndexPower> powersList) {
        powers.clear();
        for (PrimeIndexPower power : powersList) {
            powers.put(power.getPower(), power);
        }
    }
    
    public List<Hcn> getHcnList() {
        return new ArrayList<>(hcnList);
    }
    
    public TreeSet<Hcn> getHcnSet() {
        return hcnList;
    }
    
    public PrimeIndexPower addNextPrimeIndexPower() {
        int nextPowerValue = powers.lastKey() + 1;
        PrimeIndexPower newPower = new PrimeIndexPower(index, nextPowerValue);
        powers.put(nextPowerValue, newPower);
        return newPower;
    }

    public boolean isExtensionRequired(PrimeIndex previousPrimeIndex) {
        PrimeIndexPower lastPower = powers.get(powers.lastKey());
        if (!lastPower.isProved()) {
            return false;
        }
        
        if (previousPrimeIndex != null) {
            int nextPowerValue = powers.lastKey() + 1;
            PrimeIndexPower prevPower = previousPrimeIndex.powers.get(nextPowerValue);
            return prevPower != null && prevPower.isProved();
        }
        
        return true;
    }

    public boolean hasAnyProven() {
        return this.powers.values().stream().anyMatch(p -> p.isProved());
    }
    
    public void removeInactivePowers(List<PrimeIndex> allPrimeIndexes) {
        while (!powers.isEmpty() && powers.get(powers.firstKey()).getActiveCount() == 0) {
            PrimeIndexPower removed = powers.remove(powers.firstKey());
            
            for (PrimeIndex primeIndex : allPrimeIndexes) {
                primeIndex.getHcnSet().removeIf(hcn -> containsPower(hcn, removed));
            }
        }
    }
    
    private boolean containsPower(Hcn hcn, PrimeIndexPower power) {
        if (hcn.getOwnPower() == power) {
            return true;
        }
        return hcn.getParentHcn() != null && containsPower(hcn.getParentHcn(), power);
    }
}
