package com.example.wooyongproj_20202798;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class IntakeManager {

    private static final String TAG = "IntakeManager";
    private static final String PREFS_NAME = "medication_intake";

    private FirebaseFirestore db;
    private String userId;
    private Context context;

    public IntakeManager(String userId) {
        this.db = FirebaseFirestore.getInstance();
        this.userId = userId;
    }

    public IntakeManager(String userId, Context context) {
        this.db = FirebaseFirestore.getInstance();
        this.userId = userId;
        this.context = context;
    }

    // 복용 상태 업데이트 (로컬 저장소 사용)
    public void updateIntakeStatus(String medicationName, String date, String timeLabel, boolean taken, OnCompleteListener listener) {
        Log.d(TAG, "복용 상태 업데이트 (로컬): " + medicationName + " " + date + " " + timeLabel + " " + taken);

        try {
            if (context != null) {
                SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                String key = userId + "_" + medicationName + "_" + date + "_" + timeLabel;

                SharedPreferences.Editor editor = prefs.edit();
                editor.putBoolean(key, taken);
                editor.putLong(key + "_timestamp", System.currentTimeMillis());
                editor.apply();

                Log.d(TAG, "로컬 복용 상태 저장 완료: " + key + " = " + taken);
            }

            if (listener != null) {
                listener.onSuccess();
            }
        } catch (Exception e) {
            Log.e(TAG, "로컬 복용 상태 저장 실패", e);
            if (listener != null) {
                listener.onFailure(e);
            }
        }
    }

    // 특정 날짜의 모든 약물 복용 상태 조회 (로컬에서)
    public void getIntakeStatusForDate(String date, OnIntakeLoadedListener listener) {
        Log.d(TAG, "날짜별 복용 상태 조회 (로컬): " + date);

        try {
            List<MedicationIntake> intakeList = new ArrayList<>();

            if (context != null) {
                SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                Map<String, MedicationIntake> intakeMap = new HashMap<>();

                // 해당 날짜의 모든 복용 기록 조회
                Map<String, ?> allPrefs = prefs.getAll();
                String keyPrefix = userId + "_";
                String dateSuffix = "_" + date + "_";

                for (Map.Entry<String, ?> entry : allPrefs.entrySet()) {
                    String key = entry.getKey();

                    if (key.startsWith(keyPrefix) && key.contains(dateSuffix) && !key.endsWith("_timestamp")) {
                        // key 형식: userId_medicationName_date_timeLabel
                        String[] parts = key.split("_");
                        if (parts.length >= 4) {
                            String medicationName = parts[1];
                            String timeLabel = parts[3];
                            boolean taken = (Boolean) entry.getValue();

                            // MedicationIntake 객체 생성 또는 업데이트
                            MedicationIntake intake = intakeMap.get(medicationName);
                            if (intake == null) {
                                intake = new MedicationIntake(medicationName, date);
                                intakeMap.put(medicationName, intake);
                            }

                            intake.setIntakeForTime(timeLabel, taken);
                        }
                    }
                }

                intakeList.addAll(intakeMap.values());
            }

            Log.d(TAG, "조회된 로컬 복용 상태 개수: " + intakeList.size());
            if (listener != null) {
                listener.onIntakeLoaded(intakeList);
            }
        } catch (Exception e) {
            Log.e(TAG, "로컬 복용 상태 조회 실패", e);
            if (listener != null) {
                listener.onLoadFailed(e);
            }
        }
    }

    // 특정 약물의 복용 상태 조회
    public void getIntakeStatusForMedication(String medicationName, String date, OnSingleIntakeLoadedListener listener) {
        try {
            MedicationIntake intake = null;

            if (context != null) {
                SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
                Map<String, ?> allPrefs = prefs.getAll();
                String keyPrefix = userId + "_" + medicationName + "_" + date + "_";

                intake = new MedicationIntake(medicationName, date);

                for (Map.Entry<String, ?> entry : allPrefs.entrySet()) {
                    String key = entry.getKey();

                    if (key.startsWith(keyPrefix) && !key.endsWith("_timestamp")) {
                        String timeLabel = key.substring(keyPrefix.length());
                        boolean taken = (Boolean) entry.getValue();
                        intake.setIntakeForTime(timeLabel, taken);
                    }
                }
            }

            if (listener != null) {
                listener.onIntakeLoaded(intake);
            }
        } catch (Exception e) {
            Log.e(TAG, "단일 복용 상태 조회 실패", e);
            if (listener != null) {
                listener.onLoadFailed(e);
            }
        }
    }

    // 전체 복용률 계산 (해당 날짜의 모든 약물)
    public static int calculateOverallCompletionRate(List<MedicationIntake> intakeList, List<AlarmData> alarmDataList) {
        if (alarmDataList == null || alarmDataList.isEmpty()) {
            return 0;
        }

        int totalAlarms = 0;
        int takenAlarms = 0;

        // 각 약물별로 등록된 알람 개수와 복용한 개수 계산
        for (AlarmData alarmData : alarmDataList) {
            String medicationName = alarmData.getMedName();

            if (alarmData.getAlarmItems() != null) {
                for (AlarmItem alarmItem : alarmData.getAlarmItems()) {
                    if (alarmItem.isEnabled()) {
                        totalAlarms++;

                        // 해당 약물의 복용 상태 찾기
                        MedicationIntake intake = findIntakeForMedication(intakeList, medicationName);
                        if (intake != null && intake.isIntakeForTime(alarmItem.getLabel())) {
                            takenAlarms++;
                        }
                    }
                }
            }
        }

        return totalAlarms > 0 ? (takenAlarms * 100) / totalAlarms : 0;
    }

    // 특정 약물의 복용 상태 찾기
    private static MedicationIntake findIntakeForMedication(List<MedicationIntake> intakeList, String medicationName) {
        if (intakeList == null) {
            return null;
        }

        for (MedicationIntake intake : intakeList) {
            if (medicationName.equals(intake.getMedicationName())) {
                return intake;
            }
        }
        return null;
    }

    // 콜백 인터페이스들
    public interface OnCompleteListener {
        void onSuccess();
        void onFailure(Exception e);
    }

    public interface OnIntakeLoadedListener {
        void onIntakeLoaded(List<MedicationIntake> intakeList);
        void onLoadFailed(Exception e);
    }

    public interface OnSingleIntakeLoadedListener {
        void onIntakeLoaded(MedicationIntake intake);
        void onLoadFailed(Exception e);
    }
}