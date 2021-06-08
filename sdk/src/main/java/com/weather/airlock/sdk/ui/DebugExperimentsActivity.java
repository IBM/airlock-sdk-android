package com.weather.airlock.sdk.ui;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;
import com.ibm.airlock.common.util.Constants;
import com.weather.airlock.sdk.R;


/**
 * Created by amirle on 04/09/2017.
 */
public class DebugExperimentsActivity extends AppCompatActivity implements ExperimentsListFragment.OnExperimentSelectedListener, PercentageHolder {

    ExperimentsListFragment listFragment;
    ExperimentDetailsFragment expDetailsFragment;
    VariantDetailsFragment varDetailsFragment;

    JSONObject deviceContext;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.airlock_experiments);

        try {
            deviceContext = new JSONObject(getIntent().getExtras().getString(Constants.DEVICE_CONTEXT));
        } catch (JSONException e) {
            Log.d(this.getClass().getName(), "Failed to fetch device context " + e.getLocalizedMessage());
        }

        listFragment = ExperimentsListFragment.newInstance();
        FragmentManager fm = getFragmentManager();
        FragmentTransaction tr = fm.beginTransaction();
        tr.replace(R.id.experiments_content_fragment, listFragment);

        tr.commit();
    }

    @Override
    public void onExperimentSelected(JSONObject experiment) {

        expDetailsFragment = ExperimentDetailsFragment.newInstance(experiment);

        getFragmentManager().beginTransaction().replace(R.id.experiments_content_fragment, expDetailsFragment).addToBackStack(null).commit();
    }

    @Override
    public void onVariantSelected(JSONObject variant) {

        varDetailsFragment = VariantDetailsFragment.newInstance(variant);

        getFragmentManager().beginTransaction().replace(R.id.experiments_content_fragment, varDetailsFragment).addToBackStack(null).commit();
    }

    @Override
    public void onPercentageChanged() {
        listFragment.updateListData();
    }

    public JSONObject getDeviceContext() {
        return deviceContext;
    }
}
