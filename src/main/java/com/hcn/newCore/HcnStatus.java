package com.hcn.newCore;

public enum HcnStatus {
    REFERENCED_HUNTER("RF"),
    RECORDER_SURVIVED("RS"),
    RECORDER_DEACTIVATED("RD"),
    FIRST_RECORDER("FR"),
    RECORDER_IN_PASSINGZONE("RP"),
    RECORDER_HUNTER("RH");

    public final String s;

    HcnStatus(String s) { this.s = s; }
}
