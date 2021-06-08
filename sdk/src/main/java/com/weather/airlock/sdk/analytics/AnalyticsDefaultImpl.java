package com.weather.airlock.sdk.analytics;

import com.ibm.airlock.common.airlytics.AnalyticsApiInterface;

import org.json.JSONObject;
import java.util.Map;
import javax.annotation.Nullable;


public class AnalyticsDefaultImpl implements AnalyticsApiInterface {
    public void setDebugEnable(boolean isChecked){
    }
    public boolean doesAnalyticsEnvironmentExists(String name){
        return false;
    }
    public Map<String, ?> generateLogList(Object context, String environmentName){
        return null;
    }
    public String getSessionDetails(String featureName) {
        return null;
    }
    public void syncAnalytics() {
    }
    public void track(String name, @Nullable Long eventTime, @Nullable String schemaVersion, Map<String, Object> attributes) {
    }
    public void trackStreamResults(Map<String, Object> attributes){
    }
    public void verifyAnalyticsState() {
    }
    public void enableAnalyticsLifecycle(boolean enable){
    }
    public void updateUserId(String uuid){
    }
    public void setUserAttributesSchemaVersion(String version){
    }
    public String getUserAttributesSchemaVersion(){
        return null;
    }
    public void setUserAttributes(Map<String, Object> attributes, @Nullable String schemaVersion) {
    }
    public void sendAnalyticsEventsWhenGoingToBackground() {
    }
    public String getAnalyticsFeatureName(ConstantsKeys feature){
        return "";
    }
    public void sendContextAsUserAttributes(@Nullable JSONObject airlockContext){
    }
    public void addAnalyticsShardToContext(JSONObject airlockContext) {
    }
}
