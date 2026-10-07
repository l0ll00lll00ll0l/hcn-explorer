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

    public abstract ScientificNumber getCurrentHcnFactor();
    public abstract Hcn getCurrentHcn();
    public Hcn getProcessingHcn() {
        int requiredIntervalDistance = intervalLapiDistance - (RecorderList.isCurrentRecorderFirstRecorder() ? 1 : 0);
        Prime lastActivePrime = Prime.getPrimeByLapiDistance(requiredIntervalDistance);
        return Hcn.builder()
                .hcnGenerator(this)
                .value(getHcnValue(lastActivePrime))
                .factor(getHcnFactor(lastActivePrime))
                .lastActivePrime(lastActivePrime)
                .build();
    }
    public ScientificNumber getHcnValue(Prime prime) {
        return body.getValue().multiply(prime.getValueMultiplier());
    }
    public ScientificNumber getHcnFactor(Prime prime) {
        return body.getFactor().multiply(prime.getFactorMultiplier());
    }
}
