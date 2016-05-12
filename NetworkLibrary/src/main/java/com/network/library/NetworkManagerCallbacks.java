package com.network.library;

public interface NetworkManagerCallbacks {

    void onStart() throws Exception;

    void onError(String error) throws Exception;

    void onSuccess() throws Exception;

    void onCancelled() throws Exception;

}
