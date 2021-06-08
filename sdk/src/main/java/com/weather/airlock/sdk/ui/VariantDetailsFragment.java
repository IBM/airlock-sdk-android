package com.weather.airlock.sdk.ui;

import android.app.AlertDialog;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.ibm.airlock.common.AirlockNotInitializedException;
import com.ibm.airlock.common.cache.PercentageManager;
import com.ibm.airlock.common.util.Constants;
import com.weather.airlock.sdk.AirlockManager;
import com.weather.airlock.sdk.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


/**
 * Created by amirle on 06/09/2017.
 */
public class VariantDetailsFragment extends Fragment {

    private static final String EXPERIMENT_NAME = "Experiment";
    private static final String IS_ON = "Is on";
    private static final String TRACE = "Trace";
    private static final String VARIANT_NAME = "Name";
    private static final String BRANCH_NAME = "Branch";
    private PercentageManager percentageManager;
    private static String TAG = VariantDetailsFragment.class.getName();

    Switch percentageSwitch;
    PercentageHolder mCallBack;
    View mainView;
    private String mName;
    private String mExperimentName;
    private String mBranchName;
    private String mIsOn;
    private String mTrace;


    public VariantDetailsFragment() {
        this.percentageManager = AirlockManager.getInstance().getCacheManager().getPercentageManager();

    }

    public static VariantDetailsFragment newInstance(JSONObject variant) {
        VariantDetailsFragment fragment = new VariantDetailsFragment();
        Bundle args = new Bundle();
        try {
            args.putString(VARIANT_NAME, variant.getString(Constants.JSON_FEATURE_FIELD_NAME));
            args.putString(IS_ON, String.valueOf(variant.getBoolean(Constants.JSON_FEATURE_IS_ON)));
            args.putString(TRACE, variant.getString(Constants.JSON_FEATURE_TRACE));
            args.putString(EXPERIMENT_NAME, variant.getString(Constants.JSON_FIELD_EXPERIMENT_NAME));
            args.putString(BRANCH_NAME, variant.getString(Constants.JSON_FIELD_BRANCH_NAME));
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
        }
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mName = getArguments().getString(VARIANT_NAME);
            mIsOn = getArguments().getString(IS_ON);
            mTrace = getArguments().getString(TRACE);
            mExperimentName = getArguments().getString(EXPERIMENT_NAME);
            mBranchName = getArguments().getString(BRANCH_NAME);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

//         Inflate the layout for this fragment
        mainView = inflater.inflate(R.layout.fragment_variant_details, container, false);
        updateGUI();
        updatePercentageBar();
        return mainView;
    }

    private void updateGUI() {
        ((TextView) mainView.findViewById(R.id.variant_is_on_value)).setText(mIsOn);
        ((TextView) mainView.findViewById(R.id.variant_branch_name_value)).setText(mBranchName);
        ((TextView) mainView.findViewById(R.id.variant_experiment_name_value)).setText(mExperimentName);
        ((TextView) mainView.findViewById(R.id.variant_trace_value)).setText(mTrace);
        ((TextView) mainView.findViewById(R.id.variant_percentage)).setText("Variant Percentage (" + percentageManager.getPercentage(PercentageManager.Sections.EXPERIMENTS, mExperimentName + "." + mName) + "%)");
    }

    private void updatePercentageBar() {
        Double percentage = percentageManager.getPercentage(PercentageManager.Sections.EXPERIMENTS, mExperimentName + "." + mName);
        percentageSwitch = (Switch) mainView.findViewById(R.id.variant_percentage_value);
        mainView.findViewById(R.id.variant_percentage_bar).setVisibility(View.VISIBLE);
        mainView.findViewById(R.id.variant_percentage_header_bar).setVisibility(View.VISIBLE);
        if (percentage == 100.0 || percentage == 0) {
            percentageSwitch.setEnabled(false);
        }

        percentageSwitch.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());

