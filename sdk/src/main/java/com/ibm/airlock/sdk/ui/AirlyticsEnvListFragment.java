package com.ibm.airlock.sdk.ui;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;

import com.ibm.airlock.common.analytics.AnalyticsApiInterface;
import com.ibm.airlock.common.cache.PersistenceHandler;
import com.ibm.airlock.common.data.Feature;
import com.ibm.airlock.sdk.AirlockManager;
import com.ibm.airlock.sdk.analytics.AnalyticsDefaultImpl;
import com.ibm.airlock.sdk.R;

import org.json.JSONObject;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.annotation.CheckForNull;

import static com.ibm.airlock.common.util.Constants.SP_AIRLYTICS_EVENT_DEBUG;

/**
 * Created by Eitan Schreiber on 21/01/2020.
 */
public class AirlyticsEnvListFragment extends Fragment {


    //list of available environments
    private ListView listView;

    //adapter for rendering
    private ArrayAdapter<String> adapter;

    private AnalyticsDefaultImpl airlyticsImpl = new AnalyticsDefaultImpl();

    //current branches list with  the selection choice for this device.
    private Map<String, String> environments;

    //current branch name, by default is 'master'
    private String[] environmentNames;

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        final View view = inflater.inflate(R.layout.airlytics_list, container, false);
        listView = (ListView) view.findViewById(R.id.list);

        //init list
        this.environments = new Hashtable<>();

        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {

                environments = generateEnvironmentsList();
                if (environments == null) {
                    showToast("No available environments");
                    return;
                }
                java.util.Set<String> keys = environments.keySet();
                environmentNames = (keys.toArray(new String[0]));

                Arrays.sort(environmentNames);

                adapter = new ArrayAdapter<String>(getActivity(),
                        android.R.layout.simple_list_item_1, environmentNames) {
                    @Override
                    public View getView(int position, @Nullable View convertView, ViewGroup parent) {
                        if (convertView == null){
                            convertView = new TextView(getContext());
                            convertView.setPadding(20, 30, 20, 30);
                            ((TextView) convertView).setTextSize(15);
                        }
                        String envName = getItem(position);
                        if (airlyticsImpl.doesAnalyticsEnvironmentExists(envName)) {
                            ((TextView) convertView).setTextColor(Color.BLUE);
                        } else {
                            ((TextView) convertView).setTextColor(Color.BLACK);
                        }
                        ((TextView) convertView).setText(envName);
                        return convertView;
                    }
                };
                listView.setAdapter(adapter);

                LayoutInflater inflater = getActivity().getLayoutInflater();
                ViewGroup header = (ViewGroup) inflater.inflate(R.layout.airlytics_list_header, listView, false);
                ViewGroup footer = (ViewGroup) inflater.inflate(R.layout.airlytics_list_footer, listView, false);

                Switch toastDebugSwitch = (Switch) footer.findViewById(R.id.toastDebugSwitch);

                final PersistenceHandler sp = AirlockManager.getInstance().getDebuggableCache().getPersistenceHandler();
                toastDebugSwitch.setChecked(sp.readBoolean(SP_AIRLYTICS_EVENT_DEBUG, false));
                toastDebugSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                        sp.write(SP_AIRLYTICS_EVENT_DEBUG, isChecked);
                        airlyticsImpl.setDebugEnable(isChecked);
                    }
                });

                listView.addHeaderView(header);
                listView.addFooterView(footer);

                listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                        if (position > 0) {
                            if (!airlyticsImpl.doesAnalyticsEnvironmentExists(environmentNames[position - 1])) {
                                showToast(getResources().getString(R.string.environment_not_available, environmentNames[position - 1]));
                                return;
                            }

                            FragmentManager manager = getFragmentManager();
                            FragmentTransaction transaction = manager.beginTransaction();
                            transaction.add(R.id.container, AirlyticsLogListFragment.newInstance(environmentNames[position - 1]), "");
                            transaction.addToBackStack(null);
                            transaction.commit();
                        }
                    }
                });
            }
        });
        return view;
    }


    private void showToast(final String msg) {
        Log.d(this.getClass().getName(), msg);
        if (getActivity() != null) {
            getActivity().runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getActivity().getBaseContext(), msg,
                            Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    @CheckForNull
    private Map<String, String> generateEnvironmentsList() {

        AirlockManager manager = AirlockManager.getInstance();
        Feature airlyticsFeature = manager.getFeature(airlyticsImpl.getAnalyticsFeatureName(AnalyticsApiInterface.ConstantsKeys.ANALYTICS_MAIN_FEATURE));
        if (!airlyticsFeature.isOn()) {
            return null;
        }

        //read and set environments
        Feature environmentFeature =
                manager.getFeature(airlyticsImpl.getAnalyticsFeatureName(AnalyticsApiInterface.ConstantsKeys.ENVIRONMENTS_FEATURE));
        List<Feature> environments = environmentFeature.getChildren();
        if (environments.isEmpty()) {
            return null;
        }

        Map<String, String> environmentsMap = new Hashtable<>();
        for (Feature environmentFeatureItem : environments) {
            JSONObject config = environmentFeatureItem.getConfiguration();
            if (config != null) {
                environmentsMap.put(config.optString("name"), config.optString("name"));
            }
        }
        return environmentsMap;
    }
}

