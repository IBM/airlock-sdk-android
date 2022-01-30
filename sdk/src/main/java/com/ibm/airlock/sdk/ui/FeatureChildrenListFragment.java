package com.ibm.airlock.sdk.ui;

import android.app.Fragment;
import android.graphics.Color;
import android.os.Bundle;
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.ibm.airlock.sdk.AirlockManager;
import com.ibm.airlock.sdk.R;


/**
 * Created by Denis Voloshin on 30/11/2017.
 */

public class FeatureChildrenListFragment extends Fragment {

    public static final String CHILDREN_LIST_FRAGMENT = "CHILDREN_LIST_FRAGMENT";
    public static final String CHILDREN_DATA = "childrenData";
    private String parentName;

    //list of available streams
    protected ListView listView;

    //adapter for rendering
    protected ArrayAdapter<String> adapter;

    // holds an array of children
    private String[] children;


    public FeatureChildrenListFragment() {
        super();
    }

    public static Fragment newInstance(String[] childrenNames) {
        Fragment fragment = new FeatureChildrenListFragment();
        // add arguments
        Bundle arguments = new Bundle();
        arguments.putSerializable(CHILDREN_DATA, childrenNames);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            if (getArguments().getSerializable(CHILDREN_DATA) != null) {
                children = (String[]) getArguments().getSerializable(CHILDREN_DATA);
            } else {
                Toast.makeText(getActivity().getApplicationContext(), "The feature parent name is not specified", Toast.LENGTH_LONG).show();
            }
        }

        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.feature_children_list, container, false);
        listView = (ListView) view.findViewById(R.id.list);

        adapter = new ArrayAdapter<String>(getActivity(),
                android.R.layout.simple_list_item_1, children) {
            @Override
            public View getView(int position, @Nullable View convertView, ViewGroup parent) {
                convertView = new TextView(getContext());
                convertView.setPadding(20, 30, 20, 30);
                ((TextView) convertView).setTextSize(15);
                String child = getItem(position);
                if (child != null) {
                    if (isOn(child)) {
                        ((TextView) convertView).setTextColor(Color.BLUE);
                    } else {
                        ((TextView) convertView).setTextColor(Color.BLACK);
                    }
                    setText((TextView)convertView, child);
                }
                return convertView;
            }
        };
        listView.setAdapter(adapter);
        ViewGroup header = (ViewGroup) inflater.inflate(R.layout.children_list_header, listView, false);
        listView.addHeaderView(header);

        return view;
    }

    protected void setText(TextView convertView, String title){
        convertView.setText(title);
    }

    protected boolean isOn(String child){
        return AirlockManager.getInstance().getFeature(child).isOn();
    }
}
