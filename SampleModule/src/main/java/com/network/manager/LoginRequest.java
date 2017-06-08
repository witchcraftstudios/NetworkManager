package com.network.manager;

import android.util.Log;

import com.network.library.MultipartRequestParams;
import com.network.library.RequestCreator;
import com.network.library.RequestHeaders;
import com.network.library.RequestMethod;
import com.network.library.RequestParams;
import com.network.library.exceptions.CustomException;

import org.json.JSONObject;

import java.io.InputStream;

public class LoginRequest extends RequestCreator {
    private static final String TAG = "LoginRequest";

    @Override
    public String onCreateUrl() {
        return "http://ovbsalesbook.dev.wcstd.net/api/Login.json";
    }

    @Override
    public void onCreateRequestParams(RequestParams requestParams) {
        requestParams.put("login", "test");
        requestParams.put("password", "test");
    }

    @Override
    public void onCreateRequestHeaders(RequestHeaders requestHeaders) {

    }

    @Override
    public void onCreateMultipartRequestParams(MultipartRequestParams multipartRequestParams) {

    }

    @Override
    public int onCreateRetryCount() {
        return 3;
    }

    @Override
    public String onCreateRequestMethod() {
        return RequestMethod.POST;
    }

    @Override
    public Object onDownloadSuccess(InputStream inputStream) throws Exception {
        final String response = getBackgroundTask().convertInputStreamToString(inputStream);
        Log.e(TAG, "onDownloadSuccess: " + response);
        final JSONObject responseObject = new JSONObject(response);
        final JSONObject replyObject = responseObject.getJSONObject("reply");
        if (replyObject.has("error")) {
            JSONObject errorObject = replyObject.getJSONObject("error");
            throw new CustomException(errorObject.getString("description"));
        }
        return null;
    }

    @Override
    public void onResult(Object result) throws Exception {

    }
}
