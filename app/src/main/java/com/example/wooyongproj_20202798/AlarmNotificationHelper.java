package com.example.wooyongproj_20202798;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.util.Calendar;
import java.util.List;

public class AlarmNotificationHelper {

    public static void scheduleAlarms(Context context, String dateKey, List<AlarmItem> items) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        for (int i = 0; i < items.size(); i++) {
            AlarmItem item = items.get(i);
            if (!item.isEnabled()) continue;
            Calendar cal = buildCalendar(dateKey, item.getTime());
            if (cal == null || cal.before(Calendar.getInstance())) continue;

            Intent intent = new Intent(context, AlarmReceiver.class);
            intent.putExtra("label", item.getLabel());
            int requestCode = (dateKey + i).hashCode();
            PendingIntent pi = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            if (alarmManager != null) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.getTimeInMillis(), pi);
            }
        }
    }

    public static void cancelAlarms(Context context, String dateKey, int itemCount) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        for (int i = 0; i < itemCount; i++) {
            Intent intent = new Intent(context, AlarmReceiver.class);
            int requestCode = (dateKey + i).hashCode();
            PendingIntent pi = PendingIntent.getBroadcast(context, requestCode, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            if (alarmManager != null) {
                alarmManager.cancel(pi);
            }
        }
    }

    private static Calendar buildCalendar(String dateKey, String time) {
        try {
            String[] dateParts = dateKey.split("-");
            String[] timeParts = time.split(":");
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.YEAR, Integer.parseInt(dateParts[0]));
            cal.set(Calendar.MONTH, Integer.parseInt(dateParts[1]) - 1);
            cal.set(Calendar.DAY_OF_MONTH, Integer.parseInt(dateParts[2]));
            cal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(timeParts[0]));
            cal.set(Calendar.MINUTE, Integer.parseInt(timeParts[1]));
            cal.set(Calendar.SECOND, 0);
            return cal;
        } catch (Exception e) {
            return null;
        }
    }
}
