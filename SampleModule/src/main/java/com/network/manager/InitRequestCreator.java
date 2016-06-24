package com.network.manager;

import android.os.Environment;

import com.network.library.MultipartRequestParams;
import com.network.library.RequestCreator;
import com.network.library.RequestHeaders;
import com.network.library.RequestParams;

import java.io.InputStream;

public class InitRequestCreator extends RequestCreator<InitModel> {

    @Override
    public String onCreateUrl() {
        return "https://katalogrozwiazan.pl/pl/solution/217_drugs-analytics.pdf";
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
        String filePath = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).getAbsolutePath();
        getNetworkManager().convertInputStreamToFile(inputStream, filePath, "test.pdf");
        return new InitModel();
    }

    @Override
    public void onResult(InitModel result) throws Exception {

    }
}
