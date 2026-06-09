package com.hcn.newCore;

import java.util.ArrayList;
import java.util.List;

public class PrimeCenter {
    private final List<Integer> primes;

    public PrimeCenter() {
        primes = new ArrayList<>(List.of(2, 3));
    }

    public int getPrime(int primeIndex) {
        if (primeIndex >= primes.size()) {
            generatePrimesUpTo(primeIndex);
        }
        return primes.get(primeIndex);
    }

    private void generatePrimesUpTo(int targetIndex) {
        int candidate = primes.get(primes.size() - 1) + 2;

        while (primes.size() <= targetIndex) {
            if (isPrime(candidate)) {
                primes.add(candidate);
            }
            candidate += 2;
        }
    }

    private boolean isPrime(int n) {
        for (int i = 0; primes.get(i) * primes.get(i) <= n; i++) {
            if (n % primes.get(i) == 0) return false;
        }
        return true;
    }
}
