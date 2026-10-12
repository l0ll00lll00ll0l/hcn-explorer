package com.hcn.newCore;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
public abstract class HcnGenerator {
    protected Body body;
    protected int intervalLapiDistance;

    public Hcn getHcn(Prime prime) {
        return Hcn.builder()
                .hcnGenerator(this)
                .value(getHcnValue(prime))
                .factor(getHcnFactor(prime))
                .lastActivePrime(prime)
                .build();
    }
    public ScientificNumber getHcnValue(Prime prime) {
        return body.getValue().multiply(prime.getValueMultiplier());
    }
    public ScientificNumber getHcnFactor(Prime prime) {
        return body.getFactor().multiply(prime.getFactorMultiplier());
    }
}
