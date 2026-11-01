package com.hcn.newCore.interval;

import com.hcn.newCore.ScientificNumber;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class ProjectionBody {

    private ProjectionBody prev;
    private ProjectionBody next;
    private int lapiDistance;
    private int order;
    private ScientificNumber value;
    private ScientificNumber factor;
}
