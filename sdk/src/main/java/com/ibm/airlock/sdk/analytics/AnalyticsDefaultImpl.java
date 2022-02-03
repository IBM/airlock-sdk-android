package com.ibm.airlock.sdk.analytics;

import android.content.Context;

import com.ibm.airlock.common.AirlockCallback;
import com.ibm.airlock.common.AirlockNotInitializedException;
import com.ibm.airlock.common.analytics.AnalyticsApiInterface;
import com.ibm.airlock.common.cache.PersistenceHandler;
import com.ibm.airlock.common.data.Feature;
import com.ibm.airlock.common.log.Logger;
import com.ibm.airlock.common.util.AirlockMessages;
import com.ibm.airlock.common.util.Base64;
import com.ibm.airlock.common.util.Constants;
import com.ibm.airlock.sdk.AirlockManager;
import com.ibm.airlock.sdk.log.AndroidLog;
import com.ibm.airlock.sdk.util.AesGcmEncryptionUtil;
import com.ibm.airlock.sdk.util.AndroidBase64;
import com.sangupta.murmur.Murmur2;
import com.ibm.airlytics.AL;
import com.ibm.airlytics.environments.ALEnvironment;
import com.ibm.airlytics.environments.ALEnvironmentConfig;
import com.ibm.airlytics.events.ALEvent;
import com.ibm.airlytics.events.ALEventConfig;
import com.ibm.airlytics.providers.data.ALProviderConfig;
import com.ibm.airlytics.userattributes.ALUserAttribute;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;

import static android.content.ContentValues.TAG;
import static com.ibm.airlock.common.util.Constants.SP_AIRLYTICS_EVENT_DEBUG;

public class AnalyticsDefaultImpl implements AnalyticsApiInterface {
    public static final String JSON_AIRLYTICS = "airlytics";
    private static final String APP_LAUNCH_EVENT = "app-launch";
    private static final String NOTIFICATION_INTERACTED_EVENT = "notification-interacted";
    private static final String FIRST_TIME_LAUNCH_EVENT = "notification-interacted";
    private static final String DEBUG_BANNERS_PROVIDER_NAME = "DEBUG_BANNERS";
    private static final String PROVIDERS = "analytics.Providers";
    private static final String EVENTS = "analytics.Events";
    private static final String ATTRIBUTE_GROUPS = "analytics.User Attributes Grouping";
    private static final String REST_EVENT_PROXY_NAME = "REST_EVENT_PROXY";
    private static final String REST_EVENT_PROXY_HANDLER = "com.ibm.airlytics.providers.RestEventProxyProvider";
    private static final String EVENT_LOG_PROVIDER_NAME = "EVENT_LOG";
    private static final String EVENT_LOG_PROVIDER_HANDLER = "com.ibm.airlytics.providers.EventLogProvider";
    private static final String DEBUG_BANNERS_PROVIDER_HANDLER = "com.ibm.airlytics.providers.DebugBannersProvider";
    private static final String JSON_ENABLE_CLIENTSIDE_VALIDATION = "enableClientSideValidation";
    private static final String JSON_SESSION_EXPIRATION_IN_SECONDS = "sessionExpirationInSeconds";
    private static final String AIRLYTICS_STREAM_RESULT_EVENT_NAME = "stream-results";
    private static final String AIRLYTICS_STREAM_RESULT_SCHEMA_VERSION = "2.0";
    private static final String USER_ATTRIBUTES = "airlytics.User Attributes";
    private static final String DEV_TAG = "DEV";
    private static final String PROD_TAG = "PROD";
    private static final String JSON_AIRLYTICS_SHARD = "shard";

    private final Map<ConstantsKeys, String> featureNamesMap;
    private String userAttributesSchemaVersion = "17.0";
    private AirlockCallback initializationCallback = null;
    private final Map<String, ALEnvironment> airlyticsEnvironmentsMap = new ConcurrentHashMap<>();
    private static final AtomicBoolean airlyticsEnabled;
    private static final AtomicBoolean airlyticsInitialized;
    private static final Map<String, Map<String, Object>> airlyticsPendingEvents = new ConcurrentHashMap<>();
    private static Set<String> userTags;

