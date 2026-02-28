package com.hcn.util;

import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PrimeBenchmark {
    
    @Test
    public void benchmarkApproaches() {
        int target = 100000;
        
        long start1 = System.nanoTime();
        approach1(target);
        long time1 = System.nanoTime() - start1;
        
        long start2 = System.nanoTime();
        approach2(target);
        long time2 = System.nanoTime() - start2;
        
        long start3 = System.nanoTime();
        approach3(target);
        long time3 = System.nanoTime() - start3;
        
        System.out.println("Approach 1 (HashMap, check all): " + time1/1_000_000 + "ms");
        System.out.println("Approach 2 (HashMap, skip evens): " + time2/1_000_000 + "ms");
        System.out.println("Approach 3 (ArrayList, skip evens): " + time3/1_000_000 + "ms");
    }
    
    // HashMap - check every number
    private int approach1(int targetIndex) {
        Map<Integer, Integer> primes = new HashMap<>();
        primes.put(0, 2);
        int currentIndex = 0;
        int candidate = 3;
        
        while (currentIndex < targetIndex) {
            if (isPrime1(candidate, primes)) {
                primes.put(++currentIndex, candidate);
            }
            candidate++;
        }
        return primes.get(targetIndex);
    }
    
    private boolean isPrime1(int n, Map<Integer, Integer> primes) {
        for (int i = 0; primes.get(i) * primes.get(i) <= n; i++) {
            if (n % primes.get(i) == 0) return false;
        }
        return true;
    }
    
    // HashMap - skip even numbers
    private int approach2(int targetIndex) {
        Map<Integer, Integer> primes = new HashMap<>();
        primes.put(0, 2);
        if (targetIndex == 0) return 2;
        
        int currentIndex = 0;
        int candidate = 3;
        
        while (currentIndex < targetIndex) {
            if (isPrime2(candidate, primes)) {
                primes.put(++currentIndex, candidate);
            }
            candidate += 2;
        }
        return primes.get(targetIndex);
    }
    
    private boolean isPrime2(int n, Map<Integer, Integer> primes) {
        for (int i = 0; primes.get(i) * primes.get(i) <= n; i++) {
            if (n % primes.get(i) == 0) return false;
        }
        return true;
    }
    
    // ArrayList - skip even numbers (better cache locality)
    private int approach3(int targetIndex) {
        List<Integer> primes = new ArrayList<>();
        primes.add(2);
        primes.add(3);
        if (targetIndex <= 1) return primes.get(targetIndex);
        
        int candidate = 5;
        
        while (primes.size() <= targetIndex) {
            if (isPrime3(candidate, primes)) {
                primes.add(candidate);
            }
            candidate += 2;
        }
        return primes.get(targetIndex);
    }
    
    private boolean isPrime3(int n, List<Integer> primes) {
        for (int i = 0; primes.get(i) * primes.get(i) <= n; i++) {
            if (n % primes.get(i) == 0) return false;
        }
        return true;
    }
}
