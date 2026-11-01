package com.hcn.newCore.interval;

import com.hcn.newCore.Hcn;
import com.hcn.newCore.HcnStatus;
import com.hcn.newCore.Passing;
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
public class ActiveInterval {

    private static ActiveInterval currentActiveInterval;

    public static ActiveInterval getCurrentActiveInterval() {return currentActiveInterval;}
    public static void setCurrentActiveInterval(ActiveInterval activeInterval) {ActiveInterval.currentActiveInterval = activeInterval;}

    public static void reset() {
        currentActiveInterval = null;
    }

    public static void initialize() {
        currentActiveInterval = ActiveInterval.builder().interval(0).build();
    }


    private int interval;
    private final List<Hcn> activityHcn = new ArrayList<>();
    private final List<Hcn> hcnProjections = new ArrayList<>();

}
