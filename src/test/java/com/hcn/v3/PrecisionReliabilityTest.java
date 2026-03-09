package com.hcn.v3;

import org.junit.jupiter.api.Test;
import java.util.Random;

public class PrecisionReliabilityTest {
    
    @Test
    public void testPrecisionReliability() {
        int[] precisions = {5, 13};
        int[] operationCounts = {50000};
        int trialsPerConfig = 100;
        
        System.out.println("Precision Reliability Test");
        System.out.println("===========================");
        System.out.println();
        
        // Header
        System.out.print("Precision\\Ops");
        for (int ops : operationCounts) {
            System.out.printf("%10d", ops);
        }
        System.out.println("    Time(ms)");
        System.out.println("-".repeat(80));
        
        for (int precision : precisions) {
            ScientificNumber.setPrecision(precision);
            System.out.printf("10^%-2d       ", precision);
            
            long startTime = System.currentTimeMillis();
            
            for (int n : operationCounts) {
                int successCount = 0;
                
                for (int trial = 0; trial < trialsPerConfig; trial++) {
                    if (testSingleCase(n)) {
                        successCount++;
                    }
                }
                
                double successRate = (successCount * 100.0) / trialsPerConfig;
                System.out.printf("%9.1f%%", successRate);
            }
            
            long endTime = System.currentTimeMillis();
            System.out.printf("    %6d", endTime - startTime);
            System.out.println();
        }
    }
    
    private boolean testSingleCase(int n) {
        Random rand = new Random();
        
        // Random starting number
        double startValue = 1000 + rand.nextDouble() * 9000;
        
        // Generate n random primes (0-100 index range)
        int[] primeIndices = new int[n];
        boolean[] isMultiply = new boolean[n];
        
        for (int i = 0; i < n; i++) {
            primeIndices[i] = rand.nextInt(100);
            isMultiply[i] = rand.nextBoolean();
        }
        
        // Forward pass
        ScientificNumber num1 = new ScientificNumber(startValue, 0);
        for (int i = 0; i < n; i++) {
            int prime = PrimeCenter.getPrime(primeIndices[i]);
            ScientificNumber primeNum = new ScientificNumber(prime, 0);
            
            if (isMultiply[i]) {
                num1 = num1.multiply(primeNum);
            } else {
                num1 = num1.divide(primeNum);
            }
        }
        
        // Backward pass (reverse order)
        ScientificNumber num2 = new ScientificNumber(startValue, 0);
        for (int i = n - 1; i >= 0; i--) {
            int prime = PrimeCenter.getPrime(primeIndices[i]);
            ScientificNumber primeNum = new ScientificNumber(prime, 0);
            
            if (isMultiply[i]) {
                num2 = num2.multiply(primeNum);
            } else {
                num2 = num2.divide(primeNum);
            }
        }
        
        // Check if they're still equal
        return num1.compareTo(num2) == 0;
    }
}
