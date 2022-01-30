package com.ibm.airlock.sdk.ui;

import android.os.Bundle;
import android.view.View;

import com.ibm.airlock.common.data.Entitlement;
import com.ibm.airlock.sdk.AirlockManager;

import java.util.Collection;

/**
 * Created by Eitan Schreiber on 29/01/2019.
 */

public class EntitlementsListFragment extends FeaturesListFragment {


    public EntitlementsListFragment() {
        // Required empty public constructor
    }

    protected void setTitle() {
        getActivity().setTitle("Entitlements");
        searchedTxtView.setHint("Search Entitlements");
    }

    protected void addAllFeatures() {
        Collection<Entitlement> rootPurchases = AirlockManager.getInstance().getEntitlements();
        for (Entitlement entitlement : rootPurchases) {
            addEntitlement(entitlement);
        }
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }


    private void addEntitlement(Entitlement entitlement) {
        if (entitlement.getChildren().size() > 0) {
            for (Entitlement entitlementChild : entitlement.getEntitlementChildren()) {
                addEntitlement(entitlementChild);
            }
        }
        if (!originalFeatures.containsKey(entitlement.getName())) {
            this.originalFeatures.put(entitlement.getName(), entitlement);
            this.filteredFeatures.add(entitlement.clone());
        }
    }
}

