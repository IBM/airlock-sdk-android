package com.weather.airlock.sdk.ui;


import android.app.Fragment;
import android.os.Bundle;

import javax.annotation.Nullable;


public class NotificationDataFragment extends DataFragment {
    final public static String NOTIFICATION_NAME = "notification.name";
    protected String notificationName;

    public static Fragment newInstance(String notificationName) {
        Fragment fragment = new NotificationDataFragment();
        // arguments
        Bundle arguments = new Bundle();
        arguments.putString(NOTIFICATION_NAME, notificationName);
        fragment.setArguments(arguments);

        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        //init UI references
        super.onCreate(savedInstanceState);
        this.notificationName = getArguments().getString(NOTIFICATION_NAME);
    }
}

