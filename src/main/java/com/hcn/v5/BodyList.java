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
        System.out.println("[BodyList] clear() called, was size=" + size);
        smallestBody = null; size = 0;
    }

    public void remove(HcnBody body) {
        System.out.println("[BodyList] remove() body=" + body + " size before=" + size);
        if (body.getSmallerBody() != null) body.getSmallerBody().setLargerBody(body.getLargerBody());
        else smallestBody = body.getLargerBody();
        if (body.getLargerBody() != null) body.getLargerBody().setSmallerBody(body.getSmallerBody());
        size--;
        System.out.println("[BodyList] remove() done, size after=" + size);
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
        System.out.println("[BodyList] addGroup() called with " + bodies.size() + " bodies, current size=" + size);
        List<HcnBody> sorted = new ArrayList<>(bodies);
        sorted.sort(HcnBody::compareTo);

        List<HcnBody> result;
        if (smallestBody == null) {
            result = initializeFromSorted(sorted);
        } else {
            result = mergeSortedBodies(sorted);
        }
        System.out.println("[BodyList] addGroup() done, added=" + result.size() + ", size after=" + size);
        return result;
    }

    private List<HcnBody> initializeFromSorted(List<HcnBody> sorted) {
        List<HcnBody> added = new ArrayList<>();
        smallestBody = sorted.remove(0);
        added.add(smallestBody);
        System.out.println("[BodyList] initializeFromSorted() first body=" + smallestBody);
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
        System.out.println("[BodyList] mergeSortedBodies() with " + sorted.size() + " sorted bodies");
        List<HcnBody> added = new ArrayList<>();
        List<HcnBody> toDelete = new ArrayList<>();

        HcnBody currentFloorBody = smallestBody;

        for (HcnBody newBody : sorted) {
            System.out.println("[BodyList]   processing newBody=" + newBody + ", floor=" + currentFloorBody);

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
                    System.out.println("[BodyList]   -> ADDED at end, size=" + size);
                    currentFloorBody = newBody;
                } else {
                    System.out.println("[BodyList]   -> REJECTED at end (factor too low)");
                    toDelete.add(newBody);
                }
            } else {
                if (newBody.getFactor().isBiggerThan(currentFloorBody.getFactor())) {

                    HcnBody currentCeilingBody = currentFloorBody.getLargerBody();
                    int deletedCount = 0;
                    while (currentCeilingBody != null && currentCeilingBody.getFactor().isNotBiggerThan(newBody.getFactor())) {
                        System.out.println("[BodyList]   -> DELETING existing body=" + currentCeilingBody);
                        currentCeilingBody.deactivateFromLists();
                        toDelete.add(currentCeilingBody);
                        size--;
                        deletedCount++;
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
                    System.out.println("[BodyList]   -> ADDED in middle, deleted=" + deletedCount + ", size=" + size);
                    currentFloorBody = newBody;
                } else {
                    System.out.println("[BodyList]   -> REJECTED in middle (factor too low)");
                    toDelete.add(newBody);
                }
            }
        }
        System.out.println("[BodyList] mergeSortedBodies() deactivating " + toDelete.size() + " deleted bodies");
        toDelete.forEach(body -> body.getPip().getActivePrimeIndex().deactivateRecursive(body));
        return added;
    }
}
