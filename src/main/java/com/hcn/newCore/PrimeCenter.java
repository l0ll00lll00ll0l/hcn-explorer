package com.hcn.newCore;

public class PrimeCenter {

    private static Prime largestPrime;
    private static Prime firstPrime;

    public static void initialize() {
        firstPrime = Prime.builder().index(0).intValue(2).value(new ScientificNumber(2, 0)).previousPrime(null).build();
        largestPrime = Prime.builder().index(1).intValue(3).value(new ScientificNumber(3, 0)).previousPrime(firstPrime).nextPrime(null).build();
        firstPrime.setNextPrime(largestPrime);
    }

    public static Prime getPrime(int primeIndex) {
        if (primeIndex > largestPrime.getIndex()) {
            generatePrimesUpTo(primeIndex);
            return largestPrime;
        } else {
            Prime prime = firstPrime;
            while (prime.getIndex() < primeIndex) {
                prime = prime.getNextPrime();
            }
            return prime;
        }
    }

    private static void generatePrimesUpTo(int targetIndex) {
        int candidate = largestPrime.getIntValue() + 2;

        while (largestPrime.getIndex() < targetIndex) {
            if (isPrime(candidate)) {
                largestPrime.setNextPrime(Prime.builder().index(largestPrime.getIndex() + 1).intValue(candidate).value(new ScientificNumber(candidate, 0)).previousPrime(largestPrime).nextPrime(null).build());
                largestPrime = largestPrime.getNextPrime();
            }
            candidate += 2;
        }
    }

    private static boolean isPrime(int n) {
        Prime divisor = firstPrime;
        while (divisor != null && divisor.getIntValue() * divisor.getIntValue() <= n) {
            if (n % divisor.getIntValue() == 0) return false;
            divisor = divisor.getNextPrime();
        }
        return true;
    }
}
