package com.example.wooyongproj_20202798;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MainHomeActivity extends AppCompatActivity {

    private RecyclerView rvDateList;
    private MainDateAdapter dateAdapter;
    private List<MainDateItem> dateList;

    private RecyclerView rvAlarmList;
    private AlarmListAdapter alarmAdapter;
    private List<AlarmData> alarmList = new ArrayList<>();

    private String selectedDate = ""; // 가장 마지막으로 선택된 날짜 저장

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_home);

        rvDateList = findViewById(R.id.rvDateList);
        dateList = generateDatesAroundToday(30); // 오늘 기준 ±30일 → 총 61개

        // 오늘 날짜 인덱스 = 30
        dateAdapter = new MainDateAdapter(this, dateList, item -> {
            selectedDate = item.getFormattedDate(); // 클릭된 날짜 저장
            loadAlarmsForDate(selectedDate); // 날짜 바뀔 때마다 불러오기
        });

        LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false);
        rvDateList.setLayoutManager(layoutManager);
        rvDateList.setAdapter(dateAdapter);

        LinearSnapHelper snapHelper = new LinearSnapHelper();
        snapHelper.attachToRecyclerView(rvDateList);

        int todayIndex = 30;
        rvDateList.scrollToPosition(todayIndex);
        rvDateList.post(() -> {
            RecyclerView.ViewHolder viewHolder = rvDateList.findViewHolderForAdapterPosition(todayIndex);
            if (viewHolder != null) {
                int[] snapDistance = snapHelper.calculateDistanceToFinalSnap(rvDateList.getLayoutManager(), viewHolder.itemView);
                if (snapDistance != null) {
                    rvDateList.smoothScrollBy(snapDistance[0], snapDistance[1]);
                }
            }
        });

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

        rvAlarmList = findViewById(R.id.rvAlarmList);
        rvAlarmList.setLayoutManager(new LinearLayoutManager(this));
        alarmAdapter = new AlarmListAdapter(alarmList, selectedDate); // 🔥 수정됨: selectedDate 전달
        rvAlarmList.setAdapter(alarmAdapter);
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
        alarmList.clear();
        String userId = FirebaseAuth.getInstance().getCurrentUser().getEmail().split("@")[0];

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(userId)
                .collection("alarms")
                .document(dateKey)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        AlarmData data = documentSnapshot.toObject(AlarmData.class);
                        if (data != null) {
                            alarmList.add(data);
                        }
                    }
                    alarmAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> {
                    Log.e("FIRESTORE", "알림 불러오기 실패: " + e.getMessage());
                    alarmAdapter.notifyDataSetChanged();
                });
    }
}
