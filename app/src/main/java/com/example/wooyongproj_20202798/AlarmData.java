package com.example.wooyongproj_20202798;

import java.util.Collections;
import java.util.List;

public class AlarmData {
    private String medName;
    private List<AlarmItem> alarmItems; // ✅ 수정됨

    public AlarmData() {} // Firestore용 기본 생성자

    public AlarmData(String medName, List<AlarmItem> alarmItems) {
        this.medName = medName;
        this.alarmItems = alarmItems;
    }

    public String getMedName() {
        return medName;
    }

    public List<AlarmItem> getAlarmItems() {
        return alarmItems;
    }

    public void setMedName(String medName) {
        this.medName = medName;
    }

    public void setAlarmItems(List<AlarmItem> alarmItems) {
        this.alarmItems = alarmItems;
    }

    private String date; // 🔹 Firestore 문서 키로 사용되는 날짜 (예: 2024-05-19)

    public AlarmData(String medName, List<AlarmItem> alarmItems, String date) {
        this.medName = medName;
        this.alarmItems = alarmItems;
        this.date = date;
    }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    // 🔧 추가: Firebase 배열 구조 지원
    private List<AlarmData> alarms;  // Firestore 배열 필드

    public List<AlarmData> getAlarms() { return alarms; }
    public void setAlarms(List<AlarmData> alarms) { this.alarms = alarms; }
}


