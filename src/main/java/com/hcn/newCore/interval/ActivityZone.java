package com.hcn.newCore.interval;

import com.hcn.newCore.Hcn;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class ActivityZone {
    private final int hcnOrder;
    private final List<Hcn> hcns;
    private int skippingCount;
}
