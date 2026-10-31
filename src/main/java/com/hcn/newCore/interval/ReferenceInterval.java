package com.hcn.newCore.interval;

import com.hcn.newCore.Hcn;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Builder
@Slf4j
public class ReferenceInterval {

    private static ReferenceInterval currentReferenceInterval;

    public static ReferenceInterval getCurrentReferenceInterval() {return currentReferenceInterval;}

    public static void initialize() {
        currentReferenceInterval = ReferenceInterval.builder().build();
        currentReferenceInterval.getActiveIntervals().add(ActiveInterval.getCurrentActiveInterval());
        //hcns added in recorderList initialize
    }

    private final List<Hcn> referenceRecorderList = new ArrayList<>();
    private final List<ActiveInterval> activeIntervals = new ArrayList<>();
}
