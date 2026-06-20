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
    @Builder.Default
    private Integer tempId = null;

    public void matrixMaintainCheck() {
        if (!body.isProved()) {
            body.setFirstSuperiorHcn(this);
            body.matrixMaintainCheck();
        }
    }

    public void gotDominated() {
        body.setLastGeneratedHcn(this);
        body.gotDominated();
    }

    @Override
    public String toString() {
        return "Hcn{" +
                "body=" + body +
                ", lapi=" + lapi +
                ", value=" + value +
                ", factor=" + factor +
                '}';
    }
}
