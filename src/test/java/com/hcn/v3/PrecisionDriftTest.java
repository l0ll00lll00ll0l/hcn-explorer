package com.hcn.v3;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class PrecisionDriftTest {
    
    public static void main(String[] args) {
        // Generate 50000 random primes between 2 and 100
        List<Integer> primes = generatePrimes(100);
        Random rand = new Random(42);
        List<Integer> randomPrimes = new ArrayList<>();
        
        for (int i = 0; i < 50000; i++) {
            randomPrimes.add(primes.get(rand.nextInt(primes.size())));
        }
        
        System.out.println("=== Precision Drift Test ===");
        System.out.println("Multiplying by " + randomPrimes.size() + " random primes (2-100)");
        System.out.println();
        
        // Test with exact value 4.8
        double exact = 4.8;
        for (int prime : randomPrimes) {
            exact *= prime;
        }
        
        // Test with imprecise value 4.800000000000001
        double imprecise = 4.800000000000001;
        for (int prime : randomPrimes) {
            imprecise *= prime;
        }
        
        System.out.println("Exact (4.8):     " + exact);
        System.out.println("Imprecise (4.8+ε): " + imprecise);
        System.out.println();
        System.out.println("Difference: " + (imprecise - exact));
        System.out.println("Relative error: " + ((imprecise - exact) / exact * 100) + "%");
        System.out.println();
        
        // Now test with ScientificNumber
        System.out.println("=== ScientificNumber Test ===");
        
        ScientificNumber sn1 = new ScientificNumber(4.8, 0);
        ScientificNumber sn2 = new ScientificNumber(4.800000000000001, 0);
        
        for (int prime : randomPrimes) {
            ScientificNumber multiplier = new ScientificNumber(prime, 0);
            sn1 = sn1.multiply(multiplier);
            sn2 = sn2.multiply(multiplier);
        }
        
        System.out.println("SN1 (4.8):     " + sn1);
        System.out.println("SN2 (4.8+ε):   " + sn2);
        System.out.println();
        System.out.println("Mantissa difference: " + (sn2.getMantissa() - sn1.getMantissa()));
        System.out.println("Exponent difference: " + (sn2.getExponent() - sn1.getExponent()));
        System.out.println();
        
        // Test compareTo
        int compareResult = sn1.compareTo(sn2);
        System.out.println("compareTo result: " + compareResult);
        if (compareResult == 0) {
            System.out.println("✓ PRECISION works: Values are considered EQUAL");
        } else if (compareResult < 0) {
            System.out.println("✗ SN1 < SN2 (difference detected)");
        } else {
            System.out.println("✗ SN1 > SN2 (difference detected)");
        }
        
        // Show precision levels
        System.out.println();
        System.out.println("=== Precision Analysis ===");
        System.out.println("PRECISION1 (10^13): " + ScientificNumber.class.getDeclaredFields()[0].getName());
        System.out.println("PRECISION2 (10^14): " + ScientificNumber.class.getDeclaredFields()[1].getName());
        
        double m1 = sn1.getMantissa();
        double m2 = sn2.getMantissa();
        long p1 = 10000000000000L;
        long p2 = 100000000000000L;
        
        double m1_p1 = Math.round(m1 * p1) / (double) p1;
        double m2_p1 = Math.round(m2 * p1) / (double) p1;
        double m1_p2 = Math.round(m1 * p2) / (double) p2;
        double m2_p2 = Math.round(m2 * p2) / (double) p2;
        
        System.out.println();
        System.out.println("At PRECISION1 (10^13):");
        System.out.println("  SN1: " + String.format("%.15f", m1_p1));
        System.out.println("  SN2: " + String.format("%.15f", m2_p1));
        System.out.println("  Equal: " + (m1_p1 == m2_p1));
        
        System.out.println();
        System.out.println("At PRECISION2 (10^14):");
        System.out.println("  SN1: " + String.format("%.15f", m1_p2));
        System.out.println("  SN2: " + String.format("%.15f", m2_p2));
        System.out.println("  Equal: " + (m1_p2 == m2_p2));
    }
    
    private static List<Integer> generatePrimes(int max) {
        List<Integer> primes = new ArrayList<>();
        for (int i = 2; i <= max; i++) {
            if (isPrime(i)) {
                primes.add(i);
            }
        }
        return primes;
    }
    
    private static boolean isPrime(int n) {
        if (n < 2) return false;
        if (n == 2) return true;
        if (n % 2 == 0) return false;
        for (int i = 3; i * i <= n; i += 2) {
            if (n % i == 0) return false;
        }
        return true;
    }
}
