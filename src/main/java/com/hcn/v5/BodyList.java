package com.hcn.v5;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class BodyList {

    private HcnBody smallestBody = null;
    private int size = 0;

    public int size() { return size; }
    public HcnBody getSmallestBody() { return smallestBody; }

    public void clear() {
        smallestBody = null; size = 0;
    }

    public void remove(HcnBody body) {
        if (body.getSmallerBody() != null) body.getSmallerBody().setLargerBody(body.getLargerBody());
        else smallestBody = body.getLargerBody();
        if (body.getLargerBody() != null) body.getLargerBody().setSmallerBody(body.getSmallerBody());
        size--;
    }

    public Stream<HcnBody> stream() {
        return StreamSupport.stream(Spliterators.spliteratorUnknownSize(
                new java.util.Iterator<>() {
                    HcnBody current = smallestBody;
                    public boolean hasNext() { return current != null; }
                    public HcnBody next() { HcnBody c = current; current = current.getLargerBody(); return c; }
                }, Spliterator.ORDERED), false);
    }

    public List<HcnBody> addGroup(Collection<HcnBody> bodies) {
        List<HcnBody> sorted = new ArrayList<>(bodies);
        sorted.sort(HcnBody::compareTo);

        List<HcnBody> result;
        if (smallestBody == null) {
            result = initializeFromSorted(sorted);
        } else {
            result = mergeSortedBodies(sorted);
        }
        return result;
    }

    private List<HcnBody> initializeFromSorted(List<HcnBody> sorted) {
        List<HcnBody> added = new ArrayList<>();
        smallestBody = sorted.remove(0);
        added.add(smallestBody);
        smallestBody.getPip().addActiveHcnBody(smallestBody);

        HcnBody currentFloorBody = smallestBody;
        while (!sorted.isEmpty()) {
            HcnBody next = sorted.remove(0);
            if (next.getFactor().isBiggerThan(currentFloorBody.getFactor())) {
                currentFloorBody.setLargerBody(next);
                next.setSmallerBody(currentFloorBody);
                added.add(next);
                next.getPip().addActiveHcnBody(next);
                currentFloorBody = next;
            }
        }

        size = added.size();
        return added;
    }

    private List<HcnBody> mergeSortedBodies(List<HcnBody> sorted) {
        List<HcnBody> added = new ArrayList<>();
        List<HcnBody> toDelete = new ArrayList<>();

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
                    newBody.getPip().addActiveHcnBody(newBody);
                    size++;
                    currentFloorBody = newBody;
                } else {
                    toDelete.add(newBody);
                }
            } else {
                if (newBody.getFactor().isBiggerThan(currentFloorBody.getFactor())) {

                    HcnBody currentCeilingBody = currentFloorBody.getLargerBody();
                    while (currentCeilingBody != null && currentCeilingBody.getFactor().isNotBiggerThan(newBody.getFactor())) {
                        currentCeilingBody.deactivateFromLists();
                        toDelete.add(currentCeilingBody);
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
                    newBody.getPip().addActiveHcnBody(newBody);
                    size++;
                    currentFloorBody = newBody;
                } else {
                    toDelete.add(newBody);
                }
            }
        }
        toDelete.forEach(body -> body.getPip().getActivePrimeIndex().deactivateRecursive(body));
        return added;
    }
}
