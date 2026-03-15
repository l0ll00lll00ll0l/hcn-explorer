package com.hcn.v3;

import org.junit.jupiter.api.Test;
import java.util.List;

public class V2V3ComparisonTest {

    @Test
    public void findFirstDifferenceBetweenV2AndV3() {
        int count = 15000;

        com.hcn.v2.Matrix v2Matrix = new com.hcn.v2.Matrix();
        v2Matrix.proveUntilCount(count);
        List<com.hcn.v2.Hcn> v2Hcns = v2Matrix.getProvedHcnList();

        com.hcn.v3.Matrix v3Matrix = new com.hcn.v3.Matrix();
        v3Matrix.proveUntilCount(count);
        List<com.hcn.v3.Hcn> v3Hcns = v3Matrix.getProvedHcnList();

        for (int i = 0; i < count; i++) {
            com.hcn.v2.Hcn v2 = v2Hcns.get(i);
            com.hcn.v3.Hcn v3 = v3Hcns.get(i);

            String v2Value = v2.getValue().toString();
            String v3Value = v3.getValue().toString();
            String v2Factor = v2.getFactor().toString();
            String v3Factor = v3.getFactor().toString();

            if (!v2Value.equals(v3Value) || !v2Factor.equals(v3Factor)) {
                System.out.println("FIRST DIFFERENCE at HCN #" + (i + 1));
                System.out.println("  V2: " + v2.fullPrint());
                System.out.println("  V3: " + v3.fullPrint());
                System.out.println("  V2 value=" + v2Value + " factor=" + v2Factor);
                System.out.println("  V3 value=" + v3Value + " factor=" + v3Factor);

                // Print a few surrounding for context
                int start = Math.max(0, i - 2);
                int end = Math.min(count, i + 5);
                System.out.println("\nContext (HCN #" + (start + 1) + " to #" + end + "):");
                for (int j = start; j < end; j++) {
                    String marker = (j == i) ? " >>> " : "     ";
                    System.out.println(marker + "#" + (j + 1)
                            + " V2: " + v2Hcns.get(j).fullPrint()
                            + " | V3: " + v3Hcns.get(j).fullPrint());
                }
                return;
            }
        }
        System.out.println("All " + count + " HCNs match between v2 and v3!");
    }
}
