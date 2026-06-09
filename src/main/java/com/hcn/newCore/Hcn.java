package com.hcn.newCore;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class Hcn {
    private final Body body;
    private final int lapi;
    private ScientificNumber value;
    private ScientificNumber factor;
}
