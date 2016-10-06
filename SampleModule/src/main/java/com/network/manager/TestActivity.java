package com.network.manager;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.network.library.NetworkManager;
import com.network.library.NetworkManagerCallbacks;
import com.network.library.RequestCallback;

public class TestActivity extends AppCompatActivity {
    public static final String TAG = "TestActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_test);
    }

    public void onStartClick(View view) {
        NetworkManager networkManager = new NetworkManager(TestActivity.this, true);
        networkManager.init("Błąd połączenia...", 1000);

        RequestCallback<InitModel> mRequestCallback = new RequestCallback<InitModel>() {

            @Override
            public void onSuccess(InitModel pResult) throws Exception {
                Log.e(TAG, "RequestCallback" + "onSuccess");
            }

            @Override
            public void onError(String pError) {
                Log.e(TAG, "RequestCallback" + "onError:" + pError);
            }
        };

        SetupRequestCreator setupRequestCreator = new SetupRequestCreator();
        setupRequestCreator.setRequestCallback(mRequestCallback);
        networkManager.addRequest(setupRequestCreator);
        networkManager.addRequest(setupRequestCreator);
        networkManager.addRequest(setupRequestCreator);
        networkManager.addRequest(setupRequestCreator);
        networkManager.addRequest(setupRequestCreator);
        networkManager.addRequest(setupRequestCreator);
        networkManager.setNetworkManagerCallbacks(new NetworkManagerCallbacks() {
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
        });
        networkManager.execute();
    }
}
