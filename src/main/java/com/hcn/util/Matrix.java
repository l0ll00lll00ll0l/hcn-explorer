package com.hcn.util;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

public class Matrix {
    private List<PrimeIndex> primeIndexes;
    private MatrixHcnSet hcnList;
    private final List<Hcn> provedHcnList = new ArrayList<>();
    private long lastExecutionTimeMs = 0;
    private List<long[]> progressData = new ArrayList<>();
    private List<ExtensionEvent> extensionEvents = new ArrayList<>();

    public MatrixHcnSet getHcnList() {
        return hcnList;
    }
    
    public List<Hcn> getProvedHcnList() {
        return provedHcnList;
    }
    
    public long getLastExecutionTimeMs() {
        return lastExecutionTimeMs;
    }
    
    public List<long[]> getProgressData() {
        return progressData;
    }
    
    public List<ExtensionEvent> getExtensionEvents() {
        return extensionEvents;
    }
    
    private void decrementHcnActiveCount(Hcn hcn) {
        for (PrimeIndexPower power : hcn.getPowers()) {
            power.decrementActiveCount();
        }
    }
    
    private Matrix() {
        this.primeIndexes = new ArrayList<>();
        this.hcnList = new MatrixHcnSet();
    }
    
    public static Matrix initializeMatrix() {
        Matrix matrix = new Matrix();
        
        PrimeIndex p0 = PrimeIndex.initializePrimeIndex();
        matrix.primeIndexes.add(p0);
        matrix.hcnList.addAll(p0.getHcnList());
        
        return matrix;
    }
    
    public List<PrimeIndex> getPrimeIndexes() {
        return primeIndexes;
    }
    
    public void setPrimeIndexes(List<PrimeIndex> primeIndexes) {
        this.primeIndexes = primeIndexes;
    }
    
    public int addNextPrimeIndex() {
        PrimeIndex previousPrimeIndex = primeIndexes.get(primeIndexes.size() - 1);
        int lastIndex = previousPrimeIndex.getIndex();
        PrimeIndex newPrimeIndex = new PrimeIndex(lastIndex + 1);
        
        PrimeIndexPower newPower = newPrimeIndex.getPowers().get(0);
        
        for (Hcn oldHcn : previousPrimeIndex.getHcnList()) {
            int oldLastPower = oldHcn.getOwnPower().getPower();
            if (oldLastPower < newPower.getPower() || (oldLastPower == 0 && newPower.getPower() == 0)) {
                continue;
            }
            Hcn newHcn = new Hcn(oldHcn, newPower);
            if (newPower.getPower() == 0) {
                hcnList.add(newHcn);
            } else {
                newPrimeIndex.getHcnSet().add(newHcn);
            }
        }
        
        primeIndexes.add(newPrimeIndex);
        return newPrimeIndex.getIndex();
    }
    
    public int[] addNewPrimeIndexPower(PrimeIndex primeIndex) {
        PrimeIndexPower newPower = primeIndex.addNextPrimeIndexPower();
        int powerValue = newPower.getPower();
        int primeIndexValue = primeIndex.getIndex();
        
        List<Hcn> incomingHcns = new ArrayList<>();
        
        if (primeIndex.getIndex() > 0) {
            PrimeIndex previousPrimeIndex = primeIndexes.get(primeIndex.getIndex() - 1);
            for (Hcn oldHcn : previousPrimeIndex.getHcnList()) {
                int lastPower = oldHcn.getOwnPower().getPower();
                if (lastPower < newPower.getPower() || (lastPower == 0 && newPower.getPower() == 0)) {
                    continue;
                }
                Hcn newHcn = new Hcn(oldHcn, newPower);
                incomingHcns.add(newHcn);
            }
        } else {
            Hcn newHcn = new Hcn(null, newPower);
            incomingHcns.add(newHcn);
        }

        primeIndex.getHcnSet().addAll(incomingHcns);
        
        for (Hcn hcn : incomingHcns) {
            if (hcn.getOwnPower().getPower() == 0) {
                hcnList.add(hcn);
            }
        }
        
        propagateHcns(primeIndex.getIndex() + 1, incomingHcns);
        return new int[]{primeIndexValue, powerValue};
    }
    
