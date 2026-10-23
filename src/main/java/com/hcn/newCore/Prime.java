package com.hcn.newCore;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;

@Builder
@Getter
@Setter
@Slf4j
public class Prime {
    private static Prime largestPrime;
    private static Prime firstPrime;
    private final int index;
    private final int intValue;
    private final ScientificNumber value;
    private final Prime previousPrime;
    private Prime nextPrime;
    private ScientificNumber valueMultiplier;
    private ScientificNumber factorMultiplier;

    public static Prime getFirstPrime() {
        return firstPrime;
    }

    public static void initialize() {
        Prime fakePreviousPrime = Prime.builder().index(-1).intValue(1).value(new ScientificNumber(2, -1))
                .previousPrime(null).factorMultiplier(new ScientificNumber(0.25, 0)).valueMultiplier(new ScientificNumber((double) 1 / 3, 0)).build();
        firstPrime = Prime.builder().index(0).intValue(2).value(new ScientificNumber(2, 0)).previousPrime(fakePreviousPrime)
                .valueMultiplier(new ScientificNumber((double) 1 / 3, 0)).factorMultiplier(new ScientificNumber(0.5, 0)).build();
        largestPrime = Prime.builder().index(1).intValue(3).value(new ScientificNumber(3, 0)).previousPrime(firstPrime).nextPrime(null)
                .valueMultiplier(new ScientificNumber(1, 0)).factorMultiplier(new ScientificNumber(1, 0)).build();
        firstPrime.setNextPrime(largestPrime);
        //getPrime( 1);
    }

    @Override
    public String toString() {
        return "Prime{" +
                "index=" + index +
                ", intValue=" + intValue +
                ", value=" + value +
                '}';
    }

    public static String print() {
        StringBuilder sb = new StringBuilder();
        sb.append("Primes: "+"[\n");

        Prime walker = firstPrime;
        while (walker != null) {
            sb.append(walker+" vm: "+walker.valueMultiplier+" fm: "+walker.factorMultiplier+"[\n");
            walker = walker.getNextPrime();
        }
        return sb.toString();
    }

    public Prime calculateNextPrimeIfNeeded() {
        if (nextPrime == null) {
            nextPrime = getPrime(index + 1);
        }
        return nextPrime;
    }

    public Prime calculateNextPrimeIfNeeded(int distance) {

        Prime primeWalker = this;
        for (int i = 0; i < distance; i++) {
            if (primeWalker.nextPrime == null) {
                primeWalker.nextPrime = getPrime(index + 1);
            }
           primeWalker = primeWalker.nextPrime;
        }
        return primeWalker;
    }

    public Prime getPreviousPrime(int distance) {
        Prime primeWalker = this;
        for (int i = 0; i < distance; i++) {
            primeWalker = primeWalker.getPreviousPrime();
        }
        return primeWalker;
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
                ScientificNumber value = new ScientificNumber(candidate, 0);
                largestPrime.setNextPrime(Prime.builder().index(largestPrime.getIndex() + 1).intValue(candidate)
                        .value(value).previousPrime(largestPrime).nextPrime(null)
                        .valueMultiplier(largestPrime.getValueMultiplier().multiply(value))
                        .factorMultiplier(largestPrime.getFactorMultiplier().multiply(new ScientificNumber(2, 0))).build());
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

    public static void recalculateAllMultipliers(Prime prevLastMatrixIndex) {
        Prime walker = firstPrime;
        while (walker != null) {
            walker.valueMultiplier = walker.valueMultiplier.divide(prevLastMatrixIndex.getValue());
            walker.factorMultiplier = walker.factorMultiplier.divide(new ScientificNumber(2, 0));
            walker = walker.nextPrime;
        }
    }

}
