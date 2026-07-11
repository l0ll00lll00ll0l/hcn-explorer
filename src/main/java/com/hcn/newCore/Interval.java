package com.hcn.newCore;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@Builder
public class Interval {

    private int lapi;
    private ScientificNumber value;
    private ScientificNumber factor;
    private List<Hcn> hcnList;
    private Interval referenceInterval;

    public boolean isReferenced() {
        return referenceInterval != null && referenceInterval != this;
    }

    public Interval referenceCheck(Interval referenceInterval) {
        if (referenceInterval.getHcnList().size() != hcnList.size()) {
            this.referenceInterval = this;
            return this;
        }
        for (int i = 0; i < hcnList.size(); i++) {
            if (!referenceInterval.getHcnList().get(i).getBody().equals(hcnList.get(i).getBody())) {
                this.referenceInterval = this;
                return this;
            }
        }
        this.referenceInterval = referenceInterval;
        return referenceInterval;
    }
}
