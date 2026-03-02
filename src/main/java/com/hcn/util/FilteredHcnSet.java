package com.hcn.util;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

public abstract class FilteredHcnSet extends TreeSet<Hcn> {
    private static final Hcn.DivisorComparator DIVISOR_COMPARATOR = new Hcn.DivisorComparator();
    private Hcn referenceHcn = null;
    
    private void updateReferenceHcn() {
        if (isEmpty()) {
            referenceHcn = null;
            return;
        }
        
        Hcn bestCandidate = null;
        int bestScore = -1;
        
        for (Hcn hcn : this) {
            int score = calculateActiveCountScore(hcn);
            if (score > bestScore) {
                bestScore = score;
                bestCandidate = hcn;
            }
        }
        
        referenceHcn = bestCandidate;
        referenceHcn.setReferenceValue(1.0);
        referenceHcn.setReferenceFactor(1.0);
        
        for (Hcn hcn : this) {
            if (hcn != referenceHcn) {
                hcn.calculateReferences(referenceHcn);
            }
        }
    }
    
    private int calculateActiveCountScore(Hcn hcn) {
        int score = hcn.getOwnPower().getActiveCount();
        if (hcn.getParentHcn() != null) {
            score += calculateActiveCountScore(hcn.getParentHcn());
        }
        return score;
    }
    
    @Override
    public boolean add(Hcn hcn) {
        if (referenceHcn == null) {
            referenceHcn = hcn;
            hcn.setReferenceValue(1.0);
            hcn.setReferenceFactor(1.0);
        } else {
            hcn.calculateReferences(referenceHcn);
        }
        
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
                boolean wasReference = (removed == referenceHcn);
                super.remove(removed);
                onRemove(removed);
                if (wasReference) {
                    updateReferenceHcn();
                }
            }
        }
        
        return added;
    }
    
    protected abstract void onRemove(Hcn hcn);
}
