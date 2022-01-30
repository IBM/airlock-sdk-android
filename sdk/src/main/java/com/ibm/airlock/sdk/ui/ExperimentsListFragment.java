package com.ibm.airlock.sdk.ui;

import android.app.ListFragment;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.ibm.airlock.common.AirlockCallback;
import com.ibm.airlock.common.AirlockNotInitializedException;
import com.ibm.airlock.common.util.Constants;
import com.ibm.airlock.sdk.AirlockManager;
import com.ibm.airlock.sdk.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


/**
 * Created by amirle on 11/07/2017.
 */
public class ExperimentsListFragment extends ListFragment {

    private static final String EXPERIMENTS = "experiments list";
    final String VARIANTS_NAME = "variants";


    JSONArray experimentsList = new JSONArray();

    ExpandableListView experimentsListView;

    private AirlockExperimentsAdapter adapter;

    private OnExperimentSelectedListener mCallback;

    private static final String TAG = ExperimentsListFragment.class.getName();

    public ExperimentsListFragment() {

    }

    public static ExperimentsListFragment newInstance() {
        ExperimentsListFragment fragment = new ExperimentsListFragment();
        Bundle args = new Bundle();
        try {
            JSONObject experimentsJSONObject = (new JSONObject(AirlockManager.getInstance().getCacheManager().getPersistenceHandler().read(Constants
                    .JSON_FIELD_DEVICE_EXPERIMENTS_LIST, "")));
            args.putString(EXPERIMENTS, experimentsJSONObject.toString());
        } catch (JSONException e) {

        }
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        experimentsListView = view.findViewById(R.id.experiment_list);
        adapter = new AirlockExperimentsAdapter(getActivity(), experimentsList);
        experimentsListView.setAdapter(adapter);
    }

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_experiments_list, container, false);
        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            try {
                if (getArguments().getString(EXPERIMENTS) != null) {
                    experimentsList = (new JSONObject(getArguments().getString(EXPERIMENTS)).getJSONArray(Constants.JSON_FIELD_EXPERIMENTS));
                } else {
                    Toast.makeText(getActivity().getApplicationContext(), "The experiments data still not available, the airlock context is not fully "
                            + "initialized. Try to restart the app.", Toast.LENGTH_LONG).show();
                }
            } catch (JSONException e) {
                Log.e(TAG, e.getMessage());
            }
        }
    }


    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        if (context instanceof OnExperimentSelectedListener) {
            mCallback = (OnExperimentSelectedListener) context;
        } else {
            throw new RuntimeException(context.toString()
                    + " must implement OnFeatureSelectedListener");
        }
        try {
            AirlockManager.getInstance().getCacheManager().clearTimeStamps();
            AirlockManager.getInstance().pullFeatures(new AirlockCallback() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    Log.e(this.getClass().getName(), "Failed to pull Airlock features");
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getActivity().getApplicationContext(), "Failed to pull Airlock features ", Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                }

                @Override
                public void onSuccess(@NonNull String msg) {
                    Log.d(this.getClass().getName(), "Pull features is Done");
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast.makeText(getActivity().getApplicationContext(), "Pull features is Done", Toast.LENGTH_SHORT).show();
                                updateListData();
                            }
                        });
                    }
                }
            });
        } catch (AirlockNotInitializedException e) {
            Log.e(this.getClass().getName(), "Airlock pull Failed: " + e.getLocalizedMessage());
            if (getActivity() != null) {
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getActivity().getApplicationContext(), "Failed to pull Airlock features ", Toast.LENGTH_LONG).show();
                    }
                });
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().setTitle("ExperimentsCalculator");
    }


    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        super.onListItemClick(l, v, position, id);
    }

    public void updateListData() {
        if (experimentsList != null && experimentsList.length() > 0) {
            experimentsList = new JSONArray();
        }
        try {
            AirlockManager.getInstance().calculateFeatures(((DebugExperimentsActivity) getActivity()).getDeviceContext(), AirlockManager.getInstance().getPurchasedProductIdsForDebug());
            AirlockManager.getInstance().syncFeatures();
            JSONObject experimentsJSONObject = (new JSONObject(AirlockManager.getInstance().getCacheManager().getPersistenceHandler().read(Constants
                    .JSON_FIELD_DEVICE_EXPERIMENTS_LIST, "{}")));
            if (experimentsJSONObject.has(Constants.JSON_FIELD_EXPERIMENTS)) {
                experimentsList = experimentsJSONObject.getJSONArray(Constants.JSON_FIELD_EXPERIMENTS);
            }
        } catch (AirlockNotInitializedException |
                JSONException e
        ) {
            Toast.makeText(getActivity().getApplicationContext(), "Failed to calculate : " + e.toString(), Toast.LENGTH_LONG).show();
            Log.d(this.getClass().getName(), "Airlock calculate & Sync Failed: " + e.getLocalizedMessage());
        }
    }

    public interface OnExperimentSelectedListener {
        void onExperimentSelected(JSONObject experiment);

        void onVariantSelected(JSONObject variant);
    }


    //*********** Inner Class - AirlockListAdapter *******************//
    public class AirlockExperimentsAdapter extends BaseExpandableListAdapter {

        JSONArray experimentsData;

        public AirlockExperimentsAdapter(Context context, JSONArray data) {
            experimentsData = data;
        }


        @Override
        public int getGroupCount() {
            return experimentsData.length();
        }

        @Override
        public int getChildrenCount(int i) {
            int childrenCounter = 0;

            JSONObject experiment = getGroup(i);
            if (experiment != null) {
                try {
                    JSONArray variantsArray = experiment.getJSONArray(VARIANTS_NAME);
                    if (variantsArray != null && variantsArray.length() > 0) {
                        for (int j = 0; j < variantsArray.length(); j++) {
                            if (getChild(i, j) != null) {
                                childrenCounter++;
                            }
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            return childrenCounter;
        }

        @Override
        public JSONObject getGroup(int i) {
            try {
                return experimentsData.getJSONObject(i);
            } catch (JSONException e) {

            }
            return null;
        }

        @Override
        public JSONObject getChild(int i, int i1) {
            try {
                JSONObject experiment = getGroup(i);
                if (experiment != null) {
                    JSONArray experimentChilds = experiment.getJSONArray(VARIANTS_NAME);
                    if (experimentChilds != null && experimentChilds.length() != 0) {
                        return experimentChilds.getJSONObject(i1);
                    }
                }
            } catch (JSONException e) {

            }
            return null;
        }

        @Override
        public long getGroupId(int i) {
            return i;
        }

        @Override
        public long getChildId(int i, int i1) {
            return i1;
        }

        @Override
        public boolean hasStableIds() {
            return false;
        }

        @Override
        public View getGroupView(int i, boolean b, View view, ViewGroup viewGroup) {
            view = new TextView(getActivity());
            view.setPadding(20, 30, 20, 30);
            ((TextView) view).setTextSize(15);
            final JSONObject experiment = getGroup(i);
            if (experiment != null) {
                try {
                    if (experiment.getBoolean(Constants.JSON_FEATURE_IS_ON)) {
                        ((TextView) view).setTextColor(Color.BLUE);
                    } else {
                        ((TextView) view).setTextColor(Color.BLACK);
                    }
                    ((TextView) view).setTypeface(null, Typeface.BOLD);
                    ((TextView) view).setText(experiment.getString("name"));
                } catch (JSONException e) {
                }
                ((TextView) view).setCompoundDrawablesWithIntrinsicBounds(R.drawable.beaker2, 0, 0, 0);
                ((TextView) view).setGravity(Gravity.CENTER_VERTICAL);
                ((TextView) view).setCompoundDrawablePadding(20);
            }
            ((ExpandableListView) viewGroup).expandGroup(i);

            view.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mCallback.onExperimentSelected(experiment);
                }
            });

            return view;
        }

        @Override
        public View getChildView(int i, int i1, boolean b, View view, ViewGroup parent) {
            LayoutInflater inflater = (getActivity()).getLayoutInflater();
            View rowView = inflater.inflate(R.layout.variant_list_item, parent, false);
            final JSONObject variant = getChild(i, i1);

            TextView nameTxt = (TextView) rowView.findViewById(R.id.variant_item_name);
            try {
                if (variant.getBoolean(Constants.JSON_FEATURE_IS_ON)) {
                    nameTxt.setTextColor(Color.BLUE);
                } else {
                    nameTxt.setTextColor(Color.BLACK);
                }
                TextView branchNameTxt = (TextView) rowView.findViewById(R.id.variant_item_branch);
                nameTxt.setText(variant.getString(Constants.JSON_FEATURE_FIELD_NAME));
                branchNameTxt.setText("Branch: " + variant.getString(Constants.JSON_FIELD_BRANCH_NAME));
            } catch (JSONException e) {
            }
            rowView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mCallback.onVariantSelected(variant);
                }
            });

            return rowView;
        }

        @Override
        public boolean isChildSelectable(int i, int i1) {
            return true;
        }
    }
}
