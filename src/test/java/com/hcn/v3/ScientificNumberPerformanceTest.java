package com.hcn.v3;

import java.util.Random;

public class ScientificNumberPerformanceTest {
    
    private static final int ITERATIONS = 10000;
    private static final long PRECISION1 = 10000000000000L;
    
    public static void main(String[] args) {
        // Generate random numbers once
        Random rand = new Random(42); // Fixed seed for reproducibility
        double[] randomValues = new double[ITERATIONS];
        for (int i = 0; i < ITERATIONS; i++) {
            randomValues[i] = 1.0 + rand.nextDouble() * 9.0; // 1.0 to 10.0
        }
        
        System.out.println("=== ScientificNumber Performance Test ===");
        System.out.println("Iterations: " + ITERATIONS);
        System.out.println();
        
        // Test Original
        testMethod("Original (Multiple divisions)", randomValues, 0);
        
        // Test Method 1: Math.log10
        testMethod("Method 1: Math.log10", randomValues, 1);
        
        // Test Method 2: Single division with pow
        testMethod("Method 2: Single division", randomValues, 2);
        
        // Test Method 4: Rounding at end
        testMethod("Method 4: Rounding", randomValues, 4);
    }
    
    private static void testMethod(String name, double[] randomValues, int method) {
        long startTime = System.nanoTime();
        
        TestScientificNumber result = new TestScientificNumber(1.0, 0, method);
        for (double value : randomValues) {
            TestScientificNumber multiplier = new TestScientificNumber(value, 0, method);
            result = result.multiply(multiplier, method);
        }
        
        long endTime = System.nanoTime();
        double durationMs = (endTime - startTime) / 1_000_000.0;
        
        System.out.println(name + ":");
        System.out.println("  Time: " + String.format("%.3f", durationMs) + " ms");
        System.out.println("  Result: " + result);
        System.out.println("  Mantissa: " + String.format("%.15f", result.getMantissa()));
        System.out.println("  Exponent: " + result.getExponent());
        System.out.println();
    }
    
    // Test class with all 4 normalize methods
    static class TestScientificNumber {
        private double mantissa;
        private long exponent;
        
        public TestScientificNumber(double mantissa, long exponent, int method) {
            this.mantissa = mantissa;
            this.exponent = exponent;
            normalize(method);
        }
        
        private void normalize(int method) {
            if (mantissa == 0) {
                exponent = 0;
                return;
            }
            
            switch (method) {
                case 0: // Original
                    normalizeOriginal();
                    break;
                case 1: // Math.log10
                    normalizeLog10();
                    break;
                case 2: // Single division
                    normalizeSingleDivision();
                    break;
                case 4: // Rounding
                    normalizeWithRounding();
                    break;
            }
        }
        
        private void normalizeOriginal() {
            while (Math.abs(mantissa) >= 10.0) {
                mantissa /= 10.0;
                exponent++;
            }
            
            while (Math.abs(mantissa) < 1.0 && mantissa != 0) {
                mantissa *= 10.0;
                exponent--;
            }
        }
        
        private void normalizeLog10() {
            int shift = (int) Math.floor(Math.log10(Math.abs(mantissa)));
            
            if (shift != 0) {
                mantissa /= Math.pow(10, shift);
                exponent += shift;
            }
        }
        
        private void normalizeSingleDivision() {
            double abs = Math.abs(mantissa);
            
            if (abs >= 10.0) {
                int shift = 0;
                while (abs >= 10.0) {
                    abs /= 10.0;
                    shift++;
                }
                mantissa /= Math.pow(10, shift);
                exponent += shift;
            } else if (abs < 1.0) {
                int shift = 0;
                while (abs < 1.0) {
                    abs *= 10.0;
                    shift++;
                }
                mantissa *= Math.pow(10, shift);
                exponent -= shift;
            }
        }
        
        private void normalizeWithRounding() {
            while (Math.abs(mantissa) >= 10.0) {
                mantissa /= 10.0;
                exponent++;
            }
            
            while (Math.abs(mantissa) < 1.0 && mantissa != 0) {
                mantissa *= 10.0;
                exponent--;
            }
            
            // Round to 13 decimal places
            mantissa = Math.round(mantissa * PRECISION1) / (double) PRECISION1;
        }
        
        public TestScientificNumber multiply(TestScientificNumber other, int method) {
            return new TestScientificNumber(this.mantissa * other.mantissa, 
                                           this.exponent + other.exponent, 
                                           method);
        }
        
        public double getMantissa() {
            return mantissa;
        }
        
        public long getExponent() {
            return exponent;
        }
        
        @Override
        public String toString() {
            return mantissa + "e" + exponent;
        }
    }
}
