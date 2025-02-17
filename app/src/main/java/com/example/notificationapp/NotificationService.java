package com.example.notificationapp;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

public class NotificationService extends Service {
    private static final String TAG = "NotificationService";
    private static final String API_LAST_ID = "https://57kmt.duckdns.org/android/api.aspx?action=getLastId";

    private Handler handler = new Handler();
    private RequestQueue requestQueue;
    private int lastId = -1;

    @Override
    public void onCreate() {
        super.onCreate();
        requestQueue = Volley.newRequestQueue(this);
        startFetchingData();
    }

    private void startFetchingData() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                fetchLastId();
                handler.postDelayed(this, 30000); // Lặp lại mỗi 30s
            }
        }, 3000);
    }

    private void fetchLastId() {
        JsonObjectRequest jsonObjectRequest = new JsonObjectRequest(
                Request.Method.GET, API_LAST_ID, null,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            int newLastId = response.getInt("last_id");
                            if (newLastId != lastId) {
                                lastId = newLastId;
                                fetchMessageById(lastId);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Lỗi xử lý JSON: " + e.getMessage());
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Lỗi lấy ID: " + error.getMessage());
                    }
                }
        );
        requestQueue.add(jsonObjectRequest);
    }

    private void fetchMessageById(int id) {
        Log.d(TAG, "Nhận thông báo mới với ID: " + id);
        // TODO: Xử lý nhận thông báo
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}
