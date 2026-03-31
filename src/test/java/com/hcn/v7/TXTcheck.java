package com.hcn.v7;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class TXTcheck {

    private final List<int[]> referenceHcns = new ArrayList<>();

    public TXTcheck() {
        loadFromResource("hcn.txt");
    }

    private void loadFromResource(String filename) {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream(filename);
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) continue;
                referenceHcns.add(parseLine(line));
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to load " + filename, e);
        }
    }

    static int[] parseLine(String line) {
        String[] tokens = line.split("\\s+");
        List<Integer> exponents = new ArrayList<>();
        for (int i = 1; i < tokens.length; i++) {
            String[] parts = tokens[i].split("\\^");
            int power = Integer.parseInt(parts[0]);
            int count = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
            for (int j = 0; j < count; j++) {
                exponents.add(power);
            }
        }
        return exponents.stream().mapToInt(Integer::intValue).toArray();
    }

    public static int[] exponentSignature(Hcn hcn, ActivePrimeIndex lastActivePrimeIndex) {
        // only check active prime indexes from the body chain
        List<int[]> pairs = new ArrayList<>();
        HcnBody current = hcn.getBody();
        while (current != null) {
            pairs.add(0, new int[]{
                    current.getPip().getActivePrimeIndex().getIndex(),
                    current.getPip().getPower()
            });
            current = current.getParent();
        }
        return pairs.stream().mapToInt(p -> p[1]).toArray();
    }

    public static int[] referenceAtActiveIndexes(int[] fullRef, int[] activeIndexes) {
        int[] result = new int[activeIndexes.length];
        for (int i = 0; i < activeIndexes.length; i++) {
            if (activeIndexes[i] < fullRef.length) {
                result[i] = fullRef[activeIndexes[i]];
            } else {
                result[i] = 1;
            }
        }
        return result;
    }

    public static int[] getActiveIndexes(Hcn hcn) {
        List<Integer> indexes = new ArrayList<>();
        HcnBody current = hcn.getBody();
        while (current != null) {
            indexes.add(0, current.getPip().getActivePrimeIndex().getIndex());
            current = current.getParent();
        }
        return indexes.stream().mapToInt(Integer::intValue).toArray();
    }

    public List<int[]> getReferenceHcns() {
        return referenceHcns;
    }

    public int size() {
        return referenceHcns.size();
    }

    public static String signatureToString(int[] sig) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < sig.length; i++) {
            if (i > 0) sb.append(", ");
            sb.append(sig[i]);
        }
        return sb.append("]").toString();
    }

    public static boolean signaturesEqual(int[] a, int[] b) {
        if (a.length != b.length) return false;
        for (int i = 0; i < a.length; i++) {
            if (a[i] != b[i]) return false;
        }
        return true;
    }
}
