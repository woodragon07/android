package com.example.wooyongproj_20202798;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class AlarmListAdapter extends RecyclerView.Adapter<AlarmListAdapter.AlarmViewHolder> {

    private List<AlarmData> alarmList;
    private String selectedDate;

    public AlarmListAdapter(List<AlarmData> alarmList, String selectedDate) {
        this.alarmList = alarmList;
        this.selectedDate = selectedDate;
    }

    public void setSelectedDate(String selectedDate) {
        this.selectedDate = selectedDate;
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
        // 변수명을 alarmData로 사용해 다른 부분에서 혼동이 없도록 한다
        AlarmData alarmData = alarmList.get(position);
        holder.tvMedName.setText(alarmData.getMedName());

        List<AlarmItem> items = alarmData.getAlarmItems();
        if (items == null) {
            holder.tvTimes.setText("복용 시간: 없음");
            return;
        }

        List<String> labels = new ArrayList<>();
        for (AlarmItem item : items) {
            labels.add(item.getLabel());
        }
        holder.tvTimes.setText("복용 시간: " + String.join(", ", labels));

        // 상세 편집 화면으로 이동하는 클릭 리스너 추가
        holder.itemView.setOnClickListener(v -> {
            Context context = v.getContext();
            Intent intent = new Intent(context, AlarmDetailActivity.class);
            intent.putExtra("medName", alarmData.getMedName());
            intent.putExtra("selectedDate", selectedDate);
            context.startActivity(intent);
        });
    }




    @Override
    public int getItemCount() {
        return alarmList.size();
    }

    public static class AlarmViewHolder extends RecyclerView.ViewHolder {
        TextView tvMedName, tvTimes;

        public AlarmViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMedName = itemView.findViewById(R.id.tvMedName);
            tvTimes = itemView.findViewById(R.id.tvTimes);
        }
    }
}
