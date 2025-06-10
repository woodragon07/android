package com.example.wooyongproj_20202798;

import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Locale;

public class AlarmDetailActivity extends AppCompatActivity {

    private static final String TAG = "AlarmDetailActivity";

    private RecyclerView recyclerView;
    private AlarmDetailAdapter adapter;
    private ArrayList<AlarmItem> alarmItems = new ArrayList<>();
    private MedicationManager medicationManager;

    private String userId;
    private String selectedDate;
    private String medName;
    private Button btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_detail);

        // 현재 시간 정보 로깅
        logCurrentTimeInfo();

        userId = getCurrentUserId();
        medicationManager = new MedicationManager(userId);

        recyclerView = findViewById(R.id.recyclerViewAlarmDetail);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // 어댑터 사용 (알람 추가/삭제 기능 포함)
        adapter = new AlarmDetailAdapter(alarmItems, this);
        recyclerView.setAdapter(adapter);

        btnSave = findViewById(R.id.btnSave);

        selectedDate = getIntent().getStringExtra("selectedDate");
        medName = getIntent().getStringExtra("medName");

        Log.d(TAG, "=== AlarmDetailActivity 시작 ===");
        Log.d(TAG, "userId: " + userId);
        Log.d(TAG, "받은 selectedDate: " + selectedDate);
        Log.d(TAG, "받은 medName: " + medName);

        if (selectedDate == null || medName == null) {
            Toast.makeText(this, "필수 정보가 누락되었습니다", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        btnSave.setOnClickListener(v -> {
            logAlarmItemsBeforeSave();

            if (checkAlarmPermission()) {
                saveAlarmData();
            } else {
                requestAlarmPermission();
            }
        });

        loadAlarmData();
    }

    private void logCurrentTimeInfo() {
        Calendar now = Calendar.getInstance();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

        Log.d(TAG, "=== 현재 시간 정보 ===");
        Log.d(TAG, "현재 시간: " + sdf.format(now.getTime()));
        Log.d(TAG, "현재 TimeZone: " + now.getTimeZone().getID());
        Log.d(TAG, "현재 millis: " + now.getTimeInMillis());
    }

    private void logAlarmItemsBeforeSave() {
        Log.d(TAG, "=== 저장할 알람 데이터 ===");
        Log.d(TAG, "총 알람 개수: " + alarmItems.size());
        for (int i = 0; i < alarmItems.size(); i++) {
            AlarmItem item = alarmItems.get(i);
            Log.d(TAG, "알람 " + i + ":");
            Log.d(TAG, "  - label: " + item.getLabel());
            Log.d(TAG, "  - time: " + item.getTime());
            Log.d(TAG, "  - enabled: " + item.isEnabled());
        }
    }

    private void loadAlarmData() {
        Log.d(TAG, "약물별 알람 데이터 로드 시작");

        // 해당 날짜의 해당 약물 알람 정보 조회
        medicationManager.getActiveAlarmsForDate(selectedDate, new MedicationManager.OnAlarmsLoadedListener() {
            @Override
            public void onAlarmsLoaded(java.util.List<AlarmData> alarmDataList) {
                Log.d(TAG, "조회된 알람 데이터 개수: " + alarmDataList.size());

                // 해당 약물의 알람 데이터 찾기
                for (AlarmData alarmData : alarmDataList) {
                    if (medName.equals(alarmData.getMedName())) {
                        alarmItems.clear();
                        if (alarmData.getAlarmItems() != null) {
                            alarmItems.addAll(alarmData.getAlarmItems());
                        }
                        adapter.notifyDataSetChanged();

                        Log.d(TAG, "로드된 " + medName + " 알람 개수: " + alarmItems.size());
                        return;
                    }
                }

                // 해당 약물의 데이터가 없으면 기본 알람 생성
                Log.d(TAG, "기존 데이터 없음, 기본 알람 생성");
                createDefaultAlarms();
            }

            @Override
            public void onLoadFailed(Exception e) {
                Log.e(TAG, "알람 데이터 로드 실패", e);
                Toast.makeText(AlarmDetailActivity.this, "데이터 로드 실패", Toast.LENGTH_SHORT).show();
                createDefaultAlarms();
            }
        });
    }

    private void createDefaultAlarms() {
        alarmItems.clear();
        alarmItems.add(new AlarmItem("아침", "08:00", true));
        alarmItems.add(new AlarmItem("점심", "12:00", true));
        alarmItems.add(new AlarmItem("저녁", "18:00", true));
        adapter.notifyDataSetChanged();
    }

    private void saveAlarmData() {
        Log.d(TAG, "약물별 알람 데이터 저장 시작");

        // 현재 약물의 모든 활성 날짜 조회 후 업데이트
        java.util.List<String> activeDates = new ArrayList<>();
        activeDates.add(selectedDate); // 일단 현재 날짜만 추가 (실제로는 기존 날짜들도 포함해야 함)

        medicationManager.saveMedicationAlarms(medName, activeDates, alarmItems, this,
                new MedicationManager.OnCompleteListener() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "약물 알람 저장 완료");
                        Toast.makeText(AlarmDetailActivity.this, "저장 완료!", Toast.LENGTH_SHORT).show();
                        finish();
                    }

                    @Override
                    public void onFailure(Exception e) {
                        Log.e(TAG, "약물 알람 저장 실패", e);
                        Toast.makeText(AlarmDetailActivity.this, "저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private String getCurrentUserId() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getEmail() != null) {
            return user.getEmail().split("@")[0];
        }
        return "unknown_user";
    }

    private boolean checkAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
            boolean canSchedule = alarmManager.canScheduleExactAlarms();
            Log.d(TAG, "정확한 알람 권한 상태: " + canSchedule);
            return canSchedule;
        }
        return true;
    }

    private void requestAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            new AlertDialog.Builder(this)
                    .setTitle("알람 권한 필요")
                    .setMessage("정확한 시간에 알람을 받으려면 '정확한 알람' 권한이 필요합니다.")
                    .setPositiveButton("설정으로 이동", (dialog, which) -> {
                        Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                        intent.setData(Uri.parse("package:" + getPackageName()));
                        startActivity(intent);
                    })
                    .setNegativeButton("나중에", (dialog, which) -> {
                        saveAlarmData(); // 권한 없이도 데이터는 저장
                    })
                    .show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (checkAlarmPermission()) {
                Log.d(TAG, "알람 권한이 허용되었습니다");
            }
        }
    }

    // 어댑터에서 호출할 수 있는 메서드들
    public void onAlarmDeleted(int position, String label) {
        // 모든 날짜에서 해당 알람 삭제할지 묻기
        new AlertDialog.Builder(this)
                .setTitle("알람 삭제")
                .setMessage("'" + label + "' 알람을 모든 날짜에서 삭제하시겠습니까?")
                .setPositiveButton("모든 날짜에서 삭제", (dialog, which) -> {
                    medicationManager.deleteAlarmFromAllDates(medName, label, this,
                            new MedicationManager.OnCompleteListener() {
                                @Override
                                public void onSuccess() {
                                    Toast.makeText(AlarmDetailActivity.this, "모든 날짜에서 삭제되었습니다", Toast.LENGTH_SHORT).show();
                                    loadAlarmData(); // 데이터 다시 로드
                                }

                                @Override
                                public void onFailure(Exception e) {
                                    Toast.makeText(AlarmDetailActivity.this, "삭제 실패", Toast.LENGTH_SHORT).show();
                                }
                            });
                })
                .setNegativeButton("이 날짜만 삭제", (dialog, which) -> {
                    // 기존 로직: 현재 날짜에서만 삭제
                    alarmItems.remove(position);
                    adapter.notifyItemRemoved(position);
                    adapter.notifyItemRangeChanged(position, adapter.getItemCount());
                })
                .setNeutralButton("취소", null)
                .show();
    }
}