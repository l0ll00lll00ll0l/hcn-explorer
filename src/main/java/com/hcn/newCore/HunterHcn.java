package com.hcn.newCore;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@Builder
@Slf4j
public class HunterHcn implements Comparable<HunterHcn> {
    private final HunterBody hunterBody;
    private Prime lastActivePrime;
    private ScientificNumber value;
    private ScientificNumber factor;

    @Override
    public int compareTo(HunterHcn other) {
        return this.value.compareTo(other.value);
    }
}
