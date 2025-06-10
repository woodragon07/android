package com.example.wooyongproj_20202798;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.util.Log;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class AlarmReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "med_alarm_channel";
    private static final String TAG = "AlarmReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "=== 알람 수신됨 ===");
        Log.d(TAG, "Intent: " + intent);
        Log.d(TAG, "Action: " + intent.getAction());

        // BOOT_COMPLETED 인텐트는 무시 (앱 시작 시 불필요한 알림 방지)
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d(TAG, "BOOT_COMPLETED 인텐트 - 알림 표시하지 않음");
            // 여기서 필요하다면 기존 알람들을 다시 등록하는 로직을 추가할 수 있음
            return;
        }

        // Intent에서 데이터 추출
        String label = intent.getStringExtra("label");
        Log.d(TAG, "받은 label: " + label);

        // Intent의 모든 extras 로깅
        if (intent.getExtras() != null) {
            Log.d(TAG, "Intent extras: " + intent.getExtras().toString());
        } else {
            Log.w(TAG, "Intent extras가 null입니다");
        }

        createNotificationChannel(context);

        // label이 null이면 기본값 사용
        String notificationText;
        if (label != null && !label.trim().isEmpty()) {
            notificationText = label + " 복용 시간입니다";
        } else {
            notificationText = "약 복용 시간입니다";
            Log.w(TAG, "label이 null이거나 비어있음. 기본 메시지 사용");
        }

        Log.d(TAG, "알림 텍스트: " + notificationText);

        // 알림 소리와 진동 추가
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("💊 복용 알림")
                .setContentText(notificationText)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setDefaults(NotificationCompat.DEFAULT_ALL) // 소리, 진동, LED 모두 사용
                .setVibrate(new long[]{0, 1000, 500, 1000}); // 진동 패턴

        try {
            int notificationId = (int) System.currentTimeMillis();
            NotificationManagerCompat.from(context).notify(notificationId, builder.build());
            Log.d(TAG, "✅ 알림 표시 완료. ID: " + notificationId);
        } catch (Exception e) {
            Log.e(TAG, "알림 표시 실패", e);
        }

        Log.d(TAG, "=== 알람 처리 완료 ===");
    }

    private void createNotificationChannel(Context context) {
        try {
            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager.getNotificationChannel(CHANNEL_ID) == null) {
                NotificationChannel channel = new NotificationChannel(
                        CHANNEL_ID,
                        "약 복용 알림",
                        NotificationManager.IMPORTANCE_HIGH
                );
                channel.setDescription("약 복용 시간을 알려주는 알림입니다");

                // 소리 설정
                Uri defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
                if (defaultSoundUri == null) {
                    defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
                }

                AudioAttributes audioAttributes = new AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .build();

                channel.setSound(defaultSoundUri, audioAttributes);
                channel.enableVibration(true);
                channel.setVibrationPattern(new long[]{0, 1000, 500, 1000});
                channel.enableLights(true);
                channel.setLightColor(0xFF0000FF); // 파란색 LED

                manager.createNotificationChannel(channel);
                Log.d(TAG, "알림 채널 생성됨: " + CHANNEL_ID);
            } else {
                Log.d(TAG, "알림 채널 이미 존재: " + CHANNEL_ID);
            }
        } catch (Exception e) {
            Log.e(TAG, "알림 채널 생성 실패", e);
        }
    }
}