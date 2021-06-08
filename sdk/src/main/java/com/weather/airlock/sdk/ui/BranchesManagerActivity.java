package com.weather.airlock.sdk.ui;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

import android.os.Bundle;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatCheckedTextView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.ibm.airlock.common.airlytics.AnalyticsApiInterface;
import com.ibm.airlock.common.cache.PersistenceHandler;
import com.ibm.airlock.common.net.AirlockDAO;
import com.ibm.airlock.common.util.Constants;
import com.weather.airlock.sdk.AirlockManager;
import com.weather.airlock.sdk.R;
import com.weather.airlock.sdk.analytics.AnalyticsDefaultImpl;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;


/**
 * The class contains UI logic which enables a dev user to specify which
 * airlock configuration branch a device will be working with.
 * The branch purpose is to override a master configuration.
 *
 * @author Denis Voloshin
 */
public class BranchesManagerActivity extends AppCompatActivity {

    //list of available branches
    private ListView listView;

    //adapter for rendering
    private ArrayAdapter<String> adapter;

    //current branches list with  the selection choice for this device.
    private Map<String, String> branches;

    //current branch name, by default is 'master'
    private String[] branchNames;


    // holds the name of the selected branch
    @Nullable
    private String selectedDevelopBranch;

    // holds the Id of the selected branch
    @Nullable
    private String selectedDevelopBranchId;

    private AnalyticsDefaultImpl analyticsImpl = AirlockManager.getInstance().getAnalyticsImpl();


    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //set empty list
        setContentView(R.layout.branches_list);

        //init UI references
        findViewsById();

        //init list
        this.branches = new Hashtable<>();

