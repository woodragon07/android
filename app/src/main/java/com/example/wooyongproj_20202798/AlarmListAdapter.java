diff --git a/app/src/main/java/com/example/wooyongproj_20202798/AlarmListAdapter.java b/app/src/main/java/com/example/wooyongproj_20202798/AlarmListAdapter.java
index b624a0056e1fe4fd4593db93f882590ef2343531..d9be43a4b81e80cfb73aac6c90c76f08ca2e8533 100644
--- a/app/src/main/java/com/example/wooyongproj_20202798/AlarmListAdapter.java
+++ b/app/src/main/java/com/example/wooyongproj_20202798/AlarmListAdapter.java
@@ -1,36 +1,42 @@
 package com.example.wooyongproj_20202798;
 
 import android.content.Context;
 import android.content.Intent;
+import android.widget.Toast;
 import android.view.LayoutInflater;
 import android.view.View;
 import android.view.ViewGroup;
+import android.widget.ImageButton;
 import android.widget.TextView;
 
 import androidx.annotation.NonNull;
 import androidx.recyclerview.widget.RecyclerView;
+import com.google.firebase.auth.FirebaseAuth;
+import com.google.firebase.firestore.FirebaseFirestore;
+import com.google.firebase.firestore.QueryDocumentSnapshot;
+import com.example.wooyongproj_20202798.AlarmNotificationHelper;
 
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
diff --git a/app/src/main/java/com/example/wooyongproj_20202798/AlarmListAdapter.java b/app/src/main/java/com/example/wooyongproj_20202798/AlarmListAdapter.java
index b624a0056e1fe4fd4593db93f882590ef2343531..d9be43a4b81e80cfb73aac6c90c76f08ca2e8533 100644
--- a/app/src/main/java/com/example/wooyongproj_20202798/AlarmListAdapter.java
+++ b/app/src/main/java/com/example/wooyongproj_20202798/AlarmListAdapter.java
@@ -40,45 +46,71 @@ public class AlarmListAdapter extends RecyclerView.Adapter<AlarmListAdapter.Alar
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
+
+        holder.btnDelete.setOnClickListener(v -> {
+            Context context = v.getContext();
+            String userId = FirebaseAuth.getInstance().getCurrentUser().getEmail().split("@")[0];
+            FirebaseFirestore db = FirebaseFirestore.getInstance();
+
+            db.collection("users")
+                    .document(userId)
+                    .collection("alarms")
+                    .whereEqualTo("medName", alarmData.getMedName())
+                    .get()
+                    .addOnSuccessListener(querySnapshot -> {
+                        for (QueryDocumentSnapshot doc : querySnapshot) {
+                            AlarmData data = doc.toObject(AlarmData.class);
+                            AlarmNotificationHelper.cancelAlarms(context, doc.getId(), data.getAlarmItems().size());
+                            doc.getReference().delete();
+                        }
+
+                        int pos = holder.getAdapterPosition();
+                        alarmList.remove(pos);
+                        notifyItemRemoved(pos);
+                    })
+                    .addOnFailureListener(e -> Toast.makeText(context, "삭제 실패", Toast.LENGTH_SHORT).show());
+        });
     }
 
 
 
 
     @Override
     public int getItemCount() {
         return alarmList.size();
     }
 
     public static class AlarmViewHolder extends RecyclerView.ViewHolder {
         TextView tvMedName, tvTimes;
+        ImageButton btnDelete;
 
         public AlarmViewHolder(@NonNull View itemView) {
             super(itemView);
             tvMedName = itemView.findViewById(R.id.tvMedName);
             tvTimes = itemView.findViewById(R.id.tvTimes);
+            btnDelete = itemView.findViewById(R.id.btnDelete);
         }
     }
 }
