package com.example.wooyongproj_20202798;

import android.content.Context;
import android.util.Log;

import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MedicationManager {

    private static final String TAG = "MedicationManager";
    private FirebaseFirestore db;
    private String userId;

    public MedicationManager(String userId) {
        this.db = FirebaseFirestore.getInstance();
        this.userId = userId;
    }

    // 🔧 새로 추가: 여러 알람을 지원하는 저장 메서드
    public void saveMedicationAlarmsMultiple(String medicationName, List<String> dates, List<AlarmItem> alarmItems,
                                             Context context, OnCompleteListener listener) {
        Log.d(TAG, "여러 알람 지원 저장 시작: " + medicationName + ", 날짜 수: " + dates.size());

        if (dates.isEmpty()) {
            Log.e(TAG, "저장할 날짜가 없습니다");
            if (listener != null) {
                listener.onFailure(new Exception("저장할 날짜가 없습니다"));
            }
            return;
        }

        // 각 날짜별로 순차적으로 저장
        saveMultipleForNextDate(medicationName, dates, alarmItems, context, 0, listener);
    }

    private void saveMultipleForNextDate(String medicationName, List<String> dates, List<AlarmItem> alarmItems,
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
        Log.d(TAG, "날짜 저장 시작: " + currentDate);

        // 🔧 새로운 알람 데이터 생성
        AlarmData newAlarmData = new AlarmData();
        newAlarmData.setMedName(medicationName);
        newAlarmData.setAlarmItems(alarmItems);
        newAlarmData.setDate(currentDate);

        // 🔧 기존 알람들을 확인하고 추가하는 로직
        db.collection("users")
                .document(userId)
                .collection("alarms")
                .document(currentDate)
                .get()
                .addOnSuccessListener(documentSnapshot -> {

                    if (documentSnapshot.exists()) {
                        // 기존 문서가 있는 경우 - 배열에 추가
                        try {
                            // 기존 배열 구조인지 확인
                            List<Map<String, Object>> existingAlarms =
                                    (List<Map<String, Object>>) documentSnapshot.get("alarms");

                            if (existingAlarms == null) {
                                existingAlarms = new ArrayList<>();

                                // 기존 단일 알람이 있다면 배열로 변환
                                AlarmData existingAlarm = documentSnapshot.toObject(AlarmData.class);
                                if (existingAlarm != null && existingAlarm.getMedName() != null) {
                                    existingAlarms.add(convertAlarmDataToMap(existingAlarm));
                                    Log.d(TAG, "기존 단일 알람을 배열로 변환: " + existingAlarm.getMedName());
                                }
                            }

                            // 새 알람 추가
                            existingAlarms.add(convertAlarmDataToMap(newAlarmData));

                            // 🔧 final 변수로 복사
                            final List<Map<String, Object>> finalExistingAlarms = existingAlarms;

                            // 배열 구조로 저장
                            Map<String, Object> documentData = new HashMap<>();
                            documentData.put("alarms", finalExistingAlarms);

                            // 문서 업데이트
                            db.collection("users")
                                    .document(userId)
                                    .collection("alarms")
                                    .document(currentDate)
                                    .set(documentData)
                                    .addOnSuccessListener(aVoid -> {
                                        Log.d(TAG, "배열 구조로 저장 성공: " + currentDate + " (총 " + finalExistingAlarms.size() + "개)");

                                        // 알람 설정
                                        if (AlarmNotificationHelper.canScheduleExactAlarms(context)) {
                                            AlarmNotificationHelper.scheduleAlarms(context, currentDate, alarmItems);
                                        }

                                        // 다음 날짜 저장
                                        saveMultipleForNextDate(medicationName, dates, alarmItems, context, currentIndex + 1, listener);
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "배열 구조 저장 실패: " + currentDate, e);
                                        if (listener != null) {
                                            listener.onFailure(e);
                                        }
                                    });

                        } catch (Exception e) {
                            Log.e(TAG, "기존 데이터 처리 실패: " + e.getMessage());
                            if (listener != null) {
                                listener.onFailure(e);
                            }
                        }

                    } else {
                        // 새 문서 생성 - 배열 구조로 시작
                        List<Map<String, Object>> newAlarms = new ArrayList<>();
                        newAlarms.add(convertAlarmDataToMap(newAlarmData));

                        Map<String, Object> documentData = new HashMap<>();
                        documentData.put("alarms", newAlarms);

                        db.collection("users")
                                .document(userId)
                                .collection("alarms")
                                .document(currentDate)
                                .set(documentData)
                                .addOnSuccessListener(aVoid -> {
                                    Log.d(TAG, "새 문서 생성 완료: " + currentDate);

                                    // 알람 설정
                                    if (AlarmNotificationHelper.canScheduleExactAlarms(context)) {
                                        AlarmNotificationHelper.scheduleAlarms(context, currentDate, alarmItems);
                                    }

                                    // 다음 날짜 저장
                                    saveMultipleForNextDate(medicationName, dates, alarmItems, context, currentIndex + 1, listener);
                                })
                                .addOnFailureListener(e -> {
                                    Log.e(TAG, "새 문서 생성 실패: " + currentDate, e);
                                    if (listener != null) {
                                        listener.onFailure(e);
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "문서 조회 실패: " + currentDate, e);
                    if (listener != null) {
                        listener.onFailure(e);
                    }
                });
    }

    // 🔧 수정: 여러 알람을 조회하는 메서드
    public void getActiveAlarmsForDate(String date, OnAlarmsLoadedListener listener) {
        Log.d(TAG, "날짜별 활성 알람 조회 (기존 시스템): " + date);

        db.collection("users")
                .document(userId)
                .collection("alarms")
                .document(date)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    List<AlarmData> alarmDataList = new ArrayList<>();

                    if (documentSnapshot.exists()) {
                        try {
                            // 🔧 새로운 배열 구조 시도
                            List<Map<String, Object>> alarmMaps =
                                    (List<Map<String, Object>>) documentSnapshot.get("alarms");

                            if (alarmMaps != null) {
                                // 배열 구조 처리
                                for (Map<String, Object> alarmMap : alarmMaps) {
                                    AlarmData alarmData = convertMapToAlarmData(alarmMap);
                                    if (alarmData != null) {
                                        alarmDataList.add(alarmData);
                                    }
                                }
                                Log.d(TAG, "배열 구조에서 조회된 알람 개수: " + alarmDataList.size());
                            } else {
                                // 🔧 기존 단일 구조 시도
                                AlarmData alarmData = documentSnapshot.toObject(AlarmData.class);
                                if (alarmData != null && alarmData.getMedName() != null) {
                                    alarmDataList.add(alarmData);
                                    Log.d(TAG, "단일 구조에서 조회된 알람: " + alarmData.getMedName());
                                }
                            }
                        } catch (Exception e) {
                            Log.w(TAG, "데이터 구조 파싱 실패, 기존 방식으로 재시도: " + e.getMessage());

                            // 기존 방식으로 재시도
                            AlarmData alarmData = documentSnapshot.toObject(AlarmData.class);
                            if (alarmData != null && alarmData.getMedName() != null) {
                                alarmDataList.add(alarmData);
                            }
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

    // 🔧 헬퍼 메서드들
    private Map<String, Object> convertAlarmDataToMap(AlarmData alarmData) {
        Map<String, Object> map = new HashMap<>();
        map.put("medName", alarmData.getMedName());
        map.put("date", alarmData.getDate());

        List<Map<String, Object>> itemMaps = new ArrayList<>();
        if (alarmData.getAlarmItems() != null) {
            for (AlarmItem item : alarmData.getAlarmItems()) {
                Map<String, Object> itemMap = new HashMap<>();
                itemMap.put("label", item.getLabel());
                itemMap.put("time", item.getTime());
                itemMap.put("enabled", item.isEnabled());
                itemMaps.add(itemMap);
            }
        }
        map.put("alarmItems", itemMaps);

        return map;
    }

    private AlarmData convertMapToAlarmData(Map<String, Object> map) {
        try {
            String medName = (String) map.get("medName");
            String date = (String) map.get("date");

            List<AlarmItem> items = new ArrayList<>();
            List<Map<String, Object>> itemMaps = (List<Map<String, Object>>) map.get("alarmItems");

            if (itemMaps != null) {
                for (Map<String, Object> itemMap : itemMaps) {
                    String label = (String) itemMap.get("label");
                    String time = (String) itemMap.get("time");
                    Boolean enabled = (Boolean) itemMap.get("enabled");
                    items.add(new AlarmItem(label, time, enabled != null ? enabled : false));
                }
            }

            AlarmData alarmData = new AlarmData();
            alarmData.setMedName(medName);
            alarmData.setDate(date);
            alarmData.setAlarmItems(items);

            return alarmData;
        } catch (Exception e) {
            Log.e(TAG, "Map을 AlarmData로 변환 실패", e);
            return null;
        }
    }

    // 기존 메서드들은 그대로 유지
    public void saveMedicationAlarms(String medicationName, List<String> dates, List<AlarmItem> alarmItems,
                                     Context context, OnCompleteListener listener) {
        Log.d(TAG, "약물 알람 저장 시작 (기존 시스템): " + medicationName);

        if (dates.isEmpty()) {
            Log.e(TAG, "저장할 날짜가 없습니다");
            if (listener != null) {
                listener.onFailure(new Exception("저장할 날짜가 없습니다"));
            }
            return;
        }

        String selectedDate = dates.get(0);

        AlarmData alarmData = new AlarmData();
        alarmData.setMedName(medicationName);
        alarmData.setAlarmItems(alarmItems);
        alarmData.setDate(selectedDate);

        db.collection("users")
                .document(userId)
                .collection("alarms")
                .document(selectedDate)
                .set(alarmData)
                .addOnSuccessListener(aVoid -> {
                    Log.d(TAG, "기존 시스템으로 알람 데이터 저장 완료");

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

    public void deleteMedication(String medicationName, Context context, OnCompleteListener listener) {
        Log.d(TAG, "약물 삭제 (기존 시스템): " + medicationName);

        if (listener != null) {
            listener.onSuccess();
        }
    }

    public void deleteAlarmFromAllDates(String medicationName, String alarmLabel, Context context, OnCompleteListener listener) {
        Log.d(TAG, "알람 삭제 (기존 시스템): " + medicationName + " - " + alarmLabel);

        if (listener != null) {
            listener.onSuccess();
        }
    }

    public void saveMedicationAlarmsForMultipleDates(String medicationName, List<String> dates, List<AlarmItem> alarmItems,
                                                     Context context, OnCompleteListener listener) {
        Log.d(TAG, "여러 날짜에 알람 저장: " + medicationName + ", 날짜 수: " + dates.size());

        if (dates.isEmpty()) {
            if (listener != null) {
                listener.onFailure(new Exception("저장할 날짜가 없습니다"));
            }
            return;
        }

        saveForNextDate(medicationName, dates, alarmItems, context, 0, listener);
    }

    private void saveForNextDate(String medicationName, List<String> dates, List<AlarmItem> alarmItems,
                                 Context context, int currentIndex, OnCompleteListener listener) {
        if (currentIndex >= dates.size()) {
            Log.d(TAG, "모든 날짜 저장 완료");
            if (listener != null) {
                listener.onSuccess();
            }
            return;
        }

        String currentDate = dates.get(currentIndex);

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

                    if (AlarmNotificationHelper.canScheduleExactAlarms(context)) {
                        AlarmNotificationHelper.cancelAlarms(context, currentDate, alarmItems.size());
                        AlarmNotificationHelper.scheduleAlarms(context, currentDate, alarmItems);
                    }

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