    static {
        Base64.init(new AndroidBase64());
        Logger.setLogger(new AndroidLog());
        airlyticsInitialized = new AtomicBoolean(false);
        airlyticsEnabled = new AtomicBoolean(false);
        userTags = Collections.emptySet();
    }
    public AnalyticsDefaultImpl(){
        featureNamesMap = new HashMap<>();
        featureNamesMap.put(ConstantsKeys.ANALYTICS_MAIN_FEATURE, "app.Airlytics");
        featureNamesMap.put(ConstantsKeys.ENVIRONMENTS_FEATURE, "analytics.Environments");
        featureNamesMap.put(ConstantsKeys.DEV_USER_ATTRIBUTE, "devUser");
        featureNamesMap.put(ConstantsKeys.EXPERIMENT_ATTRIBUTE, "experiment");
        featureNamesMap.put(ConstantsKeys.VARIANT_ATTRIBUTE, "variant");
    }

    @Override
    public void setDebugEnable(boolean isChecked){
        AL.Companion.setDebugEnable(isChecked, DEBUG_BANNERS_PROVIDER_NAME);
    }

    @Override
    public boolean doesAnalyticsEnvironmentExists(String name){
        if (name == null) {
            return false;
        }
        return AL.Companion.getEnvironment(name) != null;
    }

    @Override
    public Map<String, ?> generateLogList(Object context, String environmentName){
        return AL.Companion.getEnvironmentLogEvents((Context) context, environmentName);
    }

    @Override
    public String getSessionDetails(String featureName) {
        JSONObject config = getSessionDetailsFeatureConfig(featureName);
        if (config == null) {
            return null;
        }
        String key = config.optString(Constants.ADS_KEY);
        JSONArray sessionDetailsFields = config.optJSONArray(Constants.ADS_SESSION_DETAILS_ARRAY);
        if (sessionDetailsFields == null) {
            return null;
        }
        ALEnvironment currentEnvironment = null;

        for (ALEnvironment environment : airlyticsEnvironmentsMap.values()) {
            if (!Collections.disjoint(environment.getConfig().getTags(), userTags)) {
                currentEnvironment = environment;
                break;
            }
        }
        if (currentEnvironment == null) {
            return null;
        }

        byte[] detailBytes = getDetailsAsByteArray(sessionDetailsFields, currentEnvironment);

        if (detailBytes == null) {
            return null;
        }

        byte[] cipherText = AesGcmEncryptionUtil.INSTANCE.encrypt(key.getBytes(StandardCharsets.UTF_8), detailBytes, false);

        return com.google.common.io.BaseEncoding.base32().lowerCase().encode(cipherText).replaceAll("=","-");
    }

