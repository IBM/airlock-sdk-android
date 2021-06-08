package com.weather.airlock.sdk.ui;

/**
 * Created by Denis Voloshin on 04/09/2017.
 */

import java.io.IOException;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Map;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.graphics.Color;
import android.os.Bundle;
import androidx.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONObject;
import com.ibm.airlock.common.net.AirlockDAO;
import com.ibm.airlock.common.streams.AirlockStream;
import com.ibm.airlock.common.util.Constants;
import com.weather.airlock.sdk.AirlockManager;
import com.weather.airlock.sdk.R;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;


public class StreamsListFragment extends Fragment {


    //list of available streams
    private ListView listView;

    //adapter for rendering
    private ArrayAdapter<String> adapter;

    //current branches list with  the selection choice for this device.
    private Map<String, String> streams;

    //current branch name, by default is 'master'
    private String[] streamNames;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.streams_list, container, false);
        listView = (ListView) view.findViewById(R.id.list);

        //init list
        this.streams = new Hashtable<>();
        AirlockManager.getInstance().getCacheManager().getPersistenceHandler().write(Constants.SP_LAST_STREAMS_FULL_DOWNLOAD_TIME, "");
        AirlockManager.getInstance().getCacheManager().getPersistenceHandler().write(Constants.SP_LAST_STREAMS_JS_UTILS_DOWNLOAD_TIME, "");
        AirlockDAO.pullStreams(AirlockManager.getInstance().getCacheManager(), new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                final String error = String.format(getResources().getString(R.string.retrieving_streams), call.request().url().toString());
                Log.e(Constants.LIB_LOG_TAG, error);
                showToast(error);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                //read the response to the string
                String responseBody;

                if (response.code() == 304) {
                    responseBody = AirlockManager.getInstance().getCacheManager().getPersistenceHandler().read(Constants.SP_FEATURE_USAGE_STREAMS, "");
                } else if (response.body() == null || response.body().toString().isEmpty() || !response.isSuccessful()) {
                    if (response.body() != null) {
                        response.body().close();
                    }
                    final String warning = getResources().getString(R.string.streams_is_empty);
                    Log.w(Constants.LIB_LOG_TAG, warning);
                    showToast(warning);
                    return;
                } else {
                    responseBody = response.body().string();
                }

                response.close();

                //update streams
                AirlockManager.getInstance().getCacheManager().getPersistenceHandler().write(Constants.SP_FEATURE_USAGE_STREAMS, responseBody);
                AirlockManager.getInstance().getStreamsManager().updateStreams();

                //parse server response,the response has to be in json format
                try {
                    final JSONObject streamsFullResponse = new JSONObject(responseBody);
                    response.body().close();
                    if (streamsFullResponse.isNull("streams")) {
                        String warning = getResources().getString(R.string.streams_is_empty);
                        Log.w(Constants.LIB_LOG_TAG, warning);
                        showToast(warning);
                        return;
                    }
                    final JSONArray streamsArray = streamsFullResponse.getJSONArray("streams");

                    if (getActivity() == null) {
                        return;
                    }

                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            streams = generateStreamsList(streamsArray);
                            java.util.Set<String> keys = streams.keySet();
                            streamNames = (keys.toArray(new String[keys.size()]));
                            Arrays.sort(streamNames);

                            adapter = new ArrayAdapter<String>(getActivity(),
                                    android.R.layout.simple_list_item_1, streamNames){
                                @Override
                                public View getView(int position, @Nullable View convertView, ViewGroup parent) {
                                    convertView = new TextView(getContext());
                                    convertView.setPadding(20, 30, 20, 30);
                                    ((TextView) convertView).setTextSize(15);
                                    String streamName = getItem(position);
                                    AirlockStream stream = AirlockManager.getInstance().getStreamsManager().getStreamByName(streamName);
                                    if (stream != null) {
                                        if (stream.isEnabled()&stream.isAssociatedWithUserGroup()) {
                                            ((TextView) convertView).setTextColor(Color.BLUE);
                                        } else {
                                            ((TextView) convertView).setTextColor(Color.BLACK);
                                        }
                                        ((TextView) convertView).setText(streamName);
                                    }
                                    return convertView;
                                }
                            };
                            listView.setAdapter(adapter);

                            LayoutInflater inflater = getActivity().getLayoutInflater();
                            ViewGroup header = (ViewGroup) inflater.inflate(R.layout.streams_list_header, listView, false);
                            listView.addHeaderView(header);

                            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                @Override
                                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                    if (position > 0) {
                                        final AirlockStream stream = AirlockManager.getInstance().getStreamsManager().getStreamByName(streamNames[position - 1]);
                                        if (stream == null) {
                                            showToast(getResources().getString(R.string.stream_not_available, streamNames[position - 1]));
                                            return;
                                        }

                                        if (!stream.isEnabled()||!stream.isAssociatedWithUserGroup()){
                                            StringBuilder sbMessage = new StringBuilder();
                                            if (!stream.isEnabled()){
                                                sbMessage.append("disabled");
                                                if (!stream.isAssociatedWithUserGroup()){
                                                    sbMessage.append(" and not associated with any active user group");
                                                }
                                            }
                                            else{
                                                sbMessage.append("not associated with any active user group");
                                            }
                                            showToast("Stream '"+ streamNames[position - 1] + "' is "+ sbMessage.toString() );
                                            return;
                                        }

                                        FragmentManager manager = getFragmentManager();
                                        FragmentTransaction transaction = manager.beginTransaction();
                                        transaction.add(R.id.container, StreamDetailFragment.newInstance(streamNames[position - 1]), StreamsManagerActivity
                                                .STREAMS_DETAILS_FRAGMENT);
                                        transaction.addToBackStack(null);
                                        transaction.commit();
                                    }
                                }
                            });
                        }
                    });
                } catch (Exception e) {
                    final String error = getResources().getString(R.string.streams_process_failed);
                    Log.e(Constants.LIB_LOG_TAG, error);
                    showToast(error);
                    Log.e(Constants.LIB_LOG_TAG, "");
                    //render only values from the cache empty list
                }
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


    private Map<String, String> generateStreamsList(JSONArray branches) {
        Map<String, String> streamsMap = new Hashtable<>();
        int branchesListLength = branches.length();
        for (int i = 0; i < branchesListLength; i++) {
            JSONObject branchJSON = branches.optJSONObject(i);
            if (branchJSON != null && branchJSON.has("name") && branchJSON.has("uniqueId")) {
                String name = branchJSON.optString("name");
                String uniqueId = branchJSON.optString("uniqueId");
                if (name != null && uniqueId != null) {
                    final AirlockStream stream = AirlockManager.getInstance().getStreamsManager().getStreamByName(name);
                    if (stream != null && stream.isAssociatedWithUserGroup()) {
                        streamsMap.put(name, uniqueId);
                    }
                }
            }
        }
        return streamsMap;
    }
}

