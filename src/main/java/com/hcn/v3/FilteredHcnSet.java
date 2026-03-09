package com.hcn.v3;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

public class FilteredHcnSet extends TreeSet<Hcn> {
    
    @Override
    public boolean add(Hcn hcn) {

        Hcn lower = lower(hcn);
        
        if (lower != null && compareByFactor(lower, hcn) >= 0) {
            checkDenyTrigger(hcn, lower);
            return false;
        }
        
        super.add(hcn);

        List<Hcn> toRemove = new ArrayList<>();
            for (Hcn higher : tailSet(hcn, false)) {
                if (compareByFactor(hcn, higher) >= 0) {
                    toRemove.add(higher);
                } else {
                    break;
                }
            }
            
            for (Hcn removed : toRemove) {
                super.remove(removed);
                checkDenyTrigger(removed, hcn);
            }

        
        return true;
    }
    
    private int compareByFactor(Hcn hcn1, Hcn hcn2) {
        return hcn1.getFactor().compareTo(hcn2.getFactor());
    }

    private void checkDenyTrigger(Hcn defeated, Hcn superior) {
        if (defeated.getBody().isDeactivated()) {
            return;
        }
        if (defeated.getBody().isProved()) {
            defeated.getBody().deactivateFromLists();
            defeated.getBody().deactivateRecursive(superior.getBody());
        }
    }
}
