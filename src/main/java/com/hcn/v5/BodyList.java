package com.hcn.v5;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class BodyList {

    private HcnBody smallestBody = null;
    private int size = 0;

    public int size() { return size; }
    public HcnBody getSmallestBody() { return smallestBody; }

    public List<HcnBody> addGroup(Collection<HcnBody> bodies) {
        List<HcnBody> sorted = new ArrayList<>(bodies);
        sorted.sort(HcnBody::compareTo);

        if (smallestBody == null) {
            return initializeFromSorted(sorted);
        }
        return mergeSortedBodies(sorted);
    }

    private List<HcnBody> initializeFromSorted(List<HcnBody> sorted) {
        List<HcnBody> added = new ArrayList<>();
        smallestBody = sorted.remove(0);
        added.add(smallestBody);

        HcnBody currentFloorBody = smallestBody;
        while (!sorted.isEmpty()) {
            HcnBody next = sorted.remove(0);
            if (next.getFactor().isBiggerThan(currentFloorBody.getFactor())) {
                currentFloorBody.setLargerBody(next);
                next.setSmallerBody(currentFloorBody);
                added.add(next);
                currentFloorBody = next;
            }
        }

        size = added.size();
        return added;
    }

    private List<HcnBody> mergeSortedBodies(List<HcnBody> sorted) {
        List<HcnBody> added = new ArrayList<>();

        HcnBody currentFloorBody = smallestBody;

        for (HcnBody newBody : sorted) {

            while (currentFloorBody.getLargerBody() != null && currentFloorBody.getLargerBody().getValue().isSmallerThan(newBody.getValue())) {
                currentFloorBody = currentFloorBody.getLargerBody();
            }

            if (currentFloorBody.getLargerBody() == null) {
                if (newBody.getFactor().isBiggerThan(currentFloorBody.getFactor())) {
                    currentFloorBody.setLargerBody(newBody);
                    newBody.setSmallerBody(currentFloorBody);
                    added.add(newBody);
                    size++;
                    currentFloorBody = newBody;
                } else {
                    // new created body was not added
                }
            } else {
                if (newBody.getFactor().isBiggerThan(currentFloorBody.getFactor())) {

                    HcnBody currentCeilingBody = currentFloorBody.getLargerBody();
                    while (currentCeilingBody != null && currentCeilingBody.getFactor().isNotBiggerThan(newBody.getFactor())) {
                        // currentCeilingBody existing member is deactivated here
                        size--;
                        currentCeilingBody = currentCeilingBody.getLargerBody();
                    }

                    currentFloorBody.setLargerBody(newBody);
                    newBody.setSmallerBody(currentFloorBody);

                    if (currentCeilingBody != null) {
                        newBody.setLargerBody(currentCeilingBody);
                        currentCeilingBody.setSmallerBody(newBody);
                    }

                    added.add(newBody);
                    size++;
                    currentFloorBody = newBody;
                } else {
                    // new created body was not added
                }
            }
        }
        return added;
    }
}
