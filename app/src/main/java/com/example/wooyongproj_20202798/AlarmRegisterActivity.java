package com.example.wooyongproj_20202798;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.widget.*;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.example.wooyongproj_20202798.AlarmNotificationHelper;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class AlarmRegisterActivity extends AppCompatActivity {

    private EditText edtMedName, edtDays;
    private Button btnDate, btnSave;
    private RadioGroup rgTime;
    private String selectedDate = "";
    private TextView tvDetailTitle;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm_register);

        // UI 요소 초기화
        edtMedName = findViewById(R.id.edtMedName);
        edtDays = findViewById(R.id.edtDays);
        btnDate = findViewById(R.id.btnPickDate);
        rgTime = findViewById(R.id.rgTime);
        btnSave = findViewById(R.id.btnSave);
        tvDetailTitle = findViewById(R.id.tvDetailTitle);

        // 상세 화면 모드 감지
        String medName = getIntent().getStringExtra("medName");
        String date = getIntent().getStringExtra("date");

        if (medName != null && date != null) {
            tvDetailTitle.setText(date + " - " + medName + " 상세 설정");
            return; // 상세 화면에서는 등록 로직 생략
        }

        // 전달된 선택 날짜
        String intentDate = getIntent().getStringExtra("selectedDate");
        if (intentDate != null) {
            selectedDate = intentDate;
            btnDate.setText(selectedDate);
        }

        btnDate.setOnClickListener(v -> {
            Calendar c = Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
                selectedDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth);
                btnDate.setText(selectedDate);
            }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH)).show();
        });

        btnSave.setOnClickListener(v -> {
            String userId = getCurrentUserId();
            String medNameText = edtMedName.getText().toString().trim();
            String daysStr = edtDays.getText().toString().trim();

            if (medNameText.isEmpty() || selectedDate.isEmpty() || daysStr.isEmpty()) {
                Toast.makeText(this, "모든 정보를 입력해주세요", Toast.LENGTH_SHORT).show();
                return;
            }

            int duration = Integer.parseInt(daysStr);

            Calendar start = Calendar.getInstance();
            String[] parts = selectedDate.split("-");
            start.set(Calendar.YEAR, Integer.parseInt(parts[0]));
            start.set(Calendar.MONTH, Integer.parseInt(parts[1]) - 1);
            start.set(Calendar.DAY_OF_MONTH, Integer.parseInt(parts[2]));

            // 🔧 수정: MedicationManager 사용해서 저장
            List<String> dateList = new ArrayList<>();
            for (int i = 0; i < duration; i++) {
                Calendar target = (Calendar) start.clone();
                target.add(Calendar.DAY_OF_MONTH, i);

                String dateKey = String.format("%04d-%02d-%02d",
                        target.get(Calendar.YEAR),
                        target.get(Calendar.MONTH) + 1,
                        target.get(Calendar.DAY_OF_MONTH));

                dateList.add(dateKey);
            }

            List<AlarmItem> dayItems = new ArrayList<>();
            dayItems.add(new AlarmItem("아침", "08:00", true));
            dayItems.add(new AlarmItem("점심", "12:00", true));
            dayItems.add(new AlarmItem("저녁", "18:00", true));

            // MedicationManager로 저장 (여러 알람 지원)
            MedicationManager medicationManager = new MedicationManager(userId);
            medicationManager.saveMedicationAlarmsMultiple(medNameText, dateList, dayItems, this,
                    new MedicationManager.OnCompleteListener() {
                        @Override
                        public void onSuccess() {
                            Toast.makeText(AlarmRegisterActivity.this, "알림 저장 완료", Toast.LENGTH_SHORT).show();
                            finish();
                        }

                        @Override
                        public void onFailure(Exception e) {
                            Toast.makeText(AlarmRegisterActivity.this, "저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            Log.e("AlarmRegister", "저장 실패", e);
                        }
                    });
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