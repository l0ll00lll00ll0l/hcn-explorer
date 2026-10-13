package com.hcn.newCore;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

@Getter
@Setter
@SuperBuilder
public class HcnGenerator {

    protected Body body;
    protected int intervalLapiDistance;
    private PassingZone hunterPassingZone;
    private Hcn hunterHcn;
    private Hcn firstDominatedHcn = null;

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

    public HcnGenerator getPreviousHunter() {
        Body candidate = body.getSmallerBody();
        while (candidate != null && candidate.getHcnGenerator() instanceof RecorderBody) {candidate = candidate.getSmallerBody();}
        return candidate.getHcnGenerator();
    }

    @Override
    public String toString() {
        return body.toString() + " intervalLapiDistance: " + intervalLapiDistance;
    }
}
