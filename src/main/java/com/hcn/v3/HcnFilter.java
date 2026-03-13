package com.hcn.v3;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;

public class HcnFilter {
    
    private TreeMap<Integer, ArrayList<Hcn>> filter = new TreeMap<>();

    public void filter(TreeSet<Hcn> generatedHcns) {
        generatedHcns.forEach(hcn -> {
            int lastActivePrime = hcn.getLastActivePrime();

            if (!filter.containsKey(lastActivePrime)) {
                if (lastActivePrime > 0 && filter.containsKey(lastActivePrime - 1)) {
                    ArrayList<Hcn> previousLevel = filter.get(lastActivePrime - 1);
                    Hcn lastFromPrevious = previousLevel.get(previousLevel.size() - 1);
                    
                    if (hcn.getFactor().isNotBiggerThan(lastFromPrevious.getFactor())) {
                        checkDenyTrigger(hcn, lastFromPrevious);
                        return;
                    }
                }
                filter.put(lastActivePrime, new ArrayList<>());
                if (lastActivePrime > 0 && filter.containsKey(lastActivePrime - 1)) {
                    ArrayList<Hcn> previousLevel = filter.get(lastActivePrime - 1);
                    filter.get(lastActivePrime).addAll(previousLevel);
                }
                filter.get(lastActivePrime).add(hcn);
                return;
            }
            
            ArrayList<Hcn> currentLevel = filter.get(lastActivePrime);
            Hcn lastInLevel = currentLevel.get(currentLevel.size() - 1);
            if (hcn.getFactor().isNotBiggerThan(lastInLevel.getFactor())) {
                checkDenyTrigger(hcn, lastInLevel);
                return;
            }
            
            currentLevel.add(hcn);
            
            for (int higherLevel = lastActivePrime + 1; filter.containsKey(higherLevel); higherLevel++) {
                ArrayList<Hcn> higherLevelList = filter.get(higherLevel);
                Hcn lastInHigher = higherLevelList.get(higherLevelList.size() - 1);
                
                if (hcn.getFactor().isBiggerThan(lastInHigher.getFactor())) {
                    higherLevelList.add(hcn);
                } else {
                    break;
                }
            }
        });
    }

    public void filterAgain(TreeSet<Hcn> sortedRawHcn) {
        sortedRawHcn.forEach(hcn -> {
            int lastActivePrime = hcn.getLastActivePrime();
            
            if (!filter.containsKey(lastActivePrime)) {
                if (!handleNewLevelForFilterAgain(hcn, lastActivePrime)) {
                    return;
                }
            }
            
            if (!handleExistingLevelForFilterAgain(hcn, lastActivePrime)) {
                return;
            }
            
            forwardPropagateForFilterAgain(hcn, lastActivePrime);
        });
    }
    
    private boolean handleNewLevelForFilterAgain(Hcn hcn, int lastActivePrime) {
        if (lastActivePrime > 0 && filter.containsKey(lastActivePrime - 1)) {
            ArrayList<Hcn> previousLevel = filter.get(lastActivePrime - 1);
            
            for (Hcn prevHcn : previousLevel) {
                if (prevHcn.getValue().isNotBiggerThan(hcn.getValue()) &&
                    prevHcn.getFactor().isNotSmallerThan(hcn.getFactor())) {
                    checkDenyTrigger(hcn, prevHcn);
                    return false;
                }
            }
        }
        filter.put(lastActivePrime, new ArrayList<>());
        if (lastActivePrime > 0 && filter.containsKey(lastActivePrime - 1)) {
            ArrayList<Hcn> previousLevel = filter.get(lastActivePrime - 1);
            filter.get(lastActivePrime).addAll(previousLevel);
        }
        return true;
    }
    
    private boolean handleExistingLevelForFilterAgain(Hcn hcn, int lastActivePrime) {
        ArrayList<Hcn> currentLevel = filter.get(lastActivePrime);
        
        for (int i = currentLevel.size() - 1; i >= 0; i--) {
            Hcn existingHcn = currentLevel.get(i);
            
            if (existingHcn.getValue().isNotBiggerThan(hcn.getValue())) {
                if (existingHcn.getFactor().isNotSmallerThan(hcn.getFactor())) {
                    checkDenyTrigger(hcn, existingHcn);
                    return false;
                }
                break;
            }
        }
        
        int insertIndex = findInsertIndex(currentLevel, hcn);
        currentLevel.add(insertIndex, hcn);
        
        List<Hcn> toRemove = new ArrayList<>();
        for (int i = insertIndex + 1; i < currentLevel.size(); i++) {
            Hcn nextHcn = currentLevel.get(i);
            if (hcn.getFactor().isNotSmallerThan(nextHcn.getFactor())) {
                toRemove.add(nextHcn);
            } else {
                break;
            }
        }
        
        for (Hcn removed : toRemove) {
            currentLevel.remove(removed);
            checkDenyTrigger(removed, hcn);
        }
        
        return true;
    }
    
