package com.hcn.v3;

import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ComparisonTest {
    
    @Test
    public void compareFirst142HcnsWithUtil() {
        // Generate 142 HCNs with v3
        com.hcn.v3.Matrix v3Matrix = new com.hcn.v3.Matrix();
        for (int i = 0; i < 142; i++) {
            v3Matrix.proveNextHcn();
        }
        List<com.hcn.v3.Hcn> v3Hcns = v3Matrix.getProvedHcnList();
        
        // Generate 144 HCNs with util (143 + 1 for p0^0)
        com.hcn.util.Matrix utilMatrix = com.hcn.util.Matrix.initializeMatrix();
        utilMatrix.proveMultipleHcns(144, 0, false, false);
        List<com.hcn.util.Hcn> utilHcns = utilMatrix.getProvedHcnList();
        
        // Skip first util HCN (p0^0 = 1)
        System.out.println("Skipping util #1: " + utilHcns.get(0).toString());
        
        // Compare counts
        System.out.println("\nV3 HCNs: " + v3Hcns.size());
        System.out.println("Util HCNs (after skipping p0^0): " + (utilHcns.size() - 1));
        assertEquals(142, v3Hcns.size(), "V3 should have 142 HCNs");
        
        // Compare each HCN by VALUE and DIVISOR COUNT
        System.out.println("\n=== Comparing HCNs by VALUE ===");
        int mismatches = 0;
        for (int i = 0; i < 142; i++) {
            com.hcn.v3.Hcn v3Hcn = v3Hcns.get(i);
            com.hcn.util.Hcn utilHcn = utilHcns.get(i + 1); // Skip first util HCN
            
            String v3Value = v3Hcn.getValue().toString();
            String utilValue = String.format("%.3e", utilHcn.getReferenceValue());
            
            String v3Divisors = v3Hcn.getFactor().toString();
            long utilDivisors = (long)utilHcn.getReferenceFactor();
            
            boolean valueMatch = v3Value.equals(utilValue);
            boolean divisorMatch = v3Divisors.equals(String.valueOf(utilDivisors));
            
            if (!valueMatch || !divisorMatch) {
                System.out.println("#" + (i+1) + " MISMATCH:");
                System.out.println("  V3:   " + v3Hcn.fullPrint());
                System.out.println("  Util: " + utilHcn.toString());
                System.out.println("  V3 value: " + v3Value + ", divisors: " + v3Divisors);
                System.out.println("  Util value: " + utilValue + ", divisors: " + utilDivisors);
                mismatches++;
            }
        }
        
        System.out.println("\n=== SUMMARY ===");
        System.out.println("Total mismatches: " + mismatches + " out of 142");
        System.out.println("Match rate: " + String.format("%.1f%%", (142 - mismatches) * 100.0 / 142));
        
        // Show next util HCN that v3 doesn't find
        System.out.println("\n=== NEXT UTIL HCN (143rd) ===");
        com.hcn.util.Hcn nextUtil = utilHcns.get(143);
        System.out.println("Util #143: " + nextUtil.toString());
        System.out.println("Value: " + String.format("%.3e", nextUtil.getReferenceValue()));
        System.out.println("Divisors: " + (long)nextUtil.getReferenceFactor());
    }
}
