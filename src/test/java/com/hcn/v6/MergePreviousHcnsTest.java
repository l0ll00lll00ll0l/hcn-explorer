package com.hcn.v6;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class MergePreviousHcnsTest {

    private LastActivePrimeIndexGroup lowerGroup;
    private LastActivePrimeIndexGroup higherGroup;

    @BeforeEach
    void setUp() {
        lowerGroup = new LastActivePrimeIndexGroup(0);
        higherGroup = new LastActivePrimeIndexGroup(1);
        lowerGroup.setHigherLapiGroup(higherGroup);
        higherGroup.setLowerLapiGroup(lowerGroup);
    }

    private Hcn hcn(double value, double factor) {
        Hcn h = new Hcn(null, 0);
        h.setValue(new ScientificNumber(value, 0));
        h.setFactor(new ScientificNumber(factor, 0));
        return h;
    }

    private void buildChain(LastActivePrimeIndexGroup group, Hcn... hcns) {
        group.setFirstHcn(hcns[0]);
        hcns[0].getSmallerHcns().put(group, null);
        for (int i = 0; i < hcns.length - 1; i++) {
            hcns[i].getLargerHcns().put(group, hcns[i + 1]);
            hcns[i + 1].getSmallerHcns().put(group, hcns[i]);
        }
        hcns[hcns.length - 1].getLargerHcns().put(group, null);
    }

    private List<Hcn> getChain(LastActivePrimeIndexGroup group) {
        List<Hcn> chain = new ArrayList<>();
        Hcn current = group.getFirstHcn();
        while (current != null) {
            chain.add(current);
            current = current.getLargerHcns().get(group);
        }
        return chain;
    }

    private void assertChainValues(LastActivePrimeIndexGroup group, double... expectedValues) {
        List<Hcn> chain = getChain(group);
        assertEquals(expectedValues.length, chain.size(), "chain size");
        for (int i = 0; i < expectedValues.length; i++) {
            double actual = chain.get(i).getValue().getMantissa()
                    * Math.pow(10, chain.get(i).getValue().getExponent());
            assertEquals(expectedValues[i], actual, 0.01, "value at index " + i);
        }
    }

    private void assertActiveValues(LastActivePrimeIndexGroup group, double... expectedValues) {
        List<Hcn> active = new ArrayList<>();
        for (Hcn h : getChain(group)) {
            if (!h.isDominated()) active.add(h);
        }
        assertEquals(expectedValues.length, active.size(), "active chain size");
        for (int i = 0; i < expectedValues.length; i++) {
            double actual = active.get(i).getValue().getMantissa()
                    * Math.pow(10, active.get(i).getValue().getExponent());
            assertEquals(expectedValues[i], actual, 0.01, "active value at index " + i);
        }
    }

    // --- Phase 1: copyHcnsFromLowerLapiGroup ---

    @Test
    void copy_allSurvive() {
        buildChain(lowerGroup, hcn(2, 4), hcn(4, 8), hcn(6, 12));
        higherGroup.copyHcnsFromLowerLapiGroup();
        assertChainValues(higherGroup, 2, 4, 6);
    }

    @Test
    void copy_singleHcn() {
        buildChain(lowerGroup, hcn(2, 4));
        higherGroup.copyHcnsFromLowerLapiGroup();
        assertChainValues(higherGroup, 2);
    }

    // --- Phase 2: mergeNewHcns ---
    // New Hcns are always larger than the smallest in the base chain.

    @Test
    void mergeNew_allSurvive() {
        buildChain(higherGroup, hcn(2, 4), hcn(8, 16));
        higherGroup.mergeNewHcns(List.of(hcn(4, 8), hcn(6, 12)));
        assertActiveValues(higherGroup, 2, 4, 6, 8);
        assertChainValues(higherGroup, 2, 4, 6, 8);
    }

    @Test
    void mergeNew_someDominated() {
        buildChain(higherGroup, hcn(2, 10), hcn(8, 16));
        higherGroup.mergeNewHcns(List.of(hcn(4, 8), hcn(6, 12)));
        assertActiveValues(higherGroup, 2, 6, 8);
        assertChainValues(higherGroup, 2, 4, 6, 8); // v=4 still in chain, just dominated
    }

    @Test
    void mergeNew_newDominatesOne() {
        buildChain(higherGroup, hcn(2, 4), hcn(4, 6), hcn(8, 16));
        higherGroup.mergeNewHcns(List.of(hcn(3, 10)));
        assertActiveValues(higherGroup, 2, 3, 8);
        assertChainValues(higherGroup, 2, 3, 4, 8); // v=4 still in chain
    }

    @Test
    void mergeNew_newDominatesMultiple() {
        buildChain(higherGroup, hcn(2, 4), hcn(4, 6), hcn(6, 8), hcn(10, 20));
        higherGroup.mergeNewHcns(List.of(hcn(3, 15)));
        assertActiveValues(higherGroup, 2, 3, 10);
        assertChainValues(higherGroup, 2, 3, 4, 6, 10); // v=4,v=6 still in chain
    }

    @Test
    void mergeNew_appendAtEnd() {
        buildChain(higherGroup, hcn(2, 4));
        higherGroup.mergeNewHcns(List.of(hcn(6, 12)));
        assertActiveValues(higherGroup, 2, 6);
    }

    @Test
    void mergeNew_allRejected() {
        buildChain(higherGroup, hcn(2, 10), hcn(6, 20));
        higherGroup.mergeNewHcns(List.of(hcn(4, 8)));
        assertActiveValues(higherGroup, 2, 6);
        assertChainValues(higherGroup, 2, 4, 6); // v=4 in chain but dominated
    }

    // --- Full two-phase scenario ---

    @Test
    void full_copyThenMerge() {
        buildChain(lowerGroup, hcn(2, 4), hcn(4, 8), hcn(6, 10));
        higherGroup.copyHcnsFromLowerLapiGroup();
        higherGroup.mergeNewHcns(List.of(hcn(5, 12), hcn(9, 20)));
        assertActiveValues(higherGroup, 2, 4, 5, 9);
    }
}
