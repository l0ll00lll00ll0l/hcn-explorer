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
    private HcnStatus hcnStatus;
    private RecorderBody prayBody;
    private int intervalDistance;

    public int getLapiIndex() {
        return lastActivePrime.getIndex();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        hcnGenerator.getBody().buildChain(sb);
        String hcnStatus = "N/A";
        if (this.hcnStatus != null) {
            hcnStatus = this.hcnStatus.toString();
        }
        return sb +
                " | p[" + getLapiIndex() +
                "] | v: " + value +
                ", f: " + factor +
                " " + hcnStatus;
    }

    @Override
    public int compareTo(Hcn other) {
        return this.value.compareTo(other.value);
    }

    public String getStoredHcnToString() {
        return lastActivePrime.getIndex() + "_" + (hcnStatus == null ? "N/A" : hcnStatus.s);
    }

    public String getValueFactorString() {
        return hcnGenerator.getBody().getBodyChain() + " f: " + factor + " v: " + value;
    }

    public void removeHunterReference() {
        hcnGenerator.getReferencedHcns().remove(this);
    }
}
