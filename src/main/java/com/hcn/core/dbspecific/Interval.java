package com.hcn.core.dbspecific;

import com.hcn.core.Hcn;
import com.hcn.core.ScientificNumber;

import java.util.ArrayList;
import java.util.List;

public class Interval {

    private final List<Hcn> hcnList = new ArrayList<>();
    private Interval referenceInterval;
    private final int lapi;
    private final ScientificNumber value;
    private final ScientificNumber factor;

    public List<Hcn> getHcnList() {return hcnList;}
    public Interval getReferenceInterval() {return referenceInterval;}
    public int getLapi() {return lapi;}
    public ScientificNumber getValue() {return value;}
    public ScientificNumber getFactor() {return factor;}


    public Interval(List<Hcn> provedHcns, Interval referenceInterval) {

        lapi = provedHcns.get(0).getLastActivePrime();
        value = provedHcns.get(0).getValue();
        factor = provedHcns.get(0).getFactor();

        boolean isSame = true;

        if (referenceInterval != null && referenceInterval.hcnList.size() == provedHcns.size()) {

            for (int i = 0; i < provedHcns.size(); i++) {
                if (!referenceInterval.hcnList.get(i).getHcnGenerator().equals(provedHcns.get(i).getHcnGenerator())) {
                    isSame = false;
                    break;
                }
            }
        } else {
            isSame = false;
        }

        if (isSame) {
            this.referenceInterval = referenceInterval;
        } else {
            hcnList.addAll(provedHcns);
            this.referenceInterval = this;
        }
    }

    private Interval(int lapi, ScientificNumber value, ScientificNumber factor) {
        this.lapi = lapi;
        this.value = value;
        this.factor = factor;
        this.referenceInterval = null;
    }

    public static Interval fromLoad(int lapi, ScientificNumber value, ScientificNumber factor) {
        return new Interval(lapi, value, factor);
    }

    public void setReferenceIntervalForLoad(Interval ref) {
        this.referenceInterval = ref;
    }
}