        AirlockDAO.pullBranches(AirlockManager.getInstance().getCacheManager(), new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                final String error = String.format(getResources().getString(R.string.retrieving_branches), call.request().url().toString());
                Log.e(Constants.LIB_LOG_TAG, error);
                showToast(error);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                //read the response to the string
                if (response.body() == null || response.body().toString().isEmpty() || !response.isSuccessful()) {

                    if (response.body() != null) {
                        response.body().close();
                    }

                    final String warning = getResources().getString(R.string.user_branches_is_empty);
                    Log.w(Constants.LIB_LOG_TAG, warning);
                    showToast(warning);
                    return;
                }

                //parse server response,the response has to be in json format
                try {
                    final JSONObject branchesFullResponse = new JSONObject(response.body().string());
                    response.body().close();
                    if (branchesFullResponse.isNull("branches")) {
                        String warning = getResources().getString(R.string.user_branches_is_empty);
                        Log.w(Constants.LIB_LOG_TAG, warning);
                        showToast(warning);
                        return;
                    }
                    final JSONArray branchesArray = branchesFullResponse.getJSONArray("branches");

                    BranchesManagerActivity.this.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            BranchesManagerActivity.this.branches = generateBranchesList(branchesArray);
                            java.util.Set<String> keys = BranchesManagerActivity.this.branches.keySet();
                            BranchesManagerActivity.this.branchNames = (keys.toArray(new String[keys.size()]));
                            Arrays.sort(BranchesManagerActivity.this.branchNames);

                            adapter = new ArrayAdapter<>(BranchesManagerActivity.this,
                                    android.R.layout.simple_list_item_single_choice, BranchesManagerActivity.this.branchNames);
                            listView.setChoiceMode(ListView.CHOICE_MODE_SINGLE);
                            listView.setAdapter(adapter);

                            for (int i = 1; i <= adapter.getCount(); i++) {
                                if (selectedDevelopBranch != null && selectedDevelopBranch.equals((BranchesManagerActivity.this.branchNames[i - 1]))) {
                                    listView.setItemChecked(i, true);
                                } else {
                                    listView.setItemChecked(i, false);
                                }
                            }

                            LayoutInflater inflater = getLayoutInflater();
                            ViewGroup header = (ViewGroup) inflater.inflate(R.layout.branches_list_header, listView, false);
                            listView.addHeaderView(header);

                            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                                @Override
                                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                                    AppCompatCheckedTextView checkedView = (AppCompatCheckedTextView) view;
                                    if (checkedView.isChecked()) {
                                        selectedDevelopBranch = BranchesManagerActivity.this.branchNames[position - 1];
                                        Map<String, Object> userAttributes = new HashMap<>();
                                        userAttributes.put(analyticsImpl.getAnalyticsFeatureName(AnalyticsApiInterface.ConstantsKeys.DEV_USER_ATTRIBUTE), true);
                                        analyticsImpl.setUserAttributes(userAttributes, analyticsImpl.getUserAttributesSchemaVersion());
                                    }
                                }
                            });
                        }
                    });
                } catch (JSONException e) {
                    final String error = getResources().getString(R.string.user_groups_process_failed);
                    Log.e(Constants.LIB_LOG_TAG, error);
                    showToast(error);
                    Log.e(Constants.LIB_LOG_TAG, "");
                    //render only values from the cache empty list
                }
            }
        });
    }

    private void showToast(final String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(BranchesManagerActivity.this.getBaseContext(), msg,
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        final PersistenceHandler ph = AirlockManager.getInstance().getCacheManager().getPersistenceHandler();

        final String previousBranchName = ph.getDevelopBranchName();
        if (selectedDevelopBranch == null || selectedDevelopBranch.equals("")) {
            ph.setDevelopBranch("");
            ph.setDevelopBranchName(selectedDevelopBranch);
            ph.setDevelopBranchId("");
            return;
        }
        final String selectedDevelopBranchId = branches.get(selectedDevelopBranch);
        AirlockDAO.pullBranchById(AirlockManager.getInstance().getCacheManager(), branches.get(selectedDevelopBranch), new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                final String error = String.format(getResources().getString(R.string.retrieving_branch_error), call.request().url().toString());
                Log.e(Constants.LIB_LOG_TAG, error);
                ph.setDevelopBranchName(previousBranchName);
                showToast(error);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                //read the response to the string
                if (response.body() == null || response.body().toString().isEmpty() || !response.isSuccessful()) {

                    if (response.body() != null) {
                        response.body().close();
                    }

                    final String warning = getResources().getString(R.string.branch_is_empty);
                    Log.w(Constants.LIB_LOG_TAG, warning);
                    showToast(warning);
                    ph.setDevelopBranchName(previousBranchName);
                    return;
                }

                //parse server response,the response has to be in json format
                try {
                    final JSONObject branchesFullResponse = new JSONObject(response.body().string());
                    response.body().close();
                    ph.setDevelopBranch(branchesFullResponse.toString());
                    //apply the selected branch to the master configuration
                    if (selectedDevelopBranch == null) {
                        selectedDevelopBranch = "";
                    }
                    ph.setDevelopBranchName(selectedDevelopBranch);
                    ph.setDevelopBranchId(selectedDevelopBranchId);
                } catch (JSONException e) {
                    final String error = getResources().getString(R.string.user_groups_process_failed);
                    Log.e(Constants.LIB_LOG_TAG, error);
                    showToast(error);
                    Log.e(Constants.LIB_LOG_TAG, "");
                    ph.setDevelopBranchName(previousBranchName);
                }
            }
        });
    }

    private Map<String, String> generateBranchesList(JSONArray branches) {
        Map<String, String> branchesMap = new Hashtable<>();
        selectedDevelopBranch = AirlockManager.getInstance().getCacheManager().getPersistenceHandler().getDevelopBranchName();
        int branchesListLength = branches.length();
        for (int i = 0; i < branchesListLength; i++) {
            JSONObject branchJSON = branches.optJSONObject(i);
            if (branchJSON != null && branchJSON.has("name") && branchJSON.has("uniqueId")) {
                String name = branchJSON.optString("name");
                String uniqueId = branchJSON.optString("uniqueId");
                if (name != null && uniqueId != null) {
                    branchesMap.put(name, uniqueId);
                }
            }
        }
        return branchesMap;
    }

    public void clearBranchSelection(View v) {
        for (int i = 1; i <= adapter.getCount(); i++) {
            listView.setItemChecked(i, false);
        }
        selectedDevelopBranch = "";
        Map<String, Object> userAttributes = new HashMap<>();
        userAttributes.put(analyticsImpl.getAnalyticsFeatureName(AnalyticsApiInterface.ConstantsKeys.DEV_USER_ATTRIBUTE), false);
        analyticsImpl.setUserAttributes(userAttributes, analyticsImpl.getUserAttributesSchemaVersion());
    }

    private void findViewsById() {
        listView = (ListView) findViewById(R.id.list);
    }
}
