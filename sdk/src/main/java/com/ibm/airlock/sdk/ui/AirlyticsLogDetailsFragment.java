package com.ibm.airlock.sdk.ui;

/**
 * Created by Eitan Schreiber on 21/01/2020.
 */

import android.app.Fragment;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.ibm.airlock.sdk.R;


public class AirlyticsLogDetailsFragment extends Fragment {


    final public static String ENV_NAME = "env.name";
    final public static String LOG_DETAILS = "log.details";

    //current branch name, by default is 'master'
    @Override
    public View onCreateView(final LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final String environmentName = getArguments().getString(ENV_NAME);

        final View view = inflater.inflate(R.layout.airlytics_log_details, container, false);

        getActivity().runOnUiThread(new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void run() {

                TextView header = view.findViewById(R.id.envName);
                header.setText(environmentName);
                TextView logDetailsView = view.findViewById(R.id.eventDetails);
                String logDetails = getArguments().getString(LOG_DETAILS);
                logDetails = logDetails.replaceAll(",", ",\n");
                logDetailsView.setText(logDetails);
            }

        });
        return view;
    }


    public static Fragment newInstance(String envName, String logDetails) {
        Fragment fragment = new AirlyticsLogDetailsFragment();
        // arguments
        Bundle arguments = new Bundle();
        arguments.putString(ENV_NAME, envName);
        arguments.putString(LOG_DETAILS, logDetails);
        fragment.setArguments(arguments);
        return fragment;
    }


}

