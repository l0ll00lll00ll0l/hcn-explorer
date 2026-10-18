package com.hcn.newCore;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.List;

@Getter
@Setter
@SuperBuilder
public class HcnGenerator implements Comparable<HcnGenerator> {

    protected Body body;
    protected int intervalLapiDistance;
    private PassingZone hunterPassingZone;
    private Hcn hunterHcn;
    private List<Hcn> generatedHunterHcns;

    protected Hcn getHcn(Prime prime, HcnStatus status) {
        return Hcn.builder()
                .hcnGenerator(this)
                .value(getHcnValue(prime))
                .factor(getHcnFactor(prime))
                .lastActivePrime(prime)
                .hcnStatus(status)
                .build();
    }

    public Hcn generateHunterHcn(Prime prime, HcnStatus status) {
        hunterHcn = getHcn(prime, status);
        generatedHunterHcns.add(hunterHcn);
        return hunterHcn;
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

    @Override
    public int compareTo(HcnGenerator other) {
        return this.body.getValue().compareTo(other.body.getValue());
    }
}
