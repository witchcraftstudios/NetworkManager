package com.network.manager;

import android.support.annotation.IntDef;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

public class IntegerManager {

    public static final int INT_ONE = 1;
    public static final int INT_TWO = 2;

    @Retention(RetentionPolicy.SOURCE)
    @IntDef({INT_ONE, INT_TWO})
    @interface IntegerMode {
    }

    public static void setInteger(@IntegerMode int integer, String test) {

    }

}
