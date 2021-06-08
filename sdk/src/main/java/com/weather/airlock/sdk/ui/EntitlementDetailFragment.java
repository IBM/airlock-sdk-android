package com.weather.airlock.sdk.ui;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.ibm.airlock.common.cache.PercentageManager;
import com.ibm.airlock.common.data.Entitlement;
import com.ibm.airlock.common.data.Feature;
import com.ibm.airlock.common.data.PurchaseOption;
import com.weather.airlock.sdk.AirlockManager;
import com.weather.airlock.sdk.R;

import java.util.ArrayList;
import java.util.Collection;

public class EntitlementDetailFragment extends FeatureDetailsFragment {

    private static final String PURCHASE_OPTIONS_NAMES = "Purchase Options Names";
    private static final String PURCHASE_OPTIONS = "Purchase Options";
    private static final String PURCHASE_OPTIONS_SIZE = "Purchase Options Size";
    private int mPurchaseOptionsChildrenNumber;
    private String[] mPurchaseOptionsData;

    @Override
    protected void setChildrenTitle() {
        ((TextView) mainView.findViewById(R.id.children_title)).setText("Entitlements");
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mPurchaseOptionsChildrenNumber = getArguments().getInt(PURCHASE_OPTIONS_SIZE);
            mPurchaseOptionsData = (String[]) getArguments().getSerializable(PURCHASE_OPTIONS);
        }
    }

    private static String[] getPurchaseOptionData(Entitlement entitlement) {
        ArrayList<String> childrenData = new ArrayList();
        Collection<PurchaseOption> children = entitlement.getPurchaseOptions();
        for (PurchaseOption child : children) {
            childrenData.add(child.toJsonObject().toString());
        }
        return childrenData.toArray(new String[0]);
    }

    private static String[] getPurchaseOptionNames(Entitlement entitlement) {
        ArrayList<String> childrenData = new ArrayList();
        Collection<PurchaseOption> children = entitlement.getPurchaseOptions();
        for (PurchaseOption child : children) {
            childrenData.add(child.getName());
        }
        return childrenData.toArray(new String[0]);
    }


    protected void initArguments(Feature entitlement) {
        super.initArguments(entitlement);
        getArguments().putSerializable(PURCHASE_OPTIONS_NAMES, getPurchaseOptionNames((Entitlement) entitlement));
        getArguments().putInt(PURCHASE_OPTIONS_SIZE, ((Entitlement) entitlement).getPurchaseOptions().size());
        getArguments().putSerializable(PURCHASE_OPTIONS, getPurchaseOptionData((Entitlement) entitlement));
    }

    protected void updateArgs(Feature entitlement) {
        super.updateArgs(entitlement);
        getArguments().putSerializable(PURCHASE_OPTIONS_NAMES, getPurchaseOptionNames((Entitlement) entitlement));
        getArguments().putInt(PURCHASE_OPTIONS_SIZE, ((Entitlement) entitlement).getPurchaseOptions().size());
        getArguments().putSerializable(PURCHASE_OPTIONS, getPurchaseOptionData((Entitlement) entitlement));
    }


    protected PercentageManager.Sections getSection() {
        return PercentageManager.Sections.ENTITLEMENTS;
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);
        view.findViewById(R.id.purchase_options).setVisibility(View.VISIBLE);

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.addRule(RelativeLayout.BELOW, R.id.purchase_options);
        view.findViewById(R.id.path_bar).setLayoutParams(params);

        ((TextView) view.findViewById(R.id.purchase_options_number)).setText(mPurchaseOptionsChildrenNumber + "");
        return view;
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        view.findViewById(R.id.show_purchase_options).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getFragmentManager().beginTransaction().replace(R.id.features_content_fragment,
                        getPurchaseOptionsListFragment(mPurchaseOptionsData)).addToBackStack(null).commit();
            }
        });
    }

    protected Feature getFeature(String name) {
        return AirlockManager.getInstance().getEntitlement(name);
    }

    protected Fragment getPurchaseOptionsListFragment(String[] mOptions) {
        Fragment fragment = new PurchaseOptionsListFragment();
        // add arguments
        Bundle arguments = new Bundle();
        arguments.putSerializable(FeatureChildrenListFragment.CHILDREN_DATA, mOptions);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    protected Fragment getChildrenListFragment(String[] mOptions) {
        Fragment fragment = new FeatureChildrenListFragment();
        // add arguments
        Bundle arguments = new Bundle();
        arguments.putSerializable(FeatureChildrenListFragment.CHILDREN_DATA, mOptions);
        fragment.setArguments(arguments);
        return fragment;
    }

    @Override
    protected void updateGUI() {
        super.updateGUI();
        mainView.findViewById(R.id.is_purchased_bar).setVisibility(View.GONE);
        mainView.findViewById(R.id.is_premium_bar).setVisibility(View.GONE);
    }
}

