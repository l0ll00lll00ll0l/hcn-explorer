package com.hcn.util;

public class ExtensionEvent {
    private long time;
    private String type;
    private String label;
    
    public ExtensionEvent(long time, String type, String label) {
        this.time = time;
        this.type = type;
        this.label = label;
    }
    
    public long getTime() { return time; }
    public String getType() { return type; }
    public String getLabel() { return label; }
}
