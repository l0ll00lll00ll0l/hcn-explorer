package com.hcn.v3;

import org.junit.jupiter.api.Test;

public class DuplicationTest {
    
    @Test
    public void testP0_5_P1_5_Duplication() {
        Matrix matrix = new Matrix();
        
        // Prove enough HCNs to trigger the issue
        for (int i = 0; i < 50; i++) {
            System.out.println("\n=== Proving HCN #" + (i+1) + " ===");
            matrix.proveNextHcn();
        }
        
        // Check p1 body list for duplicates
        System.out.println("\n=== Checking p1 neverActivatedBodyList for duplicates ===");
        var neverActivated = matrix.getLastActivePrimeIndex().getParentActivePrimeIndex().getNeverActivatedBodyList();
        System.out.println("Total never activated bodies: " + neverActivated.size());
        
        for (int i = 0; i < neverActivated.size(); i++) {
            HcnBody body = neverActivated.get(i);
            System.out.println(i + ": " + body.parentChainString() + " v:" + body.getValue() + " f:" + body.getFactor());
        }
    }
}
