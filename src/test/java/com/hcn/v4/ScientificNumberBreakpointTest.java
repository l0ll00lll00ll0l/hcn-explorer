package com.hcn.v4;

import org.junit.jupiter.api.Test;
import java.util.Random;

public class ScientificNumberBreakpointTest {

    /**
     * Start two ScientificNumbers with a tiny mantissa difference,
     * multiply both by the same random sequence, and find when compareTo stops returning 0.
     */
    @Test
    public void findBreakpointWithMinimalDivergence() {
        Random rand = new Random(42);

        // Start with two values that differ by the smallest possible amount
        double base = 5.0;
        double epsilon = 1e-15; // smallest meaningful diff for a double around 5.0

        ScientificNumber a = new ScientificNumber(base, 0);
        ScientificNumber b = new ScientificNumber(base + epsilon, 0);

        System.out.println("Starting mantissa A: " + String.format("%.20e", a.getMantissa()));
        System.out.println("Starting mantissa B: " + String.format("%.20e", b.getMantissa()));
        System.out.println("Starting diff:       " + String.format("%.20e", b.getMantissa() - a.getMantissa()));
        System.out.println("Starting compareTo:  " + a.compareTo(b));
        System.out.println();

        for (int i = 1; i <= 100000; i++) {
            int multiplier = 2 + rand.nextInt(14); // random small int 2-15 (like exponent+1)
            ScientificNumber m = new ScientificNumber(multiplier, 0);
            a = a.multiply(m);
            b = b.multiply(m);

            int cmp = a.compareTo(b);
            if (cmp != 0 && i <= 200 || i % 10000 == 0) {
                System.out.println("After " + i + " multiplications (last: *" + multiplier + "):");
                System.out.println("  A: " + String.format("%.20e", a.getMantissa()) + "e" + a.getExponent());
                System.out.println("  B: " + String.format("%.20e", b.getMantissa()) + "e" + b.getExponent());
                System.out.println("  diff: " + String.format("%.20e", b.getMantissa() - a.getMantissa()));
                System.out.println("  compareTo: " + cmp);
                if (cmp != 0) {
                    System.out.println("  >>> BROKEN at step " + i + " <<<");
                    break;
                }
            }
        }
    }

    /**
     * Same test but with divisions mixed in (like the real algorithm might do via FixedPowerGroup divide).
     */
    @Test
    public void findBreakpointWithMixedOps() {
        Random rand = new Random(42);

        double base = 5.0;
        double epsilon = 1e-15;

        ScientificNumber a = new ScientificNumber(base, 0);
        ScientificNumber b = new ScientificNumber(base + epsilon, 0);

        System.out.println("=== Mixed multiply/divide ===");
        System.out.println("Starting compareTo: " + a.compareTo(b));
        System.out.println();

        for (int i = 1; i <= 100000; i++) {
            int val = 2 + rand.nextInt(14);
            ScientificNumber m = new ScientificNumber(val, 0);

            if (rand.nextBoolean()) {
                a = a.multiply(m);
                b = b.multiply(m);
            } else {
                a = a.divide(m);
                b = b.divide(m);
            }

            int cmp = a.compareTo(b);
            if (cmp != 0) {
                System.out.println("After " + i + " operations:");
                System.out.println("  A: " + String.format("%.20e", a.getMantissa()) + "e" + a.getExponent());
                System.out.println("  B: " + String.format("%.20e", b.getMantissa()) + "e" + b.getExponent());
                System.out.println("  diff: " + String.format("%.20e", b.getMantissa() - a.getMantissa()));
                System.out.println("  compareTo: " + cmp);
                System.out.println("  >>> BROKEN at step " + i + " <<<");
                break;
            }

            if (i % 10000 == 0) {
                System.out.println("After " + i + " ops: diff=" + String.format("%.20e", b.getMantissa() - a.getMantissa()) + " compareTo=" + cmp);
            }
        }
    }

