package com.network.library;

import android.content.Context;

import java.io.InputStream;
import java.lang.ref.WeakReference;

public abstract class RequestCreator<T> {

    private BackgroundTask mBackgroundTask;

    public abstract String onCreateUrl();

    public abstract void onCreateRequestParams(RequestParams requestParams);

    public abstract void onCreateRequestHeaders(RequestHeaders requestHeaders);

    public abstract void onCreateMultipartRequestParams(MultipartRequestParams multipartRequestParams);

    public abstract int onCreateRetryCount();

    public abstract String onCreateRequestMethod();

    public abstract T onDownloadSuccess(InputStream inputStream) throws Exception;

    public abstract void onResult(T result) throws Exception;

    void setBackgroundTask(BackgroundTask networkManager) {
        this.mBackgroundTask = networkManager;
    }

    @SuppressWarnings("unused")
    public void setErrorMessage(String errorMessage) {
        this.mBackgroundTask.setErrorMassage(errorMessage);
    }

    @SuppressWarnings("unused")
    protected BackgroundTask getNetworkManager() {
        return this.mBackgroundTask;
    }

    @SuppressWarnings("unused")
    public Context getContext() {
        return getNetworkManager().getContext();
    }
}
