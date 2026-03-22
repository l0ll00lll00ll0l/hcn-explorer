package com.hcn.v5;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class BodyListTest {

    private BodyList bodyList;
    private ActivePrimeIndex dummyApi;
    private PrimeIndexPower dummyPip;

    @BeforeEach
    void setUp() {
        bodyList = new BodyList();
        dummyApi = new ActivePrimeIndex(0);
        dummyPip = new PrimeIndexPower(dummyApi, 1);
        dummyApi.getPips().put(1, dummyPip);
    }

    private HcnBody body(double value, double factor) {
        HcnBody b = new HcnBody();
        b.setValue(new ScientificNumber(value, 0));
        b.setFactor(new ScientificNumber(factor, 0));
        b.setPip(dummyPip);
        return b;
    }

    private List<HcnBody> getChain() {
        List<HcnBody> chain = new ArrayList<>();
        HcnBody current = bodyList.getSmallestBody();
        while (current != null) {
            chain.add(current);
            current = current.getLargerBody();
        }
        return chain;
    }

    private void assertChain(double... expectedValues) {
        List<HcnBody> chain = getChain();
        assertEquals(expectedValues.length, chain.size(), "chain size");
        for (int i = 0; i < expectedValues.length; i++) {
            double actual = chain.get(i).getValue().getMantissa()
                    * Math.pow(10, chain.get(i).getValue().getExponent());
            assertEquals(expectedValues[i], actual, 0.01, "value at index " + i);
        }
    }

    // --- Initialize ---

    @Test
    void init_allSurvive() {
        List<HcnBody> added = bodyList.addGroup(List.of(body(2, 4), body(4, 8), body(6, 12)));
        assertEquals(3, added.size());
        assertEquals(3, bodyList.size());
        assertChain(2, 4, 6);
    }

    @Test
    void init_middleDominated() {
        HcnBody a = body(2, 10), b = body(4, 8), c = body(6, 12);
        List<HcnBody> added = bodyList.addGroup(List.of(a, b, c));
        assertTrue(added.contains(a));
        assertFalse(added.contains(b));
        assertTrue(added.contains(c));
        assertEquals(2, bodyList.size());
        assertChain(2, 6);
    }

    @Test
    void init_lastDominated() {
        HcnBody a = body(2, 10), b = body(4, 8);
        List<HcnBody> added = bodyList.addGroup(List.of(a, b));
        assertEquals(1, added.size());
        assertTrue(added.contains(a));
        assertEquals(1, bodyList.size());
        assertChain(2);
    }

    // --- Merge: insertion ---

    @Test
    void merge_newBodyInMiddle() {
        bodyList.addGroup(List.of(body(2, 4), body(6, 12)));
        List<HcnBody> added = bodyList.addGroup(List.of(body(4, 8)));
        assertEquals(1, added.size());
        assertEquals(3, bodyList.size());
        assertChain(2, 4, 6);
    }

    @Test
    void merge_newBodyAtEnd() {
        bodyList.addGroup(List.of(body(2, 4)));
        List<HcnBody> added = bodyList.addGroup(List.of(body(6, 12)));
        assertEquals(1, added.size());
        assertEquals(2, bodyList.size());
        assertChain(2, 6);
    }

    // --- Merge: domination ---

    @Test
    void merge_newBodyDominatedByExisting() {
        bodyList.addGroup(List.of(body(2, 10), body(6, 12)));
        List<HcnBody> added = bodyList.addGroup(List.of(body(4, 8)));
        assertEquals(0, added.size());
        assertEquals(2, bodyList.size());
        assertChain(2, 6);
    }

    @Test
    void merge_newBodyDominatesOneExisting() {
        bodyList.addGroup(List.of(body(2, 4), body(4, 6), body(8, 12)));
        List<HcnBody> added = bodyList.addGroup(List.of(body(3, 10)));
        assertEquals(1, added.size());
        assertEquals(3, bodyList.size());
        assertChain(2, 3, 8);
    }

    @Test
    void merge_newBodyDominatesMultipleExisting() {
        bodyList.addGroup(List.of(body(2, 4), body(4, 6), body(6, 8), body(10, 20)));
        List<HcnBody> added = bodyList.addGroup(List.of(body(3, 15)));
        assertEquals(1, added.size());
        assertEquals(3, bodyList.size());
        assertChain(2, 3, 10);
    }

    // --- Merge: multiple new bodies ---

    @Test
    void merge_multipleNewBodies_allSurvive() {
        bodyList.addGroup(List.of(body(2, 4), body(8, 16)));
        List<HcnBody> added = bodyList.addGroup(List.of(body(4, 8), body(6, 12)));
        assertEquals(2, added.size());
        assertEquals(4, bodyList.size());
        assertChain(2, 4, 6, 8);
    }

    @Test
    void merge_multipleNewBodies_someDominated() {
        bodyList.addGroup(List.of(body(2, 10), body(8, 16)));
        List<HcnBody> added = bodyList.addGroup(List.of(body(4, 8), body(6, 12)));
        assertEquals(1, added.size());
        assertEquals(3, bodyList.size());
        assertChain(2, 6, 8);
    }

    @Test
    void merge_complexScenario_initTenSurviveEight_thenFourMixed() {
        // Init 10 bodies, 8 survive (v=6 and v=12 dominated by preceding higher factors)
        List<HcnBody> initAdded = bodyList.addGroup(List.of(
                body(2, 5), body(4, 8), body(6, 7), body(8, 10), body(10, 12),
                body(12, 11), body(14, 15), body(16, 18), body(18, 20), body(20, 25)));
        assertEquals(8, initAdded.size());
        assertEquals(8, bodyList.size());
        assertChain(2, 4, 8, 10, 14, 16, 18, 20);

        // Merge 4 bodies:
        //   v=9,f=11  -> fits between 8 and 10, no deletions
        //   v=11,f=11 -> dominated by v=10,f=12, not added
        //   v=13,f=19 -> fits after 10, deletes v=14(f=15) and v=16(f=18)
        //   v=21,f=30 -> fits at end, largest
        List<HcnBody> mergeAdded = bodyList.addGroup(List.of(
                body(9, 11), body(11, 11), body(13, 19), body(21, 30)));
        assertEquals(3, mergeAdded.size());
        assertEquals(9, bodyList.size());
        assertChain(2, 4, 8, 9, 10, 13, 18, 20, 21);
    }
}
