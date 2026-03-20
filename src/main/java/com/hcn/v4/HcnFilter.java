package com.hcn.v4;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;
import java.util.TreeSet;


public class HcnFilter {

    private TreeMap<Integer, ArrayList<Hcn>> levelMap = new TreeMap<>();

    public void addFromExpandedRange(TreeSet<Hcn> sortedNewHcns) {
        sortedNewHcns.forEach(this::appendHcn);
    }

    public void addAfterExtension(TreeSet<Hcn> generatedHcns) {
        generatedHcns.forEach(this::insertHcn);
    }

    public ArrayList<Hcn> getMaxLevelCandidates() {
        if (levelMap.isEmpty()) {
            return new ArrayList<>();
        }
        return levelMap.get(levelMap.lastKey());
    }

    public void removeProvedFirst() {
        levelMap.get(levelMap.lastKey()).remove(0);
    }

    public void discardBelowLimit(ScientificNumber lowLimit) {
        if (levelMap.isEmpty()) return;

        int maxLevel = levelMap.lastKey();

        for (Integer level : levelMap.keySet()) {
            if (level == maxLevel) continue;

            ArrayList<Hcn> levelList = levelMap.get(level);
            if (levelList.isEmpty()) continue;

            Hcn lastHcn = levelList.get(levelList.size() - 1);

            levelList.removeIf(hcn -> hcn != lastHcn && hcn.getValue().isSmallerThan(lowLimit));
        }
    }

    public void removeLevelBelow(int minLevel) {
        List<Integer> toRemove = new ArrayList<>();
        for (Integer level : levelMap.keySet()) {
            if (level < minLevel) {
                toRemove.add(level);
            }
        }
        toRemove.forEach(levelMap::remove);
    }

    // --- Append path ---

    private void appendHcn(Hcn hcn) {
        int level = hcn.getLastActivePrime();

        ensureLevelExists(level);

        if (!tryAppendToLevel(hcn, level)) {
            return;
        }

        appendToHigherLevels(hcn, level);
    }

    private boolean tryAppendToLevel(Hcn hcn, int level) {
        ArrayList<Hcn> levelList = levelMap.get(level);

        if (!levelList.isEmpty()) {
            Hcn lastInLevel = levelList.get(levelList.size() - 1);
            if (isDominatedBy(hcn, lastInLevel)) {
                notifyDefeated(hcn, lastInLevel);
                return false;
            }
        }

        levelList.add(hcn);
        return true;
    }

    private void appendToHigherLevels(Hcn hcn, int startLevel) {
        for (int level = startLevel + 1; levelMap.containsKey(level); level++) {
            ArrayList<Hcn> levelList = levelMap.get(level);
            Hcn lastInLevel = levelList.get(levelList.size() - 1);

            if (isDominatedBy(hcn, lastInLevel)) {
                break;
            }

            levelList.add(hcn);
        }
    }

    // --- Insert path ---

    private void insertHcn(Hcn hcn) {
        int level = hcn.getLastActivePrime();

        ensureLevelExists(level);

        if (!tryInsertIntoLevel(hcn, level)) {
            return;
        }

        insertIntoHigherLevels(hcn, level);
    }

    private boolean tryInsertIntoLevel(Hcn hcn, int level) {
        ArrayList<Hcn> levelList = levelMap.get(level);

        Hcn dominator = findDominator(hcn, levelList);
        if (dominator != null) {
            notifyDefeated(hcn, dominator);
            return false;
        }

        int insertIndex = findInsertPositionByValue(levelList, hcn);
        levelList.add(insertIndex, hcn);

        removeEntriesDominatedBy(hcn, levelList, insertIndex + 1, level);

        return true;
    }

    private void insertIntoHigherLevels(Hcn hcn, int startLevel) {
        for (int level = startLevel + 1; levelMap.containsKey(level); level++) {
            ArrayList<Hcn> levelList = levelMap.get(level);

            Hcn dominator = findDominator(hcn, levelList);
            if (dominator != null) {
                break;
            }

            int insertIndex = findInsertPositionByValue(levelList, hcn);
            levelList.add(insertIndex, hcn);

            removeEntriesDominatedBy(hcn, levelList, insertIndex + 1, level);
        }
    }

    // --- Shared helpers ---

    private void ensureLevelExists(int level) {
        if (levelMap.containsKey(level)) {
            return;
        }

        levelMap.put(level, new ArrayList<>());

        Integer lowerLevel = levelMap.lowerKey(level);
        if (lowerLevel != null) {
            levelMap.get(level).addAll(levelMap.get(lowerLevel));
        }
    }

    private boolean isDominatedBy(Hcn hcn, Hcn existing) {
        return existing.getValue().isNotBiggerThan(hcn.getValue())
                && existing.getFactor().isNotSmallerThan(hcn.getFactor());
    }

    private Hcn findDominator(Hcn hcn, ArrayList<Hcn> levelList) {
        for (int i = levelList.size() - 1; i >= 0; i--) {
            Hcn existing = levelList.get(i);

            if (existing.getValue().isBiggerThan(hcn.getValue())) {
                continue;
            }

            if (existing.getFactor().isNotSmallerThan(hcn.getFactor())) {
                return existing;
            }

            break;
        }
        return null;
    }

    private int findInsertPositionByValue(ArrayList<Hcn> list, Hcn hcn) {
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getValue().isBiggerThan(hcn.getValue())) {
                return i;
            }
        }
        return list.size();
    }

    private void removeEntriesDominatedBy(Hcn hcn, ArrayList<Hcn> levelList, int startIndex, int level) {
        List<Hcn> toRemove = new ArrayList<>();
        for (int i = startIndex; i < levelList.size(); i++) {
            Hcn candidate = levelList.get(i);
            if (hcn.getFactor().isNotSmallerThan(candidate.getFactor())) {
                toRemove.add(candidate);
            } else {
                break;
            }
        }
        for (Hcn removed : toRemove) {
            levelList.remove(removed);
            notifyDefeated(removed, hcn);
        }
    }

    private void notifyDefeated(Hcn defeated, Hcn superior) {
        if (defeated.getBody().isDeactivated()) {
            return;
        }
        if (defeated.getBody().isProved()) {
            defeated.getBody().deactivateFromLists();
            defeated.getBody().getPip().getActivePrimeIndex().deactivateRecursive(defeated.getBody(), superior.getBody());
        } else {
            if (superior.getLastActivePrime() <= defeated.getLastActivePrime()) {
                defeated.getBody().deactivateFromLists();
                defeated.getBody().getPip().getActivePrimeIndex().deactivateRecursive(defeated.getBody(), superior.getBody());
            }
        }
    }
}
