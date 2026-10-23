package com.hcn.newCore;

public enum HcnStatus {
    //NA,
    NONPASSING_HCN("NP"),
    DELETION_FROM_BODYLIST("DEL"),
    HUNTER_IN_PASSINGZONE("HP"),
    REFERENCED_HUNTER("RF"),
    RECORDER_SURVIVED("RS"),
    RECORDER_DEACTIVATED("RD"),
    FIRST_RECORDER("FR"),
    RECORDER_IN_PASSINGZONE("RP"),
    RECORDER_HUNTER("RH"),
    DELETED_BY_PRAYPRIME_DECREASE("D"),
    HUNTER_DURING_FINDPRAY("H-FP");


    public final String s;

    HcnStatus(String s) { this.s = s; }
}
