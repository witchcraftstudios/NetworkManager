package com.network.library;

import java.util.HashMap;

public class RequestHeaders {

    private final HashMap<String, String> urlParams = new HashMap<>();

    public void put(String key, String value) {
        urlParams.put(key, value);
    }

    public HashMap<String, String> getHeaders() {
        return urlParams;
    }
}
