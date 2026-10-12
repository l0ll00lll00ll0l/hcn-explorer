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

    private PassingZone passingZone;
    private Hcn hunterHcn;

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
