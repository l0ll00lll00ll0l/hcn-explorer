package com.hcn.core.basicdata;

public class GuiBodyRepresentative {
    private final int[] head;
    private final int[] tail;
    private int[] pipGroup;
    private int[] lastIndex;

    public GuiBodyRepresentative(int[] head, int[] tail) {
        this.head = head;
        this.tail = tail;
        calculatePipGroups(head, tail);
    }

    private void calculatePipGroups(int[] head, int[] tail) {
        this.pipGroup = new int[head.length + tail.length];
        this.lastIndex = new int[head.length + tail.length];

        for (int i = 0; i < head.length; i++) {
            pipGroup[i] = head[i];
            lastIndex[i] = i;
        }

        for (int i = 0; i < tail.length; i++) {
            pipGroup[head.length + i] = tail.length + 1 - i;
            lastIndex[head.length + i] = tail[i];
        }
    }

    public int[] getHead() { return head; }
    public int[] getTail() { return tail; }
    public int[] getPipGroup() { return pipGroup; }
    public int[] getLastIndex() { return lastIndex; }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        int previndex = -1;

        for (int i = 0; i < pipGroup.length; i++) {
            if (i > 0) sb.append(" ");
            if (lastIndex[i] - previndex > 1) {
                sb.append("(p<sub>").append(previndex + 1).append("</sub>-p<sub>").append(lastIndex[i]).append("</sub>)<sup>").append(pipGroup[i]).append("</sup>");
            } else {
                sb.append("p<sub>").append(lastIndex[i]).append("</sub><sup>").append(pipGroup[i]).append("</sup>");
            }
            previndex = lastIndex[i];
        }
        return sb.toString();
    }
}