    @Override
    public void syncAnalytics() {
        ALProviderConfig debugProviderConfig = null;

        Feature airlyticsFeature =
                getFeature(featureNamesMap.get(ConstantsKeys.ANALYTICS_MAIN_FEATURE));
        if (!airlyticsFeature.isOn()) {
            airlyticsEnabled.set(false);
            return;
        }
        airlyticsEnabled.set(true);

        //read and set providers
        Feature providersFeature =
                getFeature(PROVIDERS);
        List<Feature> providers = providersFeature.getChildren();
        List<ALProviderConfig> providerConfigs = new ArrayList<>();
        for (Feature providerFeature : providers) {
            JSONObject config = providerFeature.getConfiguration();
            if (config != null) {
                ALProviderConfig providerConfig = new ALProviderConfig(config);
                if (providerConfig.getType().equals(DEBUG_BANNERS_PROVIDER_NAME)) {
                    debugProviderConfig = providerConfig;
                }
                providerConfigs.add(providerConfig);
            }
        }

        //read and set events
        Feature eventsFeature =
                getFeature(EVENTS);
        List<Feature> events = eventsFeature.getChildren();
        ArrayList<ALEventConfig> eventConfigs = new ArrayList<>();
        for (Feature eventFeature : events) {
            JSONObject config = eventFeature.getConfiguration();
            if (config != null) {
                eventConfigs.add(new ALEventConfig(config));
            }
        }

        //read and set events
        Feature userAttributesFeature =
                getFeature(USER_ATTRIBUTES);
        ArrayList<ALUserAttribute> userAttributeConfigs = null;
        if (userAttributesFeature.isOn()){
            List<Feature> userAttributes = userAttributesFeature.getChildren();
            userAttributeConfigs = new ArrayList<>();
            for (Feature userAttribute : userAttributes) {
                JSONObject config = userAttribute.getConfiguration();
                if (config != null) {
                    userAttributeConfigs.add(new ALUserAttribute(config));
                }
            }
        }

        //read and set environments
        Feature environmentFeature =
                getFeature(featureNamesMap.get(ConstantsKeys.ENVIRONMENTS_FEATURE));
        List<Feature> environments = environmentFeature.getChildren();
        if (environments.isEmpty()) {
            airlyticsEnabled.set(false);
        }

        String userId;
        try {
            userId = AirlockManager.getInstance().getAirlockUserUniqueId();
        } catch (AirlockNotInitializedException e) {
            userId = UUID.randomUUID().toString();
            //do nothing - this is called after airlock is initialized already....
        }

        boolean airlyticsJustInitialized = false;
        userTags = getUserTags(null);

        Feature attributeGroupsFeature =
                getFeature(ATTRIBUTE_GROUPS);
        JSONObject attributeGroupsConfig = attributeGroupsFeature.getConfiguration();
        JSONArray groupsArray = null;
        if (attributeGroupsConfig != null) {
            groupsArray = attributeGroupsConfig.optJSONArray("userAttributesGrouping");
        }

        for (Feature environmentFeatureItem : environments) {
            JSONObject config = environmentFeatureItem.getConfiguration();
            if (config == null) {
                continue;
            }

            ALEnvironmentConfig environmentConfig = new ALEnvironmentConfig(config);
            String environmentName = environmentConfig.getName();
            ALEnvironment environment;

            //sync airlyrics state : providers and environments
            synchronized (airlyticsInitialized) {
                environment = airlyticsEnvironmentsMap.get(environmentName);
                //If this environment requires some user TAG and it does not exist - ignore
                if (Collections.disjoint(environmentConfig.getTags(), userTags)) {
                    //if environment was active and now stopped
                    if (environment != null) {
                        environment.disableEnvironment();
                    }
                    continue;
                }
                if (environment == null) {
                    environmentConfig.setDebugUser(AirlockManager.isDevUser.get());
                    if (!airlyticsInitialized.get()) {
                        airlyticsJustInitialized = true;
                        Map<String, String> providersMap = new HashMap<>();
                        providersMap.put(REST_EVENT_PROXY_NAME, REST_EVENT_PROXY_HANDLER);
                        providersMap.put(EVENT_LOG_PROVIDER_NAME, EVENT_LOG_PROVIDER_HANDLER);
                        providersMap.put(DEBUG_BANNERS_PROVIDER_NAME, DEBUG_BANNERS_PROVIDER_HANDLER);
                        AL.Companion.registerProviderHandlers(providersMap);
                        if (groupsArray != null) {
                            AL.Companion.setUserAttributeGroups(groupsArray);
                        }
                        airlyticsInitialized.set(true);
                    }
                    environment = AL.Companion.createEnvironment(environmentConfig, providerConfigs, eventConfigs, userAttributeConfigs, userId, UUID.fromString(AirlockManager.getInstance().getProductId()), AirlockManager.getInstance().getCacheManager().getProductVersion(), AirlockManager.getInstance().getApplicationContext());
                    airlyticsEnvironmentsMap.put(environmentName, environment);
                } else {
                    boolean enableClientSideValidation = config.optBoolean(JSON_ENABLE_CLIENTSIDE_VALIDATION);
                    int sessionExpirationInSeconds = config.optInt(JSON_SESSION_EXPIRATION_IN_SECONDS, 5);
                    environment.update(enableClientSideValidation, sessionExpirationInSeconds, providerConfigs, eventConfigs);
                }
            }

            Map<String, Object> userAttrs = getCalculatedUserAttributes();
            if (!airlyticsEnvironmentsMap.isEmpty()) {
                environment.setUserAttributes(userAttrs, userAttributesSchemaVersion);
            }
        }

        if (airlyticsEnvironmentsMap.isEmpty()) {
            airlyticsEnabled.set(false);
            return;
        }

        if (airlyticsJustInitialized) {
            PersistenceHandler persistenceHandler = AirlockManager.getInstance().getCacheManager().getPersistenceHandler();
            boolean debugEnabled = persistenceHandler.readBoolean(SP_AIRLYTICS_EVENT_DEBUG, false);
            if (!debugEnabled && debugProviderConfig != null){
                debugEnabled = debugProviderConfig.getAcceptAllEvents();
            }
            AL.Companion.setDebugEnable(debugEnabled, DEBUG_BANNERS_PROVIDER_NAME);
            for (String eventName : airlyticsPendingEvents.keySet()) {
                track(eventName, System.currentTimeMillis(), AIRLYTICS_STREAM_RESULT_SCHEMA_VERSION,  airlyticsPendingEvents.get(eventName));
            }
            airlyticsPendingEvents.clear();
            if (initializationCallback != null){
                initializationCallback.onSuccess("done");
            }
        }
    }

