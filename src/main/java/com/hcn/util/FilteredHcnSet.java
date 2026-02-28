package com.hcn.util;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

public abstract class FilteredHcnSet extends TreeSet<Hcn> {
    private static final Hcn.DivisorComparator DIVISOR_COMPARATOR = new Hcn.DivisorComparator();
    
    @Override
    public boolean add(Hcn hcn) {
        Hcn lower = lower(hcn);
        
        if (lower != null && DIVISOR_COMPARATOR.compare(lower, hcn) >= 0) {
            return false;
        }
        
        boolean added = super.add(hcn);
        
        if (added) {
            List<Hcn> toRemove = new ArrayList<>();
            for (Hcn higher : tailSet(hcn, false)) {
                if (DIVISOR_COMPARATOR.compare(hcn, higher) >= 0) {
                    toRemove.add(higher);
                } else {
                    break;
                }
            }
            
            for (Hcn removed : toRemove) {
                super.remove(removed);
                onRemove(removed);
            }
        }
        
        return added;
    }
    
    protected abstract void onRemove(Hcn hcn);
}
