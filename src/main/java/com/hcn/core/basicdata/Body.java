package com.hcn.core.basicdata;

import com.hcn.core.ActivePrimeIndex;
import com.hcn.core.FixedPowerGroup;
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

        HcnBody bodyWalk = hcnBody;
        ActivePrimeIndex api = hcnBody.getPip().getActivePrimeIndex();

        while (api != null) {
            int pip = bodyWalk.getPip().getPower();

            if (pipJumpsMoreThanOneLevel(pip, currentPower)) {
                break;
            }

            if (pipIsNextLevel(pip, currentPower)) {
                // check offspring FPG for same-level extension
                if (api.getOffspringFixedPowerGroup() != null && getFpgPower(api.getOffspringFixedPowerGroup()) == pip) {
                    tailBuf = appendToTail(tailBuf, tailSize, getLastIndexInFpg(api.getOffspringFixedPowerGroup()));
                } else {
                    tailBuf = appendToTail(tailBuf, tailSize, api.getIndex());
                }
                tailSize++;
                currentPower = pip;
            }

            // navigate to next in chain
            if (api.getParentFixedPowerGroup() != null) {
                FixedPowerGroup fpg = api.getParentFixedPowerGroup();
                int fpgPower = getFpgPower(fpg);

                if (pipIsNextLevel(fpgPower, currentPower)) {
                    tailBuf = appendToTail(tailBuf, tailSize, getLastIndexInFpg(fpg));
                    tailSize++;
                    currentPower = fpgPower;
                } else if (pipJumpsMoreThanOneLevel(fpgPower, currentPower)) {
                    break;
                }

                api = fpg.getParentPrimeIndex();
                bodyWalk = bodyWalk.getParent();
            } else if (api.getParentActivePrimeIndex() != null) {
                api = api.getParentActivePrimeIndex();
                bodyWalk = bodyWalk.getParent();
            } else {
                api = null;
                bodyWalk = null;
            }
        }

        tail = reverseTail(tailBuf, tailSize);
        head = buildHead(bodyWalk, hcnBody);
    }

    private boolean pipIsNextLevel(int pip, int currentPower) {
        return pip == currentPower + 1;
    }

    private boolean pipJumpsMoreThanOneLevel(int pip, int currentPower) {
        return pip > currentPower + 1;
    }

    private int getFpgPower(FixedPowerGroup fpg) {
        return fpg.getFixedPowerGroup().get(0).getPips().firstKey();
    }

    private int getLastIndexInFpg(FixedPowerGroup fpg) {
        return fpg.getFixedPowerGroup().get(fpg.getFixedPowerGroup().size() - 1).getIndex();
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
