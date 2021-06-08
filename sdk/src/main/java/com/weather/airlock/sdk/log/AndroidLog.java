package com.weather.airlock.sdk.log;


import android.util.Log;


/**
 * Created by Denis Voloshin on 03/11/2017.
 */

public class AndroidLog implements com.ibm.airlock.common.log.Log {
    @Override
    public int e(String tag, String msg) {
        return Log.e(tag, msg);
    }

    @Override
    public int e(String tag, String msg, Throwable tr) {
        return Log.e(tag, msg, tr);
    }

    @Override
    public int w(String tag, String msg) {
        return Log.w(tag, msg);
    }

    @Override
    public int d(String tag, String msg) {
        return Log.d(tag, msg);
    }

    @Override
    public int i(String tag, String msg) {
        return Log.i(tag, msg);
    }
}