    /**
     * Test with NO initial divergence — two identical numbers multiplied by different
     * sequences that produce the same mathematical result.
     * This is the actual scenario: same starting point, different multiplication paths.
     */
    @Test
    public void findBreakpointDifferentPaths() {
        System.out.println("=== Different paths, same mathematical result ===");
        System.out.println();

        // Try increasingly long chains where we swap 9*2^3 for 8*3^2 at different points
        // 9 * 8 = 72,  but also 3^2 * 2^3 = 72
        // So: base * 9 * ... * 2 * 2 * 2  vs  base * 8 * ... * 3 * 3
        // with the same remaining multiplications

        for (int padLength = 0; padLength <= 200; padLength += 10) {
            ScientificNumber a = new ScientificNumber(1, 0);
            ScientificNumber b = new ScientificNumber(1, 0);

            // Shared prefix: multiply both by 2, padLength times
            for (int i = 0; i < padLength; i++) {
                a = a.multiply(new ScientificNumber(2, 0));
                b = b.multiply(new ScientificNumber(2, 0));
            }

            // Path A: * 9 * 2 * 2 * 2
            a = a.multiply(new ScientificNumber(9, 0));
            a = a.multiply(new ScientificNumber(2, 0));
            a = a.multiply(new ScientificNumber(2, 0));
            a = a.multiply(new ScientificNumber(2, 0));

            // Path B: * 8 * 3 * 3
            b = b.multiply(new ScientificNumber(8, 0));
            b = b.multiply(new ScientificNumber(3, 0));
            b = b.multiply(new ScientificNumber(3, 0));

            // Shared suffix: multiply both by 2, padLength times
            for (int i = 0; i < padLength; i++) {
                a = a.multiply(new ScientificNumber(2, 0));
                b = b.multiply(new ScientificNumber(2, 0));
            }

            int cmp = a.compareTo(b);
            double diff = b.getMantissa() - a.getMantissa();
            if (cmp != 0 || padLength % 50 == 0) {
                System.out.println("pad=" + padLength + " diff=" + String.format("%.20e", diff) + " compareTo=" + cmp
                        + (cmp != 0 ? " >>> BROKEN <<<" : ""));
            }
        }

        System.out.println();
        System.out.println("--- Now with varied multipliers (not just 2) ---");

        Random rand = new Random(123);
        for (int padLength = 0; padLength <= 500; padLength += 10) {
            // Reset random for reproducibility per padLength
            rand = new Random(123);

            ScientificNumber a = new ScientificNumber(1, 0);
            ScientificNumber b = new ScientificNumber(1, 0);

            // Shared prefix with varied multipliers
            for (int i = 0; i < padLength; i++) {
                int m = 2 + (rand.nextInt(14));
                ScientificNumber mult = new ScientificNumber(m, 0);
                a = a.multiply(mult);
                b = b.multiply(mult);
            }

            // Diverge: 9*2*2*2 vs 8*3*3 (both = 72)
            a = a.multiply(new ScientificNumber(9, 0));
            a = a.multiply(new ScientificNumber(2, 0));
            a = a.multiply(new ScientificNumber(2, 0));
            a = a.multiply(new ScientificNumber(2, 0));

            b = b.multiply(new ScientificNumber(8, 0));
            b = b.multiply(new ScientificNumber(3, 0));
            b = b.multiply(new ScientificNumber(3, 0));

            // Shared suffix with varied multipliers
            rand = new Random(456);
            for (int i = 0; i < padLength; i++) {
                int m = 2 + (rand.nextInt(14));
                ScientificNumber mult = new ScientificNumber(m, 0);
                a = a.multiply(mult);
                b = b.multiply(mult);
            }

            int cmp = a.compareTo(b);
            double diff = b.getMantissa() - a.getMantissa();
            if (cmp != 0 || padLength % 100 == 0) {
                System.out.println("pad=" + padLength + " diff=" + String.format("%.20e", diff) + " compareTo=" + cmp
                        + (cmp != 0 ? " >>> BROKEN <<<" : ""));
            }
        }
    }
}
