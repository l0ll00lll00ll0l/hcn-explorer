package com.hcn.newCore;

public enum OffspringState {
    PARENT_EXCLUDER("X"),
    UNFILTERED("/"),
    DOMINATED_SUPERIOR_BODY("O"),
    HCN_GENERATOR("HCN"),
    HAS_ACTIVE_OFFSPRING_OPEN_END("OPEN"),
    HAS_ACTIVE_OFFSPRING_CLOSED_END("CLOSE"),
    DEACTIVATED_WITH_DEACTIVATED_OFFSPRING("DEACT-OFF"),
    DEACTIVATED_NO_OFFSPRING("DEACT");

    public final String s;

    OffspringState(String s) { this.s = s; }
}
