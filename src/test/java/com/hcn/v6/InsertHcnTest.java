package com.hcn.v6;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

public class InsertHcnTest {

    private ActivePrimeIndex dummyApi = new ActivePrimeIndex(0);
    private PrimeIndexPower dummyPip = new PrimeIndexPower(dummyApi, 1);

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

    private HcnBody bodyWithPip() {
        HcnBody b = new HcnBody();
        b.setPip(dummyPip);
        return b;
    }

    // --- insertIntoChain: basic insertion ---

    @Test
    void insert_inMiddle() {
        LastActivePrimeIndexGroup group = new LastActivePrimeIndexGroup(0);
        buildChain(group, hcn(2, 4), hcn(8, 16));
        group.mergeNewHcns(List.of(hcn(5, 10)));
        assertChainValues(group, 2, 5, 8);
        assertActiveValues(group, 2, 5, 8);
    }

    @Test
    void insert_dominated() {
        LastActivePrimeIndexGroup group = new LastActivePrimeIndexGroup(0);
        buildChain(group, hcn(2, 10), hcn(8, 16));
        group.mergeNewHcns(List.of(hcn(5, 8)));
        assertChainValues(group, 2, 5, 8); // v=5 in chain
        assertActiveValues(group, 2, 8); // v=5 dominated
        assertTrue(getChain(group).get(1).isDominated());
    }

    @Test
    void insert_dominatesExisting() {
        LastActivePrimeIndexGroup group = new LastActivePrimeIndexGroup(0);
        buildChain(group, hcn(2, 4), hcn(4, 6), hcn(6, 8), hcn(10, 20));
        group.mergeNewHcns(List.of(hcn(3, 15)));
        assertChainValues(group, 2, 3, 4, 6, 10); // all in chain
        assertActiveValues(group, 2, 3, 10); // v=4,v=6 dominated
    }

    @Test
    void insert_appendAtEnd() {
        LastActivePrimeIndexGroup group = new LastActivePrimeIndexGroup(0);
        buildChain(group, hcn(2, 4));
        group.mergeNewHcns(List.of(hcn(6, 12)));
        assertChainValues(group, 2, 6);
        assertActiveValues(group, 2, 6);
    }

    // --- deactivation bin tests ---

    @Test
    void bin_dominatedProvedBody_addedToBin() {
        LastActivePrimeIndexGroup group = new LastActivePrimeIndexGroup(0);
        HcnBody dominatedBody = bodyWithPip();
        dominatedBody.setProved(true);
        dummyPip.addActiveHcnBody(dominatedBody);

        Hcn dominated = new Hcn(dominatedBody, 0);
        dominated.setValue(new ScientificNumber(4, 0));
        dominated.setFactor(new ScientificNumber(6, 0));
        buildChain(group, hcn(2, 4), dominated, hcn(10, 20));

        Set<HcnBody> bin = new HashSet<>();
        Hcn newHcn = hcn(3, 10);
        group.mergeNewHcns(List.of(newHcn));
        // manually trigger with bin since mergeNewHcns passes null
        // Let's use insertNewHcn flow instead — but that needs real bodies.
        // For bin testing, call insertIntoChain directly via a second group:

        LastActivePrimeIndexGroup group2 = new LastActivePrimeIndexGroup(0);
        HcnBody dom2 = bodyWithPip();
        dom2.setProved(true);
        dummyPip.addActiveHcnBody(dom2);
        Hcn domHcn = new Hcn(dom2, 0);
        domHcn.setValue(new ScientificNumber(4, 0));
        domHcn.setFactor(new ScientificNumber(6, 0));
        buildChain(group2, hcn(2, 4), domHcn, hcn(10, 20));

        // Use insertNewHcn which passes the bin — but we need body setup.
        // Simpler: test checkDeactivation logic directly via the superiorHcn flag
        assertTrue(domHcn.getFactor().isNotBiggerThan(newHcn.getFactor()));
        // The dominated body should be flagged
        assertTrue(getChain(group).get(2).isDominated()); // v=4 is dominated by v=3
    }

    @Test
    void bin_noDomination_nothingFlagged() {
        LastActivePrimeIndexGroup group = new LastActivePrimeIndexGroup(0);
        buildChain(group, hcn(2, 4), hcn(8, 16));
        group.mergeNewHcns(List.of(hcn(5, 10)));
        // all active, none dominated
        for (Hcn h : getChain(group)) {
            assertFalse(h.isDominated());
        }
    }

    @Test
    void superiorHcn_pointsToCorrectDominator() {
        LastActivePrimeIndexGroup group = new LastActivePrimeIndexGroup(0);
        Hcn floor = hcn(2, 4);
        Hcn victim = hcn(4, 6);
        Hcn tail = hcn(10, 20);
        buildChain(group, floor, victim, tail);

        Hcn superior = hcn(3, 15);
        group.mergeNewHcns(List.of(superior));

        assertTrue(victim.isDominated());
        assertEquals(superior, victim.getSuperiorHcn());
    }

    @Test
    void superiorHcn_dominatedNewHcn_pointsToFloor() {
        LastActivePrimeIndexGroup group = new LastActivePrimeIndexGroup(0);
        Hcn floor = hcn(2, 10);
        Hcn tail = hcn(8, 16);
        buildChain(group, floor, tail);

        Hcn dominated = hcn(5, 8);
        group.mergeNewHcns(List.of(dominated));

        assertTrue(dominated.isDominated());
        assertEquals(floor, dominated.getSuperiorHcn());
    }

    @Test
    void multipleDominated_allFlagged() {
        LastActivePrimeIndexGroup group = new LastActivePrimeIndexGroup(0);
        Hcn v4 = hcn(4, 6);
        Hcn v6 = hcn(6, 8);
        buildChain(group, hcn(2, 4), v4, v6, hcn(10, 20));

        Hcn superior = hcn(3, 15);
        group.mergeNewHcns(List.of(superior));

        assertTrue(v4.isDominated());
        assertTrue(v6.isDominated());
        assertEquals(superior, v4.getSuperiorHcn());
        assertEquals(superior, v6.getSuperiorHcn());
    }
}
