package com.hcn.v4;

import org.junit.jupiter.api.Test;

/**
 * Test that reproduces the exact ScientificNumber factor computation
 * for two HCNs that should have identical factors (7.310e33).
 *
 * HCN A: [14, 8, 5, 4, 3, 3, 2, 2, 2, 2, 2, 2, 1*87] |98  (the correct one)
 * HCN B: [14, 7, 5, 4, 3, 3, 2, 2, 2, 2, 2, 2, 2, 2, 1*84] |97  (the wrong one that slipped through)
 *
 * Factor = product of (exponent + 1) for each prime.
 * A: 15 * 9 * 6 * 5 * 4 * 4 * 3^6 * 2^87
 * B: 15 * 8 * 6 * 5 * 4 * 4 * 3^8 * 2^84
 *
 * Both should equal 7309912137976241985251838630297600
 */
public class ScientificNumberFactorTest {

    @Test
    public void testDivergingFromSharedParent() {
        // Both bodies share a common factor base up to some point.
        // Then they diverge with different multiplications that produce the same mathematical result.
        // The diverging part: 9 * 2 * 2 = 36  vs  8 * 3 * 3 = 72... wait, those aren't equal.
        // Let me find the actual divergence.
        //
        // A: 15 * 9 * 6 * 5 * 4 * 4 * 3^6 * 2^87
        // B: 15 * 8 * 6 * 5 * 4 * 4 * 3^8 * 2^84
        // Shared: 15 * _ * 6 * 5 * 4 * 4 (same for both)
        // A divergent: 9 * 3^6 * 2^87 = 3^8 * 2^87
        // B divergent: 8 * 3^8 * 2^84 = 2^3 * 3^8 * 2^84 = 3^8 * 2^87
        // So the divergent parts are mathematically identical!
        //
        // But they're computed differently:
        // A: multiply by 9, then 3 six times, then 2 eighty-seven times
        // B: multiply by 8, then 3 eight times, then 2 eighty-four times

        // Start from a shared base
        ScientificNumber shared = new ScientificNumber(15, 0);
        // multiply by some common stuff to simulate a realistic base
        shared = shared.multiply(new ScientificNumber(6, 0));
        shared = shared.multiply(new ScientificNumber(5, 0));
        shared = shared.multiply(new ScientificNumber(4, 0));
        shared = shared.multiply(new ScientificNumber(4, 0));

        // Path A diverges: * 9 * 3^6 * 2^87
        ScientificNumber pathA = shared.multiply(new ScientificNumber(9, 0));
        for (int i = 0; i < 6; i++) pathA = pathA.multiply(new ScientificNumber(3, 0));
        for (int i = 0; i < 87; i++) pathA = pathA.multiply(new ScientificNumber(2, 0));

        // Path B diverges: * 8 * 3^8 * 2^84
        ScientificNumber pathB = shared.multiply(new ScientificNumber(8, 0));
        for (int i = 0; i < 8; i++) pathB = pathB.multiply(new ScientificNumber(3, 0));
        for (int i = 0; i < 84; i++) pathB = pathB.multiply(new ScientificNumber(2, 0));

        System.out.println("=== Diverging from shared parent ===");
        System.out.println("Path A mantissa: " + String.format("%.20e", pathA.getMantissa()) + " exp: " + pathA.getExponent());
        System.out.println("Path B mantissa: " + String.format("%.20e", pathB.getMantissa()) + " exp: " + pathB.getExponent());
        System.out.println("Mantissa diff:   " + String.format("%.20e", pathB.getMantissa() - pathA.getMantissa()));
        System.out.println("compareTo:       " + pathA.compareTo(pathB));
        System.out.println();

        // Minimal divergence test: just the diverging part, no shared base
        ScientificNumber divA = new ScientificNumber(9, 0);
        for (int i = 0; i < 6; i++) divA = divA.multiply(new ScientificNumber(3, 0));
        for (int i = 0; i < 87; i++) divA = divA.multiply(new ScientificNumber(2, 0));

        ScientificNumber divB = new ScientificNumber(8, 0);
        for (int i = 0; i < 8; i++) divB = divB.multiply(new ScientificNumber(3, 0));
        for (int i = 0; i < 84; i++) divB = divB.multiply(new ScientificNumber(2, 0));

        System.out.println("=== Just the diverging part ===");
        System.out.println("Div A mantissa: " + String.format("%.20e", divA.getMantissa()) + " exp: " + divA.getExponent());
        System.out.println("Div B mantissa: " + String.format("%.20e", divB.getMantissa()) + " exp: " + divB.getExponent());
        System.out.println("Mantissa diff:  " + String.format("%.20e", divB.getMantissa() - divA.getMantissa()));
        System.out.println("compareTo:      " + divA.compareTo(divB));
        System.out.println();

        // Even more minimal: does 9 * 3^6 == 8 * 3^8 / 3^2... no.
        // 9 * 3^6 = 3^2 * 3^6 = 3^8 = 6561
        // 8 * 3^8 = 8 * 6561 = 52488  -- NOT the same!
        // The equality comes from including the 2s:
        // 9 * 3^6 * 2^87 = 3^8 * 2^87
        // 8 * 3^8 * 2^84 = 2^3 * 3^8 * 2^84 = 3^8 * 2^87
        // So the key operation: does multiplying by 2 eighty-seven times
        // give the same result as multiplying by 2 eighty-four times then by 8?

        ScientificNumber twoTo87 = new ScientificNumber(1, 0);
        for (int i = 0; i < 87; i++) twoTo87 = twoTo87.multiply(new ScientificNumber(2, 0));

        ScientificNumber twoTo84times8 = new ScientificNumber(1, 0);
        for (int i = 0; i < 84; i++) twoTo84times8 = twoTo84times8.multiply(new ScientificNumber(2, 0));
        twoTo84times8 = twoTo84times8.multiply(new ScientificNumber(8, 0));

        System.out.println("=== Core question: 2^87 vs 2^84 * 8 ===");
        System.out.println("2^87     mantissa: " + String.format("%.20e", twoTo87.getMantissa()) + " exp: " + twoTo87.getExponent());
        System.out.println("2^84 * 8 mantissa: " + String.format("%.20e", twoTo84times8.getMantissa()) + " exp: " + twoTo84times8.getExponent());
        System.out.println("compareTo:         " + twoTo87.compareTo(twoTo84times8));
        System.out.println();

        // And: 9 * 3^6 vs 3^8
        ScientificNumber nineTimesThreeTo6 = new ScientificNumber(9, 0);
        for (int i = 0; i < 6; i++) nineTimesThreeTo6 = nineTimesThreeTo6.multiply(new ScientificNumber(3, 0));

        ScientificNumber threeTo8 = new ScientificNumber(1, 0);
        for (int i = 0; i < 8; i++) threeTo8 = threeTo8.multiply(new ScientificNumber(3, 0));

        System.out.println("=== 9 * 3^6 vs 3^8 ===");
        System.out.println("9*3^6 mantissa: " + String.format("%.20e", nineTimesThreeTo6.getMantissa()) + " exp: " + nineTimesThreeTo6.getExponent());
        System.out.println("3^8   mantissa: " + String.format("%.20e", threeTo8.getMantissa()) + " exp: " + threeTo8.getExponent());
        System.out.println("compareTo:      " + nineTimesThreeTo6.compareTo(threeTo8));
    }

