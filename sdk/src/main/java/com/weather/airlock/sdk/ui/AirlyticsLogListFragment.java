package com.weather.airlock.sdk.ui;

/**
 * Created by Eitan Schreiber on 21/01/2020.
 */

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;

import com.weather.airlock.sdk.R;
import com.weather.airlytics.AL;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;


public class AirlyticsLogListFragment extends Fragment {


    final public static String ENV_NAME = "env.name";

    //list of available environments
    private ListView listView;

    //adapter for rendering
    private ArrayAdapter<String> adapter;

    //current events list with  the selection choice for this device.
    private List<JSONObject> logEvents;

    //current branch name, by default is 'master'
    private String environmentName;

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        this.environmentName = getArguments().getString(ENV_NAME);

        View view = inflater.inflate(R.layout.airlytics_list, container, false);
        listView = (ListView) view.findViewById(R.id.list);

        //init list

        this.logEvents = new ArrayList<>();

        Map logEntries = generateLogList();
        for (Object entry : logEntries.values()) {
            if (entry instanceof String) {
                try {
                    logEvents.add(new JSONObject((String) entry));
                } catch (JSONException e) {
                    //do nothing
                }
            }
        }

        Collections.sort(logEvents, new Comparator<JSONObject>(){
            @Override
            public int compare(JSONObject obj1, JSONObject obj2) {
                // ## Descending order
                return Long.valueOf(obj2.optLong("eventTime")).compareTo(Long.valueOf(obj1.optLong("eventTime"))); // To compare integer values

            }
        });

        final String[] logEventNames = new String[logEvents.size()];
        for (int i = 0; i < logEvents.size(); i++) {
            logEventNames[i] = logEvents.get(i).optString("name");
        }

        getActivity().runOnUiThread(new Runnable() {
            @RequiresApi(api = Build.VERSION_CODES.M)
            @Override
            public void run() {


                adapter = new ArrayAdapter(getActivity(), android.R.layout.simple_list_item_2, android.R.id.text1, logEventNames) {
                    @Override
                    public View getView(int position, View convertView, ViewGroup parent) {
                        View view = super.getView(position, convertView, parent);
                        TextView text1 = (TextView) view.findViewById(android.R.id.text1);
                        TextView text2 = (TextView) view.findViewById(android.R.id.text2);

                        text1.setText(logEvents.get(position).optString("name"));

                        long date = logEvents.get(position).optLong("eventTime");
                        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

                        String dateString = format.format(date);
                        text2.setText(dateString);
                        return view;
                    }
                };
                listView.setAdapter(adapter);

                LayoutInflater inflater = getActivity().getLayoutInflater();
                ViewGroup header = (ViewGroup) inflater.inflate(R.layout.airlytics_log_list_header, listView, false);
                TextView textView = header.findViewById(R.id.envName);
                textView.setText(environmentName);
                listView.addHeaderView(header);

                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        if (position == 0){
                            return;
                        }
                        FragmentManager manager = getFragmentManager();
                        FragmentTransaction transaction = manager.beginTransaction();
                        transaction.replace(R.id.container, AirlyticsLogDetailsFragment.newInstance(environmentName+ "::" + logEvents.get(position-1).optString("name") + " event", logEvents.get(position-1).toString()));
                        transaction.addToBackStack(null);
                        transaction.commit();
                    }
                });
            }
        });
        return view;
    }


    private void showToast(final String msg) {
        Log.d(this.getClass().getName(), msg);
        if (getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getActivity().getBaseContext(), msg,
                            Toast.LENGTH_SHORT).show();
                }
            });
        }
    }


    @RequiresApi(api = Build.VERSION_CODES.M)
    private Map<String, ?> generateLogList() {
        return AL.Companion.getEnvironmentLogEvents(getContext(), environmentName);
    }

    public static Fragment newInstance(String envName) {
        Fragment fragment = new AirlyticsLogListFragment();
        // arguments
        Bundle arguments = new Bundle();
        arguments.putString(ENV_NAME, envName);
        fragment.setArguments(arguments);
        return fragment;
    }


}

