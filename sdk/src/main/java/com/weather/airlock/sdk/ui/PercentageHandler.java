package com.weather.airlock.sdk.ui;

import com.ibm.airlock.common.cache.PersistenceHandler;
import com.ibm.airlock.common.data.Feature;
import com.ibm.airlock.common.util.Constants;
import com.weather.airlock.sdk.AirlockManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Random;


/**
 * Created by amirle on 07/09/2017.
 */
public class PercentageHandler {

    static private PercentageHandler instance;

    HashMap<String, Double> percentageMap = new HashMap<>();

    private PercentageHandler() {
        PersistenceHandler ph = AirlockManager.getInstance().getCacheManager().getPersistenceHandler();
        try {
            if (ph.readJSON(Constants.SP_RAW_RULES) != null) {
                generateFeaturesPercentageMap(ph.readJSON(Constants.SP_RAW_RULES).getJSONObject(Constants.JSON_FIELD_ROOT));
                generateExperimentsPercentageMap(new JSONObject(ph.read(Constants.JSON_FIELD_DEVICE_EXPERIMENTS_LIST, "")));
            }
        } catch (final JSONException e) {

        }
    }


    public void reInit() throws JSONException {

        PersistenceHandler ph = AirlockManager.getInstance().getCacheManager().getPersistenceHandler();
        if (ph.readJSON(Constants.SP_RAW_RULES) != null) {
            generateFeaturesPercentageMap(ph.readJSON(Constants.SP_RAW_RULES).getJSONObject(Constants.JSON_FIELD_ROOT));
            generateExperimentsPercentageMap(new JSONObject(ph.read(Constants.JSON_FIELD_DEVICE_EXPERIMENTS_LIST, "")));
        }
    }

    private void generateFeaturesPercentageMap(JSONObject feature) throws JSONException {
        if (feature.optString(Constants.JSON_FEATURE_FIELD_TYPE).equals(Feature.Type.FEATURE.toString())) {
            percentageMap.put(feature.optString(Constants.JSON_FEATURE_FIELD_NAMESPACE) + "." + feature.optString(Constants.JSON_FEATURE_FIELD_NAME), feature.getDouble(Constants.JSON_FEATURE_FIELD_PERCENTAGE));
            JSONArray configurationRules = feature.getJSONArray(Constants.JSON_FEATURE_FIELD_CONFIGURATION_RULES);
            for (int i = 0; i < configurationRules.length(); i++) {
                generateConfigsPercentageMap(configurationRules.getJSONObject(i));
            }
        }
        JSONArray subFeatures = feature.getJSONArray(Constants.JSON_FEATURE_FIELD_FEATURES);
        for (int i = 0; i < subFeatures.length(); i++) {
            generateFeaturesPercentageMap(subFeatures.getJSONObject(i));
        }
    }

    private void generateConfigsPercentageMap(JSONObject obj) throws JSONException {
        if (obj.getString(Constants.JSON_FEATURE_FIELD_TYPE).equals(Feature.Type.CONFIGURATION_RULE.toString())) {
            percentageMap.put(obj.getString(Constants.JSON_FEATURE_FIELD_NAMESPACE) + "." + obj.getString(Constants.JSON_FEATURE_FIELD_NAME), obj.getDouble(Constants.JSON_FEATURE_FIELD_PERCENTAGE));
        } else if (obj.getString(Constants.JSON_FEATURE_FIELD_TYPE).equals(Feature.Type.CONFIG_MUTUAL_EXCLUSION_GROUP.toString())) {
            JSONArray configurationRules = obj.getJSONArray(Constants.JSON_FEATURE_FIELD_CONFIGURATION_RULES);
            for (int i = 0; i < configurationRules.length(); i++) {
                generateConfigsPercentageMap(configurationRules.getJSONObject(i));
            }
        }
    }

    private void generateExperimentsPercentageMap(JSONObject root) throws JSONException {
        JSONArray experimentsArray = root.getJSONArray(Constants.JSON_FIELD_EXPERIMENTS);
        for (int i = 0; i < experimentsArray.length(); i++) {
            JSONObject experiment = experimentsArray.getJSONObject(i);
            percentageMap.put(Constants.JSON_FIELD_EXPERIMENTS + "." + experiment.getString(Constants.JSON_FEATURE_FIELD_NAME), Double.valueOf(experiment.getDouble(Constants.JSON_FEATURE_FIELD_PERCENTAGE)));
            JSONArray variantsArray = experiment.getJSONArray(Constants.JSON_FIELD_VARIANTS);
            for (int j = 0; j < variantsArray.length(); j++) {
                JSONObject variant = variantsArray.getJSONObject(j);
                percentageMap.put(experiment.get(Constants.JSON_FEATURE_FIELD_NAME) + "." + variant.getString(Constants.JSON_FEATURE_FIELD_NAME), Double.valueOf(variant.getDouble(Constants.JSON_FEATURE_FIELD_PERCENTAGE)));
            }
        }
    }

    public HashMap<String, Double> getMap() {
        return percentageMap;
    }

    public void changePercentageState(String name, boolean newState) throws JSONException {
        PersistenceHandler handler = AirlockManager.getInstance().getCacheManager().getPersistenceHandler();
        JSONObject randomMap = handler.getFeaturesRandomMap();
        double percentage = percentageMap.get(name);
        int splitPoint = (int) Math.floor(percentage * 10000);
        if (newState) // select a user random number smaller than the split point
        {
            int rand = new Random().nextInt(splitPoint) + 1;
            randomMap.put(name, String.valueOf(rand));
            handler.setFeaturesRandomMap(randomMap);
        } else// select a user random number bigger than the split point
        {
            int rand = new Random().nextInt(1000000 - splitPoint) + splitPoint + 1;
            randomMap.put(name, String.valueOf(rand));
            handler.setFeaturesRandomMap(randomMap);
        }
    }

    public boolean checkPercentage(String name) throws JSONException {
        int threshold = (int) Math.floor(percentageMap.get(name) * 10000);
        if (threshold == 1000000) {
            return true;
        } else if (threshold == 0) {
            return false;
        }
        int featureRandom = Integer.valueOf(AirlockManager.getInstance().getCacheManager().getPersistenceHandler().getFeaturesRandomMap().getString(name));
        if (threshold > featureRandom) {
            return true;
        }
        return false;
    }
}
