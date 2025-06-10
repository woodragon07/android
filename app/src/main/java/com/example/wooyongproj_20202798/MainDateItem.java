package com.example.wooyongproj_20202798;

public class MainDateItem {
    public int year, month, day;
    public String dayOfWeek;
    public boolean isSelected;

    public MainDateItem(int year, int month, int day, String dayOfWeek, boolean isSelected) {
        this.year = year;
        this.month = month;
        this.day = day;
        this.dayOfWeek = dayOfWeek;
        this.isSelected = isSelected;
    }

    public String getFormattedDate() {
        return String.format("%04d-%02d-%02d", year, month, day);
    }

    public int getDay() {
        return this.day;
    }

    public String getDayOfWeek() {
        return this.dayOfWeek;
    }

    public boolean isSelected() {
        return this.isSelected;
    }

    public void setSelected(boolean selected) {
        this.isSelected = selected;
    }
}
