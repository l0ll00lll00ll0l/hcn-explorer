package com.hcn.newCore;

import com.hcn.event.ActivityCenter;
import com.hcn.event.MatrixExtensionActivity;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

@Getter
@Setter
@Builder
@Slf4j
public class BodyList implements Iterable<Body> {
    private Body smallestBody;
    private Body largestBody;
    private int size = 0;
    private final List<Body> successfullyAddedNewBodies = new ArrayList<>();
    private final List<Body> dominatedSuperiorBodies = new ArrayList<>();
    private final List<Body> dominatedDeactivatedBodies = new ArrayList<>();

    public void mergeBodies(List<List<Body>> otherBodyList) {
        successfullyAddedNewBodies.clear();
        dominatedSuperiorBodies.clear();
        dominatedDeactivatedBodies.clear();
        List<Body> sorted = createSortedList(otherBodyList);
        List<Body> filtered = new ArrayList<>();
        ScientificNumber factorLimit = null;
        for (Body body : sorted) {
            if (factorLimit == null || body.getFactor().isBiggerThan(factorLimit)) {
                filtered.add(body);
                factorLimit = body.getFactor();
            }
        }

        if (smallestBody == null) {
            Body previousBody = null;
            for (Body body : filtered) {
                if (previousBody == null || body.getFactor().isBiggerThan(previousBody.getFactor())) {
                    body.setSmallerBody(previousBody);
                    if (previousBody != null) previousBody.setLargerBody(body);
                    successfullyAddedNewBodies.add(body);
                    size++;
                    previousBody = body;
                }
            }
            if (!successfullyAddedNewBodies.isEmpty()) {
                smallestBody = successfullyAddedNewBodies.get(0);
                largestBody = previousBody;
            }
            return;
        }

        Body current = smallestBody;
        if (!filtered.isEmpty() && filtered.get(0).getValue().isSmallerThan(smallestBody.getValue())) {
            log.warn("mergeBodies: incoming body {} is smaller than smallestBody {}", filtered.get(0), smallestBody);
        }
        for (Body newBody : filtered) {
            // advance current until we find the floor (largest body smaller than newBody)
            while (current.getLargerBody() != null && current.getLargerBody().getValue().isSmallerThan(newBody.getValue())) {
                current = current.getLargerBody();
            }

            // check if newBody's factor is superior to floor's factor
            if (!newBody.getFactor().isBiggerThan(current.getFactor())) {
                continue;
            }

            // remove dominated bodies above the insertion point
            Body ceiling = current.getLargerBody();
            while (ceiling != null && ceiling.getFactor().isNotBiggerThan(newBody.getFactor())) {
                Body next = ceiling.getLargerBody();
                ceiling.setSmallerBody(null);
                ceiling.setLargerBody(null);
                if (ceiling.isDeactivated()) {
                    dominatedDeactivatedBodies.add(ceiling);
                } else {
                    dominatedSuperiorBodies.add(ceiling);
                }
                size--;
                ceiling = next;
            }

            // insert newBody between current and ceiling
            current.setLargerBody(newBody);
            newBody.setSmallerBody(current);
            newBody.setLargerBody(ceiling);
            if (ceiling != null) ceiling.setSmallerBody(newBody);
            newBody.getBodyNode().getActiveBodies().add(newBody);
            if (newBody.getParent() != null) {
                newBody.getParent().getOffsprings().add(newBody);
            }
            successfullyAddedNewBodies.add(newBody);
            size++;

            if (ceiling == null) largestBody = newBody;
            current = newBody;
        }

        dominatedSuperiorBodies.forEach(body -> {
            body.setSmallerBody(null);
            body.setLargerBody(null);
            body.deletedDuringExtension();
        });
        dominatedDeactivatedBodies.forEach(body -> {
            body.setSmallerBody(null);
            body.setLargerBody(null);
            body.deletedDuringExtension();
        });
    }

    public List<Body> createSortedList(List<List<Body>> otherBodyList) {
        List<Body> previousSortedList = new ArrayList<>(otherBodyList.get(0));

        for (int i = 1; i < otherBodyList.size(); i++) {
            List<Body> currentMerge = new ArrayList<>();
            List<Body> nextList = otherBodyList.get(i);
            int p = 0, q = 0;
            while (p < previousSortedList.size() && q < nextList.size()) {
                if (previousSortedList.get(p).compareTo(nextList.get(q)) <= 0) {
                    currentMerge.add(previousSortedList.get(p++));
                } else {
                    currentMerge.add(nextList.get(q++));
                }
            }
            while (p < previousSortedList.size()) currentMerge.add(previousSortedList.get(p++));
            while (q < nextList.size()) currentMerge.add(nextList.get(q++));
            previousSortedList = currentMerge;
        }

        return previousSortedList;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("BodyList[\n");
        Body current = smallestBody;
        int i = 0;
        while (current != null) {
            sb.append("  ").append(i++).append(": ").append(current).append("\n");
            current = current.getLargerBody();
        }
        sb.append("]");
        return sb.toString();
    }

    public List<Body> deactivatedMaintain(ScientificNumber smallestPossibleExtension) {

        List<Body> deletedBodies = new ArrayList<>();
        Body bodyToDelete = smallestBody;
        while (bodyToDelete.getLargerBody() != null && bodyToDelete.getLargerBody().getValue().isSmallerThan(smallestPossibleExtension)) {
            Body nextBody = bodyToDelete.getLargerBody();
            if (bodyToDelete.isDeactivated()) {
                if (bodyToDelete.getDeactivatedOffsprings().isEmpty()) {
                    if (bodyToDelete == smallestBody) {
                        smallestBody = smallestBody.getLargerBody();
                        smallestBody.setSmallerBody(null);
                        bodyToDelete.setLargerBody(null);
                    } else {
                        bodyToDelete.getSmallerBody().setLargerBody(bodyToDelete.getLargerBody());
                        bodyToDelete.getLargerBody().setSmallerBody(bodyToDelete.getSmallerBody());
                        bodyToDelete.setSmallerBody(null);
                        bodyToDelete.setLargerBody(null);
                    }
                    bodyToDelete.deleteDuringBodyListMaintain();
                    deletedBodies.add(bodyToDelete);
                    size--;
                }
            }
            bodyToDelete = nextBody;
        }
        return deletedBodies;
    }

    @Override
    public Iterator<Body> iterator() {
        return new Iterator<>() {
            Body current = smallestBody;
            public boolean hasNext() { return current != null; }
            public Body next() { Body c = current; current = current.getLargerBody(); return c; }
        };
    }

    public void maintainHcnGeneratorList() {
        dominatedSuperiorBodies.forEach(b -> { HcnGeneratorList.remove(b); });
        successfullyAddedNewBodies.forEach(b -> { HcnGeneratorList.add(b); });
        if (ActivityCenter.isDbMode()) {
            MatrixExtensionActivity mea = ActivityCenter.getLastMatrixExtensionActivity();
            mea.setCreatedActiveBodyCount(successfullyAddedNewBodies.size());
            mea.setDeletedActiveBodyCount(dominatedSuperiorBodies.size());
            mea.setDeletedDeactivatedBodyCount(dominatedDeactivatedBodies.size());
        }
    }
}