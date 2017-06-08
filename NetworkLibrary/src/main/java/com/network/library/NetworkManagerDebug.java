package com.network.library;

public class NetworkManagerDebug {
    public static final boolean DEBUG = false;

    private static final String TAG_DEBUG = "DEBUG:";

    public static void logE(int content) {
        if (DEBUG)
            try {
                NetworkManagerDebug.logE(TAG_DEBUG, TAG_DEBUG + " " + content);
            } catch (Exception e) {
                e.printStackTrace();
            }
    }

    public static void logE(String title, String content) {
        if (DEBUG)
            try {
                NetworkManagerDebug.logE(title, TAG_DEBUG + " " + content);
            } catch (Exception e) {
                e.printStackTrace();
            }
    }

    public static void logE(double content) {
        if (DEBUG)
            try {
                NetworkManagerDebug.logE(TAG_DEBUG, TAG_DEBUG + " " + content);
            } catch (Exception e) {
                e.printStackTrace();
            }
    }

    public static void logE(float content) {
        if (DEBUG)
            try {
                NetworkManagerDebug.logE(TAG_DEBUG, TAG_DEBUG + " " + content);
            } catch (Exception e) {
                e.printStackTrace();
            }
    }

    public static void logE(String title, int content) {
        if (DEBUG)
            try {
                NetworkManagerDebug.logE(title, TAG_DEBUG + " " + content);
            } catch (Exception e) {
                e.printStackTrace();
            }
    }

    public static void logE(String content) {
        if (DEBUG)
            try {
                NetworkManagerDebug.logE(TAG_DEBUG, TAG_DEBUG + " " + content);
            } catch (Exception e) {
                e.printStackTrace();
            }
    }

    public static void logE(String text, float content) {
        if (DEBUG)
            try {
                NetworkManagerDebug.logE(text, TAG_DEBUG + " " + content);
            } catch (Exception e) {
                e.printStackTrace();
            }
    }
}