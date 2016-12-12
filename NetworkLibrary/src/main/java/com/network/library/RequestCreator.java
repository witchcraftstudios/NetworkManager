package com.network.library;

import android.content.Context;

import java.io.InputStream;
import java.lang.ref.WeakReference;

public abstract class RequestCreator<T> {

    private WeakReference<RequestCallback<T>> mRequestCallbackReference;

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

    @SuppressWarnings({"unused", "unchecked"})
    public void setRequestCallback(RequestCallback<T> pRequestCallback) {
        this.mRequestCallbackReference = new WeakReference<>(pRequestCallback);
    }

    void onSuccess(T result) throws Exception {
        if (this.mRequestCallbackReference != null) {
            final RequestCallback<T> mRequestCallback = this.mRequestCallbackReference.get();
            if (mRequestCallback != null) {
                mRequestCallback.onSuccess(result);
            }
        }
    }

    void onError(String error) throws Exception {
        if (this.mRequestCallbackReference != null) {
            final RequestCallback<T> mRequestCallback = this.mRequestCallbackReference.get();
            if (mRequestCallback != null) {
                mRequestCallback.onError(error);
            }
        }
    }
}
