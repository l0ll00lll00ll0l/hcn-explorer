package com.hcn.newCore;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.TreeMap;

@Getter
@Setter
@Builder
public class BodyList implements Iterable<Body> {
    private Body smallestBody;

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

    public List<Body> mergeBodies(Collection<Body> otherBodyList) {
        List<Body> sorted = new ArrayList<>(otherBodyList);
        sorted.sort(Body::compareTo);
        List<Body> added = new java.util.ArrayList<>();

        if (smallestBody == null) {
            Body previousBody = null;
            for (Body body : sorted) {
                if (previousBody == null || body.getFactor().isBiggerThan(previousBody.getFactor())) {
                    body.setSmallerBody(previousBody);
                    if (previousBody != null) previousBody.setLargerBody(body);
                    added.add(body);
                    previousBody = body;
                }
            }
            if (!added.isEmpty()) smallestBody = added.get(0);
            return added;
        }

        Body current = smallestBody;
        List<Body> dominated = new ArrayList<>();
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
                dominated.add(ceiling);
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
            added.add(newBody);
            current = newBody;
        }

        dominated.forEach(Body::gotDominated);
        return added;
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

    public void deactivatedMaintain() {
        while (smallestBody.isDeactivated()) {
            smallestBody = smallestBody.getLargerBody();
            smallestBody.getSmallerBody().setLargerBody(null);
            smallestBody.setSmallerBody(null);
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
}