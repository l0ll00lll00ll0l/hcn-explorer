package com.hcn.newCore;

import lombok.experimental.SuperBuilder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@SuperBuilder
@Slf4j
public class RecorderBody extends HcnGenerator{

    private RecorderBody previousRecorder;
    private RecorderBody nextRecorder;
    private final List<RecorderBody> recorderHunters = new ArrayList<>();
    private Hcn prayHcn;
    private RecorderBody prayBody = null;
    private PassingZone passingZone = null;
    private final List<Hcn> generatedRecorderHcns = new ArrayList<>();


    @Override
    public String toString() {
        return body.toString() + " intervalLapiDistance: " + intervalLapiDistance;
    }


    public Hcn getRecorderHcn(Prime prime, HcnStatus status) {
        prayHcn = getHcn(prime, status);
        generatedRecorderHcns.add(prayHcn);
        return prayHcn;
    }
}
