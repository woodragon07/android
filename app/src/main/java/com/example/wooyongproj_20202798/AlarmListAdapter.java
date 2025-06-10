package com.example.wooyongproj_20202798;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AlarmListAdapter extends RecyclerView.Adapter<AlarmListAdapter.AlarmViewHolder> {

    private static final String TAG = "AlarmListAdapter";

    private List<AlarmData> alarmList;
    private String selectedDate;
    private MedicationManager medicationManager;
    private IntakeManager intakeManager;
    private Map<String, MedicationIntake> intakeMap;

    public AlarmListAdapter(List<AlarmData> alarmList, String selectedDate) {
        this.alarmList = alarmList;
        this.selectedDate = selectedDate;
        this.intakeMap = new HashMap<>();

        Log.d(TAG, "AlarmListAdapter 생성 - 기본 생성자");
        Log.d(TAG, "초기 알람 개수: " + (alarmList != null ? alarmList.size() : "null"));

        String userId = getCurrentUserId();
        if (userId != null) {
            this.medicationManager = new MedicationManager(userId);
        }
    }

    // Context를 받는 생성자
    public AlarmListAdapter(List<AlarmData> alarmList, String selectedDate, Context context) {
        this.alarmList = alarmList;
        this.selectedDate = selectedDate;
        this.intakeMap = new HashMap<>();

        Log.d(TAG, "AlarmListAdapter 생성 - Context 포함 생성자");
        Log.d(TAG, "초기 알람 개수: " + (alarmList != null ? alarmList.size() : "null"));

        String userId = getCurrentUserId();
        if (userId != null) {
            this.medicationManager = new MedicationManager(userId);
            this.intakeManager = new IntakeManager(userId, context);
        }
    }

    public void setSelectedDate(String selectedDate) {
        Log.d(TAG, "날짜 변경: " + this.selectedDate + " → " + selectedDate);
        this.selectedDate = selectedDate;
        this.intakeMap.clear();
        loadIntakeData();
    }

    // 알람 목록 업데이트 메서드
    public void updateAlarmList(List<AlarmData> newAlarmList) {
        Log.d(TAG, "updateAlarmList 호출됨");
        Log.d(TAG, "기존 개수: " + (this.alarmList != null ? this.alarmList.size() : "null"));
        Log.d(TAG, "새 개수: " + (newAlarmList != null ? newAlarmList.size() : "null"));

        if (this.alarmList == null) {
            this.alarmList = new ArrayList<>();
        }

        this.alarmList.clear();
        if (newAlarmList != null) {
            this.alarmList.addAll(newAlarmList);
        }

        Log.d(TAG, "업데이트 후 최종 개수: " + this.alarmList.size());

        // 복용 상태 로드
        loadIntakeData();

        // UI 업데이트
        notifyDataSetChanged();
        Log.d(TAG, "notifyDataSetChanged() 호출 완료");
    }

    // 복용 상태 데이터 로드
    private void loadIntakeData() {
        if (intakeManager == null || selectedDate == null || selectedDate.isEmpty()) {
            Log.d(TAG, "IntakeManager 또는 날짜가 null - 복용 상태 로드 스킵");
            return;
        }

        Log.d(TAG, "복용 상태 로드 시작: " + selectedDate);

        intakeManager.getIntakeStatusForDate(selectedDate, new IntakeManager.OnIntakeLoadedListener() {
            @Override
            public void onIntakeLoaded(List<MedicationIntake> intakeList) {
                Log.d(TAG, "복용 상태 로드 성공: " + intakeList.size() + "개");

                intakeMap.clear();
                for (MedicationIntake intake : intakeList) {
                    intakeMap.put(intake.getMedicationName(), intake);
                }
                notifyDataSetChanged();
            }

            @Override
            public void onLoadFailed(Exception e) {
                Log.e(TAG, "복용 상태 로드 실패", e);
            }
        });
    }

    @NonNull
    @Override
    public AlarmViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Log.d(TAG, "onCreateViewHolder 호출됨");
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_alarm_list, parent, false);
        return new AlarmViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AlarmViewHolder holder, int position) {
        Log.d(TAG, "onBindViewHolder 호출됨 - position: " + position);

        if (alarmList == null || position >= alarmList.size()) {
            Log.e(TAG, "Invalid position or null alarmList");
            return;
        }

        AlarmData alarmData = alarmList.get(position);
        String medicationName = alarmData.getMedName();

        Log.d(TAG, "약물명 설정: " + medicationName);

        // 약물명 설정
        holder.tvMedName.setText(medicationName);

        // 복용 시간 정보 표시 (기존)
        updateTimeInfo(holder, alarmData);

        // 🔧 추가: 복용률 프로그레스 바 설정
        setupMedicationProgress(holder, alarmData);

        // 🔧 추가: 체크박스 설정
        setupTimeCheckboxes(holder, alarmData);

        // 상세 편집 화면으로 이동하는 클릭 리스너
        holder.itemView.setOnClickListener(v -> {
            Context context = v.getContext();
            Intent intent = new Intent(context, AlarmDetailActivity.class);
            intent.putExtra("medName", alarmData.getMedName());
            intent.putExtra("selectedDate", selectedDate);
            context.startActivity(intent);
        });

        // 삭제 버튼 설정
        setupDeleteButton(holder, alarmData);

        Log.d(TAG, "onBindViewHolder 완료 - position: " + position);
    }

    // 🔧 새로 추가: 약물별 복용률 설정
    private void setupMedicationProgress(AlarmViewHolder holder, AlarmData alarmData) {
        if (intakeManager == null || selectedDate == null) {
            Log.d(TAG, "IntakeManager 또는 날짜가 null - 복용률 숨김");
            hideProgressBar(holder);
            return;
        }

        String medicationName = alarmData.getMedName();

        // 해당 약물의 복용 상태 조회
        intakeManager.getIntakeStatusForMedication(medicationName, selectedDate,
                new IntakeManager.OnSingleIntakeLoadedListener() {
                    @Override
                    public void onIntakeLoaded(MedicationIntake intake) {
                        // 복용률 계산
                        double progress = calculateMedicationProgress(intake, alarmData);

                        // UI 업데이트 (메인 스레드에서)
                        if (holder.itemView.getContext() instanceof android.app.Activity) {
                            ((android.app.Activity) holder.itemView.getContext()).runOnUiThread(() -> {
                                updateProgressBarUI(holder, progress);
                            });
                        }
                    }

                    @Override
                    public void onLoadFailed(Exception e) {
                        Log.e(TAG, "복용 상태 조회 실패", e);
                        hideProgressBar(holder);
                    }
                });
    }

    // 🔧 새로 추가: 체크박스 설정
    private void setupTimeCheckboxes(AlarmViewHolder holder, AlarmData alarmData) {
        if (intakeManager == null || selectedDate == null) {
            Log.d(TAG, "IntakeManager 또는 날짜가 null - 체크박스 숨김");
            hideCheckboxes(holder);
            return;
        }

        String medicationName = alarmData.getMedName();

        // 기존 체크박스들 찾기 (레이아웃에서)
        CheckBox cbMorning = holder.itemView.findViewById(R.id.cbMorning);
        CheckBox cbLunch = holder.itemView.findViewById(R.id.cbLunch);
        CheckBox cbEvening = holder.itemView.findViewById(R.id.cbEvening);

        // 일단 모든 체크박스 숨기기
        hideAllCheckboxes(cbMorning, cbLunch, cbEvening);

        // 해당 약물의 복용 상태 조회
        intakeManager.getIntakeStatusForMedication(medicationName, selectedDate,
                new IntakeManager.OnSingleIntakeLoadedListener() {
                    @Override
                    public void onIntakeLoaded(MedicationIntake intake) {
                        // UI 업데이트 (메인 스레드에서)
                        if (holder.itemView.getContext() instanceof android.app.Activity) {
                            ((android.app.Activity) holder.itemView.getContext()).runOnUiThread(() -> {
                                setupCheckboxesUI(holder, alarmData, intake, cbMorning, cbLunch, cbEvening);
                            });
                        }
                    }

                    @Override
                    public void onLoadFailed(Exception e) {
                        Log.e(TAG, "복용 상태 조회 실패", e);
                        hideCheckboxes(holder);
                    }
                });
    }

    // 🔧 새로 추가: 체크박스 UI 설정
    private void setupCheckboxesUI(AlarmViewHolder holder, AlarmData alarmData, MedicationIntake intake,
                                   CheckBox cbMorning, CheckBox cbLunch, CheckBox cbEvening) {

        String medicationName = alarmData.getMedName();
        List<AlarmItem> alarmItems = alarmData.getAlarmItems();

        if (alarmItems == null) return;

        // 알람 시간대에 따라 체크박스 표시 및 설정
        for (AlarmItem item : alarmItems) {
            if (!item.isEnabled()) continue;

            CheckBox targetCheckbox = null;
            String timeLabel = item.getLabel();

            // 시간대에 맞는 체크박스 선택
            if ("아침".equals(timeLabel)) {
                targetCheckbox = cbMorning;
            } else if ("점심".equals(timeLabel)) {
                targetCheckbox = cbLunch;
            } else if ("저녁".equals(timeLabel)) {
                targetCheckbox = cbEvening;
            }

            if (targetCheckbox != null) {
                // 체크박스 표시
                targetCheckbox.setVisibility(View.VISIBLE);

                // 현재 복용 상태 설정
                boolean taken = intake != null && intake.isIntakeForTime(timeLabel);
                targetCheckbox.setChecked(taken);

                // 리스너 설정 (이전 리스너 제거 후)
                targetCheckbox.setOnCheckedChangeListener(null);
                targetCheckbox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    // 복용 상태 업데이트
                    intakeManager.updateIntakeStatus(medicationName, selectedDate, timeLabel, isChecked,
                            new IntakeManager.OnCompleteListener() {
                                @Override
                                public void onSuccess() {
                                    Log.d(TAG, medicationName + " " + timeLabel + " 복용 상태 변경: " + isChecked);

                                    // 복용률 다시 계산
                                    setupMedicationProgress(holder, alarmData);

                                    // 전체 복용률 업데이트
                                    updateOverallProgress(holder);
                                }

                                @Override
                                public void onFailure(Exception e) {
                                    Log.e(TAG, "복용 상태 저장 실패", e);
                                }
                            });
                });
            }
        }

        // 체크박스 레이아웃 표시
        LinearLayout layoutCheckboxes = holder.itemView.findViewById(R.id.layoutCheckboxes);
        if (layoutCheckboxes != null) {
            layoutCheckboxes.setVisibility(View.VISIBLE);
        }
    }

    // 🔧 새로 추가: 복용률 계산
    private double calculateMedicationProgress(MedicationIntake intake, AlarmData alarmData) {
        if (alarmData.getAlarmItems() == null) {
            return 0.0;
        }

        int totalSlots = 0;
        int takenSlots = 0;

        for (AlarmItem item : alarmData.getAlarmItems()) {
            if (item.isEnabled()) {
                totalSlots++;
                if (intake != null && intake.isIntakeForTime(item.getLabel())) {
                    takenSlots++;
                }
            }
        }

        return totalSlots > 0 ? (takenSlots * 100.0 / totalSlots) : 0.0;
    }

    // 🔧 새로 추가: 프로그레스 바 UI 업데이트 (커스텀 프로그레스 바용)
    private void updateProgressBarUI(AlarmViewHolder holder, double progress) {
        View progressBarLayout = holder.itemView.findViewById(R.id.progressBar);
        if (progressBarLayout != null) {
            // 커스텀 프로그레스 바 요소들 찾기
            View progressFill = progressBarLayout.findViewById(R.id.progressFill);
            TextView progressText = progressBarLayout.findViewById(R.id.tvPercentage);

            if (progressFill != null && progressText != null) {
                // 프로그레스 바의 부모 뷰 (FrameLayout)
                ViewGroup progressContainer = (ViewGroup) progressFill.getParent();

                if (progressContainer != null) {
                    // 컨테이너의 크기를 기다린 후 진행률 설정
                    progressContainer.post(() -> {
                        int containerWidth = progressContainer.getWidth();
                        if (containerWidth > 0) {
                            // 진행률에 따른 width 계산
                            int fillWidth = (int) (containerWidth * progress / 100.0);

                            // 프로그레스 바 width 설정
                            ViewGroup.LayoutParams params = progressFill.getLayoutParams();
                            params.width = fillWidth;
                            progressFill.setLayoutParams(params);
                        }
                    });
                }

                // 퍼센트 텍스트 설정
                progressText.setText(String.format("%.0f%%", progress));
                progressBarLayout.setVisibility(View.VISIBLE);

                Log.d(TAG, "커스텀 복용률 UI 업데이트: " + progress + "%");
            } else {
                Log.w(TAG, "커스텀 프로그레스 바 요소를 찾을 수 없음");
                Log.w(TAG, "progressFill: " + (progressFill != null) + ", progressText: " + (progressText != null));
            }
        }
    }

    // 🔧 새로 추가: 전체 복용률 업데이트 알림
    private void updateOverallProgress(AlarmViewHolder holder) {
        // MainActivity의 복용률 업데이트 메서드 호출
        Context context = holder.itemView.getContext();
        if (context instanceof MainHomeActivity) {
            ((MainHomeActivity) context).updateDailyProgress();
        }
    }

    // 🔧 새로 추가: UI 숨김 헬퍼 메서드들
    private void hideProgressBar(AlarmViewHolder holder) {
        View progressBarLayout = holder.itemView.findViewById(R.id.progressBar);
        if (progressBarLayout != null) {
            progressBarLayout.setVisibility(View.GONE);
        }
    }

    private void hideCheckboxes(AlarmViewHolder holder) {
        LinearLayout layoutCheckboxes = holder.itemView.findViewById(R.id.layoutCheckboxes);
        if (layoutCheckboxes != null) {
            layoutCheckboxes.setVisibility(View.GONE);
        }
    }

    private void hideAllCheckboxes(CheckBox... checkboxes) {
        for (CheckBox cb : checkboxes) {
            if (cb != null) {
                cb.setVisibility(View.GONE);
                cb.setOnCheckedChangeListener(null);
            }
        }
    }

    private void updateTimeInfo(AlarmViewHolder holder, AlarmData alarmData) {
        List<AlarmItem> items = alarmData.getAlarmItems();
        if (items == null || items.isEmpty()) {
            holder.tvTimes.setText("복용 시간: 없음");
        } else {
            List<String> activeLabels = new ArrayList<>();
            for (AlarmItem item : items) {
                if (item.isEnabled()) {
                    activeLabels.add(item.getLabel() + "(" + item.getTime() + ")");
                }
            }

            if (activeLabels.isEmpty()) {
                holder.tvTimes.setText("활성화된 알람: 없음");
            } else {
                holder.tvTimes.setText("알람 시간: " + String.join(", ", activeLabels));
            }
        }
    }

    private void setupDeleteButton(AlarmViewHolder holder, AlarmData alarmData) {
        if (holder.btnDelete != null) {
            holder.btnDelete.setOnClickListener(v -> {
                Context context = v.getContext();

                if (medicationManager == null) {
                    Toast.makeText(context, "사용자 인증 실패", Toast.LENGTH_SHORT).show();
                    return;
                }

                // 확인 다이얼로그 표시
                new androidx.appcompat.app.AlertDialog.Builder(context)
                        .setTitle("약물 삭제")
                        .setMessage("'" + alarmData.getMedName() + "' 약물을 모든 날짜에서 완전히 삭제하시겠습니까?")
                        .setPositiveButton("삭제", (dialog, which) -> {
                            // 전체 약물 삭제
                            medicationManager.deleteMedication(alarmData.getMedName(), context,
                                    new MedicationManager.OnCompleteListener() {
                                        @Override
                                        public void onSuccess() {
                                            int pos = holder.getAdapterPosition();
                                            if (pos != RecyclerView.NO_POSITION) {
                                                alarmList.remove(pos);
                                                notifyItemRemoved(pos);
                                                Toast.makeText(context, "약물이 완전히 삭제되었습니다", Toast.LENGTH_SHORT).show();
                                            }
                                        }

                                        @Override
                                        public void onFailure(Exception e) {
                                            Toast.makeText(context, "삭제 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                            Log.e(TAG, "약물 삭제 실패", e);
                                        }
                                    });
                        })
                        .setNegativeButton("취소", null)
                        .show();
            });
        }
    }

    @Override
    public int getItemCount() {
        int count = alarmList != null ? alarmList.size() : 0;
        Log.d(TAG, "getItemCount 호출됨: " + count);
        return count;
    }

    // Firebase 사용자 ID 가져오기
    private String getCurrentUserId() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getEmail() != null) {
            return user.getEmail().split("@")[0];
        }
        return null;
    }

    // 전체 복용률 계산 (외부에서 사용)
    public int calculateOverallProgress() {
        return IntakeManager.calculateOverallCompletionRate(
                new ArrayList<>(intakeMap.values()),
                alarmList
        );
    }

    public static class AlarmViewHolder extends RecyclerView.ViewHolder {
        TextView tvMedName, tvTimes;
        ImageButton btnDelete;
        View progressBar;
        LinearLayout containerCheckboxes;

        public AlarmViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMedName = itemView.findViewById(R.id.tvMedName);
            tvTimes = itemView.findViewById(R.id.tvTimes);
            btnDelete = itemView.findViewById(R.id.btnDelete);
            progressBar = itemView.findViewById(R.id.progressBar);
            containerCheckboxes = itemView.findViewById(R.id.containerCheckboxes);

            Log.d("AlarmListAdapter", "ViewHolder 생성됨");
            Log.d("AlarmListAdapter", "tvMedName: " + (tvMedName != null ? "OK" : "NULL"));
            Log.d("AlarmListAdapter", "tvTimes: " + (tvTimes != null ? "OK" : "NULL"));
        }
    }
}