package com.hcn.core;

import org.junit.jupiter.api.Test;
import java.util.List;

public class TXTcheckTest {

    @Test
    public void findFirstMismatchAgainstReference() {

        TXTcheck txt = new TXTcheck();
        List<int[]> ref = txt.getReferenceHcns();
        int count = ref.size();

        Matrix matrix = new Matrix();
        matrix.initialize();
        long batchStart = System.currentTimeMillis();

        for (int i = 0; i < count; i++) {
            Hcn provedHcn = matrix.proveNextSuperior();
            int[] fullExpected = ref.get(i);
            int[] activeIndexes = TXTcheck.getActiveIndexes(provedHcn);
            if (activeIndexes.length == 0) continue;
            int[] expected = TXTcheck.referenceAtActiveIndexes(fullExpected, activeIndexes);
            int[] actual = TXTcheck.exponentSignature(provedHcn, matrix.getLastActivePrimeIndex());

            if (!TXTcheck.signaturesEqual(expected, actual)) {
                System.out.println("FIRST MISMATCH at HCN #" + (i + 1));
                System.out.println("  Expected: " + TXTcheck.signatureToString(expected));
                System.out.println("  Actual:   " + TXTcheck.signatureToString(actual));
                System.out.println("  Indexes:  " + TXTcheck.signatureToString(activeIndexes));
                System.out.println("  FullRef:  " + TXTcheck.signatureToString(fullExpected));
                System.out.println("  Hcn:      " + provedHcn.fullPrint());
                return;
            }

            if ((i + 1) % 1000 == 0) {
                long elapsed = System.currentTimeMillis() - batchStart;
                System.out.println("Checked " + (i + 1) + "/" + count + " OK (" + elapsed + " ms)");
                batchStart = System.currentTimeMillis();
            }
        }
        System.out.println("All " + count + " HCNs match the reference!");


    }
}
