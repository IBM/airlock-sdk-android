package com.ibm.airlock.sdk.ui;

/**
 * Created by Denis Voloshin on 04/09/2017.
 */

import java.util.ArrayList;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;
import com.ibm.airlock.common.streams.AirlockStream;
import com.ibm.airlock.sdk.AirlockManager;
import com.ibm.airlock.sdk.R;


public class TraceListFragment extends StreamDataFragment {

    //list of available streams
    private ListView listView;

    //adapter for rendering
    private CustomAction adapter;


    public static Fragment newInstance(String streamName) {
        Fragment fragment = new TraceListFragment();
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
        View view = inflater.inflate(R.layout.trace_list, container, false);
        listView = (ListView) view.findViewById(R.id.list);

        final AirlockStream stream = AirlockManager.getInstance().getStreamsManager().getStreamByName(streamName);
        String[] trace = stream.getTraceRecords();
        ArrayList<String> notNullTrace = new ArrayList<>();
        for (String trace_row : trace) {
            if (trace_row != null) {
                notNullTrace.add(trace_row);
            }
        }
        adapter = new CustomAction(getActivity(), notNullTrace);
        listView.setAdapter(adapter);
        ViewGroup header = (ViewGroup) getActivity().getLayoutInflater().inflate(R.layout.streams_trace_header, listView, false);
        listView.addHeaderView(header);
        return view;
    }


    static class CustomAction extends BaseAdapter {
        private Context context; //context
        private ArrayList<String> items; //data source of the list adapter

        //public constructor
        public CustomAction(Context context, ArrayList<String> items) {
            this.context = context;
            this.items = items;
        }

        @Override
        public int getCount() {
            return items.size(); //returns total of items in the list
        }

        @Override
        public Object getItem(int position) {
            return items.get(position); //returns list item at the specified position
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // inflate the layout for each list row
            if (convertView == null) {
                convertView = LayoutInflater.from(context).
                        inflate(R.layout.stream_trace_row, parent, false);
            }

            // get current item to be displayed
            String currentItem = (String) getItem(position);

            // get the TextView for item name and item description
            TextView message = (TextView)
                    convertView.findViewById(R.id.message);
            TextView timestamp = (TextView)
                    convertView.findViewById(R.id.timestamp);

            //sets the text for item name and item description from the current item object
            if (currentItem != null && currentItem.indexOf(";;") != -1) {
                message.setText(currentItem.split(";;")[0]);
                timestamp.setText(currentItem.split(";;")[1]);
            } else {
                message.setText(currentItem);
            }

            // returns the view for the current row
            return convertView;
        }
    }
}

