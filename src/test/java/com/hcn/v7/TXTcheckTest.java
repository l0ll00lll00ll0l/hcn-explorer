package com.hcn.v7;

import org.junit.jupiter.api.Test;
import java.util.List;

public class TXTcheckTest {

    @Test
    public void findFirstMismatchAgainstReference() {
        TXTcheck txt = new TXTcheck();
        List<int[]> ref = txt.getReferenceHcns();
        int count = ref.size() - 1; // skip ref[0] which is HCN=1

        HcnGenerator hcnGenerator = new HcnGenerator();
        hcnGenerator.initialize();
        long batchStart = System.currentTimeMillis();

        for (int i = 0; i < count; i++) {
            Hcn provedHcn = hcnGenerator.proveNextSuperior();
            int[] fullExpected = ref.get(i + 1);
            int[] activeIndexes = TXTcheck.getActiveIndexes(provedHcn);
            int[] expected = TXTcheck.referenceAtActiveIndexes(fullExpected, activeIndexes);
            int[] actual = TXTcheck.exponentSignature(provedHcn, hcnGenerator.getLastActivePrimeIndex());

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
