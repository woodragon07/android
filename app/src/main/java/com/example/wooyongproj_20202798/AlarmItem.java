package com.example.wooyongproj_20202798;

public class AlarmItem {
    private String label;
    private String time;
    private boolean enabled;

    public AlarmItem() {} // Firestore용

    public AlarmItem(String label, String time, boolean enabled) {
        this.label = label;
        this.time = time;
        this.enabled = enabled;
    }

    public String getLabel() { return label; }
    public String getTime() { return time; }
    public boolean isEnabled() { return enabled; }

    public void setLabel(String label) { this.label = label; }
    public void setTime(String time) { this.time = time; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
}
