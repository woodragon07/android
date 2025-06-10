package com.example.wooyongproj_20202798;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class MainDateAdapter extends RecyclerView.Adapter<MainDateAdapter.DateViewHolder> {

    private List<MainDateItem> dateList;
    private Context context;
    private OnDateClickListener listener;
    private int selectedPosition = -1;

    public interface OnDateClickListener {
        void onDateClick(MainDateItem item);
    }

    public MainDateAdapter(Context context, List<MainDateItem> dateList, OnDateClickListener listener) {
        this.context = context;
        this.dateList = dateList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public DateViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.maindate_item, parent, false);
        return new DateViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull DateViewHolder holder, int position) {
        MainDateItem item = dateList.get(position);

        holder.tvDayOfWeek.setText(item.getDayOfWeek());
        holder.tvDay.setText(String.valueOf(item.getDay()));

        // 선택 상태에 따라 배경 변경
        if (item.isSelected()) {
            holder.itemView.setBackground(ContextCompat.getDrawable(context, R.drawable.bg_maindate_selected));
        } else {
            holder.itemView.setBackground(ContextCompat.getDrawable(context, R.drawable.bg_maindate_unselected));
        }

        holder.itemView.setOnClickListener(v -> {
            // 이전 선택 해제
            if (selectedPosition != -1) {
                dateList.get(selectedPosition).setSelected(false);
                notifyItemChanged(selectedPosition);
            }

            // 새 선택
            item.setSelected(true);
            selectedPosition = holder.getAdapterPosition();
            notifyItemChanged(selectedPosition);

            if (listener != null) {
                listener.onDateClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return dateList.size();
    }

    public static class DateViewHolder extends RecyclerView.ViewHolder {
        TextView tvDayOfWeek, tvDay;

        public DateViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDayOfWeek = itemView.findViewById(R.id.tvDayOfWeek);
            tvDay = itemView.findViewById(R.id.tvDay);
        }
    }
}
