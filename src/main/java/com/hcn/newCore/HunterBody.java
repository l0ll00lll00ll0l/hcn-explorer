package com.hcn.newCore;

import lombok.experimental.SuperBuilder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@SuperBuilder
@Slf4j
public class HunterBody extends HcnGenerator {

    private RecorderBody prayBody;

    public ScientificNumber getCurrentHcnFactor() {
        int prayLapiDistance = intervalLapiDistance - prayBody.getIntervalLapiDistance();
        return getHcnFactor(Prime.getPrimeByLapiDistance(prayBody.getIntervalLapiDistance())).divide(new ScientificNumber(Math.pow(2, prayLapiDistance), 0));
    }

    private ScientificNumber getCurrentHcnValue() {
        return  getHcnValue(Prime.getPrimeByLapiDistance(intervalLapiDistance));
    }

    public Hcn getCurrentHcn() {
        return Hcn.builder().hcnGenerator(this).value(getCurrentHcnValue()).factor(getCurrentHcnFactor()).lastActivePrime(Prime.getPrimeByLapiDistance(intervalLapiDistance)).build();
    }

    public HunterBody getPreviousHunter() {
        Body candidate = body.getSmallerBody();
        while (candidate != null && candidate.getHunterBody() == null) {candidate = candidate.getSmallerBody();}
        return candidate.getHunterBody();
    }

    @Override
    public String toString() {
        return body.toString() + " intervalLapiDistance: " + intervalLapiDistance;
    }
}
