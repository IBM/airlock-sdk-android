package com.ibm.airlock.sdk.ui;


import android.app.Fragment;
import android.os.Bundle;

import javax.annotation.Nullable;


public class EntitlementDataFragment extends DataFragment {
    final public static String PURCHASE_NAME = "purchase.name";
    protected String purchaseName;

    public static Fragment newInstance(String notificationName) {
        Fragment fragment = new EntitlementDataFragment();
        // arguments
        Bundle arguments = new Bundle();
        arguments.putString(PURCHASE_NAME, notificationName);
        fragment.setArguments(arguments);

        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        //init UI references
        super.onCreate(savedInstanceState);
        this.purchaseName = getArguments().getString(PURCHASE_NAME);
    }
}

