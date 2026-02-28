package com.hcn.util;

public class MatrixHcnSet extends FilteredHcnSet {
    @Override
    public boolean add(Hcn hcn) {
        boolean added = super.add(hcn);
        if (added) {
            for (PrimeIndexPower power : hcn.getPowers()) {
                power.incrementActiveCount();
            }
        }
        return added;
    }
    
    @Override
    protected void onRemove(Hcn hcn) {
        for (PrimeIndexPower power : hcn.getPowers()) {
            power.decrementActiveCount();
        }
    }
}