                        alertDialogBuilder
                                .setMessage(
                                        "Are you sure you want to turn " + (((Switch) mainView.findViewById(R.id.variant_percentage_value)).isChecked() ? "off" : "on") + " rollout percentage for the Variant " + mName + " ?")
                                .setCancelable(true)
                                .setPositiveButton("YES",
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog,
                                                                int which) {
                                                dialog.cancel();
                                                try {
                                                    percentageSwitch.performClick();
                                                    percentageManager.setDeviceInItemPercentageRange(PercentageManager.Sections.EXPERIMENTS, mExperimentName + "." + mName, percentageSwitch.isChecked());
                                                    try {
                                                        AirlockManager.getInstance().calculateFeatures(((DebugExperimentsActivity) getActivity()).getDeviceContext(),
                                                                AirlockManager.getInstance().getPurchasedProductIdsForDebug());
                                                        AirlockManager.getInstance().syncFeatures();
                                                        Toast.makeText(getActivity().getApplicationContext(), "Calculate & Sync is done", Toast.LENGTH_SHORT).show();
                                                        updateFragment(findVariantByName(mExperimentName, mName));
                                                        updateGUI();
                                                        mCallBack.onPercentageChanged();
                                                    } catch (AirlockNotInitializedException | JSONException e) {
                                                        Toast.makeText(getActivity().getApplicationContext(), "Failed to calculate : " + e.toString(), Toast.LENGTH_LONG).show();
                                                        Log.d(this.getClass().getName(), "Airlock calculate & Sync Failed: " + e.getLocalizedMessage());
                                                    }
                                                } catch (JSONException e) {
                                                    Log.w(Constants.LIB_LOG_TAG, "Error while updating Experiment's random number: ", e);
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
    }

    private JSONObject findVariantByName(String expName, String variantName) {
        try {
            JSONObject root = new JSONObject(AirlockManager.getInstance().getCacheManager().getPersistenceHandler().read(Constants.JSON_FIELD_DEVICE_EXPERIMENTS_LIST, ""));
            JSONArray expArr = root.getJSONArray(Constants.JSON_FIELD_EXPERIMENTS);
            for (int i = 0; i < expArr.length(); i++) {
                JSONObject experiment = expArr.getJSONObject(i);
                if (experiment.get(Constants.JSON_FIELD_NAME).equals(expName)) {
                    JSONArray varArr = experiment.getJSONArray(Constants.JSON_FIELD_VARIANTS);
                    for (int j = 0; j < varArr.length(); j++) {
                        JSONObject variant = varArr.getJSONObject(j);
                        if (variant.get(Constants.JSON_FIELD_NAME).equals(variantName)) {
                            return variant;
                        }
                    }
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
        }
        return null;
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
        if (percentageSwitch != null) {
            try {
                percentageSwitch.setChecked(percentageManager.isDeviceInItemPercentageRange(PercentageManager.Sections.EXPERIMENTS, mExperimentName + "." + mName));
            } catch (JSONException e) {
                percentageSwitch.setEnabled(false);
                Log.w(Constants.LIB_LOG_TAG, "Error while processing Variant's random number: ", e);
            }
        }
    }

    public void updateFragment(JSONObject variant) {
        updateArgs(variant);
        mName = getArguments().getString(VARIANT_NAME);
        mIsOn = getArguments().getString(IS_ON);
        mTrace = getArguments().getString(TRACE);
        mExperimentName = getArguments().getString(EXPERIMENT_NAME);
        mBranchName = getArguments().getString(BRANCH_NAME);
    }

    private void updateArgs(JSONObject variant) {
        Bundle args = getArguments();
        try {
            args.putString(EXPERIMENT_NAME, variant.getString(Constants.JSON_FEATURE_FIELD_NAME));
            args.putString(IS_ON, String.valueOf(variant.getBoolean(Constants.JSON_FEATURE_IS_ON)));
            args.putString(TRACE, variant.getString(Constants.JSON_FEATURE_TRACE));
            args.putString(EXPERIMENT_NAME, variant.getString(Constants.JSON_FIELD_EXPERIMENT_NAME));
            args.putString(BRANCH_NAME, variant.getString(Constants.JSON_FIELD_BRANCH_NAME));
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
        }
    }
}
