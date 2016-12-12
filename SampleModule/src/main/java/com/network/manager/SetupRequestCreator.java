package com.network.manager;

import android.os.Environment;
import android.util.Log;

import com.network.library.MultipartRequestParams;
import com.network.library.RequestCreator;
import com.network.library.RequestHeaders;
import com.network.library.RequestParams;

import java.io.File;
import java.io.InputStream;

public class SetupRequestCreator extends RequestCreator<InitModel> {

    private static final String TAG = "SetupRequestCreator";

    @Override
    public String onCreateUrl() {
        return "http://app.witchcraftstudios.com/finspi/finspi/NaviSail-Files.zip";
    }

    @Override
    public void onCreateRequestParams(RequestParams requestParams) {

    }

    @Override
    public void onCreateRequestHeaders(RequestHeaders requestHeaders) {

    }

    @Override
    public void onCreateMultipartRequestParams(MultipartRequestParams multipartRequestParams) {

    }

    @Override
    public int onCreateRetryCount() {
        return 10;
    }

    @Override
    public String onCreateRequestMethod() {
        return null;
    }

    @Override
    public InitModel onDownloadSuccess(InputStream inputStream) throws Exception {
        final File storagePublicDirectory = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS + "/NetworkManager/");
        if (!storagePublicDirectory.exists()) {
            final boolean success = storagePublicDirectory.mkdirs();
            Log.e(TAG, "createFolder: " + success);
        }


        getNetworkManager().convertInputStreamToFile(inputStream, storagePublicDirectory.getAbsolutePath(), "test.zip");
        return new InitModel();
    }

    @Override
    public void onResult(InitModel result) throws Exception {

    }
}
