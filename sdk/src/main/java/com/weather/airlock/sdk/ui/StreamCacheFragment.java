package com.weather.airlock.sdk.ui;

/**
 * Created by Denis Voloshin on 04/09/2017.
 */

import javax.annotation.Nullable;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import com.ibm.airlock.common.streams.AirlockStream;
import com.weather.airlock.sdk.AirlockManager;
import com.weather.airlock.sdk.R;


/**
 * Class a android fragment to show stream cache value
 */

public class StreamCacheFragment extends StreamDataFragment {

    public static Fragment newInstance(String streamName) {
        Fragment fragment = new StreamCacheFragment();
        // arguments
        Bundle arguments = new Bundle();
        arguments.putString(STREAM_NAME, streamName);
        fragment.setArguments(arguments);
        return fragment;
    }


    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.stream_data, container, false);
        final AirlockStream stream = AirlockManager.getInstance().getStreamsManager().getStreamByName(streamName);
        if (stream != null) {
            final TextView textView = (TextView) view.findViewById(R.id.stream_data);
            textView.setMovementMethod(new ScrollingMovementMethod());
            textView.setText(StreamDataFragment.formatString(stream.getCache() == null
                    || stream.getCache().isEmpty() ? "{}" : stream.getCache()));

            textView.setOnLongClickListener(new View.OnLongClickListener() {
                @Override
                public boolean onLongClick(View arg1) {
                    Intent intent = new Intent(Intent.ACTION_SEND);
                    intent.setType("plain/json");
                    intent.putExtra(Intent.EXTRA_EMAIL, new String[] {});
                    intent.putExtra(Intent.EXTRA_SUBJECT, "Stream [" + stream.getName() + "] cache");
                    intent.putExtra(Intent.EXTRA_TEXT, textView.getText().toString());
                    startActivity(Intent.createChooser(intent, ""));
                    return true;
                }
            });
        }
        return view;
    }
}

