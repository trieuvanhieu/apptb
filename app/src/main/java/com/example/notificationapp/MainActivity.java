package com.example.notificationapp;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import org.json.JSONException;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {
    private static final String CHANNEL_ID = "notification_channel";
    private static final int NOTIFICATION_PERMISSION_CODE = 1001;
    private static final String API_LAST_ID = "https://57kmt.duckdns.org/android/api.aspx?action=last_id";
    private static final String API_GET_ID = "https://57kmt.duckdns.org/android/api.aspx?action=get_id&id=";
    private int lastId = -1;
    private Handler handler = new Handler();
    private TextView txtLog;
    private ScrollView scrollView;
    private RequestQueue requestQueue;
    private Ringtone ringtone; // Giữ nhạc chuông để tránh phát lại nhiều lần

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txtLog = findViewById(R.id.txtLog);
        scrollView = findViewById(R.id.scrollView);
        requestQueue = Volley.newRequestQueue(this);

        createNotificationChannel();

        // Kiểm tra quyền thông báo trước khi bắt đầu
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS},
                    NOTIFICATION_PERMISSION_CODE);
        } else {
            startFetchingData();
        }
    }

    private void startFetchingData() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                fetchLastId();
                handler.postDelayed(this, 30000); // Lặp lại mỗi 30s
            }
        }, 3000); // Chạy lần đầu sau 3s
    }

    private void fetchLastId() {
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, API_LAST_ID, null,
                response -> {
                    try {
                        if (!response.has("last_id")) {
                            logMessage("⚠️ API không trả về 'last_id'");
                            return;
                        }
                        int newLastId = response.getInt("last_id");
                        logMessage("🔄 Last ID: " + newLastId);

                        if (newLastId != lastId) {
                            lastId = newLastId;
                            fetchMessageById(lastId);
                        }
                    } catch (JSONException e) {
                        logMessage("❌ Lỗi xử lý JSON: " + e.getMessage());
                    }
                }, error -> logMessage("⚠️ Lỗi API last_id: " + error.getMessage())
        );

        requestQueue.add(jsonObjectRequest);
    }

    private void fetchMessageById(int id) {
        String url = API_GET_ID + id;
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(Request.Method.GET, url, null,
                response -> {
                    try {
                        if (!response.has("msg")) {
                            logMessage("⚠️ API không có 'msg'");
                            return;
                        }
                        String message = response.getString("msg");
                        logMessage("📩 Nhận tin nhắn: " + message);
                        sendNotification(message);
                    } catch (JSONException e) {
                        logMessage("❌ Lỗi JSON khi lấy ID: " + e.getMessage());
                    }
                }, error -> logMessage("⚠️ Lỗi API get_id: " + error.getMessage())
        );

        requestQueue.add(jsonObjectRequest);
    }

    private void sendNotification(String message) {
        Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        Intent intent = new Intent(this, NotificationActivity.class);
        intent.putExtra("notification_content", message);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("🔔 Tin nhắn mới")
                .setContentText(message)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(message))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setDefaults(Notification.DEFAULT_ALL)
                .setSound(soundUri)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Chưa cấp quyền thông báo!", Toast.LENGTH_SHORT).show();
            return;
        }

        notificationManager.notify(1, builder.build());
        playSound();
    }

    private void playSound() {
        try {
            if (ringtone == null || !ringtone.isPlaying()) {
                Uri soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
                ringtone = RingtoneManager.getRingtone(getApplicationContext(), soundUri);
                ringtone.play();
            }
        } catch (Exception e) {
            logMessage("❌ Lỗi phát âm thanh: " + e.getMessage());
        }
    }

    private void logMessage(String msg) {
        runOnUiThread(() -> {
            txtLog.append("\n" + msg);
            scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN)); // Tự động cuộn xuống
            Log.d("API_LOG", msg);
        });
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Kênh thông báo";
            String description = "Nhận thông báo từ API";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
            channel.setDescription(description);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_CODE && grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "✅ Quyền thông báo đã cấp!", Toast.LENGTH_SHORT).show();
            startFetchingData();
        }
    }
}
