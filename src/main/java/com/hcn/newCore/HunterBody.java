package com.hcn.newCore;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@Builder
@Slf4j
public class HunterBody {

    private Body body;
    private RecorderBody prayBody;
    private Integer prayLapiDistance;

    public ScientificNumber getCurrentHcnFactor() {
        return getHcnFactor(Prime.getPrimeByLapiDistance(prayBody.getIntervalLapiDistance())).divide(new ScientificNumber(Math.pow(2, prayLapiDistance), 0));
    }

    public ScientificNumber getCurrentHcnValue() {
        return  getHcnValue(Prime.getPrimeByLapiDistance(getOverallLapiDistance()));
    }

    public HunterHcn getCurrentHunterHcn() {
        return HunterHcn.builder().hunterBody(this).value(getCurrentHcnValue()).factor(getCurrentHcnFactor()).lastActivePrime(Prime.getPrimeByLapiDistance(getOverallLapiDistance())).build();
    }

    public int getOverallLapiDistance() {
        return prayBody.getIntervalLapiDistance() + prayLapiDistance;
    }

    public HunterHcn getProcessingHunterHcn() {
        int requiredIntervalDistance = getOverallLapiDistance() - (RecorderList.isCurrentRecorderFirstRecorder() ? 1 : 0);
        Prime lastActivePrime = Prime.getPrimeByLapiDistance(requiredIntervalDistance);
        return HunterHcn.builder()
                .hunterBody(this)
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

    public HunterBody getPreviousHunter() {
        Body candidate = body.getSmallerBody();
        while (candidate != null && candidate.getHunterBody() == null) {candidate = candidate.getSmallerBody();}
        return candidate.getHunterBody();
    }

    @Override
    public String toString() {
        return body.toString() + " prayLapiDistance: " + prayLapiDistance;
    }
}
