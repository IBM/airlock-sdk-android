package com.weather.airlock.sdk.ui;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import androidx.annotation.Nullable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.ibm.airlock.common.AirlockNotInitializedException;
import com.ibm.airlock.common.cache.PercentageManager;
import com.ibm.airlock.common.data.Feature;
import com.ibm.airlock.common.util.Constants;
import com.weather.airlock.sdk.AirlockManager;
import com.weather.airlock.sdk.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;


/**
 * A simple {@link Fragment} subclass.
 * Activities that contain this fragment must implement the
 * {@link PercentageHolder} interface
 * to handle interaction events.
 */
public class FeatureDetailsFragment extends Fragment {

    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String FEATURE_NAME = "Name";
    protected static final String CHILDREN_NAMES = "Children Names";
    private static final String PATH = "Path";
    private static final String IS_ON = "Is on";
    private static final String SOURCE = "Source";
    protected static final String CHILDREN_NUMBER = "Children number";
    private static final String CONFIGURATION = "Configuration";
    private static final String CONFIGURATION_NAME_LIST = "Configuration names";
    private static final String TRACE = "Trace";
    private static final String IS_PREMIUM = "Is Premium";
    private static final String IS_PURCHASED = "Is Purchased";
    List<String> configurations = new ArrayList<>();
    protected View mainView;
    Switch percentageSwitch;
    NestedListView configurationsListView;
    protected String mName;
    private String mPath;
    private int mChildrenNumber;
    private String mIsOn;
    private String mIsPremium;
    private String mIsPurchased;
    private String mSource;
    private String mConfiguration;
    private String mTrace;
    private ArrayAdapter<String> adapter;
    private String[] mChildrenNames;


    private PercentageHolder mCallBack;

    private boolean cacheCleared = false;

    private PercentageManager percentageManager;

    public FeatureDetailsFragment() {
        percentageManager = AirlockManager.getInstance().getCacheManager().getPercentageManager();
    }

