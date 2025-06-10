package com.example.wooyongproj_20202798;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;

public class AlarmDetailActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private AlarmDetailAdapter adapter;
    private ArrayList<AlarmItem> alarmItems = new ArrayList<>();
    private FirebaseFirestore db;

    private String userId;               // Firebase에 저장된 사용자 ID
    private String selectedDate;         // ex: "2025-06-09"
    private Button btnSave;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_detail);

        db = FirebaseFirestore.getInstance();
        userId = getCurrentUserId();

        recyclerView = findViewById(R.id.recyclerViewAlarmDetail);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AlarmDetailAdapter(alarmItems);
        recyclerView.setAdapter(adapter);

        btnSave = findViewById(R.id.btnSave);

        selectedDate = getIntent().getStringExtra("selectedDate");

        // ✅ 디버깅 로그 출력
        Log.d("AlarmDetail", "userId: " + userId);
        Log.d("AlarmDetail", "받은 selectedDate: " + selectedDate);

        if (selectedDate == null) {
            Toast.makeText(this, "날짜 정보 없음", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSave.setOnClickListener(v -> saveAlarmDataToFirestore());

        loadAlarmDataFromFirestore();
    }

    private void loadAlarmDataFromFirestore() {
        db.collection("users")
                .document(userId)
                .collection("alarms")
                .document(selectedDate)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        AlarmData alarmData = documentSnapshot.toObject(AlarmData.class);
                        if (alarmData != null && alarmData.getAlarmItems() != null) {
                            alarmItems.clear();
                            alarmItems.addAll(alarmData.getAlarmItems());
                            adapter.notifyDataSetChanged();
                        }
                    } else {
                        Log.d("AlarmDetail", "해당 날짜에 데이터 없음");
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "불러오기 실패", Toast.LENGTH_SHORT).show();
                    Log.e("AlarmDetail", "불러오기 실패", e);
                });
    }

    private void saveAlarmDataToFirestore() {
        AlarmData updatedData = new AlarmData();
        updatedData.setMedName("kamki"); // TODO: 실제 이름 연동 필요 시 수정
        updatedData.setAlarmItems(alarmItems);
        updatedData.setDate(selectedDate);

        db.collection("users")
                .document(userId)
                .collection("alarms")
                .document(selectedDate)
                .set(updatedData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "저장 완료!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private String getCurrentUserId() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getEmail() != null) {
            return user.getEmail().split("@")[0];
        }
        return "unknown_user";
    }
}
