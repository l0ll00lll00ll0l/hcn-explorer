package com.hcn.newCore;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@Builder
@Slf4j
public class Hcn {
    private final Body body;
    private Lapi lapi;
    private ScientificNumber value;
    private ScientificNumber factor;
    @Builder.Default
    private Integer tempId = null;

    public int getLapiIndex() {
        return lapi.getPrime().getIndex();
    }

    public void matrixMaintainCheck() {

            body.setFirstSuperiorHcn(this);
            body.matrixMaintainCheck();

    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        body.buildChain(sb);
        return "{" + sb +
                " | " + getLapiIndex() +
                " | v: " + value +
                ", f: " + factor +
                '}';
    }
}
