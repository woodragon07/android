package com.example.wooyongproj_20202798;

import android.app.TimePickerDialog;
import android.content.Context;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class AlarmDetailAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_ALARM_ITEM = 0;
    private static final int TYPE_ADD_BUTTON = 1;

    private List<AlarmItem> alarmItems;
    private Context context;

    public AlarmDetailAdapter(List<AlarmItem> alarmItems, Context context) {
        this.alarmItems = alarmItems;
        this.context = context;
    }

    @Override
    public int getItemViewType(int position) {
        if (position < alarmItems.size()) {
            return TYPE_ALARM_ITEM;
        } else {
            return TYPE_ADD_BUTTON;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_ALARM_ITEM) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_alarm_detail, parent, false);
            return new AlarmViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_add_alarm_button, parent, false);
            return new AddButtonViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder instanceof AlarmViewHolder) {
            AlarmItem item = alarmItems.get(position);
            AlarmViewHolder alarmHolder = (AlarmViewHolder) holder;

            alarmHolder.edtLabel.setText(item.getLabel());
            alarmHolder.edtTime.setText(item.getTime());
            alarmHolder.switchEnabled.setChecked(item.isEnabled());

            // ✅ 라벨 텍스트 실시간 반영
            alarmHolder.edtLabel.addTextChangedListener(new TextWatcher() {
                public void afterTextChanged(Editable s) {
                    item.setLabel(s.toString());
                }
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                public void onTextChanged(CharSequence s, int start, int before, int count) {}
            });

            // ✅ 시간 클릭 → TimePickerDialog 띄우기
            alarmHolder.edtTime.setOnClickListener(v -> {
                String currentTime = alarmHolder.edtTime.getText().toString();
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
                            alarmHolder.edtTime.setText(newTime);
                        }, hour, minute, true);
                dialog.show();
            });

            // ✅ ON/OFF 토글
            alarmHolder.switchEnabled.setOnCheckedChangeListener((buttonView, isChecked) -> {
                item.setEnabled(isChecked);
            });

            // ✅ 삭제 버튼 (최소 1개 유지)
            alarmHolder.btnDelete.setOnClickListener(v -> {
                if (alarmItems.size() <= 1) {
                    Toast.makeText(v.getContext(), "최소 1개는 유지되어야 합니다", Toast.LENGTH_SHORT).show();
                    return;
                }

                // AlarmDetailActivity의 삭제 메서드 호출
                if (v.getContext() instanceof AlarmDetailActivity) {
                    AlarmDetailActivity activity = (AlarmDetailActivity) v.getContext();
                    activity.onAlarmDeleted(position, item.getLabel());
                } else {
                    // 기본 삭제 로직
                    alarmItems.remove(position);
                    notifyItemRemoved(position);
                    notifyItemRangeChanged(position, getItemCount());
                }
            });

        } else if (holder instanceof AddButtonViewHolder) {
            AddButtonViewHolder addHolder = (AddButtonViewHolder) holder;
            addHolder.btnAddAlarm.setOnClickListener(v -> {
                // 새 알람 추가
                AlarmItem newItem = new AlarmItem("새 알람", "12:00", true);
                alarmItems.add(newItem);
                notifyItemInserted(alarmItems.size() - 1);
                notifyItemChanged(getItemCount() - 1); // 추가 버튼 위치 업데이트

                Toast.makeText(v.getContext(), "새 알람이 추가되었습니다", Toast.LENGTH_SHORT).show();
            });
        }
    }

    @Override
    public int getItemCount() {
        return alarmItems.size() + 1; // 알람 아이템들 + 추가 버튼
    }

    public List<AlarmItem> getAlarmItems() {
        return alarmItems;
    }

    // 기존 알람 아이템 ViewHolder
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

    // 추가 버튼 ViewHolder
    static class AddButtonViewHolder extends RecyclerView.ViewHolder {
        Button btnAddAlarm;

        public AddButtonViewHolder(@NonNull View itemView) {
            super(itemView);
            btnAddAlarm = itemView.findViewById(R.id.btnAddAlarm);
        }
    }
}