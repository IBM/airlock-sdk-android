package com.weather.airlock.sdk.ui;

import android.os.Bundle;
import androidx.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.TextView;

import com.ibm.airlock.common.data.PurchaseOption;
import com.weather.airlock.sdk.R;

import org.json.JSONException;
import org.json.JSONObject;

public class PurchaseOptionsListFragment extends FeatureChildrenListFragment {

    FeatureDetailsFragment detailsFragment;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        TextView nameTxt = (TextView) listView.findViewById(R.id.textViewId1);
        nameTxt.setText("List of purchase options in calculated order");

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                PurchaseOption purchaseOption = null;
                try {
                    purchaseOption = new PurchaseOption(new JSONObject(adapter.getItem(position-1)));
                } catch (JSONException e) {
                    ///
                }
                if (detailsFragment != null) {
                    detailsFragment.updateFragment(purchaseOption);
                } else {

                    detailsFragment = new PurchaseOptionDetailFragment();
                    detailsFragment.initArguments(purchaseOption);
                }
                getFragmentManager().beginTransaction().replace(R.id.features_content_fragment, detailsFragment).addToBackStack(null).commit();
            }

        });


        return view;
    }

    @Override
    protected void setText(TextView convertView, String data){
        JSONObject jsonData = null;
        try {
            jsonData = new JSONObject(data);
        } catch (JSONException e) {
            //do nothing
        }
        if (jsonData != null){
            convertView.setText(jsonData.optString("fullName"));
        }
    }

    @Override
    protected boolean isOn(String data){
        JSONObject jsonData = null;
        try {
            jsonData = new JSONObject(data);
        } catch (JSONException e) {
            //do nothing
        }
        if (jsonData != null){
            return jsonData.optBoolean("isON");
        }
        return false;
    }
}
