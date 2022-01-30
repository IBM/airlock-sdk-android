package com.ibm.airlock.sdk.ui;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentActivity;

import com.ibm.airlock.sdk.R;


/**
 * The class contains UI logic which enables a dev user to specify which
 * airlock configuration branch a device will be working with.
 * The branch purpose is to override a master configuration.
 *
 * @author Denis Voloshin
 */
public class NotificationsManagerActivity extends FragmentActivity {

    public static String NOTIFICATIONS_DETAILS_FRAGMENT = "notifications.fragment.tag";
    public static String NOTIFICATIONS_EVENTS_FRAGMENT = "notifications.events.fragment.tag";


    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Make to run your application only in portrait mode
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.notifications);
    }
}
