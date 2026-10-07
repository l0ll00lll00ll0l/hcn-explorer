package com.hcn.newCore;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@Builder
@Slf4j
public class Hcn implements Comparable<Hcn> {
    private HcnGenerator hcnGenerator;
    private Prime lastActivePrime;
    private ScientificNumber value;
    private ScientificNumber factor;
    @Builder.Default
    private Integer tempId = null;

    public int getLapiIndex() {
        return lastActivePrime.getIndex();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        hcnGenerator.getBody().buildChain(sb);
        return "{" + sb +
                " | " + getLapiIndex() +
                " | v: " + value +
                ", f: " + factor +
                '}';
    }

    @Override
    public int compareTo(Hcn other) {
        return this.value.compareTo(other.value);
    }
}
