package com.hcn.newCore;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Getter
@Setter
@Builder
public class BodyList {
    private Body smallestBody;

    public int size() {
        int count = 0;
        Body current = smallestBody;
        while (current != null) {
            count++;
            current = current.getLargerBody();
        }
        return count;
    }

    public void initialBodylist(List<Body> initialBodies) {

        initialBodies.sort(Body::compareTo);
        Body previousBody = null;
        for (Body body : initialBodies) {
            body.setSmallerBody(previousBody);
            if (previousBody != null) { previousBody.setLargerBody(body); }
            previousBody = body;
        }
        previousBody.setLargerBody(null);
        smallestBody = initialBodies.get(0);
    }

    public List<Body> mergeBodies(Collection<Body> otherBodyList) {
        List<Body> sorted = new ArrayList<>(otherBodyList);
        sorted.sort(Body::compareTo);
        List<Body> added = new java.util.ArrayList<>();

        Body current = smallestBody;
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
                ceiling.gotDominated();
                ceiling = next;
            }

            // insert newBody between current and ceiling
            current.setLargerBody(newBody);
            newBody.setSmallerBody(current);
            newBody.setLargerBody(ceiling);
            if (ceiling != null) ceiling.setSmallerBody(newBody);

            added.add(newBody);
            current = newBody;
        }

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
}