package com.network.library;

public interface RequestCallback<T> {

    void onSuccess(T pResult) throws Exception;

    void onError(String pError) throws Exception;
}
