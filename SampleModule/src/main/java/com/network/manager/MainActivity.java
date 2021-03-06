package com.network.manager;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.network.library.BackgroundTask;
import com.network.library.NetworkManager;
import com.network.library.NetworkManagerCallbacks;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_main);
    }

    public void onStartClick(View view) {
        final NetworkManager mNetworkManager = NetworkManager.getInstance(getApplication());
        final BackgroundTask mBackgroundTask = mNetworkManager.createBackgroundTask(this, true);
        mBackgroundTask.init("Błąd podczas połączenia...", "Błąd pobierania danych..",
                "Brak dostępu do internetu...","Bład połączenia...", 1000);
        mBackgroundTask.addRequest(new LoginRequest());
        mBackgroundTask.setNetworkManagerCallbacks(this.mNetworkManagerCallbacks);
        mBackgroundTask.execute();
    }

    private final NetworkManagerCallbacks mNetworkManagerCallbacks = new NetworkManagerCallbacks() {
        @Override
        public void onStart() throws Exception {
            Log.e(TAG, "onStart");
        }

        @Override
        public void onError(String error) {
            Log.e(TAG, error);
        }

        @Override
        public void onSuccess() {
            Log.e(TAG, "onSuccess");
        }

        @Override
        public void onCancelled() throws Exception {
            Log.e(TAG, "onCancelled");
        }
    };

    public void onNextClick(View view) {

    }
}
