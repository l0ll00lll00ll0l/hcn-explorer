package com.hcn.newCore;

import com.hcn.event.ActivityCenter;
import com.hcn.event.MatrixExtensionActivity;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

@Getter
@Setter
@Builder
@Slf4j
public class BodyList implements Iterable<Body> {
    private Body smallestBody;
    private final List<Body> successfullyAddedNewBodies = new ArrayList<>();
    private final List<Body> dominatedBodies = new ArrayList<>();

    public int size() {
        int count = 0;
        Body current = smallestBody;
        while (current != null) {
            if (!current.isDeactivated()) {
                count++;
            }
            current = current.getLargerBody();
        }
        return count;
    }

    public void mergeBodies(Collection<Body> otherBodyList) {
        successfullyAddedNewBodies.clear();
        dominatedBodies.clear();
        List<Body> sorted = new ArrayList<>(otherBodyList);
        sorted.sort(Body::compareTo);

        if (smallestBody == null) {
            Body previousBody = null;
            for (Body body : sorted) {
                if (previousBody == null || body.getFactor().isBiggerThan(previousBody.getFactor())) {
                    body.setSmallerBody(previousBody);
                    if (previousBody != null) previousBody.setLargerBody(body);
                    successfullyAddedNewBodies.add(body);
                    previousBody = body;
                }
            }
            if (!successfullyAddedNewBodies.isEmpty()) smallestBody = successfullyAddedNewBodies.get(0);
            return;
        }

        Body current = smallestBody;
        if (!sorted.isEmpty() && sorted.get(0).getValue().isSmallerThan(smallestBody.getValue())) {
            log.warn("mergeBodies: incoming body {} is smaller than smallestBody {}", sorted.get(0), smallestBody);
        }
        for (Body newBody : sorted) {
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
                dominatedBodies.add(ceiling);
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
            current = newBody;
        }

        dominatedBodies.forEach(body -> {
            body.setSmallerBody(null);
            body.setLargerBody(null);
            body.deletedDuringExtension();
        });
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

    public void deactivatedMaintain(ScientificNumber smallestPossibleExtension) {
        while (smallestBody.isDeactivated() &&
                smallestBody.getLargerBody().getValue().isSmallerThan(smallestPossibleExtension) &&
                smallestBody.getDeactivatedOffsprings().isEmpty()) {
            Body pruned = smallestBody;
            smallestBody = smallestBody.getLargerBody();
            smallestBody.setSmallerBody(null);
            pruned.setLargerBody(null);
            pruned.deleteDuringBodyListMaintain();
        }
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
        dominatedBodies.forEach(Body::removeFromHcnGeneratorList);
        successfullyAddedNewBodies.forEach(Body::addToHcnGeneratorList);
        if (ActivityCenter.isDbMode()) {
            MatrixExtensionActivity mea = ActivityCenter.getLastMatrixExtensionActivity();
            mea.setCreatedActiveBodyCount(successfullyAddedNewBodies.size());
            mea.setDeactivatedBodyCount(dominatedBodies.size());
        }
    }
}