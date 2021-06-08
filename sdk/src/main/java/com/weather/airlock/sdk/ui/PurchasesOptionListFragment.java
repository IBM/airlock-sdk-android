package com.weather.airlock.sdk.ui;

import com.ibm.airlock.common.data.Entitlement;
import com.weather.airlock.sdk.AirlockManager;

import java.util.Collection;

/**
 * Created by Eitan Schreiber on 29/01/2019.
 */

public class PurchasesOptionListFragment extends FeaturesListFragment {

    public PurchasesOptionListFragment() {
        // Required empty public constructor
    }

    protected void setTitle(){
        getActivity().setTitle("Purchases Options");
        searchedTxtView.setHint("Search Purchases Option");
    }

    protected void addAllFeatures() {
        Collection<Entitlement> rootPurchases = AirlockManager.getInstance().getEntitlements();
        for (Entitlement feature : rootPurchases) {
            if (!originalFeatures.containsKey(feature.getName())) {
                this.originalFeatures.put(feature.getName(), feature);
                this.filteredFeatures.add(feature.clone());
            }
        }
    }
}

