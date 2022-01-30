package com.ibm.airlock.sdk.ui;

/*
  Created by Denis Voloshin on 04/09/2017.
 */

import android.app.Fragment;
import android.os.Bundle;

import javax.annotation.Nullable;


public class StreamDataFragment extends DataFragment {
    final public static String STREAM_NAME = "stream.name";
    protected String streamName;

    public static Fragment newInstance(String streamName) {
        Fragment fragment = new StreamDetailFragment();
        // arguments
        Bundle arguments = new Bundle();
        arguments.putString(STREAM_NAME, streamName);
        fragment.setArguments(arguments);

        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        //init UI references
        super.onCreate(savedInstanceState);
        this.streamName = getArguments().getString(STREAM_NAME);
    }
}