    @Override
    public void addAnalyticsShardToContext(@Nullable JSONObject airlockContext) throws AirlockNotInitializedException, JSONException {
        if (airlockContext != null) {
            final int NUMBER_OF_SHARDS = 1000;
            byte[] value = AirlockManager.getInstance().getAirlockUserUniqueId().getBytes();
            long no = Murmur2.hash(value, value.length, 894157739);
            airlockContext.put(JSON_AIRLYTICS_SHARD, ((Long) (no % NUMBER_OF_SHARDS)).intValue());
        }
    }

    @Override
    public void track(String name, @Nullable Long eventTime, @Nullable String schemaVersion, Map<String, Object> attributes) {
        if (!airlyticsEnabled.get()) {
            if (name.equals(APP_LAUNCH_EVENT) || name.equals(FIRST_TIME_LAUNCH_EVENT) || name.equals(NOTIFICATION_INTERACTED_EVENT)) {
                persistAirlyticsAppStart(name, attributes);
            }
            return;
        }
        if (eventTime == null) {
            eventTime = System.currentTimeMillis();
        }
        for (ALEnvironment environment : airlyticsEnvironmentsMap.values()) {
            if (!Collections.disjoint(environment.getConfig().getTags(), userTags)) {
                ALEvent event = new ALEvent(name, attributes, eventTime, environment, schemaVersion);
                environment.track(event);
            }
        }
    }

    @Override
    public void trackStreamResults(Map<String, Object> attributes){
        track(AIRLYTICS_STREAM_RESULT_EVENT_NAME, System.currentTimeMillis(), AIRLYTICS_STREAM_RESULT_SCHEMA_VERSION, attributes);
    }

    @Override
    public void verifyAnalyticsState() {
        AL.Companion.verifyLifecycleStarted();
    }

    @Override
    public void updateUserId(String uuid){
        AL.Companion.updateUserId(uuid);
    }

    @Override
    public void setUserAttributesSchemaVersion(String version){
        this.userAttributesSchemaVersion = version;
    }

    @Override
    public void setAirlyticsInitializationCallback(AirlockCallback callback){
        this.initializationCallback = callback;
    }

    @Override
    public String getUserAttributesSchemaVersion(){
        return userAttributesSchemaVersion;
    }

    //verify Eitan
    @Override
    public void setUserAttributes(Map<String, Object> attributes, @Nullable String schemaVersion) {
        if (!airlyticsEnabled.get()) {
            return;
        }
        boolean switchedToDev = false;

        if (!AirlockManager.isDevUser.get()) {
            Boolean devUser = (Boolean) attributes.get(getAnalyticsFeatureName(ConstantsKeys.DEV_USER_ATTRIBUTE));
            if (devUser != null && devUser) {
                AirlockManager.isDevUser.set(true);
                switchedToDev = true;
            }
        }

        if (userTags.isEmpty()) {
            userTags = getUserTags(attributes);
        }

        for (ALEnvironment environment : airlyticsEnvironmentsMap.values()) {
            if (!Collections.disjoint(environment.getConfig().getTags(), userTags)) {
                environment.setUserAttributes(attributes, schemaVersion);
            }
        }

        if (switchedToDev) {
            syncAnalytics();
        }
    }

