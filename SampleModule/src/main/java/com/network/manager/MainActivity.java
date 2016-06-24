package com.network.manager;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;

import com.network.library.NetworkManager;
import com.network.library.NetworkManagerCallbacks;

public class MainActivity extends AppCompatActivity {
    public static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
    }

    public void onStartClick(View view) {
        NetworkManager networkManager = new NetworkManager(MainActivity.this, true);
        networkManager.init("Błąd połączenia...", 1000);
        networkManager.setDialog("Logowanie...", ProgressDialog.STYLE_SPINNER);
        networkManager.addRequest(new InitRequestCreator());
        networkManager.addRequest(new InitRequestCreator());
        networkManager.addRequest(new InitRequestCreator());
        networkManager.addRequest(new InitRequestCreator());
        networkManager.addRequest(new InitRequestCreator());
        networkManager.addRequest(new InitRequestCreator());
        networkManager.addRequest(new InitRequestCreator());
        networkManager.setNetworkManagerCallbacks(new NetworkManagerCallbacks() {
            @Override
            public void onStart() throws Exception {
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
