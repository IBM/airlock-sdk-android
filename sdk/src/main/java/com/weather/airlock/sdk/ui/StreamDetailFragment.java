package com.weather.airlock.sdk.ui;

/**
 * Created by Denis Voloshin on 04/09/2017.
 */

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.ibm.airlock.common.log.Logger;
import com.ibm.airlock.common.streams.AirlockStream;
import com.ibm.airlock.common.util.Constants;
import com.weather.airlock.sdk.AirlockManager;
import com.weather.airlock.sdk.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Random;

import javax.annotation.Nullable;


public class StreamDetailFragment extends StreamDataFragment {


    private static String[] DISPLAYS_OPTIONS = {"Trace", "Events Queue", "Cache", "Results"};
    private static String[] ACTION_OPTIONS = {
            "Reset Stream", "Clear Trace", "Process Stream", "Percentage", "Verbose", "Suspend Process"
    };

    private String eventsQueue = "";
    private String cache = "";
    private ListView action;
    private ListView displayList;
    private BaseAdapter displayAdapter;


    private static final ThreadLocal<DateFormat> yyyyMMddTHHmmss = new ThreadLocal<DateFormat>() {
        @Override
        protected DateFormat initialValue() {
            return new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.stream_details, container, false);
        displayList = (ListView) view.findViewById(R.id.listDisplay);
        displayAdapter = new CustomDisplay(getFragmentManager(), getActivity(), DISPLAYS_OPTIONS, streamName);
        displayList.setAdapter(displayAdapter);
        displayList.setItemsCanFocus(true);

        action = (ListView) view.findViewById(R.id.listActions);
        action.setAdapter(new CustomAction(getActivity(), ACTION_OPTIONS, eventsQueue, cache, streamName, displayList));
        action.setItemsCanFocus(true);

        TextView stream_name = (TextView) view.findViewById(R.id.stream_name);
        stream_name.setText(this.streamName);
        return view;
    }


    static class CustomDisplay extends BaseAdapter {
        String[] data;
        FragmentManager manager;
        private String streamName = "";
        private static LayoutInflater inflater = null;

        public CustomDisplay(FragmentManager manager, Context context, String[] data, String streamName) {
            this.data = data;
            this.manager = manager;
            this.streamName = streamName;
            inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public boolean isEnabled(int position) {
            return true;
        }

        @Override
        public int getCount() {
            // TODO Auto-generated method stub
            return data.length;
        }

        @Override
        public Object getItem(int position) {
            // TODO Auto-generated method stub
            return data[position];
        }

        @Override
        public long getItemId(int position) {
            // TODO Auto-generated method stub
            return position;
        }


        @Override
        public View getView(final int position, View convertView, ViewGroup parent) {
            // TODO Auto-generated method stub
            View view = inflater.inflate(R.layout.display_row, parent, false);

            view.setClickable(true);
            view.setFocusable(true);

            if (position == 0) {
                setOnClickListener(view, TraceListFragment.newInstance(streamName), manager);
            }
            if (position == 1) {
                setOnClickListener(view, EventsListFragment.newInstance(streamName), manager);
            }
            if (position == 2) {
                setOnClickListener(view, StreamCacheFragment.newInstance(streamName), manager);
            }
            if (position == 3) {
                setOnClickListener(view, StreamResultFragment.newInstance(streamName), manager);
            }

            TextView title = (TextView) view.findViewById(R.id.title);
            title.setText(DISPLAYS_OPTIONS[position]);
            TextView value = (TextView) view.findViewById(R.id.value);

            if (position == 1) {
                AirlockStream stream = AirlockManager.getInstance().getStreamsManager().getStreamByName(streamName);
                if (stream != null) {
                    value.setText(Integer.valueOf(stream.getEvents().length()).toString());
                }
            }
            if (position == 2) {
                AirlockStream stream = AirlockManager.getInstance().getStreamsManager().getStreamByName(streamName);
                if (stream != null) {
                    value.setText(stream.getCache() == null ? "0" : stream.getCache().getBytes().length + " bytes");
                }
            }
            if (position == 3) {
                AirlockStream stream = AirlockManager.getInstance().getStreamsManager().getStreamByName(streamName);
                if (stream != null) {
                    value.setText(stream.getResult() == null ? "0" : stream.getResult().getBytes().length + " bytes");
                }
            }

            return view;
        }
    }

