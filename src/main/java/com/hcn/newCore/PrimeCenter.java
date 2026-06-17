package com.hcn.newCore;

import java.util.ArrayList;
import java.util.List;

public class PrimeCenter {
    private final List<Prime> primes;

    public PrimeCenter() {
        primes = new ArrayList<>(List.of(Prime.builder().index(0).intValue(2).value(new ScientificNumber(2, 0)).build(),
                Prime.builder().index(1).intValue(3).value(new ScientificNumber(3, 0)).build()));
    }

    public Prime getPrime(int primeIndex) {
        if (primeIndex >= primes.size()) {
            generatePrimesUpTo(primeIndex);
        }
        return primes.get(primeIndex);
    }

    private void generatePrimesUpTo(int targetIndex) {
        int candidate = primes.get(primes.size() - 1).getIntValue() + 2;

        while (primes.size() <= targetIndex) {
            if (isPrime(candidate)) {
                primes.add(Prime.builder().index(primes.size()).intValue(candidate).value(new ScientificNumber(candidate, 0)).build());
            }
            candidate += 2;
        }
    }

    private boolean isPrime(int n) {
        for (int i = 0; primes.get(i).getIntValue() * primes.get(i).getIntValue() <= n; i++) {
            if (n % primes.get(i).getIntValue() == 0) return false;
        }
        return true;
    }
}
