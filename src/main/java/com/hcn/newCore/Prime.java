package com.hcn.newCore;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Builder
@Getter
@Setter
public class Prime {
    private final int index;
    private final int intValue;
    private final ScientificNumber value;
    private final Prime previousPrime;
    private Prime nextPrime;

    @Override
    public String toString() {
        return "Prime{" +
                "index=" + index +
                ", intValue=" + intValue +
                ", value=" + value +
                '}';
    }

    public Prime getNextPrime() {
        if (nextPrime == null) PrimeCenter.getPrime(index + 1);
        return nextPrime;
    }
}