    private static void setOnClickListener(View view, final Fragment fragment, final FragmentManager manager) {
        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentTransaction transaction = manager.beginTransaction();
                transaction.replace(R.id.container, fragment, StreamsManagerActivity
                        .STREAMS_EVENTS_FRAGMENT);
                transaction.addToBackStack(null);
                transaction.commit();
            }
        });
    }


    static class CustomAction extends BaseAdapter {
        Context context;
        String[] data;
        FragmentManager manager;
        private String eventsQueue = "";
        private String cache = "";
        private String streamName = "";
        private ListView displayList;
        private TextView processingDateField;
        private TextView steam_process_action;
        private static LayoutInflater inflater = null;

        public CustomAction(Context context, String[] data, String eventsQueue, String cache, String streamName, ListView displayList) {
            this.context = context;
            this.data = data;
            this.eventsQueue = eventsQueue;
            this.streamName = streamName;
            this.displayList = displayList;
            this.cache = cache;
            inflater = (LayoutInflater) context
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public boolean isEnabled(int position) {
            return true;
        }

        @Override
        public int getCount() {
            // TODO Auto-generated method stub
            return data.length;
        }

        @Override
        public Object getItem(int position) {
            // TODO Auto-generated method stub
            return data[position];
        }

        @Override
        public long getItemId(int position) {
            // TODO Auto-generated method stub
            return position;
        }


        @Override
        public View getView(final int position, final View convertView, final ViewGroup parent) {
            // TODO Auto-generated method stub
            View view = convertView;
            if (view == null) {
                if (position < 3) {
                    view = inflater.inflate(R.layout.action_row, parent, false);
                } else {
                    view = inflater.inflate(R.layout.action_row_adv, parent, false);
                }
            }
            Button stream_action = (Button) view.findViewById(R.id.stream_action);
            view.setClickable(true);
            view.setFocusable(true);

            final AirlockStream stream = AirlockManager.getInstance().getStreamsManager().getStreamByName(streamName);

            if (stream != null) {

                // Clear data
                if (position == 0) {
                    stream_action.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (stream != null) {
                                stream.clearProcessingData();
                                displayList.invalidateViews();
                            }
                        }
                    });
                }

                // Clear trace
                if (position == 1) {
                    stream_action.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (stream != null) {
                                stream.clearTrace();
                            }
                        }
                    });
                }

                // start process for all streams
                if (position == 2) {
                    stream_action.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (!stream.isProcessingSuspended()){
                                JSONArray events = stream.getEvents();
                                stream.clearEvents();
                                AirlockManager.getInstance().getStreamsManager().calculateAndSaveStreams(events, true, new String[]{stream.getName()}, null, null);
                                processingDateField.setText(getLastStreamProcessTime(stream));
                                displayList.invalidateViews();
                            }
                        }
                    });
                    processingDateField = (TextView) view.findViewById(R.id.date);
                    steam_process_action = stream_action;
                    processingDateField.setText(getLastStreamProcessTime(stream));
                    steam_process_action.setTextColor(AirlockManager.getInstance().getCacheManager().getPersistenceHandler().readBoolean(Constants.SP_STREAMS_PROCESS_SUSPENDED,
                            false) ? Color
                            .BLACK : Color.parseColor("#3753df"));

                    ((TextView) view.findViewById(R.id.last_perform)).setVisibility(View.VISIBLE);
                }

                // Percentage
                if (position == 3) {
                    ToggleButton stream_checkbox = (ToggleButton) view.findViewById(R.id.checkbox);
                    stream_checkbox.setEnabled(stream.getRolloutPercentage() != 100 && stream.getRolloutPercentage() != 0);
                    try {
                        stream_checkbox.setChecked(Long.valueOf(AirlockManager.getInstance().getCacheManager().getPersistenceHandler().getStreamsRandomMap().getString(stream
                                .getName())) <=
                                stream.getRolloutPercentage()*10000);
                    } catch (JSONException e) {
                        Logger.log.e(Constants.LIB_LOG_TAG, "Error while fetching Stream's random number: ", e);
                    }
                    final TextView stream_action_value = (TextView) view.findViewById(R.id.stream_action_value);
                    stream_checkbox.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ToggleButton stream_checkbox = (ToggleButton) v;
                            JSONObject streamsRandomMap = AirlockManager.getInstance().getCacheManager().getPersistenceHandler().getStreamsRandomMap();
                            int splitPoint = (int) Math.floor(stream.getRolloutPercentage() * 10000);
                            try {
                                if (stream_checkbox.isChecked()) // select a user random number smaller than the split point
                                {
                                    int rand = new Random().nextInt(splitPoint) + 1;
                                    streamsRandomMap.put(stream.getName(), String.valueOf(rand));
                                    AirlockManager.getInstance().getCacheManager().getPersistenceHandler().setStreamsRandomMap(streamsRandomMap);

                                } else// select a user random number bigger than the split point
                                {
                                    int rand = new Random().nextInt(1000000 - splitPoint) + splitPoint + 1;
                                    streamsRandomMap.put(stream.getName(), String.valueOf(rand));
                                    AirlockManager.getInstance().getCacheManager().getPersistenceHandler().setStreamsRandomMap(streamsRandomMap);
                                }
                                stream.setProcessingEnablement();
                            } catch (JSONException e) {
                                Logger.log.e(Constants.LIB_LOG_TAG, "Error while updating Stream's random number: ", e);
                            }

//                            stream_action_value.setText((stream.getRolloutPercentage()) + "%");
                        }
                    });
                    ((Button) view.findViewById(R.id.stream_action)).setTextColor(Color.BLACK);
                    stream_action_value.setText((stream.getRolloutPercentage()) + "%");
                }
                if (position == 4) {
                    ((Button) view.findViewById(R.id.stream_action)).setTextColor(Color.BLACK);
                }
                // Suspend processing for stream
                if (position == 5) {
                    ToggleButton stream_checkbox = (ToggleButton) view.findViewById(R.id.checkbox);
                    ((Button) view.findViewById(R.id.stream_action)).setTextColor(Color.BLACK);
                    stream_checkbox.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            ToggleButton stream_checkbox = (ToggleButton) v;
                            AirlockManager.getInstance().getStreamsManager().getStreamByName(streamName).setProcessingSuspended(stream_checkbox.isChecked());
                            steam_process_action.setTextColor(stream_checkbox.isChecked() ? Color.BLACK : Color.parseColor("#3753df"));
                            steam_process_action.setEnabled(!stream_checkbox.isChecked());
                            stream.persist(AirlockManager.getInstance().getCacheManager().getPersistenceHandler());

                        }
                    });
                    steam_process_action.setEnabled(!stream.isProcessingSuspended());
                    stream_checkbox.setChecked(stream.isProcessingSuspended());
                }

                stream_action.setText(ACTION_OPTIONS[position]);
            }
            return view;
        }


        private String getLastStreamProcessTime(AirlockStream stream) {
            if (stream.getLastProcessedTime().isEmpty()) {
                return "n/a";
            }
            DateFormat df = yyyyMMddTHHmmss.get();
            return df.format(Long.valueOf(stream.getLastProcessedTime()));
        }
    }
}