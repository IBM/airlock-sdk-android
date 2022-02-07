package com.ibm.airlock.sdk.analytics;

import com.ibm.airlock.common.AirlockCallback;
import com.ibm.airlock.common.AirlockNotInitializedException;
import com.ibm.airlock.common.analytics.AnalyticsApiInterface;
import com.ibm.airlytics.environments.ALEnvironment;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

public class AnalyticsEmptyImpl implements AnalyticsApiInterface {


    @Override
    public void setDebugEnable(boolean isChecked){
    }

    @Override
    public boolean doesAnalyticsEnvironmentExists(String name){
        return false;
    }

    @Override
    public Map<String, ?> generateLogList(Object context, String environmentName){
        return new HashMap<>();
    }

    @Override
    public String getSessionDetails(String featureName) {
        return "";
    }

    @Override
    public void syncAnalytics() {
    }

    @Override
    public void addAnalyticsShardToContext(@Nullable JSONObject airlockContext) throws AirlockNotInitializedException, JSONException {
    }

    @Override
    public void track(String name, @Nullable Long eventTime, @Nullable String schemaVersion, Map<String, Object> attributes) {
    }

    @Override
    public void trackStreamResults(Map<String, Object> attributes){
    }

    @Override
    public void verifyAnalyticsState() {
    }

    @Override
    public void updateUserId(String uuid){
    }

    @Override
    public void setUserAttributesSchemaVersion(String version){
    }

    @Override
    public void setAirlyticsInitializationCallback(AirlockCallback callback){
    }

    @Override
    public String getUserAttributesSchemaVersion(){
        return "";
    }

    @Override
    public void setUserAttributes(Map<String, Object> attributes, @Nullable String schemaVersion) {
    }

    @Override
    public void sendAnalyticsEventsWhenGoingToBackground() {
    }

    @Override
    public String getAnalyticsFeatureName(ConstantsKeys feature){
        return "";
    }

    @Override
    public void sendContextAsUserAttributes(@Nullable JSONObject airlockContext){
    }

    @SuppressWarnings("unused")
    public void addAirlyticsShardToContext(@Nullable JSONObject airlockContext) throws AirlockNotInitializedException, JSONException {
    }

    private void setUserAttributes(Map<String, Object> attributes) {
    }

    @CheckForNull
    private byte[] getDetailsAsByteArray(JSONArray sessionDetailsFields, ALEnvironment currentEnvironment) {
        return null;

    }
}
