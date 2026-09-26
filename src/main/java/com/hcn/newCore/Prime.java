package com.hcn.newCore;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;

@Builder
@Getter
@Setter
public class Prime {
    private static final ArrayList<Prime> hcnProducerPrimes = new ArrayList<>();
    private static Prime largestPrime;
    private static Prime firstPrime;
    private final int index;
    private final int intValue;
    private final ScientificNumber value;
    private final Prime previousPrime;
    private Prime nextPrime;
    private ScientificNumber valueMultiplier;
    private ScientificNumber factorMultiplier;

    public static Prime getLowestHcnProducerPrime() {
        return hcnProducerPrimes.get(hcnProducerPrimes.size() - 1);
    }
    public static Prime getHighestHcnProducerPrime() {
        return hcnProducerPrimes.get(0);
    }
    public static ArrayList<Prime> getHcnProducerPrimes() {
        return hcnProducerPrimes;
    }
    public static Prime getFirstPrime() {
        return firstPrime;
    }

    public static void initialize() {
        firstPrime = Prime.builder().index(0).intValue(2).value(new ScientificNumber(2, 0)).previousPrime(null).build();
        largestPrime = Prime.builder().index(1).intValue(3).value(new ScientificNumber(3, 0)).previousPrime(firstPrime).nextPrime(null).build();
        firstPrime.setNextPrime(largestPrime);
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

        Prime walker = getLowestHcnProducerPrime();
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
        Prime walker = getLowestHcnProducerPrime();
        while (walker != null) {
            walker.valueMultiplier = walker.valueMultiplier.divide(prevLastMatrixIndex.getValue());
            walker.factorMultiplier = walker.factorMultiplier.divide(new ScientificNumber(2, 0));
            walker = walker.nextPrime;
        }
    }

    public static void addNextHcnProducer() {
        hcnProducerPrimes.add(0, getHighestHcnProducerPrime().calculateNextPrimeIfNeeded());
    }

    public static Prime getPrimeByLapiDistance(int lapiDistance) {
        if (lapiDistance > -1) {
            if (lapiDistance < hcnProducerPrimes.size()) {
                return hcnProducerPrimes.get(lapiDistance);
            } else {
                Prime walker = getHighestHcnProducerPrime();
                for (int i = 0; i < lapiDistance; i++) {
                    walker = walker.getPreviousPrime();
                }
                return walker;
            }

        } else {
            Prime walker = getHighestHcnProducerPrime();
            for (int i = 0; i > lapiDistance; i--) {
                walker = walker.calculateNextPrimeIfNeeded();
            }
            return walker;
        }
    }
}
