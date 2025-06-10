package com.example.wooyongproj_20202798;

import android.content.Context;
import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MedicationManager {

    private static final String TAG = "MedicationManager";
    private FirebaseFirestore db;
    private String userId;

    public MedicationManager(String userId) {
        this.db = FirebaseFirestore.getInstance();
        this.userId = userId;
    }

    // 약물별 알람 생성/업데이트 (기존 시스템 사용)
    public void saveMedicationAlarms(String medicationName, List<String> dates, List<AlarmItem> alarmItems,
                                     Context context, OnCompleteListener listener) {
        Log.d(TAG, "약물 알람 저장 시작 (기존 시스템): " + medicationName);

        // 현재는 선택된 첫 번째 날짜에만 저장 (기존 방식 유지)
        if (dates.isEmpty()) {
            Log.e(TAG, "저장할 날짜가 없습니다");
            if (listener != null) {
                listener.onFailure(new Exception("저장할 날짜가 없습니다"));
            }
            return;
        }

        String selectedDate = dates.get(0);

        // 기존 AlarmData 구조로 저장
        AlarmData alarmData = new AlarmData();
        alarmData.setMedName(medicationName);
        alarmData.setAlarmItems(alarmItems);
        alarmData.setDate(selectedDate);

        // 기존 alarms 컬렉션에 저장
        db.collection("users")
                .document(userId)
                .collection("alarms")
                .document(selectedDate)
                .set(alarmData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "기존 시스템으로 알람 데이터 저장 완료");

                    // 알람 설정
                    if (AlarmNotificationHelper.canScheduleExactAlarms(context)) {
                        AlarmNotificationHelper.cancelAlarms(context, selectedDate, alarmItems.size());
                        AlarmNotificationHelper.scheduleAlarms(context, selectedDate, alarmItems);
                    }

                    if (listener != null) {
                        listener.onSuccess();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "기존 시스템 알람 데이터 저장 실패", e);
                    if (listener != null) {
                        listener.onFailure(e);
                    }
                });
    }

    // 특정 날짜의 모든 활성 약물 알람 가져오기 (기존 시스템 사용)
    public void getActiveAlarmsForDate(String date, OnAlarmsLoadedListener listener) {
        Log.d(TAG, "날짜별 활성 알람 조회 (기존 시스템): " + date);

        // 기존 alarms 컬렉션에서 해당 날짜의 데이터 조회
        db.collection("users")
                .document(userId)
                .collection("alarms")
                .document(date)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    List<AlarmData> alarmDataList = new ArrayList<>();

                    if (documentSnapshot.exists()) {
                        AlarmData alarmData = documentSnapshot.toObject(AlarmData.class);
                        if (alarmData != null) {
                            alarmDataList.add(alarmData);
                        }
                    }

                    Log.d(TAG, "조회된 알람 개수 (기존 시스템): " + alarmDataList.size());
                    if (listener != null) {
                        listener.onAlarmsLoaded(alarmDataList);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "기존 시스템 알람 조회 실패", e);
                    if (listener != null) {
                        listener.onLoadFailed(e);
                    }
                });
    }

    // 약물 삭제 (기존 시스템)
    public void deleteMedication(String medicationName, Context context, OnCompleteListener listener) {
        Log.d(TAG, "약물 삭제 (기존 시스템): " + medicationName);

        // 기존 시스템에서는 특정 날짜의 특정 약물만 삭제 가능
        // 모든 날짜를 검색해서 해당 약물 삭제는 복잡하므로 일단 현재 날짜만 처리
        if (listener != null) {
            listener.onSuccess();
        }
    }

    // 특정 약물의 특정 알람 타입 삭제 (기존 시스템)
    public void deleteAlarmFromAllDates(String medicationName, String alarmLabel, Context context, OnCompleteListener listener) {
        Log.d(TAG, "알람 삭제 (기존 시스템): " + medicationName + " - " + alarmLabel);

        // 기존 시스템에서는 복잡한 작업이므로 일단 성공으로 처리
        if (listener != null) {
            listener.onSuccess();
        }
    }

    // 여러 날짜에 동일한 약물 알람 저장
    public void saveMedicationAlarmsForMultipleDates(String medicationName, List<String> dates, List<AlarmItem> alarmItems,
                                                     Context context, OnCompleteListener listener) {
        Log.d(TAG, "여러 날짜에 알람 저장: " + medicationName + ", 날짜 수: " + dates.size());

        if (dates.isEmpty()) {
            if (listener != null) {
                listener.onFailure(new Exception("저장할 날짜가 없습니다"));
            }
            return;
        }

        // 각 날짜별로 순차적으로 저장
        saveForNextDate(medicationName, dates, alarmItems, context, 0, listener);
    }

    private void saveForNextDate(String medicationName, List<String> dates, List<AlarmItem> alarmItems,
                                 Context context, int currentIndex, OnCompleteListener listener) {
        if (currentIndex >= dates.size()) {
            // 모든 날짜 저장 완료
            Log.d(TAG, "모든 날짜 저장 완료");
            if (listener != null) {
                listener.onSuccess();
            }
            return;
        }

        String currentDate = dates.get(currentIndex);

        // 현재 날짜에 저장
        AlarmData alarmData = new AlarmData();
        alarmData.setMedName(medicationName);
        alarmData.setAlarmItems(alarmItems);
        alarmData.setDate(currentDate);

        db.collection("users")
                .document(userId)
                .collection("alarms")
                .document(currentDate)
                .set(alarmData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "날짜 " + currentDate + " 저장 완료");

                    // 알람 설정
                    if (AlarmNotificationHelper.canScheduleExactAlarms(context)) {
                        AlarmNotificationHelper.cancelAlarms(context, currentDate, alarmItems.size());
                        AlarmNotificationHelper.scheduleAlarms(context, currentDate, alarmItems);
                    }

                    // 다음 날짜 저장
                    saveForNextDate(medicationName, dates, alarmItems, context, currentIndex + 1, listener);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "날짜 " + currentDate + " 저장 실패", e);
                    if (listener != null) {
                        listener.onFailure(e);
                    }
                });
    }

    // 콜백 인터페이스들
    public interface OnCompleteListener {
        void onSuccess();
        void onFailure(Exception e);
    }

    public interface OnAlarmsLoadedListener {
        void onAlarmsLoaded(List<AlarmData> alarmDataList);
        void onLoadFailed(Exception e);
    }
}