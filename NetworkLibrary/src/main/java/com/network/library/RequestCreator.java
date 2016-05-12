package com.network.library;

import android.content.Context;

public abstract class RequestCreator<T> {

    private NetworkManager networkManager;

    public abstract String onCreateUrl();

    public abstract void onCreateRequestParams(RequestParams requestParams);

    public abstract void onCreateRequestHeaders(RequestHeaders requestHeaders);

    public abstract void onCreateMultipartRequestParams(MultipartRequestParams multipartRequestParams);

    public abstract int onCreateRetryCount();

    public abstract String onCreateRequestMethod();

    public abstract T onDownloadSuccess(String response) throws Exception;

    public abstract void onResult(T result) throws Exception;

    public void setNetworkManager(NetworkManager networkManager) {
        this.networkManager = networkManager;
    }

    @SuppressWarnings("unused")
    public void setErrorMessage(String errorMessage) {
        this.networkManager.setErrorMassage(errorMessage);
    }

    @SuppressWarnings("unused")
    public NetworkManager getNetworkManager() {
        return this.networkManager;
    }

    @SuppressWarnings("unused")
    public Context getContext(){
        return getNetworkManager().getContext();
    }

}
