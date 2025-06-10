package com.example.wooyongproj_20202798;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;
import android.Manifest;
import android.content.pm.PackageManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MainHomeActivity extends AppCompatActivity {

    private static final String TAG = "MainHomeActivity";

    private RecyclerView rvDateList;
    private MainDateAdapter dateAdapter;
    private List<MainDateItem> dateList;

    private RecyclerView rvAlarmList;
    private AlarmListAdapter alarmAdapter;
    private List<AlarmData> alarmList = new ArrayList<>();

    private String selectedDate = ""; // 가장 마지막으로 선택된 날짜 저장
    private MedicationManager medicationManager;
    private boolean isInitialLoad = true; // 첫 로드인지 확인

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_home);

        Log.d(TAG, "=== MainHomeActivity 시작 ===");

        // 알림 권한 요청
        requestNotificationPermission();

        // 약물 관리자 초기화
        String userId = getCurrentUserId();
        medicationManager = new MedicationManager(userId);

        // 날짜 리스트 초기화
        initializeDateList();

        // 알람 리스트 초기화
        initializeAlarmList();

        // 버튼 설정
        setupButtons();

        // 오늘 날짜로 자동 설정 및 데이터 로드
        setTodayAsDefaultAndLoad();
    }

    private void initializeDateList() {
        rvDateList = findViewById(R.id.rvDateList);
        dateList = generateDatesAroundToday(30); // 오늘 기준 ±30일 → 총 61개

        // 날짜 선택 콜백 개선
        dateAdapter = new MainDateAdapter(this, dateList, item -> {
            String newSelectedDate = item.getFormattedDate();
            Log.d(TAG, "날짜 선택됨: " + selectedDate + " → " + newSelectedDate);

            selectedDate = newSelectedDate;
            alarmAdapter.setSelectedDate(selectedDate);
            loadAlarmsForDate(selectedDate);
        });

        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        rvDateList.setLayoutManager(layoutManager);
        rvDateList.setAdapter(dateAdapter);

        LinearSnapHelper snapHelper = new LinearSnapHelper();
        snapHelper.attachToRecyclerView(rvDateList);
    }

    private void initializeAlarmList() {
        rvAlarmList = findViewById(R.id.rvAlarmList);
        rvAlarmList.setLayoutManager(new LinearLayoutManager(this));
        alarmAdapter = new AlarmListAdapter(alarmList, selectedDate);
        rvAlarmList.setAdapter(alarmAdapter);
    }

    private void setupButtons() {
        Button btnAddAlarm = findViewById(R.id.btnAddAlarm);
        btnAddAlarm.setOnClickListener(v -> {
            if (selectedDate.isEmpty()) {
                Toast.makeText(this, "날짜를 먼저 선택해주세요", Toast.LENGTH_SHORT).show();
                return;
            }
            Intent intent = new Intent(MainHomeActivity.this, AlarmRegisterActivity.class);
            intent.putExtra("selectedDate", selectedDate);
            startActivity(intent);
        });
    }

    private void setTodayAsDefaultAndLoad() {
        // 오늘 날짜를 기본값으로 설정
        Calendar today = Calendar.getInstance();
        selectedDate = String.format("%04d-%02d-%02d",
                today.get(Calendar.YEAR),
                today.get(Calendar.MONTH) + 1,
                today.get(Calendar.DAY_OF_MONTH));

        Log.d(TAG, "기본 선택 날짜 설정: " + selectedDate);

        // 오늘 날짜로 스크롤
        int todayIndex = 30;
        rvDateList.scrollToPosition(todayIndex);
        rvDateList.post(() -> {
            RecyclerView.ViewHolder viewHolder = rvDateList.findViewHolderForAdapterPosition(todayIndex);
            if (viewHolder != null) {
                LinearSnapHelper snapHelper = new LinearSnapHelper();
                int[] snapDistance = snapHelper.calculateDistanceToFinalSnap(rvDateList.getLayoutManager(), viewHolder.itemView);
                if (snapDistance != null) {
                    rvDateList.smoothScrollBy(snapDistance[0], snapDistance[1]);
                }
            }
        });

        // 어댑터에 선택된 날짜 설정
        alarmAdapter.setSelectedDate(selectedDate);

        // 즉시 오늘 날짜의 알람 로드
        loadAlarmsForDate(selectedDate);
    }

    private List<MainDateItem> generateDatesAroundToday(int range) {
        List<MainDateItem> list = new ArrayList<>();
        String[] weekDays = {"일", "월", "화", "수", "목", "금", "토"};
        Calendar today = Calendar.getInstance();

        for (int i = -range; i <= range; i++) {
            Calendar date = (Calendar) today.clone();
            date.add(Calendar.DAY_OF_MONTH, i);

            int year = date.get(Calendar.YEAR);
            int month = date.get(Calendar.MONTH) + 1;
            int day = date.get(Calendar.DAY_OF_MONTH);
            int dayOfWeek = date.get(Calendar.DAY_OF_WEEK);

            boolean isToday = (i == 0);

            list.add(new MainDateItem(year, month, day, weekDays[dayOfWeek - 1], isToday));
        }

        return list;
    }

    private void loadAlarmsForDate(String dateKey) {
        Log.d(TAG, "날짜별 알람 로드 시작: " + dateKey);

        // 기존 목록 초기화
        alarmList.clear();
        alarmAdapter.notifyDataSetChanged();

        // 새로운 시스템으로 알람 로드
        medicationManager.getActiveAlarmsForDate(dateKey, new MedicationManager.OnAlarmsLoadedListener() {
            @Override
            public void onAlarmsLoaded(List<AlarmData> alarmDataList) {
                Log.d(TAG, "로드된 알람 개수: " + alarmDataList.size());

                runOnUiThread(() -> {
                    alarmList.clear();
                    alarmList.addAll(alarmDataList);
                    alarmAdapter.updateAlarmList(alarmDataList);

                    // 첫 로드일 때 로그 출력
                    if (isInitialLoad) {
                        isInitialLoad = false;
                        Log.d(TAG, "✅ 초기 로드 완료 - 오늘(" + dateKey + ") 알람 " + alarmDataList.size() + "개");
                    }
                });
            }

            @Override
            public void onLoadFailed(Exception e) {
                Log.e(TAG, "알람 로드 실패: " + dateKey, e);

                runOnUiThread(() -> {
                    alarmList.clear();
                    alarmAdapter.notifyDataSetChanged();

                    // 실패 시 토스트는 첫 로드일 때만 표시
                    if (isInitialLoad) {
                        Toast.makeText(MainHomeActivity.this, "알람 목록 로드 실패", Toast.LENGTH_SHORT).show();
                        isInitialLoad = false;
                    }
                });
            }
        });
    }

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1001);
            }
        }
    }

    private String getCurrentUserId() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getEmail() != null) {
            return user.getEmail().split("@")[0];
        }
        return "unknown_user";
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(TAG, "onResume - 현재 선택된 날짜: " + selectedDate);

        // 화면으로 돌아올 때마다 현재 선택된 날짜의 최신 데이터 로드
        if (!selectedDate.isEmpty()) {
            Log.d(TAG, "onResume에서 데이터 다시 로드");
            loadAlarmsForDate(selectedDate);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1001) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "알림 권한 허용됨");
                Toast.makeText(this, "알림 권한이 허용되었습니다", Toast.LENGTH_SHORT).show();
            } else {
                Log.w(TAG, "알림 권한 거부됨");
                Toast.makeText(this, "알림 권한이 필요합니다", Toast.LENGTH_LONG).show();
            }
        }
    }
}