    @Override
    public void sendAnalyticsEventsWhenGoingToBackground() {
        for (ALEnvironment environment : airlyticsEnvironmentsMap.values()) {
            environment.sendEventsWhenGoingToBackground();
        }
    }

    @Override
    public String getAnalyticsFeatureName(ConstantsKeys feature){
        String name = "";
        if (this.featureNamesMap != null && this.featureNamesMap.containsKey(feature)) {
            name = this.featureNamesMap.get(feature);
        }
        return name;
    }

    @Override
    public void sendContextAsUserAttributes(@Nullable JSONObject airlockContext){
        if (airlockContext == null) {
            return;
        }
        JSONObject airlyticsContext = airlockContext.optJSONObject(JSON_AIRLYTICS);
        Map<String, Object> userAttributes = new HashMap<>();
        if (airlyticsContext != null) {
            Iterator<?> keys = airlyticsContext.keys();
            while (keys.hasNext()) {
                String key = (String) keys.next();
                Object value = airlyticsContext.opt(key);
                if (value != null) {
                    userAttributes.put(key, value);
                }
            }
        }
        if (getFeature(USER_ATTRIBUTES).isOn()){
            Map<String, Object> analyticsFields = getContextFieldsValuesForAirlyticsAsMap(airlockContext);

            if (!analyticsFields.isEmpty()){
                userAttributes.putAll(analyticsFields);
            }
            if (!userAttributes.isEmpty()) {
                setUserAttributes(userAttributes);
            }
        }

    }

    @SuppressWarnings("unused")
    public void addAirlyticsShardToContext(@Nullable JSONObject airlockContext) throws AirlockNotInitializedException, JSONException {
        if (airlockContext != null) {
            final int NUMBER_OF_SHARDS = 1000;
            byte[] value = AirlockManager.getInstance().getAirlockUserUniqueId().getBytes();
            long no = Murmur2.hash(value, value.length, 894157739);
            airlockContext.put(JSON_AIRLYTICS_SHARD, ((Long) (no % NUMBER_OF_SHARDS)).intValue());
        }
    }

    private void setUserAttributes(Map<String, Object> attributes) {
        setUserAttributes(attributes, userAttributesSchemaVersion);
    }

    @CheckForNull
    private byte[] getDetailsAsByteArray(JSONArray sessionDetailsFields, ALEnvironment currentEnvironment) {
        final int BYTE_SIZE = 8;
        int detailsSize = 0;
        Map<String, Object> fieldsMap = new HashMap<>();

        String airlockId;
        try {
            airlockId = AirlockManager.getInstance().getAirlockUserUniqueId();
        } catch (AirlockNotInitializedException e) {
            Logger.log.d(TAG, AirlockMessages.ERROR_SDK_NOT_INITIALIZED);
            return null;
        }

        //because the order of the fields is important - we can not add the values to byte array immediately
        for (int i = 0; i < sessionDetailsFields.length(); i++) {
            String field = sessionDetailsFields.optString(i);
            switch (field) {
                case Constants.ADS_AIRLOCK_ID:
                    detailsSize += BYTE_SIZE * 2;
                    fieldsMap.put(field, UUID.fromString(airlockId));
                    break;
                case Constants.ADS_SESSION_ID:
                    detailsSize += BYTE_SIZE * 2;
                    fieldsMap.put(field, currentEnvironment.getSessionId());
                    break;
                case Constants.ADS_SESSION_START_TIME:
                    detailsSize += BYTE_SIZE;
                    fieldsMap.put(field, currentEnvironment.getSessionStartTime());
                    break;
            }
        }
        if (detailsSize < 1) {
            return null;
        }
        ByteBuffer detailsByteBuffer = ByteBuffer.wrap(new byte[detailsSize+1]);

        byte mode = 0;
        if (AirlockManager.isDevUser.get()){
            mode = 1;
        }
        detailsByteBuffer.put(mode);
        String[] uuidFields = {Constants.ADS_AIRLOCK_ID, Constants.ADS_SESSION_ID};
        for (String field : uuidFields) {
            UUID uuid = (UUID) fieldsMap.get(field);
            if (uuid != null) {
                detailsByteBuffer.putLong(uuid.getMostSignificantBits());
                detailsByteBuffer.putLong(uuid.getLeastSignificantBits());
            }
        }
        if (fieldsMap.containsKey(Constants.ADS_SESSION_START_TIME)) {
            Long startTime = (Long) fieldsMap.get(Constants.ADS_SESSION_START_TIME);
            if (startTime != null) {
                detailsByteBuffer.putLong(startTime);
            }
        }
        return detailsByteBuffer.array();

    }

