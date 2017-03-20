package com.network.manager;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.network.library.BackgroundTask;
import com.network.library.NetworkManager;
import com.network.library.NetworkManagerCallbacks;

public class MainActivity extends AppCompatActivity {
    public static final int PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE = 1;
    public static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_main);
    }

    public void onStartClick(View view) {
        if (onCheckPermissions()) {
            final NetworkManager mNetworkManager = NetworkManager.getInstance(getApplication());
            final BackgroundTask mBackgroundTask = mNetworkManager.createBackgroundTask(this, true);
            mBackgroundTask.init("Błąd połączenia...", 1000);

            SetupRequestCreator mSetupRequestCreator = new SetupRequestCreator();

            mBackgroundTask.addRequest(mSetupRequestCreator);
            mBackgroundTask.setNetworkManagerCallbacks(this.mNetworkManagerCallbacks);
            mBackgroundTask.execute();
        }
    }

    public boolean onCheckPermissions() {
        final String state = Environment.getExternalStorageState();
        if (!Environment.MEDIA_MOUNTED.equals(state)) {
            Log.e(TAG, "Error: External storage is unavailable");
            return false;
        }
        if (Environment.MEDIA_MOUNTED_READ_ONLY.equals(state)) {
            Log.e(TAG, "Error: External storage is read only.");
            return false;
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "permission: WRITE_EXTERNAL_STORAGE: NOT granted!");
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE);
            return false;
        }
        return true;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String permissions[], @NonNull int[] grantResults) {
        switch (requestCode) {
            case PERMISSIONS_REQUEST_WRITE_EXTERNAL_STORAGE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {

                }
            }
        }
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
        finish();
        Intent mIntent = new Intent(this, TestActivity.class);
        startActivity(mIntent);
    }
}
