package com.hcn.v7;

import java.util.ArrayList;
import java.util.List;

public class PrimeCenter {
    private static final List<Integer> primes = new ArrayList<>();
    
    static {
        primes.add(2);
        primes.add(3);
    }
    
    private PrimeCenter() {}
    
    public static int getPrime(int primeIndex) {
        if (primeIndex >= primes.size()) {
            generatePrimesUpTo(primeIndex);
        }
        return primes.get(primeIndex);
    }
    
    private static void generatePrimesUpTo(int targetIndex) {
        int candidate = primes.get(primes.size() - 1) + 2;
        
        while (primes.size() <= targetIndex) {
            if (isPrime(candidate)) {
                primes.add(candidate);
            }
            candidate += 2;
        }
    }
    
    private static boolean isPrime(int n) {
        for (int i = 0; primes.get(i) * primes.get(i) <= n; i++) {
            if (n % primes.get(i) == 0) return false;
        }
        return true;
    }
}
