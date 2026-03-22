package com.hcn.v6;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class FilteredHcnBodySet extends TreeSet<HcnBody> {
    
    private int compareByFactor(HcnBody body1, HcnBody body2) {
        return body1.getFactor().compareTo(body2.getFactor());
    }
    
    public Set<HcnBody> addGroup(Collection<HcnBody> bodies) {
        System.out.println("[FilteredSet] addGroup() called with " + bodies.size() + " bodies, current size=" + size());
        Set<HcnBody> successfullyAdded = new HashSet<>();
        Set<HcnBody> bodiesToDeactivate = new HashSet<>();
        
        // 1. Add all bodies to the TreeSet
        for (HcnBody body : bodies) {
            super.add(body);
        }
        
        // 2. Single pass: current is recorder, check if next should be removed
        List<HcnBody> allBodies = new ArrayList<>(this);
        HcnBody recorder = null;
        
        for (HcnBody current : allBodies) {
            if (recorder != null && compareByFactor(recorder, current) >= 0) {
                // Recorder beats current, remove it
                System.out.println("[FilteredSet]   -> DELETING body=" + current + " (dominated by " + recorder + ")");
                super.remove(current);
                bodiesToDeactivate.add(current);
            } else {
                // Current is superior, becomes new recorder
                recorder = current;
                if (bodies.contains(current)) {
                    System.out.println("[FilteredSet]   -> ADDED new body=" + current);
                    current.getPip().addActiveHcnBody(current);
                    successfullyAdded.add(current);
                }
            }
        }
        
        System.out.println("[FilteredSet] addGroup() done, added=" + successfullyAdded.size() + ", deleted=" + bodiesToDeactivate.size() + ", size after=" + size());
        // 3. Deactivate removed bodies
        for (HcnBody removed : bodiesToDeactivate) {
            // Call deactivateFromLists only for old bodies
            if (!bodies.contains(removed)) {
                removed.deactivateFromLists();
            }
            // Call deactivateRecursive for all removed bodies
            HcnBody superior = this.ceiling(removed);
            if (superior == null) {
                superior = this.floor(removed);
            }
            removed.getPip().getActivePrimeIndex().deactivateRecursive(removed, superior);
        }
        
        return successfullyAdded;
    }
}
