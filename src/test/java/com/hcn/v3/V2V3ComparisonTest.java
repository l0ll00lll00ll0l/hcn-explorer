package com.hcn.v3;

import org.junit.jupiter.api.Test;
import java.util.List;

public class V2V3ComparisonTest {

    @Test
    public void findFirstDifferenceBetweenV2AndV3() {
        int count = 15000;

        com.hcn.v3.Matrix v3Matrix = new com.hcn.v3.Matrix();
        v3Matrix.proveUntilCount(count);
        List<com.hcn.v3.Hcn> v3Hcns = v3Matrix.getProvedHcnList();

        com.hcn.v4.Matrix v4Matrix = new com.hcn.v4.Matrix();
        v4Matrix.proveUntilCount(count);
        List<com.hcn.v4.Hcn> v4Hcns = v4Matrix.getProvedHcnList();

        for (int i = 0; i < count; i++) {
            com.hcn.v3.Hcn v3 = v3Hcns.get(i);
            com.hcn.v4.Hcn v4 = v4Hcns.get(i);

            String v3Value = v3.getValue().toString();
            String v4Value = v4.getValue().toString();
            String v3Factor = v3.getFactor().toString();
            String v4Factor = v4.getFactor().toString();

            if (!v3Value.equals(v4Value) || !v3Factor.equals(v4Factor)) {
                System.out.println("FIRST DIFFERENCE at HCN #" + (i + 1));
                System.out.println("  V3: " + v3.fullPrint());
                System.out.println("  V4: " + v4.fullPrint());
                System.out.println("  V3 value=" + v3Value + " factor=" + v3Factor);
                System.out.println("  V4 value=" + v4Value + " factor=" + v4Factor);

                int start = Math.max(0, i - 2);
                int end = Math.min(count, i + 5);
                System.out.println("\nContext (HCN #" + (start + 1) + " to #" + end + "):");
                for (int j = start; j < end; j++) {
                    String marker = (j == i) ? " >>> " : "     ";
                    System.out.println(marker + "#" + (j + 1)
                            + " V3: " + v3Hcns.get(j).fullPrint()
                            + " | V4: " + v4Hcns.get(j).fullPrint());
                }
                return;
            }
        }
        System.out.println("All " + count + " HCNs match between v3 and v4!");
    }
}