    private void propagateHcns(int startIndex, List<Hcn> incomingHcns) {
        if (startIndex >= primeIndexes.size() || incomingHcns.isEmpty()) {
            return;
        }
        
        PrimeIndex currentPrimeIndex = primeIndexes.get(startIndex);
        List<Hcn> newHcns = new ArrayList<>();
        
        for (Hcn incomingHcn : incomingHcns) {
            for (PrimeIndexPower power : currentPrimeIndex.getPowers()) {
                int lastPower = incomingHcn.getOwnPower().getPower();
                if (lastPower < power.getPower() || (lastPower == 0 && power.getPower() == 0)) {
                    continue;
                }
                Hcn newHcn = new Hcn(incomingHcn, power);
                newHcns.add(newHcn);
            }
        }
        
        addHcnsToList(currentPrimeIndex, newHcns);
        propagateHcns(startIndex + 1, newHcns);
    }
    
    private void addHcnsToList(PrimeIndex primeIndex, List<Hcn> hcns) {
        for (Hcn hcn : hcns) {
            if (hcn.getOwnPower().getPower() == 0) {
                hcnList.add(hcn);
            } else {
                primeIndex.getHcnSet().add(hcn);
            }
        }
    }

    private void proveNextHcn(long startTime, boolean trackNextPrime, boolean trackPowerExtension) {
        Hcn provedRecordHcn = hcnList.first();
        provedHcnList.add(provedRecordHcn);
        provedRecordHcn.setProved();

        primeIndexPowerExtensionCheck(startTime, trackPowerExtension);
        nextPrimeIndexExtensionCheck(startTime, trackNextPrime);
        
        hcnList.remove(provedRecordHcn);
        decrementHcnActiveCount(provedRecordHcn);
        
        removeInactivePowers();
    }
    
    public void proveMultipleHcns(int count, int sampleInterval, boolean trackNextPrime, boolean trackPowerExtension) {
        progressData.clear();
        extensionEvents.clear();
        long startTime = System.currentTimeMillis();
        int startCount = provedHcnList.size();
        
        for (int i = 0; i < count; i++) {
            proveNextHcn(startTime, trackNextPrime, trackPowerExtension);
            if (sampleInterval > 0 && (i + 1) % sampleInterval == 0) {
                long elapsed = System.currentTimeMillis() - startTime;
                int currentCount = provedHcnList.size() - startCount;
                progressData.add(new long[]{elapsed, currentCount});
            }
        }
        
        lastExecutionTimeMs = System.currentTimeMillis() - startTime;
        if (sampleInterval > 0) {
            int finalCount = provedHcnList.size() - startCount;
            if (progressData.isEmpty() || progressData.get(progressData.size() - 1)[1] != finalCount) {
                progressData.add(new long[]{lastExecutionTimeMs, finalCount});
            }
        }
    }
    
    private void removeInactivePowers() {
        this.primeIndexes.forEach(primeIndex -> primeIndex.removeInactivePowers(primeIndexes));
    }

    private void nextPrimeIndexExtensionCheck(long startTime, boolean track) {
        if (this.primeIndexes.get(this.primeIndexes.size() - 1).hasAnyProven()) {
            int newIndex = addNextPrimeIndex();
            if (track) {
                long elapsed = System.currentTimeMillis() - startTime;
                extensionEvents.add(new ExtensionEvent(elapsed, "nextPrime", "p" + newIndex));
            }
        }
    }

    private void primeIndexPowerExtensionCheck(long startTime, boolean track) {
        for (int i = 0; i < primeIndexes.size(); i++) {
            PrimeIndex primeIndex = primeIndexes.get(i);
            PrimeIndex previousPrimeIndex = i > 0 ? primeIndexes.get(i - 1) : null;
            if (primeIndex.isExtensionRequired(previousPrimeIndex)) {
                int[] result = addNewPrimeIndexPower(primeIndex);
                if (track) {
                    long elapsed = System.currentTimeMillis() - startTime;
                    extensionEvents.add(new ExtensionEvent(elapsed, "powerExtension", "p" + result[0] + "^" + result[1]));
                }
            }
        }
    }
}
