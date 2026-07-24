package com.hcn.newCore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HcnGeneratorList {

    private static final Logger log = LoggerFactory.getLogger(HcnGeneratorList.class);

    private static Body smallestBody;
    private static Body largestBody;
    private static int size = 0;

    public static Body getSmallestBody() { return smallestBody; }
    public static Body getLargestBody() { return largestBody; }
    public static int getSize() { return size; }

    public static void initialize(Body smallest) {
        smallestBody = smallest;
        size = 0;
        Body current = smallest;
        while (current != null) {
            size++;
            largestBody = current;
            current = current.getLargerHcnGenerator();
        }
        log.debug("initialize: size={}", size);
    }

    public static void add(Body body) {
        Body prev = body.getPrevActiveBody();
        Body next = body.getNextActiveBody();

        body.setSmallerHcnGenerator(prev);
        body.setLargerHcnGenerator(next);

        if (prev != null) prev.setLargerHcnGenerator(body);
        else smallestBody = body;

        if (next != null) next.setSmallerHcnGenerator(body);
        else largestBody = body;
        size++;
        //log.debug("add body={}: size={}", body, size);
    }

    public static void remove(Body body) {
        Body prev = body.getSmallerHcnGenerator();
        Body next = body.getLargerHcnGenerator();

        if (prev != null) prev.setLargerHcnGenerator(next);
        else smallestBody = next;

        if (next != null) next.setSmallerHcnGenerator(prev);
        else largestBody = prev;

        body.setSmallerHcnGenerator(null);
        body.setLargerHcnGenerator(null);
        size--;
        //log.debug("remove body={}: size={}", body, size);
    }
}
