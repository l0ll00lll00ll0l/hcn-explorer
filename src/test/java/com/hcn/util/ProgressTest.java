package com.hcn.util;

public class ProgressTest {
    public static void main(String[] args) {
        System.out.println("=== Test 1: Fresh start, 500 HCNs ===");
        Matrix matrix1 = Matrix.initializeMatrix();
        matrix1.proveMultipleHcns(500, 50);
        System.out.println("Total time: " + matrix1.getLastExecutionTimeMs() + " ms");
        System.out.println("Progress data points: " + matrix1.getProgressData().size());
        for (long[] point : matrix1.getProgressData()) {
            System.out.println("  Time: " + point[0] + " ms, Count: " + point[1]);
        }
        
        System.out.println("\n=== Test 2: Same matrix, 500 more HCNs ===");
        matrix1.proveMultipleHcns(500, 50);
        System.out.println("Total time: " + matrix1.getLastExecutionTimeMs() + " ms");
        System.out.println("Progress data points: " + matrix1.getProgressData().size());
        for (long[] point : matrix1.getProgressData()) {
            System.out.println("  Time: " + point[0] + " ms, Count: " + point[1]);
        }
        
        System.out.println("\n=== Test 3: Fresh start, 1000 HCNs ===");
        Matrix matrix2 = Matrix.initializeMatrix();
        matrix2.proveMultipleHcns(1000, 50);
        System.out.println("Total time: " + matrix2.getLastExecutionTimeMs() + " ms");
        System.out.println("Progress data points: " + matrix2.getProgressData().size());
        for (long[] point : matrix2.getProgressData()) {
            System.out.println("  Time: " + point[0] + " ms, Count: " + point[1]);
        }
    }
}
