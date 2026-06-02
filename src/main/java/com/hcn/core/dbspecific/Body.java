package com.hcn.core.dbspecific;

import com.hcn.core.HcnBody;
import com.hcn.core.HcnGenerator;

import java.util.ArrayList;
import java.util.List;

public class Body {
    private int[] head;
    private int[] tail;

    public Body(HcnGenerator hcnGenerator) {
        computeHeadAndTail(hcnGenerator.getCurrentHcnBody());
    }

    public int[] getHead() { return head; }
    public int[] getTail() { return tail; }

    private void computeHeadAndTail(HcnBody hcnBody) {
        List<int[]> pairs = new ArrayList<>();
        HcnBody current = hcnBody;
        while (current != null) {
            pairs.add(0, new int[]{current.getPip().getActivePrimeIndex().getIndex(), current.getPip().getPower()});
            current = current.getParent();
        }

        if (pairs.isEmpty()) {
            head = null;
            tail = new int[0];
            return;
        }

        int maxPower = pairs.get(0)[1];

        int[] lastIndexAtPower = new int[maxPower + 1];
        for (int[] pair : pairs) {
            lastIndexAtPower[pair[1]] = Math.max(lastIndexAtPower[pair[1]], pair[0]);
        }

        List<Integer> tailList = new ArrayList<>();
        int splitPower = maxPower + 1;
        for (int p = 2; p <= maxPower; p++) {
            tailList.add(lastIndexAtPower[p]);
            if (p < maxPower) {
                int gapBetweenPowers = lastIndexAtPower[p] - lastIndexAtPower[p + 1];
                if (gapBetweenPowers > 1) {
                    splitPower = p + 1;
                    break;
                }
            }
        }

        tail = tailList.stream().mapToInt(Integer::intValue).toArray();

        if (splitPower <= maxPower) {
            List<Integer> headList = new ArrayList<>();
            for (int[] pair : pairs) {
                if (pair[1] >= splitPower) {
                    headList.add(pair[1]);
                }
            }
            head = headList.stream().mapToInt(Integer::intValue).toArray();
        } else {
            head = null;
        }
    }
}
