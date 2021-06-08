package com.weather.airlock.sdk.notifications;

import android.app.AlarmManager;
import android.app.PendingIntent;

import com.ibm.airlock.common.cache.Context;
import com.ibm.airlock.common.cache.PersistenceHandler;
import com.ibm.airlock.common.engine.AirlockContextManager;
import com.ibm.airlock.common.notifications.NotificationsManager;

/**
 * Created by SEitan on 03/12/2017.
 */

public class AndroidNotificationsManager extends NotificationsManager {
    public AndroidNotificationsManager(Context context, PersistenceHandler ph, String appVersion, AirlockContextManager airlockScriptScope) {
        super(context, ph, appVersion, airlockScriptScope);
    }

    @Override
    public void scheduleNotificationAlarm(long dueDate) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(android.content.Context.ALARM_SERVICE);
        if (alarmManager != null && this.notificationIntent != null) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, dueDate, (PendingIntent) this.notificationIntent);
        }
    }
}