    @Test
    public void testExactFactorPaths() {
        // Path A: exponents [14, 8, 5, 4, 3, 3, 2, 2, 2, 2, 2, 2] + 87 primes with exponent 1
        // Factor multipliers: (14+1)=15, (8+1)=9, (5+1)=6, (4+1)=5, (3+1)=4, (3+1)=4, (2+1)=3 x6, (1+1)=2 x87
        ScientificNumber factorA = new ScientificNumber(1, 0);
        factorA = factorA.multiply(new ScientificNumber(15, 0));
        factorA = factorA.multiply(new ScientificNumber(9, 0));
        factorA = factorA.multiply(new ScientificNumber(6, 0));
        factorA = factorA.multiply(new ScientificNumber(5, 0));
        factorA = factorA.multiply(new ScientificNumber(4, 0));
        factorA = factorA.multiply(new ScientificNumber(4, 0));
        for (int i = 0; i < 6; i++) factorA = factorA.multiply(new ScientificNumber(3, 0));
        for (int i = 0; i < 87; i++) factorA = factorA.multiply(new ScientificNumber(2, 0));

        // Path B: exponents [14, 7, 5, 4, 3, 3, 2, 2, 2, 2, 2, 2, 2, 2] + 84 primes with exponent 1
        // Factor multipliers: 15, 8, 6, 5, 4, 4, 3 x8, 2 x84
        ScientificNumber factorB = new ScientificNumber(1, 0);
        factorB = factorB.multiply(new ScientificNumber(15, 0));
        factorB = factorB.multiply(new ScientificNumber(8, 0));
        factorB = factorB.multiply(new ScientificNumber(6, 0));
        factorB = factorB.multiply(new ScientificNumber(5, 0));
        factorB = factorB.multiply(new ScientificNumber(4, 0));
        factorB = factorB.multiply(new ScientificNumber(4, 0));
        for (int i = 0; i < 8; i++) factorB = factorB.multiply(new ScientificNumber(3, 0));
        for (int i = 0; i < 84; i++) factorB = factorB.multiply(new ScientificNumber(2, 0));

        System.out.println("Factor A mantissa: " + String.format("%.20e", factorA.getMantissa()) + " exp: " + factorA.getExponent());
        System.out.println("Factor B mantissa: " + String.format("%.20e", factorB.getMantissa()) + " exp: " + factorB.getExponent());
        System.out.println("Mantissa diff:     " + String.format("%.20e", factorB.getMantissa() - factorA.getMantissa()));
        System.out.println("compareTo result:  " + factorA.compareTo(factorB));
        System.out.println();

        // Now simulate how the algorithm actually builds factors:
        // It doesn't start from 1 and multiply all — it inherits from parent body.
        // Body chain builds incrementally: each body multiplies parent's factor by (power+1).
        // But the FixedPowerGroup collapses some primes, so the multiplication
        // happens in a different order/grouping.

        // Let's also test: does the order of multiplication matter?
        // Same factors as A, but multiplied in reverse order
        ScientificNumber factorA_reversed = new ScientificNumber(1, 0);
        for (int i = 0; i < 87; i++) factorA_reversed = factorA_reversed.multiply(new ScientificNumber(2, 0));
        for (int i = 0; i < 6; i++) factorA_reversed = factorA_reversed.multiply(new ScientificNumber(3, 0));
        factorA_reversed = factorA_reversed.multiply(new ScientificNumber(4, 0));
        factorA_reversed = factorA_reversed.multiply(new ScientificNumber(4, 0));
        factorA_reversed = factorA_reversed.multiply(new ScientificNumber(5, 0));
        factorA_reversed = factorA_reversed.multiply(new ScientificNumber(6, 0));
        factorA_reversed = factorA_reversed.multiply(new ScientificNumber(9, 0));
        factorA_reversed = factorA_reversed.multiply(new ScientificNumber(15, 0));

        System.out.println("Factor A reversed mantissa: " + String.format("%.20e", factorA_reversed.getMantissa()) + " exp: " + factorA_reversed.getExponent());
        System.out.println("A vs A_reversed compareTo:  " + factorA.compareTo(factorA_reversed));
        System.out.println("A vs B compareTo:           " + factorA.compareTo(factorB));
        System.out.println();

        // The real algorithm builds incrementally from the body chain.
        // Body A chain: p0^14 -> p1^8 -> p2^5 -> p3^4 -> p4^3 -> p5^3 -> p6^2 -> (FPG: p7^2..p10^2) -> p11^2 -> p12^1 -> p13^1 -> p14^1
        // Body B chain: p0^14 -> p1^7 -> p2^5 -> p3^4 -> p4^3 -> p5^3 -> p6^2 -> (FPG: p7^2..p10^2) -> p11^2 -> p12^2 -> p13^2 -> p14^1
        //
        // Factor A is built as: 15 * 9 * 6 * 5 * 4 * 4 * 3 * (FPG: 3^4) * 3 * 2 * 2 * 2 * (factory: 2^84)
        // Factor B is built as: 15 * 8 * 6 * 5 * 4 * 4 * 3 * (FPG: 3^4) * 3 * 3 * 3 * 2 * (factory: 2^84)
        //
        // But FPG multiplies as a single batch, not individual multiplies.
        // Let's simulate that:

        System.out.println("=== Simulating actual algorithm build order ===");

        // Path A: body chain builds factor incrementally, then FPG applied as batch, then factory
        ScientificNumber algA = new ScientificNumber(15, 0);  // p0^14
        algA = algA.multiply(new ScientificNumber(9, 0));     // p1^8
        algA = algA.multiply(new ScientificNumber(6, 0));     // p2^5
        algA = algA.multiply(new ScientificNumber(5, 0));     // p3^4
        algA = algA.multiply(new ScientificNumber(4, 0));     // p4^3
        algA = algA.multiply(new ScientificNumber(4, 0));     // p5^3
        algA = algA.multiply(new ScientificNumber(3, 0));     // p6^2
        // FPG for p7^2..p10^2: factor multiplier = 3^4 = 81, applied as single multiply
        algA = algA.multiply(new ScientificNumber(81, 0));
        algA = algA.multiply(new ScientificNumber(3, 0));     // p11^2
        algA = algA.multiply(new ScientificNumber(2, 0));     // p12^1
        algA = algA.multiply(new ScientificNumber(2, 0));     // p13^1
        algA = algA.multiply(new ScientificNumber(2, 0));     // p14^1
        // Factory: 84 more primes with exponent 1
        for (int i = 0; i < 84; i++) algA = algA.multiply(new ScientificNumber(2, 0));

        // Path B: body chain builds factor incrementally, then FPG applied as batch, then factory
        ScientificNumber algB = new ScientificNumber(15, 0);  // p0^14
        algB = algB.multiply(new ScientificNumber(8, 0));     // p1^7
        algB = algB.multiply(new ScientificNumber(6, 0));     // p2^5
        algB = algB.multiply(new ScientificNumber(5, 0));     // p3^4
        algB = algB.multiply(new ScientificNumber(4, 0));     // p4^3
        algB = algB.multiply(new ScientificNumber(4, 0));     // p5^3
        algB = algB.multiply(new ScientificNumber(3, 0));     // p6^2
        // FPG for p7^2..p10^2: same batch
        algB = algB.multiply(new ScientificNumber(81, 0));
        algB = algB.multiply(new ScientificNumber(3, 0));     // p11^2
        algB = algB.multiply(new ScientificNumber(3, 0));     // p12^2
        algB = algB.multiply(new ScientificNumber(3, 0));     // p13^2
        algB = algB.multiply(new ScientificNumber(2, 0));     // p14^1
        // Factory: 84 more primes with exponent 1
        for (int i = 0; i < 84; i++) algB = algB.multiply(new ScientificNumber(2, 0));

        System.out.println("Alg A mantissa: " + String.format("%.20e", algA.getMantissa()) + " exp: " + algA.getExponent());
        System.out.println("Alg B mantissa: " + String.format("%.20e", algB.getMantissa()) + " exp: " + algB.getExponent());
        System.out.println("Mantissa diff:  " + String.format("%.20e", algB.getMantissa() - algA.getMantissa()));
        System.out.println("compareTo:      " + algA.compareTo(algB));
    }
}
