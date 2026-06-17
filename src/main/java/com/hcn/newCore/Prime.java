package com.hcn.newCore;

import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class Prime {
    private final int index;
    private final int intValue;
    private final ScientificNumber value;

    @Override
    public String toString() {
        return "Prime{" +
                "index=" + index +
                ", intValue=" + intValue +
                ", value=" + value +
                '}';
    }
}
