package com.hcn.newCore;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;

@Builder
@Getter
@Setter
public class Prime {
    private final int index;
    private final int intValue;
    private final ScientificNumber value;
    private final Prime previousPrime;
    private Prime nextPrime;
    private ScientificNumber valueMultiplier;
    private ScientificNumber factorMultiplier;
    private static final ArrayList<Prime> hcnProducerPrimes = new ArrayList<>();

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
            nextPrime = PrimeCenter.getPrime(index + 1);
        }
        return nextPrime;
    }

    public static void recalculateAllMultipliers(Prime prevLastMatrixIndex) {
        System.out.println(" pp: " + prevLastMatrixIndex.getIntValue());
        Prime walker = getLowestHcnProducerPrime();
        System.out.println(" walker: " + walker);
        while (walker != null) {
            walker.valueMultiplier = walker.valueMultiplier.divide(prevLastMatrixIndex.getValue());
            walker.factorMultiplier = walker.factorMultiplier.divide(new ScientificNumber(2, 0));
            walker = walker.nextPrime;
            System.out.println(" walker: " + walker);
        }
    }

    public static Prime getLowestHcnProducerPrime() {
        return hcnProducerPrimes.get(hcnProducerPrimes.size() - 1);
    }

    public static Prime getHighestHcnProducerPrime() {
        return hcnProducerPrimes.get(0);
    }


    public static void deleteHcnProducerPrimeUnder() {
        //log.debug("DeletingLapi Under {}", Interval.getCurrentInterval().getLowestRecorderLapi());

        while (getLowestHcnProducerPrime().getIndex() < Interval.getCurrentInterval().getLowestRecorderLapi()) {
            //log.debug(" deleting lowestLapi= {}", lowestLapi.getPrime().getIndex());
            hcnProducerPrimes.remove(getLowestHcnProducerPrime());
        }
    }

    public static ArrayList<Prime> getHcnProducerPrimes() {
        return hcnProducerPrimes;
    }

    public static void addNextHcnProducer() {
        hcnProducerPrimes.add(0, getHighestHcnProducerPrime().calculateNextPrimeIfNeeded());
    }

    public static Prime getPrimeByLapiDistance(int lapiDistance) {
        if (lapiDistance > -1) {
            return hcnProducerPrimes.get(lapiDistance);
        } else {
            Prime walker = getHighestHcnProducerPrime();
            for (int i = 0; i > lapiDistance; i--) {
                walker = walker.calculateNextPrimeIfNeeded();
            }
            return walker;
        }
    }
}
