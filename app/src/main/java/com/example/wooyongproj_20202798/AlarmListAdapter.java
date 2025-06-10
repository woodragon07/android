package com.example.wooyongproj_20202798;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class AlarmListAdapter extends RecyclerView.Adapter<AlarmListAdapter.AlarmViewHolder> {

    private static final String TAG = "AlarmListAdapter";

    private List<AlarmData> alarmList;
    private String selectedDate;
    private MedicationManager medicationManager;

    public AlarmListAdapter(List<AlarmData> alarmList, String selectedDate) {
        this.alarmList = alarmList;
        this.selectedDate = selectedDate;

        String userId = getCurrentUserId();
        if (userId != null) {
            this.medicationManager = new MedicationManager(userId);
        }
    }

    public void setSelectedDate(String selectedDate) {
        this.selectedDate = selectedDate;
    }

    // 알람 목록 업데이트 메서드
    public void updateAlarmList(List<AlarmData> newAlarmList) {
        this.alarmList.clear();
        this.alarmList.addAll(newAlarmList);
        notifyDataSetChanged();
        Log.d(TAG, "알람 목록 업데이트됨: " + alarmList.size() + "개");
    }

    @NonNull
    @Override
    public AlarmViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_alarm_list, parent, false);
        return new AlarmViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AlarmViewHolder holder, int position) {
        AlarmData alarmData = alarmList.get(position);
        holder.tvMedName.setText(alarmData.getMedName());

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
                holder.tvTimes.setText("활성 알람: " + String.join(", ", activeLabels));
            }
        }

        // 상세 편집 화면으로 이동하는 클릭 리스너
        holder.itemView.setOnClickListener(v -> {
            Context context = v.getContext();
            Intent intent = new Intent(context, AlarmDetailActivity.class);
            intent.putExtra("medName", alarmData.getMedName());
            intent.putExtra("selectedDate", selectedDate);
            context.startActivity(intent);
        });

        // 삭제 버튼 클릭 리스너 (약물 전체 삭제)
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
        return alarmList.size();
    }

    // Firebase 사용자 ID 가져오기
    private String getCurrentUserId() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null && user.getEmail() != null) {
            return user.getEmail().split("@")[0];
        }
        return null;
    }

    public static class AlarmViewHolder extends RecyclerView.ViewHolder {
        TextView tvMedName, tvTimes;
        ImageButton btnDelete;

        public AlarmViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMedName = itemView.findViewById(R.id.tvMedName);
            tvTimes = itemView.findViewById(R.id.tvTimes);

            // 삭제 버튼이 레이아웃에 있는 경우에만 참조
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}