    protected void initArguments(Feature feature) {
        Bundle args = new Bundle();
        args.putString(FEATURE_NAME, String.valueOf(feature.getName()));
        args.putString(PATH, calculateTreePath(feature));
        args.putString(IS_ON, String.valueOf(feature.isOn()));
        args.putString(IS_PREMIUM, String.valueOf(feature.isPremium()));
        args.putString(IS_PURCHASED, String.valueOf(feature.isPurchased()));
        setChildrenData(args, feature);
        args.putString(SOURCE, feature.getSource().name());
        args.putString(CONFIGURATION, feature.getConfiguration() != null ? feature.getConfiguration().toString() : "");
        args.putString(TRACE, feature.getTraceInfo());
        ArrayList<String> l = new ArrayList<>();
        JSONArray arr = feature.getConfigurationStatuses();
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                try {
                    l.add(arr.getJSONObject(i).toString());
                } catch (JSONException e) {
                    Log.w(Constants.LIB_LOG_TAG, "Error while processing Feature's configuration status: ", e);
                }
            }
        }
        args.putStringArrayList(CONFIGURATION_NAME_LIST, l);

        setArguments(args);
    }

    protected void setChildrenData(Bundle args, Feature feature) {
        args.putSerializable(CHILDREN_NAMES, getChildrenNames(feature));
        args.putInt(CHILDREN_NUMBER, feature.getChildren().size());
    }

    private static String[] getChildrenNames(Feature feature) {
        ArrayList<String> childrenNames = new ArrayList();
        List<Feature> children = feature.getChildren();
        for (Feature child : children) {
            childrenNames.add(child.getName());
        }
        return childrenNames.toArray(new String[0]);
    }

    private static String calculateTreePath(Feature feature) {
        Feature parent = feature.getParent();
        if (parent != null && !parent.getName().equals("ROOT")) {
            return calculateTreePath(parent) + "/" + feature.getName();
        } else {
            return feature.getName();
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mName = getArguments().getString(FEATURE_NAME);
            mPath = getArguments().getString(PATH);
            mIsOn = getArguments().getString(IS_ON);
            mIsPremium = getArguments().getString(IS_PREMIUM);
            mIsPurchased = getArguments().getString(IS_PURCHASED);
            mSource = getArguments().getString(SOURCE);
            mChildrenNumber = getArguments().getInt(CHILDREN_NUMBER);
            mConfiguration = getArguments().getString(CONFIGURATION);
            mTrace = getArguments().getString(TRACE);
            mChildrenNames = (String[]) getArguments().getSerializable(CHILDREN_NAMES);
            configurations = getArguments().getStringArrayList(CONFIGURATION_NAME_LIST);
        }
        try {
            percentageManager.forcedReInit();
        } catch (JSONException e) {
            Log.e(Constants.LIB_LOG_TAG, "PercentageManager reInit failed", e);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        mainView = inflater.inflate(R.layout.fragment_feature_details, container, false);
        updateGUI();
        updatePercentageBar();
        setChildrenTitle();
        return mainView;
    }

    protected void setChildrenTitle() {
        //do nothing
    }

    protected Feature getFeature(String name) {
        return AirlockManager.getInstance().getFeature(name);
    }

    private void updatePercentageBar() {
        Double percentage = percentageManager.getPercentage(getSection(), mName);
        if (percentage != null && !cacheCleared && !getFeature(mName).getSource().equals(Feature.Source.DEFAULT)) {
            percentageSwitch = mainView.findViewById(R.id.percentage_value);
            mainView.findViewById(R.id.percentage_bar).setVisibility(View.VISIBLE);
            mainView.findViewById(R.id.percentage_header_bar).setVisibility(View.VISIBLE);
            if (percentage == 100.0 || percentage == 0) {
                percentageSwitch.setEnabled(false);
            }

            percentageSwitch.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View view, MotionEvent motionEvent) {
                    final boolean isChecked = ((Switch) mainView.findViewById(R.id.percentage_value)).isChecked();
                    switch (motionEvent.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());

                            alertDialogBuilder
                                    .setMessage(
                                            "Are you sure you want to turn " + (((Switch) mainView.findViewById(R.id.percentage_value)).isChecked() ? "off" : "on") + " rollout percentage for the feature " + mName + " ?")
                                    .setCancelable(true)
                                    .setPositiveButton("YES",
                                            new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog,
                                                                    int which) {
                                                    dialog.cancel();
                                                    try {
                                                        //percentageSwitch.performClick();
                                                        percentageManager.setDeviceInItemPercentageRange(getSection(), mName, percentageSwitch.isChecked());
                                                        try {
                                                            AirlockManager.getInstance().calculateFeatures(((DebugFeaturesActivity) getActivity()).getDeviceContext(),
                                                                    AirlockManager.getInstance().getPurchasedProductIdsForDebug());
                                                            AirlockManager.getInstance().syncFeatures();
                                                            Toast.makeText(getActivity().getApplicationContext(), "Calculate & Sync is done", Toast.LENGTH_SHORT).show();
                                                            updateFragment(getFeature(mName));
                                                            updateGUI();
                                                            mCallBack.onPercentageChanged();
                                                        } catch (AirlockNotInitializedException | JSONException e) {
                                                            Toast.makeText(getActivity().getApplicationContext(), "Failed to calculate : " + e.toString(), Toast.LENGTH_LONG).show();
                                                            Log.d(this.getClass().getName(), "Airlock calculate & Sync Failed: " + e.getLocalizedMessage());
                                                        }
                                                    } catch (JSONException e) {
                                                        Log.w(Constants.LIB_LOG_TAG, "Error while updating Feature's random number: ", e);
                                                    }
                                                }
                                            })

                                    .setNegativeButton(
                                            "NO",
                                            new DialogInterface.OnClickListener() {

                                                @Override
                                                public void onClick(DialogInterface dialog,
                                                                    int which) {
                                                    ((Switch) mainView.findViewById(R.id.percentage_value)).setChecked(isChecked);
                                                    dialog.cancel();
                                                }
                                            });

                            AlertDialog alertDialog = alertDialogBuilder.create();
                            alertDialog.show();
                            return true;
                        case MotionEvent.ACTION_UP:
                            view.performClick();
                            break;
                        default:
                            break;
                    }
                    return false;
                }
            });
        } else {
            mainView.findViewById(R.id.percentage_bar).setVisibility(View.INVISIBLE);
            mainView.findViewById(R.id.percentage_header_bar).setVisibility(View.INVISIBLE);
            percentageSwitch = null;
        }
    }

    protected void updateGUI() {
        ((TextView) mainView.findViewById(R.id.path_value)).setText(mPath);
        ((TextView) mainView.findViewById(R.id.is_on_value)).setText(mIsOn);
        ((TextView) mainView.findViewById(R.id.is_premium_value)).setText(mIsPremium);
        ((TextView) mainView.findViewById(R.id.is_purchased_value)).setText(mIsPurchased);
        ((TextView) mainView.findViewById(R.id.source_value)).setText(mSource);
        ((TextView) mainView.findViewById(R.id.children_number)).setText(mChildrenNumber + "");
        ((TextView) mainView.findViewById(R.id.trace_value)).setText(mTrace);
        ((TextView) mainView.findViewById(R.id.configuration_value)).setText(mConfiguration);
        ((TextView) mainView.findViewById(R.id.percentage)).setText("Feature Percentage (" + percentageManager.getPercentage(getSection(), mName) + "%)");
    }

    protected PercentageManager.Sections getSection() {
        return PercentageManager.Sections.FEATURES;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        configurationsListView = (NestedListView) view.findViewById(R.id.configuration_list);

        adapter = new ConfigurationsListAdapter(getActivity(), R.layout.feature_configuration_list_item, configurations);
        configurationsListView.setAdapter(adapter);
        configurationsListView.setEnabled(false);

        Drawable myIcon = getResources().getDrawable(R.drawable.ic_keyboard_arrow_right_black);
        myIcon.setColorFilter(Color.parseColor("#4963cd"), PorterDuff.Mode.SRC_ATOP);
        ImageView showFeatureChildrensAction = (ImageView) view.findViewById(R.id.show_children);
        showFeatureChildrensAction.setImageDrawable(myIcon);

        if (mChildrenNumber == 0) {
            showFeatureChildrensAction.setEnabled(false);
        }

        showFeatureChildrensAction.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getFragmentManager().beginTransaction().replace(R.id.features_content_fragment, getChildrenListFragment(mChildrenNames)).addToBackStack(null).commit();
            }
        });
    }

    protected Fragment getChildrenListFragment(String[] mChildrenNames) {
        return FeatureChildrenListFragment.newInstance(mChildrenNames);
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof PercentageHolder) {
            mCallBack = (PercentageHolder) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnPercentageChangedListener");
        }
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mCallBack = null;
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().setTitle(mName);
        if (percentageSwitch != null && !cacheCleared) {
            try {
                percentageSwitch.setChecked(percentageManager.isDeviceInItemPercentageRange(getSection(), mName));
            } catch (JSONException e) {
                percentageSwitch.setEnabled(false);
                Log.w(Constants.LIB_LOG_TAG, "Error while processing Feature's random number: ", e);
            }
        }
    }

    public void setCacheClearedFlag(boolean cacheClearedState) {
        cacheCleared = cacheClearedState;
    }


    public void refreshAfterActivityAction() {
        updateFragment(getFeature(mName));
        updateGUI();
        updatePercentageBar();
        if (percentageSwitch != null && !cacheCleared) {
            try {
                percentageSwitch.setChecked(percentageManager.isDeviceInItemPercentageRange(getSection(), mName));
            } catch (JSONException e) {
                percentageSwitch.setEnabled(false);
                Log.w(Constants.LIB_LOG_TAG, "Error while processing Feature's random number: ", e);
            }
        }
    }

    public void updateFragment(Feature updatedFeature) {
        updateArgs(updatedFeature);
        mName = getArguments().getString(FEATURE_NAME);
        mPath = getArguments().getString(PATH);
        mIsOn = getArguments().getString(IS_ON);
        mSource = getArguments().getString(SOURCE);
        mConfiguration = getArguments().getString(CONFIGURATION);
        mTrace = getArguments().getString(TRACE);
        configurations.clear();
        configurations.addAll(getArguments().getStringArrayList(CONFIGURATION_NAME_LIST));
        mChildrenNames = (String[]) getArguments().getSerializable(CHILDREN_NAMES);
        mChildrenNumber = getArguments().getInt(CHILDREN_NUMBER);

        adapter.notifyDataSetChanged();
    }

    protected void updateArgs(Feature feature) {
        Bundle args = getArguments();
        args.putString(FEATURE_NAME, feature.getName());
        args.putString(PATH, calculateTreePath(feature));
        args.putString(IS_ON, String.valueOf(feature.isOn()));
        args.putString(SOURCE, feature.getSource().name());
        args.putString(CONFIGURATION, feature.getConfiguration() != null ? feature.getConfiguration().toString() : "");
        args.putString(TRACE, feature.getTraceInfo() != null ? feature.getTraceInfo().toString() : "");
        setChildrenData(args, feature);
        ArrayList<String> l = new ArrayList<>();
        JSONArray arr = feature.getConfigurationStatuses();
        if (arr != null) {
            for (int i = 0; i < arr.length(); i++) {
                try {
                    l.add(arr.getJSONObject(i).toString());
                } catch (JSONException e) {
                    Log.w(Constants.LIB_LOG_TAG, "Error while processing Feature's Configuration status: ", e);
                }
            }
        }
        args.putStringArrayList(CONFIGURATION_NAME_LIST, l);
    }

    public static class NestedListView extends ListView implements View.OnTouchListener, AbsListView.OnScrollListener {

        private static final int MAXIMUM_LIST_ITEMS_VIEWABLE = 99;
        private int listViewTouchAction;
        private LayoutParams layoutParams;

        public NestedListView(Context context, AttributeSet attrs) {
            super(context, attrs);
            listViewTouchAction = -1;
            setOnScrollListener(this);
            setOnTouchListener(this);
            this.layoutParams = new LayoutParams(
                    LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
        }

        @Override
        public void onScroll(AbsListView view, int firstVisibleItem,
                             int visibleItemCount, int totalItemCount) {
            if (getAdapter() != null && getAdapter().getCount() > MAXIMUM_LIST_ITEMS_VIEWABLE) {
                if (listViewTouchAction == MotionEvent.ACTION_MOVE) {
                    scrollBy(0, -1);
                }
            }
        }

        @Override
        public void onScrollStateChanged(AbsListView view, int scrollState) {
        }

        @Override
        protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);

            int newHeight = 0;
            final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
            int heightSize = MeasureSpec.getSize(heightMeasureSpec);
            if (heightMode != MeasureSpec.EXACTLY) {
                ListAdapter listAdapter = getAdapter();
                if (listAdapter != null && !listAdapter.isEmpty()) {
                    int listPosition = 0;
                    for (listPosition = 0; listPosition < listAdapter.getCount()
                            && listPosition < MAXIMUM_LIST_ITEMS_VIEWABLE; listPosition++) {
                        View listItem = listAdapter.getView(listPosition, null, this);
                        //now it will not throw a NPE if listItem is a ViewGroup instance
                        if (listItem instanceof ViewGroup) {
                            listItem.setLayoutParams(layoutParams);
                        }
                        listItem.measure(widthMeasureSpec, heightMeasureSpec);
                        newHeight += listItem.getMeasuredHeight();
                    }
                    newHeight += getDividerHeight() * listPosition;
                }
                if ((heightMode == MeasureSpec.AT_MOST) && (newHeight > heightSize)) {
                    if (newHeight > heightSize) {
                        newHeight = heightSize;
                    }
                }
            } else {
                newHeight = getMeasuredHeight();
            }
            setMeasuredDimension(getMeasuredWidth(), newHeight);
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            switch (event.getAction()) {
                case MotionEvent.ACTION_DOWN:
                    if (getAdapter() != null && getAdapter().getCount() > MAXIMUM_LIST_ITEMS_VIEWABLE) {
                        if (listViewTouchAction == MotionEvent.ACTION_MOVE) {
                            scrollBy(0, 1);
                        }
                    }
                    break;
                case MotionEvent.ACTION_UP:
                    v.performClick();
                    break;
                default:
                    break;
            }
            return true;
        }
    }

    /**
     * This interface must be implemented by activities that contain this
     * fragment to allow an interaction in this fragment to be communicated
     * to the activity and potentially other fragments contained in that
     * activity.
     * <p/>
     * See the Android Training lesson <a href=
     * "http://developer.android.com/training/basics/fragments/communicating.html"
     * >Communicating with Other Fragments</a> for more information.
     */

    public class ConfigurationsListAdapter extends ArrayAdapter<String> {
        public ConfigurationsListAdapter(Context context, int resource, List<String> objects) {
            super(context, resource, objects);
        }

        @Override
        public View getView(int position, @Nullable View xView, ViewGroup parent) {
            LayoutInflater inflater = ((Activity) getContext()).getLayoutInflater();
            View rowView = inflater.inflate(R.layout.feature_configuration_list_item, parent, false);

            TextView configNameTxt = (TextView) rowView.findViewById(R.id.config_name);
            TextView configDescription = (TextView) rowView.findViewById(R.id.config_description);
            final Switch configSwitch = (Switch) rowView.findViewById(R.id.config_switch);

            try {
                JSONObject configData = new JSONObject(getItem(position));
                final String configName = configData.optString("name");
                Double configPercentage = percentageManager.getPercentage(getSection(), configName);
                if (configPercentage != null) {
                    final boolean state = percentageManager.isDeviceInItemPercentageRange(getSection(), configName);
                    configNameTxt.setText(configName);
                    configDescription.setText("Rule - " + (configData.optBoolean("isON") ? "ON" : "OFF") + ", Percentage - " + configPercentage + "%");

                    configSwitch.setChecked(state);

                    if (percentageManager.getPercentage(getSection(), configName) == 100.0 || percentageManager.getPercentage(getSection(), configName) == 0) {
                        configSwitch.setEnabled(false);
                    }

                    configSwitch.setOnTouchListener(new View.OnTouchListener() {
                        @Override
                        public boolean onTouch(View view, MotionEvent motionEvent) {
                            switch (motionEvent.getAction()) {
                                case MotionEvent.ACTION_DOWN:
                                    AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());

                                    alertDialogBuilder
                                            .setMessage(
                                                    "Are you sure you want to turn " + (state ? "off" : "on") + " rollout percentage for the configuration " + configName + " ?")
                                            .setCancelable(true)
                                            .setPositiveButton("YES",
                                                    new DialogInterface.OnClickListener() {
                                                        @Override
                                                        public void onClick(DialogInterface dialog,
                                                                            int which) {
                                                            dialog.cancel();

                                                            try {
                                                                configSwitch.performClick();
                                                                percentageManager.setDeviceInItemPercentageRange(getSection(), configName, !state);
                                                                try {
                                                                    AirlockManager.getInstance().calculateFeatures(((DebugFeaturesActivity) getActivity()).getDeviceContext(),
                                                                            AirlockManager.getInstance().getPurchasedProductIdsForDebug());
                                                                    AirlockManager.getInstance().syncFeatures();
                                                                    Toast.makeText(getActivity().getApplicationContext(), "Calculate & Sync is done", Toast.LENGTH_SHORT).show();
                                                                    updateFragment(AirlockManager.getInstance().getFeature(mName));
                                                                    updateGUI();
                                                                } catch (AirlockNotInitializedException | JSONException e) {
                                                                    Toast.makeText(getActivity().getApplicationContext(), "Failed to calculate : " + e.toString(), Toast.LENGTH_LONG).show();
                                                                    Log.d(this.getClass().getName(), "Airlock calculate & Sync Failed: " + e.getLocalizedMessage());
                                                                }
                                                            } catch (JSONException e) {
                                                                Log.w(Constants.LIB_LOG_TAG, "Error while updating Feature's random number: ", e);
                                                            }
                                                        }
                                                    })

                                            .setNegativeButton(
                                                    "NO",
                                                    new DialogInterface.OnClickListener() {

                                                        @Override
                                                        public void onClick(DialogInterface dialog,
                                                                            int which) {

                                                            dialog.cancel();
                                                        }
                                                    });

                                    AlertDialog alertDialog = alertDialogBuilder.create();
                                    alertDialog.show();
                                    return true;
                                case MotionEvent.ACTION_UP:
                                    view.performClick();
                                    break;
                                default:
                                    break;
                            }
                            return false;
                        }
                    });
                } else {
                    rowView.setVisibility(View.INVISIBLE);
                }
            } catch (JSONException e) {
                Log.w(Constants.LIB_LOG_TAG, "Error while processing Configuration data: ", e);
            }
            return rowView;
        }
    }
}
