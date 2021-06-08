package com.weather.airlock.sdk.ui;


import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.ibm.airlock.common.notifications.AirlockNotification;
import com.weather.airlock.sdk.AirlockManager;
import com.weather.airlock.sdk.R;

import javax.annotation.Nullable;


public class NotificationExtraInfoFragment extends NotificationDataFragment {
    final public static String NOTIFICATION_DATA_TYPE = "notification.data type";
    private DataType type;

    public enum DataType {
        CONFIGURATION("Configuration"),
        PREVIOUSLY_FIRED("Previously Fired"),
        HISTORY("History"),
        TRACE("Trace");

        private final String tag;

        DataType(String tag) {
            this.tag = tag;
        }

        public String getTag(){
            return this.tag;
        }

        public static DataType getType(String value) {
            for (DataType type : DataType.values()) {
                if (type.tag == value) {
                    return type;
                }
            }
            return null;
        }
    }

    public static Fragment newInstance(String notificationName,DataType type) {
        Fragment fragment = new NotificationExtraInfoFragment();
        // arguments
        Bundle arguments = new Bundle();
        arguments.putString(NOTIFICATION_NAME, notificationName);
        arguments.putString(NOTIFICATION_DATA_TYPE,type.getTag());
        fragment.setArguments(arguments);

        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        //init UI references
        super.onCreate(savedInstanceState);
        this.type = DataType.getType(getArguments().getString(NOTIFICATION_DATA_TYPE));
    }


    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.stream_data, container, false);

        final AirlockNotification notification = AirlockManager.getInstance().getCacheManager().getNotificationsManager().getNotification(notificationName);
        if (notification != null) {
            final TextView headerTextView = (TextView) view.findViewById(R.id.header);
            headerTextView.setText("Notification's "+type.getTag()+" data:");

            final TextView textView = (TextView) view.findViewById(R.id.stream_data);
            textView.setMovementMethod(new ScrollingMovementMethod());

            textView.setText(getFragmentText(notification));

            textView.setOnLongClickListener(new  View.OnLongClickListener() {
                @Override
                public boolean onLongClick( View arg1) {
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setType("plain/json");
                    intent.putExtra(Intent.EXTRA_EMAIL, new String[]{});
                    intent.putExtra(Intent.EXTRA_SUBJECT, "Notification ["+notification.getName()+"] configuration");
                    intent.putExtra(Intent.EXTRA_TEXT, textView.getText().toString());
                    startActivity(Intent.createChooser(intent, ""));
                    return true;
                }
            });
        }

        return view;
    }

    private String getFragmentText(AirlockNotification notification){
        String text="";
        switch (type){
            case CONFIGURATION:
                text = NotificationDataFragment.formatString(notification.getConfiguration() == null
                        || notification.getConfiguration().isEmpty() ? "{}" : notification.getConfiguration());
                break;
            case PREVIOUSLY_FIRED:
                text = (notification.getFiredHistory() == null
                        || notification.getFiredHistory().length()<=0) ? "The specified notification was not fired yet!" : formatString(notification.getFiredHistory().toString());
                break;
            case HISTORY:
                text = (notification.getRegistrationHistory() == null
                        || notification.getRegistrationHistory().length()<=0) ? "The specified notification has no history records!" : formatString(notification.getRegistrationHistory().toString());
                break;
            case TRACE:
                text = NotificationDataFragment.formatString(notification.getTraceInfo() == null
                        || notification.getTraceInfo().isEmpty() ? "{}" : notification.getTraceInfo());
                break;
        }
        return text;
    }

}

