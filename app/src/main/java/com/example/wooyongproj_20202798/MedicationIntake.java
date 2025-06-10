package com.example.wooyongproj_20202798;

import java.util.HashMap;
import java.util.Map;

// 약물 복용 체크 정보를 저장하는 클래스
public class MedicationIntake {
    private String medicationName;  // 약물명
    private String date;           // 날짜 (2025-06-11)
    private Map<String, Boolean> intakeStatus; // 복용 상태 (아침:true, 점심:false, 저녁:true)
    private long lastUpdated;      // 마지막 업데이트 시간

    // 기본 생성자 (Firestore용)
    public MedicationIntake() {
        this.intakeStatus = new HashMap<>();
    }

    public MedicationIntake(String medicationName, String date) {
        this.medicationName = medicationName;
        this.date = date;
        this.intakeStatus = new HashMap<>();
        this.lastUpdated = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getMedicationName() { return medicationName; }
    public void setMedicationName(String medicationName) { this.medicationName = medicationName; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public Map<String, Boolean> getIntakeStatus() { return intakeStatus; }
    public void setIntakeStatus(Map<String, Boolean> intakeStatus) { this.intakeStatus = intakeStatus; }

    public long getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(long lastUpdated) { this.lastUpdated = lastUpdated; }

    // 유틸리티 메서드들
    public void setIntakeForTime(String timeLabel, boolean taken) {
        if (intakeStatus == null) {
            intakeStatus = new HashMap<>();
        }
        intakeStatus.put(timeLabel, taken);
        this.lastUpdated = System.currentTimeMillis();
    }

    public boolean isIntakeForTime(String timeLabel) {
        if (intakeStatus == null) {
            return false;
        }
        return intakeStatus.getOrDefault(timeLabel, false);
    }

    // 해당 약물의 복용률 계산 (0-100)
    public int getCompletionPercentage() {
        if (intakeStatus == null || intakeStatus.isEmpty()) {
            return 0;
        }

        int totalAlarms = intakeStatus.size();
        int takenCount = 0;

        for (Boolean taken : intakeStatus.values()) {
            if (taken != null && taken) {
                takenCount++;
            }
        }

        return totalAlarms > 0 ? (takenCount * 100) / totalAlarms : 0;
    }

    // 복용한 횟수 반환
    public int getTakenCount() {
        if (intakeStatus == null) {
            return 0;
        }

        int count = 0;
        for (Boolean taken : intakeStatus.values()) {
            if (taken != null && taken) {
                count++;
            }
        }
        return count;
    }

    // 전체 알람 횟수 반환
    public int getTotalAlarmCount() {
        return intakeStatus != null ? intakeStatus.size() : 0;
    }
}