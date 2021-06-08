package com.weather.airlock.sdk.ui;

import com.ibm.airlock.common.data.Feature;
import com.weather.airlock.sdk.R;


/**
 * The class contains UI logic which enables a dev user to debug entitlements model on the fly
 *
 * @author Eitan
 */
public class EntitlementsManagerActivity extends DebugFeaturesActivity implements FeaturesListFragment.OnFeatureSelectedListener {

    @Override
    protected FeaturesListFragment getFeaturesList() {
        return new EntitlementsListFragment();
    }


    @Override
    public void onFeatureSelected(Feature feature) {
        if (detailsFragment != null) {
            detailsFragment.updateFragment(feature);
        } else {
            detailsFragment = new EntitlementDetailFragment();
            detailsFragment.initArguments(feature);
        }
        getFragmentManager().beginTransaction().replace(R.id.features_content_fragment, detailsFragment).addToBackStack(null).commit();
    }
}
