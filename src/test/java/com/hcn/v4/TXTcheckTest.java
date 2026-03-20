package com.hcn.v4;

import org.junit.jupiter.api.Test;
import java.util.List;

public class TXTcheckTest {

    private static final int COMPARE_COUNT = 789000;

    @Test
    public void findFirstMismatchAgainstReference() {
        TXTcheck txt = new TXTcheck();
        List<int[]> ref = txt.getReferenceHcns();

        int count = Math.min(COMPARE_COUNT, ref.size() - 1); // -1 because we skip first ref entry (HCN=1)

        int BATCH = 1000;
        Matrix matrix = new Matrix();
        int checked = 0;

        for (int batch = BATCH; batch <= count; batch += BATCH) {
            long t0 = System.currentTimeMillis();
            matrix.proveUntilCount(batch);
            long elapsed = System.currentTimeMillis() - t0;
            List<Hcn> proved = matrix.getProvedHcnList();

            for (int i = checked; i < batch; i++) {
                int[] expected = ref.get(i + 1);
                int[] actual = TXTcheck.exponentSignature(proved.get(i));

                if (!TXTcheck.signaturesEqual(expected, actual)) {
                    System.out.println("FIRST MISMATCH at HCN #" + (i + 1));
                    System.out.println("  Expected: " + TXTcheck.signatureToString(expected));
                    System.out.println("  Actual:   " + TXTcheck.signatureToString(actual));
                    System.out.println("  Hcn:      " + proved.get(i).fullPrint());
                    HcnBody b = proved.get(i).getBody();
                    while (b != null) {
                        System.out.println("  body node: primeIndex=" + b.getPip().getActivePrimeIndex().getIndex() + " power=" + b.getPip().getPower());
                        b = b.getParent();
                    }
                    int start = Math.max(0, i - 2);
                    int end = Math.min(batch, i + 3);
                    System.out.println("\nContext:");
                    for (int j = start; j < end; j++) {
                        String marker = (j == i) ? " >>> " : "     ";
                        System.out.println(marker + "#" + (j + 1)
                                + " ref=" + TXTcheck.signatureToString(ref.get(j + 1))
                                + " v4=" + TXTcheck.signatureToString(TXTcheck.exponentSignature(proved.get(j))));
                    }
                    return;
                }
            }
            checked = batch;
            System.out.println("Checked " + batch + "/" + count + " OK (" + elapsed + " ms)");
        }
        System.out.println("All " + count + " HCNs match the reference!");
    }
}
