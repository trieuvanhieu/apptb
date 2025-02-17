package com.example.notificationapp;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class NotificationActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notification);

        TextView txtNotificationContent = findViewById(R.id.txtNotificationContent);

        // Lấy nội dung từ Intent
        Intent intent = getIntent();
        if (intent != null && intent.hasExtra("notification_content")) {
            String message = intent.getStringExtra("notification_content");
            txtNotificationContent.setText(message);
        }
    }
}
