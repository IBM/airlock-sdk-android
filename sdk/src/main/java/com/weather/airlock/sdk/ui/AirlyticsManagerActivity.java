package com.weather.airlock.sdk.ui;

import android.content.pm.ActivityInfo;
import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.weather.airlock.sdk.R;


/**
 * The class contains UI logic which enables a dev user to specify which
 * airlock configuration branch a device will be working with.
 * The branch purpose is to override a master configuration.
 *
 * @author Denis Voloshin
 */
public class AirlyticsManagerActivity extends FragmentActivity {

    public static String STREAMS_DETAILS_FRAGMENT = "streams.fragment.tag";
    public static String STREAMS_EVENTS_FRAGMENT = "streams.events.fragment.tag";


    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Make to run your application only in portrait mode
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.airlytics);
    }
}
