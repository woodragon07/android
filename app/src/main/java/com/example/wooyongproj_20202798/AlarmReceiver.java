package com.example.wooyongproj_20202798;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class AlarmReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "med_alarm_channel";

    @Override
    public void onReceive(Context context, Intent intent) {
        String label = intent.getStringExtra("label");
        createNotificationChannel(context);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("복용 알림")
                .setContentText(label + " 복용 시간입니다")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManagerCompat.from(context).notify((int) System.currentTimeMillis(), builder.build());
    }

    private void createNotificationChannel(Context context) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "Medication Alarm", NotificationManager.IMPORTANCE_HIGH);
            manager.createNotificationChannel(channel);
        }
    }
}
