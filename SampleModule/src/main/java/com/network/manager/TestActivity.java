package com.network.manager;

import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.network.library.BackgroundTask;
import com.network.library.NetworkManager;
import com.network.library.NetworkManagerCallbacks;
import com.network.library.RequestCallback;

public class TestActivity extends AppCompatActivity implements Runnable {
    private static final String TAG = "TestActivity";

    private final Handler mHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_test);
    }

    public void onStartClick(View view) {
        this.mHandler.postDelayed(this, 2000);
    }

    private final RequestCallback<InitModel> mRequestCallback = new RequestCallback<InitModel>() {

        @Override
        public void onSuccess(InitModel pResult) throws Exception {
            Log.e(TAG, "RequestCallback" + "onSuccess");
        }

        @Override
        public void onError(String pError) {
            Log.e(TAG, "RequestCallback" + "onError:" + pError);
        }
    };

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

    private BackgroundTask mBackgroundTask;

    @Override
    public void run() {
        final NetworkManager mNetworkManager = NetworkManager.getInstance(getApplication());

        if (this.mBackgroundTask != null && !this.mBackgroundTask.isFinished()) {
            this.mBackgroundTask.cancelRequests();
        }

        mBackgroundTask = mNetworkManager.createBackgroundTask(this, true);
        mBackgroundTask.init("Błąd połączenia...", 1000);

        final SetupRequestCreator mSetupRequestCreator = new SetupRequestCreator();
        mSetupRequestCreator.setRequestCallback(this.mRequestCallback);

        mBackgroundTask.addRequest(mSetupRequestCreator);
        mBackgroundTask.setNetworkManagerCallbacks(this.mNetworkManagerCallbacks);
        mBackgroundTask.execute();

        this.mHandler.postDelayed(this, 2000);
    }

    @Override
    protected void onPause() {
        super.onPause();
        this.mHandler.removeCallbacksAndMessages(null);
    }
}
