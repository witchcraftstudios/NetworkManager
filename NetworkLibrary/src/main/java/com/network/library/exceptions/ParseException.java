package com.network.library.exceptions;

import org.json.JSONException;

public class ParseException extends JSONException {
    public ParseException(String message) {
        super(message);
    }
}
