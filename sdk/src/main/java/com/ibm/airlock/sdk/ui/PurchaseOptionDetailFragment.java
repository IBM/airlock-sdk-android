package com.ibm.airlock.sdk.ui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.ibm.airlock.common.AirlockNotInitializedException;
import com.ibm.airlock.common.cache.PercentageManager;
import com.ibm.airlock.common.data.Entitlement;
import com.ibm.airlock.common.data.Feature;
import com.ibm.airlock.common.data.PurchaseOption;
import com.ibm.airlock.common.util.Constants;
import com.ibm.airlock.sdk.AirlockManager;
import com.ibm.airlock.sdk.R;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.Collection;

import javax.annotation.CheckForNull;

public class PurchaseOptionDetailFragment extends FeatureDetailsFragment {

    JSONArray storeProductIds;

    protected void setChildrenData(Bundle args, Feature feature) {
        storeProductIds = ((PurchaseOption) feature).getStores();
    }

    protected String getProductId(String name) {
        Feature feature = getFeature(name);
        return ((PurchaseOption) feature).getStores().optJSONObject(0).optString(Constants.JSON_FIELD_STORE_PRODUCT_ID);
    }

    protected Feature getFeature(String name) {
        Collection<Entitlement> entitlements = AirlockManager.getInstance().getEntitlements();
        for (Entitlement entitlement : entitlements) {
            Feature purchaseOption = purchaseOptionLookUp(entitlement, name);
            if (purchaseOption != null) {
                return purchaseOption;
            }
        }
        return null;
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        view.findViewById(R.id.purchase_header_bar).setVisibility(View.VISIBLE);
        view.findViewById(R.id.purchase_bar_control).setVisibility(View.VISIBLE);
        initIsPurchasedSwitch(view);
        return view;
    }


    @Override
    public void onResume() {
        super.onResume();
        Switch purchasedOption = getActivity().findViewById(R.id.purchase_value);
        purchasedOption.setChecked(isPurchased());
    }

    private void initIsPurchasedSwitch(View view) {
        Switch purchasedOption = view.findViewById(R.id.purchase_value);
        purchasedOption.setChecked(isPurchased());
        purchasedOption.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                final boolean isChecked = ((Switch) mainView.findViewById(R.id.purchase_value)).isChecked();
                switch (motionEvent.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getActivity());

                        alertDialogBuilder
                                .setMessage(
                                        "Are you sure you want to " + (((Switch) mainView.findViewById(R.id.purchase_value)).isChecked() ? "disable" : "enable") + " the option purchase simulation?")
                                .setCancelable(true)
                                .setPositiveButton("YES",
                                        new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog,
                                                                int which) {
                                                dialog.cancel();
                                                try {
                                                    if (((Switch) mainView.findViewById(R.id.purchase_value)).isChecked()) {
                                                        AirlockManager.getInstance().addPurchasedProductsId(getProductId(mName));
                                                    } else {
                                                        AirlockManager.getInstance().removePurchasedProductId(getProductId(mName));
                                                    }
                                                    try {
                                                        AirlockManager.getInstance().calculateFeatures(((DebugFeaturesActivity) getActivity()).getDeviceContext(),
                                                                AirlockManager.getInstance().getPurchasedProductIdsForDebug());
                                                        AirlockManager.getInstance().syncFeatures();
                                                        Toast.makeText(getActivity().getApplicationContext(), "Calculate & Sync is done", Toast.LENGTH_SHORT).show();
                                                        updateFragment(getFeature(mName));
                                                        updateGUI();
                                                        //mCallBack.onPercentageChanged();
                                                    } catch (AirlockNotInitializedException | JSONException e) {
                                                        Toast.makeText(getActivity().getApplicationContext(), "Failed to calculate : " + e.toString(), Toast.LENGTH_LONG).show();
                                                        Log.d(this.getClass().getName(), "Airlock calculate & Sync Failed: " + e.getLocalizedMessage());
                                                    }
                                                } catch (Exception e) {
                                                    Log.w(Constants.LIB_LOG_TAG, "Error while simulate purchase process: ", e);
                                                }
                                            }
                                        })

                                .setNegativeButton(
                                        "NO",
                                        new DialogInterface.OnClickListener() {

                                            @Override
                                            public void onClick(DialogInterface dialog,
                                                                int which) {

                                                ((Switch) mainView.findViewById(R.id.purchase_value)).setChecked(isChecked);
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

    private boolean isPurchased() {
        Collection<String> purchasedProductIds = AirlockManager.getInstance().getPurchasedProductIdsForDebug();
        return purchasedProductIds.contains(getProductId(mName));

    }

    @CheckForNull
    private Feature purchaseOptionLookUp(Entitlement entitlement, String name) {
        if (entitlement.getEntitlementChildren().size() > 0) {
            for (Entitlement child : entitlement.getEntitlementChildren()) {
                Feature purchaseOption = purchaseOptionLookUp(child, name);
                if (purchaseOption != null) {
                    return purchaseOption;
                }
            }
        }
        for (PurchaseOption purchaseOption : entitlement.getPurchaseOptions()) {
            if (purchaseOption.getName().equals(name)) {
                return purchaseOption;
            }
        }
        return null;
    }


    protected void initArguments(Feature entitlement) {
        super.initArguments(entitlement);
    }

    protected PercentageManager.Sections getSection() {
        return PercentageManager.Sections.ENTITLEMENTS;
    }

    @Override
    protected void updateGUI() {
        super.updateGUI();
        mainView.findViewById(R.id.children).setVisibility(View.INVISIBLE);
        ((LinearLayout) mainView.findViewById(R.id.purchase_bar)).setVisibility(View.VISIBLE);
        ((TextView) mainView.findViewById(R.id.ids_value)).setText(storeProductIds.toString());
        mainView.findViewById(R.id.is_purchased_bar).setVisibility(View.GONE);
        mainView.findViewById(R.id.is_premium_bar).setVisibility(View.GONE);
    }
}

