package com.example.wooyongproj_20202798;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;

import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

public class AlarmNotificationHelper {

    private static final String TAG = "AlarmHelper";

    public static void scheduleAlarms(Context context, String dateKey, List<AlarmItem> items) {
        Log.d(TAG, "=== scheduleAlarms 시작 ===");
        Log.d(TAG, "dateKey: " + dateKey);
        Log.d(TAG, "items count: " + (items != null ? items.size() : "null"));

        if (items == null || items.isEmpty()) {
            Log.w(TAG, "알람 아이템이 없습니다");
            return;
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        // Android 12+ 권한 체크
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                Log.w(TAG, "정확한 알람 권한이 없습니다. 일반 알람으로 대체합니다.");
                scheduleAlarmsWithoutExact(context, dateKey, items);
                return;
            } else {
                Log.d(TAG, "정확한 알람 권한이 있습니다.");
            }
        }

        for (int i = 0; i < items.size(); i++) {
            AlarmItem item = items.get(i);
            Log.d(TAG, "--- 알람 " + i + " 처리 중 ---");
            Log.d(TAG, "item: " + (item != null ? "존재" : "null"));

            if (item == null) {
                Log.w(TAG, "알람 아이템이 null입니다. 스킵.");
                continue;
            }

            Log.d(TAG, "enabled: " + item.isEnabled());
            Log.d(TAG, "time: " + item.getTime());
            Log.d(TAG, "label: " + item.getLabel());

            if (!item.isEnabled()) {
                Log.d(TAG, "비활성화된 알람. 스킵.");
                continue;
            }

            Calendar cal = buildCalendar(dateKey, item.getTime());
            if (cal == null) {
                Log.e(TAG, "Calendar 생성 실패");
                continue;
            }

            // 현재 시간과 비교 (같은 시간대로)
            Calendar now = Calendar.getInstance();
            Log.d(TAG, "설정될 시간 (로컬): " + cal.getTime());
            Log.d(TAG, "현재 시간 (로컬): " + now.getTime());
            Log.d(TAG, "설정될 시간 millis: " + cal.getTimeInMillis());
            Log.d(TAG, "현재 시간 millis: " + now.getTimeInMillis());

            // 같은 날짜에 이미 지난 시간이면 다음 날로 설정
            if (cal.before(now)) {
                cal.add(Calendar.DAY_OF_MONTH, 1);
                Log.w(TAG, "과거 시간이므로 다음 날로 설정: " + cal.getTime());
            }

            Intent intent = new Intent(context, AlarmReceiver.class);
            intent.putExtra("label", item.getLabel());
            Log.d(TAG, "Intent에 전달할 label: " + item.getLabel());

            int requestCode = (dateKey + i).hashCode();
            Log.d(TAG, "requestCode: " + requestCode);

            PendingIntent pi = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            if (alarmManager != null) {
                try {
                    // 정확한 알람 설정 시도
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
                    Log.d(TAG, "✅ 정확한 알람 설정 완료: " + item.getLabel() + " at " + cal.getTime());
                } catch (SecurityException e) {
                    Log.e(TAG, "권한 없음, 일반 알람으로 대체: " + item.getLabel(), e);
                    // 권한 없을 때 일반 알람으로 대체
                    alarmManager.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
                    Log.d(TAG, "✅ 일반 알람 설정 완료: " + item.getLabel());
                }
            } else {
                Log.e(TAG, "AlarmManager가 null입니다");
            }
        }
        Log.d(TAG, "=== scheduleAlarms 완료 ===");
    }

    // 권한 없을 때 사용하는 일반 알람
    private static void scheduleAlarmsWithoutExact(Context context, String dateKey, List<AlarmItem> items) {
        Log.d(TAG, "일반 알람으로 설정 시작");
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        for (int i = 0; i < items.size(); i++) {
            AlarmItem item = items.get(i);
            if (!item.isEnabled()) continue;

            Calendar cal = buildCalendar(dateKey, item.getTime());
            if (cal == null) continue;

            // 과거 시간이면 다음 날로 설정
            Calendar now = Calendar.getInstance();
            if (cal.before(now)) {
                cal.add(Calendar.DAY_OF_MONTH, 1);
            }

            Intent intent = new Intent(context, AlarmReceiver.class);
            intent.putExtra("label", item.getLabel());
            int requestCode = (dateKey + i).hashCode();
            PendingIntent pi = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            if (alarmManager != null) {
                alarmManager.set(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
                Log.d(TAG, "일반 알람 설정 완료: " + item.getLabel());
            }
        }
    }

    public static void cancelAlarms(Context context, String dateKey, int itemCount) {
        Log.d(TAG, "알람 취소: " + dateKey + ", count: " + itemCount);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        for (int i = 0; i < itemCount; i++) {
            Intent intent = new Intent(context, AlarmReceiver.class);
            int requestCode = (dateKey + i).hashCode();
            PendingIntent pi = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            if (alarmManager != null) {
                alarmManager.cancel(pi);
                Log.d(TAG, "알람 취소됨: requestCode " + requestCode);
            }
        }
    }

    private static Calendar buildCalendar(String dateKey, String time) {
        try {
            Log.d(TAG, "Calendar 생성: dateKey=" + dateKey + ", time=" + time);
            String[] dateParts = dateKey.split("-");
            String[] timeParts = time.split(":");

            // 시스템 기본 시간대 사용 (한국 시간대)
            Calendar cal = Calendar.getInstance();

            // 로그로 현재 시간대 확인
            Log.d(TAG, "현재 시간대: " + cal.getTimeZone().getID());
            Log.d(TAG, "현재 시간대 표시명: " + cal.getTimeZone().getDisplayName());

            cal.set(Calendar.YEAR, Integer.parseInt(dateParts[0]));
            cal.set(Calendar.MONTH, Integer.parseInt(dateParts[1]) - 1);
            cal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(dateParts[2]));
            cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeParts[0]));
            cal.set(Calendar.MINUTE, Integer.parseInt(timeParts[1]));
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);

            Log.d(TAG, "생성된 Calendar: " + cal.getTime());
            return cal;
        } catch (Exception e) {
            Log.e(TAG, "Calendar 생성 실패", e);
            return null;
        }
    }

    // 알람 권한 상태 확인 메서드
    public static boolean canScheduleExactAlarms(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            return alarmManager.canScheduleExactAlarms();
        }
        return true;
    }
}