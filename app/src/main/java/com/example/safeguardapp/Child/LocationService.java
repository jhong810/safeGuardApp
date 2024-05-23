package com.example.safeguardapp.Child;

import static com.example.safeguardapp.Child.ChildMainActivity.latitude;
import static com.example.safeguardapp.Child.ChildMainActivity.longitude;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.example.safeguardapp.LogIn.LoginPageFragment;
import com.example.safeguardapp.R;
import com.example.safeguardapp.RetrofitClient;
import com.example.safeguardapp.UserRetrofitInterface;
import com.google.gson.Gson;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LocationService extends Service {

    private static final String CHANNEL_ID = "LocationServiceChannel";
    private static final int INTERVAL = 10000; // 10초
    private Handler handler = new Handler();
    private Runnable runnable;
    private static String id;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }


    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        SharedPreferences sharedPreferences1 = getSharedPreferences("LocationPrefs", Context.MODE_PRIVATE);
//        final double latitude = Double.longBitsToDouble(sharedPreferences1.getLong("latitude_v2", Double.doubleToLongBits(0.0)));
//        final double longitude = Double.longBitsToDouble(sharedPreferences1.getLong("longitude_v2", Double.doubleToLongBits(0.0)));
        SharedPreferences sharedPreferences2 = getSharedPreferences("loginID", Context.MODE_PRIVATE);
        id = sharedPreferences2.getString("saveID", null);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Location Service")
                .setContentText("Sending location data every 10 seconds")
                .build();

        startForeground(1, notification);

        runnable = new Runnable() {
            @Override
            public void run() {
                transmitCoordinate(latitude, longitude);
                handler.postDelayed(this, INTERVAL);
            }
        };
        handler.post(runnable);

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(runnable);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public static void transmitCoordinate(double latitude, double longitude) {
        String type = "Child";
        LocationSendRequest locationDTO = new LocationSendRequest(type, id, latitude, longitude);
        RetrofitClient retrofitClient = RetrofitClient.getInstance();
        UserRetrofitInterface userRetrofitInterface = retrofitClient.getUserRetrofitInterface();

        Gson gson = new Gson();
        String locationInfo = gson.toJson(locationDTO);
        Log.e("JSON", locationInfo);

        Call<ResponseBody> call = userRetrofitInterface.sendLocation(locationDTO);
        call.enqueue(new Callback<ResponseBody>() {
            @Override
            public void onResponse(Call<ResponseBody> call, Response<ResponseBody> response) {
                if (response.isSuccessful()) {
                    Log.e("POST", "전달 성공");
                    try {
                        String responseBody = response.body().string();
                        Log.e("POST", "Response Body: " + responseBody);
                    } catch (IOException e) {
                        Log.e("POST", "응답 본문 처리 중 오류 발생: " + e.getMessage());
                    }
                } else {
                    Log.e("POST", "전달 실패, HTTP Status: " + response.code());
                    try {
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();
                            Log.e("POST", "Error Body: " + errorBody);
                        } else {
                            Log.e("POST", "Error Body: null");
                        }
                    } catch (IOException e) {
                        Log.e("POST", "오류 본문 처리 중 오류 발생: " + e.getMessage());
                    }
                }
            }

            @Override
            public void onFailure(Call<ResponseBody> call, Throwable t) {
                Log.e("POST", "통신 실패");
            }
        });
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Location Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }
}

