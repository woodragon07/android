package com.example.wooyongproj_20202798;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class ProgressBarHelper {

    public static void setProgress(View progressBarLayout, int percentage) {
        if (progressBarLayout == null) return;

        // 프로그레스 바의 진행률 설정
        View progressFill = progressBarLayout.findViewById(R.id.progressFill);
        TextView tvPercentage = progressBarLayout.findViewById(R.id.tvPercentage);

        if (progressFill != null && tvPercentage != null) {
            // 퍼센트 텍스트 업데이트
            tvPercentage.setText(percentage + "%");

            // 프로그레스 바 너비 애니메이션
            ViewGroup.LayoutParams layoutParams = progressFill.getLayoutParams();

            // 부모 뷰의 너비 가져오기
            progressBarLayout.post(() -> {
                // 프로그레스 바 컨테이너의 실제 너비 계산
                View container = progressBarLayout.findViewById(R.id.progressFill).getParent() instanceof View ?
                        (View) progressBarLayout.findViewById(R.id.progressFill).getParent() : null;

                if (container != null) {
                    int containerWidth = container.getWidth();
                    int targetWidth = (containerWidth * percentage) / 100;

                    // 너비 설정
                    layoutParams.width = Math.max(0, targetWidth);
                    progressFill.setLayoutParams(layoutParams);
                }
            });
        }
    }

    // 색상을 퍼센트에 따라 변경하는 메서드 (옵션)
    public static void setProgressWithColor(View progressBarLayout, int percentage) {
        setProgress(progressBarLayout, percentage);

        View progressFill = progressBarLayout.findViewById(R.id.progressFill);
        TextView tvPercentage = progressBarLayout.findViewById(R.id.tvPercentage);

        if (progressFill != null && tvPercentage != null) {
            // 퍼센트에 따른 색상 변경
            int color;
            if (percentage >= 80) {
                color = 0xFF4CAF50; // 초록색 (좋음)
            } else if (percentage >= 50) {
                color = 0xFFFF9800; // 주황색 (보통)
            } else {
                color = 0xFFF44336; // 빨간색 (나쁨)
            }

            progressFill.setBackgroundColor(color);
            tvPercentage.setTextColor(color);
        }
    }
}