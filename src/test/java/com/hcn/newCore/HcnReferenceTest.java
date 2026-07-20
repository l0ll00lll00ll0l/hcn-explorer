package com.hcn.newCore;

import com.hcn.db.DbBody;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class HcnReferenceTest {

    record RefBody(int[] head, int[] tail, int lapi) {}

    static RefBody parseLine(String line) {
        String[] tokens = line.trim().split("\\s+");
        // tokens[0] = total prime count (ignored for body)
        // tokens[1..n] = exponents descending, power^count or single power, skip power=1

        List<Integer> exponents = new ArrayList<>();
        for (int i = 1; i < tokens.length; i++) {
            String[] parts = tokens[i].split("\\^");
            int power = Integer.parseInt(parts[0]);
            if (power == 1) break;
            int count = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
            for (int j = 0; j < count; j++) exponents.add(power);
        }

        if (exponents.isEmpty()) return new RefBody(new int[0], new int[0], Integer.parseInt(tokens[0]) - 1);

        // walk from end: collect last prime index per power while consecutive (+1 each step)
        List<Integer> tail = new ArrayList<>();
        int i = exponents.size() - 1;
        int expectedPower = 2;

        while (i >= 0 && exponents.get(i) == expectedPower) {
            tail.add(0, i);
            while (i > 0 && exponents.get(i - 1) == expectedPower) i--;
            i--;
            expectedPower++;
        }

        // remaining exponents go to head
        List<Integer> head = new ArrayList<>();
        for (int j = 0; j <= i; j++) head.add(exponents.get(j));

        return new RefBody(
            head.stream().mapToInt(Integer::intValue).toArray(),
            tail.stream().mapToInt(Integer::intValue).toArray(),
                Integer.parseInt(tokens[0]) - 1
        );
    }

    private Matrix matrix;

    private Hcn nextHcn() {
        if (matrix.getProvedHcns().isEmpty()) {
            matrix.proveLapi(1);
        }
        return matrix.getProvedHcns().remove(0);
    }

    //@Test
    void testMatchMatrix() {
        matrix = Matrix.builder().build();
        matrix.initialize();

        try (InputStream is = getClass().getClassLoader().getResourceAsStream("hcn.txt");
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line;
            boolean first = true;
            int count = 0;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                if (first) { first = false; continue; } // skip HCN=1
                RefBody rb = parseLine(line);
                Hcn hcn = nextHcn();
                DbBody db = new DbBody(hcn.getBody());
                count++;
                assertEquals(rb.lapi(), hcn.getLapi(), "lapi mismatch at HCN #" + count);
                assertArrayEquals(rb.head(), db.getHead(), "head mismatch at HCN #" + count);
                assertArrayEquals(rb.tail(), db.getTail(), "tail mismatch at HCN #" + count);
            }
        } catch (Exception e) {
            fail(e.getMessage());
        }
    }

}
