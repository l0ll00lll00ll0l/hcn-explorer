package com.hcn.db;

import com.hcn.newCore.Body;
import com.hcn.newCore.TransitionNode;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;

@Getter
@Setter
public class DbBody {
    private int[] head;
    private int[] tail;
    private Integer firstHcnLapi;
    private Integer firstSuperiorHcnLapi;
    private Integer firstDominatedHcnLapi;

    public DbBody(int[] head, int[] tail) {
        this.head = head;
        this.tail = tail;
    }

    public DbBody(Body body) {

        ArrayList<Integer> tail = new ArrayList<>();
        ArrayList<Integer> head = new ArrayList<>();
        int lastPower = 0;
        Body currentBody = body;

        while (currentBody.getBodyNode().getParentNode() instanceof TransitionNode transitionNode) {
            tail.add(currentBody.getBodyNode().getBodyNodeId() - 1);
            currentBody = currentBody.getParent();
            lastPower = transitionNode.getTransitionFrom();
        }

        if (tail.get(tail.size() - 1) == currentBody.getBodyNode().getParentNode().getIndexes().get(0).getIndex()) {
            lastPower--;
            tail.remove(tail.size() - 1);

        }

        //carry on with apiNodes to identify last tailIndex
        while (currentBody != null) {
            int currentApiPower = currentBody.getBodyNode().getBodyNodeId();

            if (currentApiPower > lastPower + 1) {
                break;
            } else {
                if (currentApiPower > lastPower) {
                    tail.add(currentBody.getBodyNode().getParentNode().getIndexes().get(0).getIndex());
                    lastPower = currentApiPower;
                }
            }
            currentBody = currentBody.getParent();
        }

        while (currentBody != null) {
            head.add(currentBody.getBodyNode().getBodyNodeId());
            currentBody = currentBody.getParent();
        }

        this.head = new int[head.size()];
        for (int i = 0; i < head.size(); i++) {
            this.head[head.size() -1 -i] = head.get(i);
        }
        this.tail = new int[tail.size()];
        for (int i = 0; i < tail.size(); i++) {
            this.tail[tail.size() -1 -i] = tail.get(i);
        }
    }

    public String toString() {

        StringBuilder sb = new StringBuilder();
        int startingRelationIndex = 0;
        int powerGroup;

        if (head.length > 0) {
            powerGroup = head[0];

            for (int currentIndex = 1; currentIndex < head.length; currentIndex++) {
                int currentPower = head[currentIndex];
                if (powerGroup > currentPower) {
                    if (currentIndex - startingRelationIndex == 1) {
                        sb.append("p").append(startingRelationIndex).append("^").append(powerGroup).append(" ");
                    } else {
                        sb.append("p").append(startingRelationIndex).append("-").append(currentIndex - 1).append("^").append(powerGroup).append(" ");
                    }

                    startingRelationIndex = currentIndex;
                    powerGroup = currentPower;
                }
            }

            if (head.length == startingRelationIndex + 1) {
                sb.append("p").append(startingRelationIndex).append("^").append(powerGroup).append(" ");
            } else if (head.length > startingRelationIndex + 1) {
                sb.append("p").append(startingRelationIndex).append("-").append(head.length - 1).append("^").append(powerGroup).append(" ");
            }
        }
        startingRelationIndex = head.length;
        powerGroup = tail.length + 1;

        for (int i = 0; i < tail.length; i++) {

            int currentIndex = tail[i];

            if (startingRelationIndex == currentIndex) {
                sb.append("p").append(startingRelationIndex).append("^").append(powerGroup).append(" ");
            } else {
                sb.append("p").append(startingRelationIndex).append("-").append(currentIndex).append("^").append(powerGroup).append(" ");
            }
            startingRelationIndex = currentIndex + 1;
            powerGroup --;
        }

        return sb.toString();
    }
}
