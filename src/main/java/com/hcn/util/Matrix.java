package com.hcn.util;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

public class Matrix {
    private List<PrimeIndex> primeIndexes;
    private MatrixHcnSet hcnList;
    private final List<Hcn> provedHcnList = new ArrayList<>();

    public MatrixHcnSet getHcnList() {
        return hcnList;
    }
    
    public List<Hcn> getProvedHcnList() {
        return provedHcnList;
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
    
    public void addNextPrimeIndex() {
        PrimeIndex previousPrimeIndex = primeIndexes.get(primeIndexes.size() - 1);
        int lastIndex = previousPrimeIndex.getIndex();
        PrimeIndex newPrimeIndex = new PrimeIndex(lastIndex + 1);
        
        PrimeIndexPower newPower = newPrimeIndex.getPowers().get(0);
        
        for (Hcn oldHcn : previousPrimeIndex.getHcnList()) {
            int oldLastPower = oldHcn.getPowers().get(oldHcn.getPowers().size() - 1).getPower();
            if (oldLastPower < newPower.getPower() || (oldLastPower == 0 && newPower.getPower() == 0)) {
                continue;
            }
            Hcn newHcn = new Hcn();
            newHcn.getPowers().addAll(oldHcn.getPowers());
            newHcn.getPowers().add(newPower);
            newPrimeIndex.getHcnSet().add(newHcn);
            if (newPower.getPower() == 0) {
                hcnList.add(newHcn);
            }
        }
        
        primeIndexes.add(newPrimeIndex);

        System.out.println("new prime index " + newPrimeIndex.getIndex() + " added");
    }
    
    public void addNewPrimeIndexPower(PrimeIndex primeIndex) {
        PrimeIndexPower newPower = primeIndex.addNextPrimeIndexPower();

        System.out.println("new power added to p" + primeIndex.getIndex() + "^" + newPower.getPower());
        
        List<Hcn> incomingHcns = new ArrayList<>();
        
        if (primeIndex.getIndex() > 0) {
            PrimeIndex previousPrimeIndex = primeIndexes.get(primeIndex.getIndex() - 1);
            for (Hcn oldHcn : previousPrimeIndex.getHcnList()) {
                int lastPower = oldHcn.getPowers().get(oldHcn.getPowers().size() - 1).getPower();
                if (lastPower < newPower.getPower() || (lastPower == 0 && newPower.getPower() == 0)) {
                    continue;
                }
                Hcn newHcn = new Hcn();
                newHcn.getPowers().addAll(oldHcn.getPowers());
                newHcn.getPowers().add(newPower);
                incomingHcns.add(newHcn);
            }
        } else {
            Hcn newHcn = new Hcn();
            newHcn.getPowers().add(newPower);
            incomingHcns.add(newHcn);
        }

        primeIndex.getHcnSet().addAll(incomingHcns);
        
        for (Hcn hcn : incomingHcns) {
            if (hcn.getPowers().get(hcn.getPowers().size() - 1).getPower() == 0) {
                hcnList.add(hcn);
            }
        }
        
        propagateHcns(primeIndex.getIndex() + 1, incomingHcns);
    }
    
    private void propagateHcns(int startIndex, List<Hcn> incomingHcns) {
        if (startIndex >= primeIndexes.size() || incomingHcns.isEmpty()) {
            return;
        }
        
        PrimeIndex currentPrimeIndex = primeIndexes.get(startIndex);
        List<Hcn> newHcns = new ArrayList<>();
        
        for (Hcn incomingHcn : incomingHcns) {
            for (PrimeIndexPower power : currentPrimeIndex.getPowers()) {
                int lastPower = incomingHcn.getPowers().get(incomingHcn.getPowers().size() - 1).getPower();
                if (lastPower < power.getPower() || (lastPower == 0 && power.getPower() == 0)) {
                    continue;
                }
                Hcn newHcn = new Hcn();
                newHcn.getPowers().addAll(incomingHcn.getPowers());
                newHcn.getPowers().add(power);
                newHcns.add(newHcn);
            }
        }
        
        addHcnsToList(currentPrimeIndex, newHcns);
        propagateHcns(startIndex + 1, newHcns);
    }
    
    private void addHcnsToList(PrimeIndex primeIndex, List<Hcn> hcns) {
        for (Hcn hcn : hcns) {
            primeIndex.getHcnSet().add(hcn);
            if (hcn.getPowers().get(hcn.getPowers().size() - 1).getPower() == 0) {
                hcnList.add(hcn);
            }
        }
    }

    public void proveNextHcn() {
        Hcn provedRecordHcn = hcnList.first();
        provedHcnList.add(provedRecordHcn);
        provedRecordHcn.setProved();

        primeIndexPowerExtensionCheck();
        nextPrimeIndexExtensionCheck();
        
        hcnList.remove(provedRecordHcn);
        decrementHcnActiveCount(provedRecordHcn);
        
        removeInactivePowers();
    }
    
    public void proveMultipleHcns(int count) {
        for (int i = 0; i < count; i++) {
            proveNextHcn();
        }
    }
    
    private void removeInactivePowers() {
        this.primeIndexes.forEach(primeIndex -> primeIndex.removeInactivePowers(primeIndexes));
    }

    private void nextPrimeIndexExtensionCheck() {
        if (this.primeIndexes.get(this.primeIndexes.size() - 1).hasAnyProven()) {
            addNextPrimeIndex();
        }
    }

    private void primeIndexPowerExtensionCheck() {
        for (int i = 0; i < primeIndexes.size(); i++) {
            PrimeIndex primeIndex = primeIndexes.get(i);
            PrimeIndex previousPrimeIndex = i > 0 ? primeIndexes.get(i - 1) : null;
            if (primeIndex.isExtensionRequired(previousPrimeIndex)) {
                addNewPrimeIndexPower(primeIndex);
            }
        }
    }
}
