package com.example.wooyongproj_20202798;

import java.util.ArrayList;
import java.util.List;

// 약물 정보를 담는 새로운 클래스
public class MedicationData {
    private String medicationId;        // 약물 고유 ID
    private String medicationName;      // 약물 이름 (예: "zzz")
    private List<String> activeDates;   // 활성 날짜들 (예: ["2025-06-10", "2025-06-11", "2025-06-12"])
    private List<AlarmItem> alarmItems; // 알람 아이템들
    private String userId;              // 사용자 ID
    private long createdAt;             // 생성 시간
    private long updatedAt;             // 수정 시간

    // 기본 생성자 (Firestore용)
    public MedicationData() {
        this.alarmItems = new ArrayList<>();
        this.activeDates = new ArrayList<>();
    }

    public MedicationData(String medicationId, String medicationName, String userId) {
        this.medicationId = medicationId;
        this.medicationName = medicationName;
        this.userId = userId;
        this.alarmItems = new ArrayList<>();
        this.activeDates = new ArrayList<>();
        this.createdAt = System.currentTimeMillis();
        this.updatedAt = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getMedicationId() { return medicationId; }
    public void setMedicationId(String medicationId) { this.medicationId = medicationId; }

    public String getMedicationName() { return medicationName; }
    public void setMedicationName(String medicationName) {
        this.medicationName = medicationName;
        this.updatedAt = System.currentTimeMillis();
    }

    public List<String> getActiveDates() { return activeDates; }
    public void setActiveDates(List<String> activeDates) {
        this.activeDates = activeDates;
        this.updatedAt = System.currentTimeMillis();
    }

    public List<AlarmItem> getAlarmItems() { return alarmItems; }
    public void setAlarmItems(List<AlarmItem> alarmItems) {
        this.alarmItems = alarmItems;
        this.updatedAt = System.currentTimeMillis();
    }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(long updatedAt) { this.updatedAt = updatedAt; }

    // 유틸리티 메서드들
    public void addActiveDate(String date) {
        if (!activeDates.contains(date)) {
            activeDates.add(date);
            this.updatedAt = System.currentTimeMillis();
        }
    }

    public void removeActiveDate(String date) {
        activeDates.remove(date);
        this.updatedAt = System.currentTimeMillis();
    }

    public boolean isActiveOnDate(String date) {
        return activeDates.contains(date);
    }

    public void addAlarmItem(AlarmItem item) {
        alarmItems.add(item);
        this.updatedAt = System.currentTimeMillis();
    }

    public void removeAlarmItem(int index) {
        if (index >= 0 && index < alarmItems.size()) {
            alarmItems.remove(index);
            this.updatedAt = System.currentTimeMillis();
        }
    }
}