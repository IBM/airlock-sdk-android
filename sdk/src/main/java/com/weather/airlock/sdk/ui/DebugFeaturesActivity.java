package com.weather.airlock.sdk.ui;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.ibm.airlock.common.AirlockCallback;
import com.ibm.airlock.common.AirlockInvalidFileException;
import com.ibm.airlock.common.AirlockNotInitializedException;
import com.ibm.airlock.common.cache.PercentageManager;
import com.ibm.airlock.common.data.Feature;
import com.ibm.airlock.common.util.Constants;
import com.weather.airlock.sdk.AirlockManager;
import com.weather.airlock.sdk.R;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class DebugFeaturesActivity extends AppCompatActivity implements FeaturesListFragment.OnFeatureSelectedListener, PercentageHolder {

    FeatureDetailsFragment detailsFragment;
    FeaturesListFragment listFragment;

    JSONObject deviceContext;
    int defaultFileId;
    String productVersion;
    List<String> purchasedProductIds;
    private static final String TAG = DebugFeaturesActivity.class.getName();


    private PercentageManager percentageManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.airlock_features);
        purchasedProductIds = new ArrayList<>();
        percentageManager = AirlockManager.getInstance().getCacheManager().getPercentageManager();

        try {
            deviceContext = new JSONObject(getIntent().getExtras().getString(Constants.DEVICE_CONTEXT));
            defaultFileId = (getIntent().getExtras().getInt(Constants.DEFAULT_FILE_ID));
            productVersion = (getIntent().getExtras().getString(Constants.PRODUCT_VERSION));

            if (getIntent().getExtras().getString(Constants.PURCHASED_PRODUCT_IDS) != null) {
                JSONArray purchasedProductIdsAsJsonArray = new JSONArray(getIntent().getExtras().getString(Constants.PURCHASED_PRODUCT_IDS));

                if (purchasedProductIds != null) {
                    for (int i = 0; i < purchasedProductIdsAsJsonArray.length(); i++) {
                        purchasedProductIds.add(purchasedProductIdsAsJsonArray.getString(i));
                    }
                }
            }


        } catch (JSONException e) {
            Log.d(this.getClass().getName(), "Failed to fetch device context " + e.getLocalizedMessage());
        }


        listFragment = getFeaturesList();
        FragmentManager fm = getFragmentManager();
        FragmentTransaction tr = fm.beginTransaction();
        tr.replace(R.id.features_content_fragment, listFragment);
        tr.commit();
    }

    protected FeaturesListFragment getFeaturesList() {
        return new FeaturesListFragment();
    }

    @Override
    public void onFeatureSelected(Feature feature) {
        if (detailsFragment != null) {
            detailsFragment.updateFragment(feature);
        } else {
            detailsFragment = new FeatureDetailsFragment();
            detailsFragment.initArguments(feature);
        }
        getFragmentManager().beginTransaction().replace(R.id.features_content_fragment, detailsFragment).addToBackStack(null).commit();
    }

    @Override
    public void onPercentageChanged() {
        listFragment.updateListData();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (R.id.action_pull == item.getItemId()) {
            pullFeatures();
        } else if (R.id.action_calculate == item.getItemId()) {
            calculateFeatures();
        } else if (R.id.action_clear_cache == item.getItemId()) {
            clearCache();
        }
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.airlock_features_menu, menu);
        return true;
    }


    private void pullFeatures() {
        try {
            AirlockManager.getInstance().pullFeatures(new AirlockCallback() {
                @Override
                public void onFailure(@NonNull final Exception e) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "Failed to pull: " + e.toString(), Toast.LENGTH_LONG).show();
                        }
                    });
                }

                @Override
                public void onSuccess(String msg) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(), "Pull is Done", Toast.LENGTH_SHORT).show();
                            listFragment.refreshAfterActivityAction();
                            if (detailsFragment != null) {
                                detailsFragment.setCacheClearedFlag(false);
                                detailsFragment.refreshAfterActivityAction();
                            }
                        }
                    });
                }
            });
        } catch (final AirlockNotInitializedException e) {
            Log.d(this.getClass().getName(), "Airlock pull Failed: " + e.getLocalizedMessage());
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(), "Failed to pull: " + "AirlockNotInitializedException", Toast.LENGTH_LONG).show();
                }
            });
        }
    }

    private void calculateFeatures() {
        try {
            AirlockManager.getInstance().calculateFeatures(deviceContext, purchasedProductIds);
            AirlockManager.getInstance().syncFeatures();
            Toast.makeText(getApplicationContext(), "Calculate & Sync is done", Toast.LENGTH_SHORT).show();
            listFragment.refreshAfterActivityAction();
            if (detailsFragment != null) {
                detailsFragment.refreshAfterActivityAction();
            }
        } catch (AirlockNotInitializedException e) {
            Toast.makeText(getApplicationContext(), "Failed to calculate: AirlockNotInitializedException", Toast.LENGTH_LONG).show();
        } catch (JSONException e) {
            Toast.makeText(getApplicationContext(), "Failed to calculate: JSONException", Toast.LENGTH_LONG).show();
        }
    }

    private void clearCache() {
        AirlockManager.getInstance().reset(getApplicationContext(), false);
        AirlockManager.getInstance().getCacheManager().clearRuntimeData();
        AirlockManager.getInstance().getCacheManager().resetSPOnNewSeasonId();

        try {
            AirlockManager.getInstance().initSDK(getApplicationContext(), defaultFileId, productVersion);
            AirlockManager.getInstance().calculateFeatures(deviceContext, AirlockManager.getInstance().getPurchasedProductIdsForDebug());
            AirlockManager.getInstance().syncFeatures();
        } catch (AirlockNotInitializedException e) {
            Toast.makeText(getApplicationContext(), "Failed to calculate: AirlockNotInitializedException", Toast.LENGTH_LONG).show();
        } catch (JSONException e) {
            Toast.makeText(getApplicationContext(), "Failed to calculate: JSONException", Toast.LENGTH_LONG).show();
        } catch (AirlockInvalidFileException e) {
            Log.d(this.getClass().getName(), "Something went wrong while airlock initialization", e);
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
        if (percentageManager.isEmpty()) {
            try {
                percentageManager.reInit();
            } catch (final JSONException e) {
                Log.w(Constants.LIB_LOG_TAG, getResources().getString(R.string.percentage_map_process_failed), e);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            }
        }

        listFragment.refreshAfterActivityAction();
        if (detailsFragment != null) {
            detailsFragment.setCacheClearedFlag(true);
            detailsFragment.refreshAfterActivityAction();
        }
    }

    public JSONObject getDeviceContext() {
        return deviceContext;
    }
}
