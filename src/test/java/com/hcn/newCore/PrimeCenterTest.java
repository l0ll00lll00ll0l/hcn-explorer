package com.hcn.newCore;
import org.junit.jupiter.api.Test;

public class PrimeCenterTest {

    @Test
    public void testGetPrime() {
        PrimeCenter.initialize();
        System.out.println(PrimeCenter.getPrime(4000));
    }
}
