package com.hcn.db;

import com.hcn.newCore.ScientificNumber;
import lombok.Getter;

import java.util.ArrayList;
import java.util.List;

@Getter
public class DbInterval {
    private final int lapi;
    private final ScientificNumber value;
    private final ScientificNumber factor;
    private final long firstHcn;
    private final int size;
    private final Integer referenceInterval;
    private final List<DbHcn> hcnlist = new ArrayList<>();

    public DbInterval(int lapi, double valueMantissa, long valueExponent, double factorMantissa, long factorExponent, long firstHcn, int size, Integer referenceInterval) {
        this.lapi = lapi;
        this.value = new ScientificNumber(valueMantissa, valueExponent);
        this.factor = new ScientificNumber(factorMantissa, factorExponent);
        this.firstHcn = firstHcn;
        this.size = size;
        this.referenceInterval = referenceInterval;
    }
}
