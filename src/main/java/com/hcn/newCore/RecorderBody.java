package com.hcn.newCore;

import lombok.experimental.SuperBuilder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

@Getter
@Setter
@SuperBuilder
@Slf4j
public class RecorderBody extends HcnGenerator{

    private RecorderBody previousRecorder;
    private RecorderBody nextRecorder;
    private final List<HunterBody> hunters = new ArrayList<>();
    private final List<RecorderBody> recorderHunters = new ArrayList<>();
    private Hcn firstDominatedHcn = null;
    private Hcn firstSuperiorHcn = null;
    private RecorderBody prayBody = null;
    private PassingZone passingZone = null;


    @Override
    public String toString() {
        return body.toString() + " intervalLapiDistance: " + intervalLapiDistance;
    }
}
