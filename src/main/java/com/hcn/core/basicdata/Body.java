package com.hcn.core.basicdata;

import com.hcn.core.HcnBody;

public class Body {
    private int[] head;
    private int[] tail;

    public Body(HcnBody hcnBody) {
        computeHeadAndTail(hcnBody);
    }

    public int[] getHead() { return head; }
    public int[] getTail() { return tail; }

    private void computeHeadAndTail(HcnBody hcnBody) {
        int tailSize = 0;
        int[] tailBuf = new int[16];
        int currentPower = 1;
        int fpgPower = 2;

        HcnBody walk = hcnBody;
        while (walk != null) {
            HcnBody parent = walk.getParent();
            if (parent == null) {
                // reached p0, check its pip
                int pip = walk.getPip().getPower();
                if (pipIsNextLevel(pip, currentPower)) {
                    tailBuf = appendToTail(tailBuf, tailSize, walk.getPip().getActivePrimeIndex().getIndex());
                    tailSize++;
                    currentPower = pip;
                } else if (pip > currentPower + 1) {
                    break;
                }
                walk = null;
                break;
            }

            int walkIndex = walk.getPip().getActivePrimeIndex().getIndex();
            int parentIndex = parent.getPip().getActivePrimeIndex().getIndex();
            int parentPip = parent.getPip().getPower();

            if (walkIndex - parentIndex > 1) {
                // FPG gap detected
                if (fpgPower == currentPower + 1) {
                    // FPG is next level
                    tailBuf = appendToTail(tailBuf, tailSize, walkIndex - 1);
                    tailSize++;
                    currentPower = fpgPower;
                } else if (fpgPower > currentPower + 1) {
                    break;
                }
                fpgPower++;

                // now check parent pip
                if (pipIsNextLevel(parentPip, currentPower)) {
                    tailBuf = appendToTail(tailBuf, tailSize, parentIndex);
                    tailSize++;
                    currentPower = parentPip;
                } else if (parentPip > currentPower + 1) {
                    walk = parent;
                    break;
                }
            } else {
                // regular step, gap == 1
                if (pipIsNextLevel(parentPip, currentPower)) {
                    tailBuf = appendToTail(tailBuf, tailSize, parentIndex);
                    tailSize++;
                    currentPower = parentPip;
                } else if (parentPip > currentPower + 1) {
                    walk = parent;
                    break;
                }
            }

            walk = parent;
        }

        tail = reverseTail(tailBuf, tailSize);
        head = buildHead(walk, hcnBody);
    }

    private boolean pipIsNextLevel(int pip, int currentPower) {
        return pip == currentPower + 1;
    }

    private int[] appendToTail(int[] tailBuf, int tailSize, int index) {
        if (tailSize == tailBuf.length) {
            tailBuf = java.util.Arrays.copyOf(tailBuf, tailSize * 2);
        }
        tailBuf[tailSize] = index;
        return tailBuf;
    }

    private int[] reverseTail(int[] tailBuf, int tailSize) {
        int[] result = new int[tailSize];
        for (int i = 0; i < tailSize; i++) {
            result[i] = tailBuf[tailSize - 1 - i];
        }
        return result;
    }

    private int[] buildHead(HcnBody breakPoint, HcnBody hcnBody) {
        if (breakPoint == null) {
            return null;
        }
        int headLength = breakPoint.getPip().getActivePrimeIndex().getIndex() + 1;
        int[] result = new int[headLength];
        HcnBody walk = hcnBody;
        while (walk != null) {
            int idx = walk.getPip().getActivePrimeIndex().getIndex();
            if (idx < headLength) {
                result[idx] = walk.getPip().getPower();
            }
            walk = walk.getParent();
        }
        return result;
    }
}