    private int findInsertIndex(ArrayList<Hcn> list, Hcn hcn) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getValue().isBiggerThan(hcn.getValue())) {
                return i;
            }
        }
        return list.size();
    }
    
    private void forwardPropagateForFilterAgain(Hcn hcn, int lastActivePrime) {
        for (int higherLevel = lastActivePrime + 1; filter.containsKey(higherLevel); higherLevel++) {
            ArrayList<Hcn> higherLevelList = filter.get(higherLevel);
            
            if (isBlockedByExistingHcn(hcn, higherLevelList)) {
                break;
            }
            
            int idx = findInsertIndex(higherLevelList, hcn);
            higherLevelList.add(idx, hcn);
            
            List<Hcn> toRemove = new ArrayList<>();
            for (int i = idx + 1; i < higherLevelList.size(); i++) {
                Hcn nextHcn = higherLevelList.get(i);
                if (hcn.getFactor().isNotSmallerThan(nextHcn.getFactor())) {
                    toRemove.add(nextHcn);
                } else {
                    break;
                }
            }
            
            for (Hcn removed : toRemove) {
                higherLevelList.remove(removed);
                checkDenyTrigger(removed, hcn);
            }
        }
    }
    
    private boolean isBlockedByExistingHcn(Hcn hcn, ArrayList<Hcn> higherLevelList) {
        for (Hcn higherHcn : higherLevelList) {
            if (higherHcn.getValue().isNotBiggerThan(hcn.getValue()) &&
                higherHcn.getFactor().isNotSmallerThan(hcn.getFactor())) {
                return true;
            }
        }
        return false;
    }
    
    public ArrayList<Hcn> getMaxLevelHcnList() {
        if (filter.isEmpty()) {
            return new ArrayList<>();
        }
        return filter.get(filter.lastKey());
    }

    private void checkDenyTrigger(Hcn defeated, Hcn superior) {
        if (defeated.getBody().isDeactivated()) {
            return;
        }
        if (defeated.getBody().isProved()) {
            defeated.getBody().deactivateFromLists();
            defeated.getBody().deactivateRecursive(superior.getBody());
        } else {
            if (superior.getLastActivePrime() <= defeated.getLastActivePrime()) {
                defeated.getBody().deactivateFromLists();
                defeated.getBody().deactivateRecursive(superior.getBody());
            }
        }
    }

    private void printFilterState(String label) {
        System.out.println("\n[FILTER_STATE] === " + label + " ===");
        if (filter.isEmpty()) {
            System.out.println("[FILTER_STATE] Filter is EMPTY");
            return;
        }
        for (Integer level : filter.keySet()) {
            ArrayList<Hcn> hcnList = filter.get(level);
            System.out.println("[FILTER_STATE] Level " + level + " (" + hcnList.size() + " HCNs):");
            for (int i = 0; i < hcnList.size(); i++) {
                System.out.println("[FILTER_STATE]   [" + i + "] " + hcnList.get(i).fullPrint());
            }
        }
        System.out.println("[FILTER_STATE] ====================\n");
    }

    public void lowLimitUpdate(ScientificNumber lowLimit) {
        if (filter.isEmpty()) return;
        
        int maxLevel = filter.lastKey();
        ArrayList<Integer> levelsToRemove = new ArrayList<>();

        for (Integer level : filter.keySet()) {
            if (level == maxLevel) continue;
            
            ArrayList<Hcn> levelList = filter.get(level);
            if (levelList.isEmpty()) continue;
            
            Hcn lastHcn = levelList.get(levelList.size() - 1);
            
            if (lastHcn.getValue().isSmallerThan(lowLimit)) {
                levelsToRemove.add(level);
            } else {
                levelList.removeIf(hcn -> hcn != lastHcn && hcn.getValue().isSmallerThan(lowLimit));
            }
        }
        for (Integer level : levelsToRemove) {
            filter.remove(level);
        }
    }

    public void rmoveFirst() {
        filter.get(filter.lastKey()).remove(0);
    }
    
}