    //called in updateAnalytics method
    private Map<String, Object> getCalculatedUserAttributes() {
        Map<String, Object> userAttributes = new HashMap<>();

        userAttributes.put(getAnalyticsFeatureName(ConstantsKeys.DEV_USER_ATTRIBUTE), AirlockManager.isDevUser.get());

        Map<String, String> experimentInfo = AirlockManager.getInstance().getExperimentInfo();
        String variant = null;
        String experiment = null;
        if (experimentInfo != null) {
            variant = experimentInfo.get(Constants.JSON_FIELD_VARIANT);
            experiment = experimentInfo.get(Constants.JSON_FIELD_EXPERIMENT);
            //Do not send to airlytics empty values
            if (variant.isEmpty()) {
                variant = null;
            }
            if (experiment.isEmpty()) {
                experiment = null;
            }
        }
        userAttributes.put(getAnalyticsFeatureName(ConstantsKeys.VARIANT_ATTRIBUTE), variant);
        userAttributes.put(getAnalyticsFeatureName(ConstantsKeys.EXPERIMENT_ATTRIBUTE), experiment);
        return userAttributes;
    }

    private Map<String, Object> getContextFieldsValuesForAirlyticsAsMap(JSONObject contextObject) {

        Map<String, Object> map = new HashMap<>();
        JSONObject calculatedFeatures = AirlockManager.getInstance().getContextFieldsValuesForAnalytics(contextObject, false);
        if (calculatedFeatures != null) {
            Iterator<String> keysItr = calculatedFeatures.keys();
            while (keysItr.hasNext()) {
                String key = keysItr.next();
                Object value = calculatedFeatures.opt(key);
                key = key.replace("context.streams.", "streams.");
                map.put(key, value);
            }
        }
        return map;
    }

    @SuppressWarnings("unused")
    private boolean isAirlyticsEnabled() {
        return airlyticsEnabled.get();
    }

    private Set<String> getUserTags(@Nullable Map<String, Object> attributes) {
        Set<String> userTags = new HashSet<>();
        if (!AirlockManager.isDevUser.get()) {
            if (attributes != null) {
                Boolean devUser = (Boolean) attributes.get(AirlockManager.getInstance().getAnalyticsImpl().getAnalyticsFeatureName(ConstantsKeys.DEV_USER_ATTRIBUTE));
                if (devUser != null) {
                    AirlockManager.isDevUser.set(devUser);
                }
            }
            if (!AirlockManager.getInstance().getDeviceUserGroups().isEmpty()
                    || !AirlockManager.getInstance().getDevelopBranchName().isEmpty()) {
                AirlockManager.isDevUser.set(true);
            }
        }
        if (AirlockManager.isDevUser.get()) {
            userTags.add(DEV_TAG);
        } else {
            userTags.add(PROD_TAG);
        }
        return userTags;
    }

    private void persistAirlyticsAppStart(String name, Map<String, Object> attributes) {
        airlyticsPendingEvents.put(name, attributes);
    }

    private JSONObject getSessionDetailsFeatureConfig(String featureName){
        Feature adSessionDetails = AirlockManager.getInstance().getFeature(featureName);
        if (!adSessionDetails.isOn()) {
            return null;
        }
        return adSessionDetails.getConfiguration();
    }

    private Feature getFeature(String name) {
        return AirlockManager.getInstance().getFeature(name);
    }
}
