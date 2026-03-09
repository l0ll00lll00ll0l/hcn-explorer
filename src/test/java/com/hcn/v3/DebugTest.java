package com.hcn.v3;

public class DebugTest {
    public static void main(String[] args) {
        // Find the failing case
        for(int i=48; i<100; i++) {
            for (int j=3; j<i; j++ ) {
                ScientificNumber num = new ScientificNumber(i, 1);
                ScientificNumber num2 = new ScientificNumber(i, 1)
                    .divide(new ScientificNumber(j, 1))
                    .multiply(new ScientificNumber(j, 1));
                
                if (num.compareTo(num2) != 0) {
                    System.out.println("FAIL: i=" + i + ", j=" + j);
                    System.out.println("num1: " + num + " mantissa=" + num.getMantissa());
                    System.out.println("num2: " + num2 + " mantissa=" + num2.getMantissa());
                    System.out.println("diff: " + (num.getMantissa() - num2.getMantissa()));
                    return;
                }
            }
        }
        System.out.println("All passed!");
    }
}
