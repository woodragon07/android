package com.example.wooyongproj_20202798;

import android.app.TimePickerDialog;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AlarmDetailAdapter extends RecyclerView.Adapter<AlarmDetailAdapter.AlarmViewHolder> {

    private List<AlarmItem> alarmItems;

    public AlarmDetailAdapter(List<AlarmItem> alarmItems) {
        this.alarmItems = alarmItems;
    }

    @NonNull
    @Override
    public AlarmViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_alarm_detail, parent, false);
        return new AlarmViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AlarmViewHolder holder, int position) {
        AlarmItem item = alarmItems.get(position);

        holder.edtLabel.setText(item.getLabel());
        holder.edtTime.setText(item.getTime());
        holder.switchEnabled.setChecked(item.isEnabled());

        // ✅ 라벨 텍스트 실시간 반영
        holder.edtLabel.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                item.setLabel(s.toString());
            }
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {}
        });

        // ✅ 시간 클릭 → TimePickerDialog 띄우기
        holder.edtTime.setOnClickListener(v -> {
            String currentTime = holder.edtTime.getText().toString();
            int hour = 8, minute = 0;

            if (currentTime.contains(":")) {
                String[] parts = currentTime.split(":");
                hour = Integer.parseInt(parts[0]);
                minute = Integer.parseInt(parts[1]);
            }

            TimePickerDialog dialog = new TimePickerDialog(v.getContext(),
                    (TimePicker view, int hourOfDay, int minuteOfHour) -> {
                        String newTime = String.format("%02d:%02d", hourOfDay, minuteOfHour);
                        item.setTime(newTime);
                        holder.edtTime.setText(newTime);
                    }, hour, minute, true);
            dialog.show();
        });

        // ✅ ON/OFF 토글
        holder.switchEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
            item.setEnabled(isChecked);
        });

        // ✅ 삭제 버튼 (최소 1개 유지)
        holder.btnDelete.setOnClickListener(v -> {
            if (alarmItems.size() <= 1) {
                Toast.makeText(v.getContext(), "최소 1개는 유지되어야 합니다", Toast.LENGTH_SHORT).show();
                return;
            }
            alarmItems.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, alarmItems.size());
        });
    }

    @Override
    public int getItemCount() {
        return alarmItems.size();
    }

    public List<AlarmItem> getAlarmItems() {
        return alarmItems;
    }

    static class AlarmViewHolder extends RecyclerView.ViewHolder {
        EditText edtLabel, edtTime;
        Switch switchEnabled;
        ImageButton btnDelete;

        public AlarmViewHolder(@NonNull View itemView) {
            super(itemView);
            edtLabel = itemView.findViewById(R.id.edtLabel);
            edtTime = itemView.findViewById(R.id.edtTime);
            switchEnabled = itemView.findViewById(R.id.switchEnabled);
            btnDelete = itemView.findViewById(R.id.btnDelete);
        }
    }
}
