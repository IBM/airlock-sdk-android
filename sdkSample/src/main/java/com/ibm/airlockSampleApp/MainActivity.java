package com.ibm.airlockSampleApp;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.ibm.airlock.common.AirlockCallback;
import com.ibm.airlock.common.AirlockInvalidFileException;
import com.ibm.airlock.common.AirlockNotInitializedException;
import com.ibm.airlock.common.data.Feature;
import com.ibm.airlock.common.data.FeaturesList;
import com.ibm.airlock.common.engine.AirlockEnginePerformanceMetric;
import com.ibm.airlock.common.net.AirlockDAO;
import com.ibm.airlock.common.util.AirlockMessages;
import com.ibm.airlock.common.util.Constants;
import com.ibm.airlock.sdk.AirlockManager;
import com.ibm.airlock.sdk.ui.AirlockSelectServerActivity;
import com.ibm.airlock.sdk.ui.AirlyticsManagerActivity;
import com.ibm.airlock.sdk.ui.BranchesManagerActivity;
import com.ibm.airlock.sdk.ui.DebugExperimentsActivity;
import com.ibm.airlock.sdk.ui.DebugFeaturesActivity;
import com.ibm.airlock.sdk.ui.EntitlementsManagerActivity;
import com.ibm.airlock.sdk.ui.GroupsManagerActivity;
import com.ibm.airlock.sdk.ui.NotificationsManagerActivity;
import com.ibm.airlock.sdk.ui.StreamsManagerActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity {

    private static final int RESULT_SETTINGS = 1;
    private static final String TAG = "MAIN AIRLOCK TEST APP";
    private int defaultFileId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // replace airlock_defaults with the default file of our Airlock project
        // The default file could be downloaded from the Airlock Console (Product administration section)
        defaultFileId = R.raw.airlock_defaults;
        if (readRawTextFile(getBaseContext(), R.raw.airlock_defaults).equals("{}")) {
            Context context = getApplicationContext();
            CharSequence text = "Airlock defaults file is missing, pls follow the documentation to specify it";
            int duration = Toast.LENGTH_SHORT;

            Toast toast = Toast.makeText(context, text, duration);
            toast.show();
            return;
        }

        try {
            AirlockManager.getInstance().initSDK(getApplicationContext(), defaultFileId, "1.0.0");
            AirlockManager.getInstance().getNotificationsManager().setSupported(true);
        } catch (Exception e) {
            Log.e(TAG, "Failed to init exit app ", e);
            finish();
        }
        initButtons();
    }

    @Override
    public void onResume() {
        super.onResume();
        printMap();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.sdk_sample, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.menu_user_groups:
                startActivity(new Intent(this, GroupsManagerActivity.class));
                break;

            case R.id.menu_server_list:
                startActivity(new Intent(this, AirlockSelectServerActivity.class));
                break;

            case R.id.menu_branches:
                startActivity(new Intent(this, BranchesManagerActivity.class));
                break;
            case R.id.menu_airlock_experiments:
                Intent intent = new Intent(this, DebugExperimentsActivity.class);
                intent.putExtra(Constants.DEVICE_CONTEXT, "{}");
                startActivity(intent);
                break;
            case R.id.menu_airlock_features:
                Intent i = new Intent(this, DebugFeaturesActivity.class);
                i.putExtra(Constants.DEVICE_CONTEXT, getDeviceContextAsString());
                i.putExtra(Constants.DEFAULT_FILE_ID, defaultFileId);
                i.putExtra(Constants.PRODUCT_VERSION, "8.1");
                startActivity(i);
                break;
            case R.id.menu_airlock_notifications:
                startActivity(new Intent(this, NotificationsManagerActivity.class));
                break;

            case R.id.menu_init:
                try {
                    AirlockManager.getInstance().initSDK(getApplicationContext(), defaultFileId, "8.1");
                } catch (Exception e) {
                    Toast.makeText(getApplicationContext(), "Failed to sync: " + e.toString(), Toast.LENGTH_LONG).show();
                }
                Toast.makeText(getApplicationContext(), "init is Done", Toast.LENGTH_SHORT).show();
                printMap();
                break;

            case R.id.menu_reset:
                AirlockManager.getInstance().reset(this);
                Toast.makeText(getApplicationContext(), "reset is Done", Toast.LENGTH_SHORT).show();
                printMap();
                break;

            case R.id.streams:
                startActivity(new Intent(this, StreamsManagerActivity.class));
                break;

            case R.id.airlytics:
                startActivity(new Intent(this, AirlyticsManagerActivity.class));
                break;

            case R.id.menu_data_provider:
                AirlockDAO.DataProviderType providerType = AirlockManager.getInstance().getDataProviderType();
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle(R.string.data_provider_type_title)
                        .setSingleChoiceItems(R.array.data_provider_type_array, (providerType.getValue()),
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        AirlockManager.getInstance().setDataProviderType(AirlockDAO.DataProviderType.getType(which));
                                    }
                                });
                builder.setNeutralButton("Done", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });
                builder.create().show();
                break;
            case R.id.menu_show_sp:
                SharedPreferences sp = getSharedPreferences(Constants.SP_NAME, Context.MODE_PRIVATE);
                String spString = sp.getAll().toString();
                final AlertDialog.Builder spBuilder = new AlertDialog.Builder(this);
                spBuilder.setTitle(R.string.shared_preferences_title)
                        .setMessage(spString);
                spBuilder.setNeutralButton("Close", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
                spBuilder.create().show();
                break;
            case R.id.menu_get_last_js_errors:

                final AlertDialog.Builder jsBuilder = new AlertDialog.Builder(this);
                jsBuilder.setTitle(R.string.last_js_errors_title)
                        .setMessage(AirlockManager.getInstance().getCacheManager().getLastJSCalculateErrors().toString());
                jsBuilder.setNeutralButton("Close", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
                jsBuilder.create().show();
                break;
            case R.id.entitlements:
                i = new Intent(this, EntitlementsManagerActivity.class);
                i.putExtra(Constants.DEVICE_CONTEXT, getDeviceContextAsString());
                i.putExtra(Constants.DEFAULT_FILE_ID, defaultFileId);
                i.putExtra(Constants.PRODUCT_VERSION, "8.1");
                startActivity(i);
                break;
        }
        return true;
    }


    private void initButtons() {
        Button pull = (Button) findViewById(R.id.pull_button);
        pull.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
                                    Toast.makeText(getApplicationContext(), "pull is Done", Toast.LENGTH_SHORT).show();
                                    printMap();
                                }
                            });
                        }
                    });
                } catch (AirlockNotInitializedException e) {
                    Toast.makeText(getApplicationContext(), "Failed to pull: " + "AirlockNotInitializedException", Toast.LENGTH_LONG).show();
                    Log.e(TAG, "Failed to pull: ", e);
                }
            }
        });

        Button calculate = (Button) findViewById(R.id.calculate_button);
        calculate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Thread t1 = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            //AirlockManager.getInstance().calculate(null, null);
                            AirlockEnginePerformanceMetric metricReporter = AirlockEnginePerformanceMetric.getAirlockEnginePerformanceMetric();
                            metricReporter.startMeasuring();
                            AirlockManager.getInstance().calculateFeatures(null, new JSONObject(readAndroidFile(R.raw.context_summery)));
                            Log.d("Performance", metricReporter.getReport().toString());
                            metricReporter.stopMeasuring();
                        } catch (AirlockNotInitializedException e) {
                            Toast.makeText(getApplicationContext(), "Failed to calculate: AirlockNotInitializedException", Toast.LENGTH_LONG).show();
                            Log.e(TAG, "Failed to calculate:", e);
                        } catch (JSONException e) {
                            Toast.makeText(getApplicationContext(), "Failed to calculate: JSONException", Toast.LENGTH_LONG).show();
                            Log.e(TAG, "Failed to calculate:", e);
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to calculate:", e);
                        }
                    }
                });

                Thread t2 = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            //AirlockManager.getInstance().calculate(null, null);
                            AirlockEnginePerformanceMetric metricReporter = AirlockEnginePerformanceMetric.getAirlockEnginePerformanceMetric();
                            metricReporter.startMeasuring();
                            AirlockManager.getInstance().calculateFeatures(null, new JSONObject(readAndroidFile(R.raw.context_summery)));
                            Log.d("Performance", metricReporter.getReport().toString());
                            metricReporter.stopMeasuring();
                        } catch (AirlockNotInitializedException e) {
                            Toast.makeText(getApplicationContext(), "Failed to calculate: AirlockNotInitializedException", Toast.LENGTH_LONG).show();
                            Log.e(TAG, "Failed to calculate:", e);
                        } catch (JSONException e) {
                            Toast.makeText(getApplicationContext(), "Failed to calculate: JSONException", Toast.LENGTH_LONG).show();
                            Log.e(TAG, "Failed to calculate:", e);
                        } catch (Exception e) {
                            Log.e(TAG, "Failed to calculate:", e);
                        }
                    }
                });

                t1.start();
                //t2.start();

                Toast.makeText(getApplicationContext(), "calculate is done", Toast.LENGTH_SHORT).show();
                printMap();
            }
        });

        Button sync = (Button) findViewById(R.id.sync_button);
        sync.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    AirlockManager.getInstance().syncFeatures();
                    AirlockManager.getInstance().getFeature("ads.AdsConfig");
                } catch (AirlockNotInitializedException e) {
                    Log.e(TAG, "Failed to sync:", e);
                    Toast.makeText(getApplicationContext(), "Failed to sync: AirlockNotInitializedException", Toast.LENGTH_LONG).show();
                }
                Toast.makeText(getApplicationContext(), "Sync is Done", Toast.LENGTH_SHORT).show();
                printMap();
            }
        });
    }

    private String readAndroidFile(int fileId) throws AirlockInvalidFileException, IOException {
        if (fileId == Constants.INVALID_FILE_ID) {
            throw new AirlockInvalidFileException(AirlockMessages.ERROR_INVALID_FILE_ID);
        }
        InputStream inStream = this.getResources().openRawResource(fileId);
        BufferedReader br = new BufferedReader(new InputStreamReader(inStream));
        StringBuilder sBuilder = new StringBuilder();
        String strLine;
        while ((strLine = br.readLine()) != null) {
            sBuilder.append(strLine);
        }
        inStream.reset();
        br.close();
        return sBuilder.toString();
    }

    private void checkDeviceUserGroup() {
        List<String> a = new ArrayList<>();
        a.add("banana");
        a.add("mango");
        a.add("apple");
        AirlockManager.getInstance().setDeviceUserGroups(a);
        List<String> b = AirlockManager.getInstance().getDeviceUserGroups();

        List<String> aa = new ArrayList<>();
        AirlockManager.getInstance().setDeviceUserGroups(aa);
        List<String> c = AirlockManager.getInstance().getDeviceUserGroups();

        assert (a.toString().equals(b.toString()));
        assert (aa.toString().equals(c.toString()));
    }


    private void mapToJson() {
        FeaturesList fl = AirlockManager.getInstance().getCacheManager().getSyncFeatureList();
        JSONObject result = new JSONObject();
        Feature root = fl.getFeature("ROOT");

        JSONObject toReturn = new JSONObject();
        try {
//            result.put(Constants.JSON_FEATURE_FULL_NAME,root.getName());
            result.put(Constants.JSON_FEATURE_IS_ON, root.isOn());
            result.put(Constants.JSON_FEATURE_CONFIGURATION, root.getConfiguration());
            result.put(Constants.JSON_FEATURE_FIELD_FEATURES, parseChildren(root, fl));

            toReturn.put("root", result);
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
        }
        Log.d(TAG, "mapToJson = " + toReturn.toString());
    }

    private String getDeviceContextAsString() {
        InputStream ins = getResources().openRawResource(R.raw.big_context);
        BufferedReader br = new BufferedReader(new InputStreamReader(ins));
        StringBuilder sBuilder = new StringBuilder();
        String strLine;
        try {
            while ((strLine = br.readLine()) != null) {
                sBuilder.append(strLine);
            }
            ins.reset();
            br.close();
        } catch (IOException e) {
            Log.e(TAG, e.getMessage());
        }
        return sBuilder.toString();
    }

    private JSONArray parseChildren(Feature root, FeaturesList fl) {
        JSONArray result = new JSONArray();
        List<Feature> children = root.getChildren();
        for (Feature f : children) {
            JSONObject childJson = new JSONObject();
            try {
                childJson.put(Constants.JSON_FEATURE_FULL_NAME, f.getName());
                childJson.put(Constants.JSON_FEATURE_IS_ON, f.isOn());
                childJson.put(Constants.JSON_FEATURE_CONFIGURATION, f.getConfiguration());
                childJson.put(Constants.JSON_FEATURE_FIELD_FEATURES, parseChildren(f, fl));
                result.put(childJson);
            } catch (JSONException e) {
                Log.e(TAG, e.getMessage());
            }
        }
        return result;
    }

    private void printMap() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                TextView features = (TextView) findViewById(R.id.featureList);
                features.setTextColor(Color.parseColor("#006400"));
                features.setText("");
                String featureList = AirlockManager.getInstance().getCacheManager().getSyncFeatureList().printableToString();
                features.setText(featureList);
                Log.d(TAG, "Map = " + featureList);
            }
        });
    }

    public static String readRawTextFile(Context ctx, int resId) {
        InputStream inputStream = ctx.getResources().openRawResource(resId);
        InputStreamReader inputReader = new InputStreamReader(inputStream);
        BufferedReader buffReader = new BufferedReader(inputReader);
        String line;
        StringBuilder text = new StringBuilder();
        try {
            while ((line = buffReader.readLine()) != null) {
                text.append(line);
            }
        } catch (IOException e) {
            return null;
        }
        return text.toString();
    }
}
