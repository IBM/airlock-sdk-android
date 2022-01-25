package com.weather.airlock.sdk;

import android.content.Context;

import com.ibm.airlock.common.AirlockCallback;
import com.ibm.airlock.common.AirlockInvalidFileException;
import com.ibm.airlock.common.AirlockNotInitializedException;
import com.ibm.airlock.common.BaseAirlockProductManager;
import com.ibm.airlock.common.analytics.AnalyticsApiInterface;
import com.ibm.airlock.common.cache.InMemoryCache;
import com.ibm.airlock.common.cache.PersistenceHandler;
import com.ibm.airlock.common.cache.RuntimeLoader;
import com.ibm.airlock.common.data.Feature;
import com.ibm.airlock.common.data.Servers;
import com.ibm.airlock.common.inapp.PurchasesManager;
import com.ibm.airlock.common.log.Logger;
import com.ibm.airlock.common.net.ConnectionManager;
import com.ibm.airlock.common.streams.AirlockStreamResultsTracker;
import com.ibm.airlock.common.streams.StreamsManager;
import com.ibm.airlock.common.util.AirlockMessages;
import com.ibm.airlock.common.util.Base64;
import com.weather.airlock.sdk.analytics.AnalyticsDefaultImpl;
import com.weather.airlock.sdk.cache.AndroidContext;
import com.weather.airlock.sdk.cache.AndroidPersistenceHandler;
import com.weather.airlock.sdk.log.AndroidLog;
import com.weather.airlock.sdk.net.AndroidOkHttpClientBuilder;
import com.weather.airlock.sdk.notifications.AndroidNotificationsManager;
import com.weather.airlock.sdk.util.AndroidBase64;
import com.weather.airlock.sdk.util.FileUtil;

import org.jetbrains.annotations.TestOnly;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.CheckForNull;
import javax.annotation.Nullable;


/**
 * The main Airlock class that is used by the application.
 *
 * @author Rachel Levy
 */
@SuppressWarnings("unused")
public class AirlockManager extends BaseAirlockProductManager {

    private static final String ANDROID_PRODUCT_NAME = "ANDROID_PRODUCT_NAME";
    private static final String TAG = "AirlockManager";
    private static final String DEV_TAG = "DEV";
    private static final String PROD_TAG = "PROD";

    private static final Object lock = new Object();
    private volatile static AirlockManager instance;
    private Context applicationContext = null;
    private Map<String,Object> additionalParams = new HashMap<>();

    private AnalyticsDefaultImpl airlyticsImpl = new AnalyticsDefaultImpl();
    public static final AtomicBoolean isDevUser;

    static {
        Base64.init(new AndroidBase64());
        Logger.setLogger(new AndroidLog());
        InMemoryCache.setIsEnabled(false);
        isDevUser = new AtomicBoolean(false);
    }

    private AirlockManager() {
        super();
    }

    /**
     * Returns an AirlockManager instance.
     *
     * @return Returns an AirlockManager instance.
     */
    public static AirlockManager getInstance() {
        if (instance == null) {
            synchronized (lock) {
                if (instance == null) {
                    instance = new AirlockManager();
                }
            }
        }
        return instance;
    }

    /**
     * Initializes AirlockManager with application information.
     * InitSDK loads the defaults file specified by the defaultFileId and
     * merges it with the current feature set.
     *
     * @param appContext     The current Android context.
     * @param defaultFileId  Resource ID of the defaults file. This defaults file should be part of the application. You can get this by running the Airlock
     *                       Code Assistant plugin.
     * @param productVersion The application version. Use periods to separate between major and minor version numbers, for example: 6.3.4
     * @throws AirlockInvalidFileException Thrown when the defaults file does not contain the proper content.
     * @throws IOException                 Thrown when the defaults file cannot be opened.
     */
    public synchronized void initSDK(Context appContext, int defaultFileId, String productVersion, Object...additionalParams) throws AirlockInvalidFileException, IOException {
        initSDK(new AndroidContext(appContext), defaultFileId, productVersion, additionalParams);
    }

    /**
     * Initializes AirlockManager with application information.
     * InitSDK loads the defaults file specified by the defaultFileId and
     * merges it with the current feature set.
     *
     * @param appContext     The current Android context.
     * @param defaultFileId    The defaults file. This defaults file should be part of the application. You can get this by running the Airlock Code Assistant
     *                       plugin.
     * @param productVersion The application version. Use periods to separate between major and minor version numbers, for example: 6.3.4
     * @param additionalParams Optional. Map that can contain the following parameters:
     *      "userAttributesSchemaVersion": <schema version for userAttributes as string (airlytics)>
     *      "initCallback": <callback function to be called when airlock is ready and functional></>,
     *      "isDevUser": boolean if user is  dev user - true
     *      "enableAnalyticsLifecycle" : boolean the defines whether use airlytics lifecycle or not
     *
     * @throws AirlockInvalidFileException Thrown when the defaults file does not contain the proper content.
     * @throws IOException                 Thrown when the defaults file cannot be opened.
     */
    @Override
    public synchronized void initSDK(com.ibm.airlock.common.cache.Context appContext, int defaultFileId, String productVersion, Object...additionalParams) throws
            AirlockInvalidFileException,
            IOException {
        /**
         * Allows multiple initSDK calls; skip initialization logic if it's already done.
         */
        if (init) {
            return;
        }
        String defaultFile = FileUtil.readAndroidFile(defaultFileId, appContext);
        parseAndApplyAdditionalParams(additionalParams);
        String encryptionKey = this.additionalParams != null ? (String) this.additionalParams.get("encryptionKey") : "";
        PersistenceHandler persistenceHandler = new AndroidPersistenceHandler(appContext);
        this.streamsManager = new StreamsManager(persistenceHandler, productVersion);
        this.purchasesManager = new PurchasesManager(persistenceHandler, productVersion);
        this.notificationsManager = new AndroidNotificationsManager(appContext, persistenceHandler, productVersion, this.cacheManager.getAirlockContextManager());
        this.cacheManager.init(ANDROID_PRODUCT_NAME, appContext, defaultFile, productVersion, persistenceHandler, this
                .streamsManager, this.notificationsManager, new ConnectionManager(new AndroidOkHttpClientBuilder(), encryptionKey));
        connectionManager = this.cacheManager.getConnectionManager();
        applicationContext = ((AndroidContext) appContext).context;

        if (!isDevUser.get()) {
            List userGroups = getDeviceUserGroups();
            isDevUser.set(userGroups.size() > 0 || !getDevelopBranchName().isEmpty());
        }
        airlyticsImpl.syncAnalytics();
        init = true;
    }

    private void parseAndApplyAdditionalParams(Object[] additionalParams) {
        if (additionalParams != null) {
            for (int i = 0; i < additionalParams.length; i++) {
                Object param = additionalParams[i];
                if (param instanceof Map){
                    this.additionalParams = (Map<String, Object>) param;
                } else if (param instanceof String) {
                    this.additionalParams.put("userAttributesSchemaVersion", param);
                } else if (param instanceof AirlockCallback) {
                    this.additionalParams.put("initCallback", param);
                } else if (param instanceof Boolean) {
                    if (!this.additionalParams.containsKey("isDevUser")){
                        this.additionalParams.put("isDevUser", param);
                    } else {
                        this.additionalParams.put("enableAnalyticsLifecycle", param);
                    }
                }
            }
            applyAdditionalParams();
        }
    }

    private void applyAdditionalParams() {
        for (String key: this.additionalParams.keySet()){
            switch (key){
                case "isDevUser":
                    AirlockManager.isDevUser.set((Boolean) this.additionalParams.get(key));
                    break;
                case "enableAnalyticsLifecycle":
                    airlyticsImpl.enableAnalyticsLifecycle(!(Boolean) this.additionalParams.get(key));
                    break;
                case "userAttributesSchemaVersion":
                    airlyticsImpl.setUserAttributesSchemaVersion((String) this.additionalParams.get(key));
                    break;
                case "initCallback":
                    airlyticsImpl.setAirlyticsInitializationCallback((AirlockCallback) this.additionalParams.get(key));
                    break;
            }
        }
    }

    public Feature calculateFeatures(@Nullable JSONObject context) {
        airlyticsImpl.sendContextAsUserAttributes(context);
        return new Feature();
    }

    @Override
    public void calculateFeatures(@Nullable JSONObject context, Collection<String> purchasedProducts) throws AirlockNotInitializedException, JSONException {
        airlyticsImpl.addAnalyticsShardToContext(context);
        super.calculateFeatures(context, purchasedProducts);
        airlyticsImpl.sendContextAsUserAttributes(context);
    }

    public void calculateFeatures(@Nullable JSONObject userProfile, @Nullable JSONObject airlockContext) throws AirlockNotInitializedException, JSONException {
        airlyticsImpl.addAnalyticsShardToContext(airlockContext);
        super.calculateFeatures(userProfile, airlockContext);
        airlyticsImpl.sendContextAsUserAttributes(airlockContext);
    }

    @Override
    public void syncFeatures() throws AirlockNotInitializedException {
        if (!this.init) {
            throw new AirlockNotInitializedException(AirlockMessages.ERROR_SDK_NOT_INITIALIZED);
        } else {
            cacheManager.syncFeatures();
            airlyticsImpl.syncAnalytics();
        }
    }

    @CheckForNull
    @Override
    public JSONArray addStreamsEvent(JSONObject event) {
        JSONArray events = new JSONArray();
        events.put(event);
        return addStreamsEvent(events, true);
    }

    @CheckForNull
    @Override
    public JSONArray addStreamsEvent(JSONArray events, boolean processImmediately) {
        return streamsManager.calculateAndSaveStreams(events, processImmediately, null, getContextFieldsForAnalytics(), new AirlockStreamResultsTracker() {
                @Override
                public void trackResults(Map<String, Object> map) {
                    airlyticsImpl.trackStreamResults(map);
                }
            });
    }

    @Override
    public void updateProductContext(String context) {

    }

    @Override
    public void updateProductContext(String context, boolean clearPreviousContext) {

    }

    @Override
    public void removeProductContextField(String fieldPath) {
    }

    public Map<String, String> getContextFieldsValuesForAnalyticsAsMap(JSONObject contextObject) {
        JSONObject calculatedFeatures = this.getContextFieldsValuesForAnalytics(contextObject, true);
        Map<String, String> map = new HashMap<>();

        if (calculatedFeatures != null) {
            Iterator<String> keysItr = calculatedFeatures.keys();
            while (keysItr.hasNext()) {
                String key = keysItr.next();
                String value = calculatedFeatures.optString(key);
                map.put(key, value);
            }
        }
        return map;
    }

    private JSONObject getSessionDetailsFeatureConfig(String featureName){
        Feature adSessionDetails = getFeature(featureName);
        if (!adSessionDetails.isOn()) {
            return null;
        }

        return adSessionDetails.getConfiguration();
    }

    public Map<String,String> getSessionDetailsMap(String featureName) {
        Map<String,String> result = new HashMap<>();
        JSONObject config = getSessionDetailsFeatureConfig(featureName);
        if (config == null){
            return result;
        }
        String encodedCipherText = airlyticsImpl.getSessionDetails(featureName);
        if (encodedCipherText == null){
            return result;
        }
        int maxHeaderLength = config.optInt("maxCharactersHeaderLength", 38);
        String headerName = config.optString("headerName","ltv");
        for (int index = 0, ltvIndex = 0; index < encodedCipherText.length();ltvIndex++,index +=maxHeaderLength){
            String headerSuffix = ltvIndex == 0 ? "": String.valueOf(ltvIndex+1);
            int endIndex = Math.min(index+maxHeaderLength, encodedCipherText.length());
            result.put(headerName + headerSuffix, encodedCipherText.substring(index, endIndex));
        }
        return result;
    }

    /**
     * Specifies a list of user groups selected for the device.
     *
     * @param userGroups List of the selected user groups.
     */
    @Override
    public void setDeviceUserGroups(@Nullable List<String> userGroups) {
        super.setDeviceUserGroups(userGroups);
        isDevUser.set(true);
        Map<String, Object> userAttributes = new HashMap<>();
        userAttributes.put(airlyticsImpl.getAnalyticsFeatureName(AnalyticsApiInterface.ConstantsKeys.DEV_USER_ATTRIBUTE), true);
        airlyticsImpl.setUserAttributes(userAttributes, airlyticsImpl.getUserAttributesSchemaVersion());
    }

    @Override
    public String resetAirlockId() {
        String newUUID = super.resetAirlockId();
        airlyticsImpl.updateUserId(newUUID);
        return newUUID;
    }

    public Context getApplicationContext() {
        return applicationContext;
    }

    /*
    Analytics/ Airlytics methods
     */
    ///////////////////////////////////////////
    public void verifyAirlyticsState() {
        airlyticsImpl.verifyAnalyticsState();
    }

    public void enableAirlytics(){
        airlyticsImpl.enableAnalyticsLifecycle(true);
    }

    public AnalyticsDefaultImpl getAnalyticsImpl() {
        return airlyticsImpl;
    }

    public void setAirlyticsUserAttributes(Map<String, Object> attributes, @Nullable String schemaVersion) {
        airlyticsImpl.setUserAttributes(attributes, schemaVersion);
    }

    public void setAirlyticsUserAttribute(String name, @Nullable Object value, @Nullable String schemaVersion) {
        Map<String, Object> attributes = new HashMap<>();
        attributes.put(name, value);
        setAirlyticsUserAttributes(attributes, schemaVersion);
    }

    public void sendAirlyticsEventsWhenGoingToBackground() {
        airlyticsImpl.sendAnalyticsEventsWhenGoingToBackground();
    }

    public void track(String name, @Nullable String schemaVersion, Map<String, Object> attributes) {
        track(name, null, schemaVersion, attributes);
    }

    public void track(String name, @Nullable Long eventTime, @Nullable String schemaVersion, Map<String, Object> attributes) {
        airlyticsImpl.track(name, eventTime, schemaVersion, attributes);
    }

    @SuppressWarnings("WeakerAccess")
    public void track(String name, Map<String, Object> attributes) {
        track(name, null, attributes);
    }

    @CheckForNull
    public String getSessionDetails(String featureName) {
        return airlyticsImpl.getSessionDetails(featureName);
    }

    /*
    Test and Debug related methods
     */

    @TestOnly
    @Override
    public void reset(com.ibm.airlock.common.cache.Context context, boolean simulateUninstall) {
        try {
            PersistenceHandler sp = cacheManager.getPersistenceHandler();
            if (sp == null) {
                sp = new AndroidPersistenceHandler(context);
                cacheManager.setPersistenceHandler(sp);
            }

            if (simulateUninstall) {
                sp.reset(context);
            } else {
                sp.clearInMemory();
            }
        } catch (Exception e) {
            Logger.log.d(TAG, AirlockMessages.ERROR_SP_NOT_INIT_CANT_CLEAR);
            // continue, this is because the SP is not init
        }
        cacheManager.resetFeatureLists();
        if (streamsManager != null) {
            streamsManager.clearStreams();
        }
        init = false;
    }

    @SuppressWarnings({"deprecation", "ConstantConditions"})
    @TestOnly
    public void reset(Context context) {
        if (cacheManager.getServers() != null) {
            cacheManager.getServers().nullifyServerList();
        }
        this.reset(context, true);
    }

    /*
    Method for avoiding publish of cacheManager
     */
    public void pullDefaultFile(final Servers.Server server, final Servers.Product product, final AirlockCallback airlockCallback) {
        this.cacheManager.pullDefaultFile(server, product, airlockCallback);
    }

    public void pullProductList(final Servers.Server server, final AirlockCallback airlockCallback) {
        this.cacheManager.pullProductList(server, airlockCallback);
    }

    public void pullServerList(final AirlockCallback airlockCallback) {
        this.cacheManager.pullServerList(airlockCallback);
    }

    public String readProperty(String name, String defaultValue){
        return this.cacheManager.getPersistenceHandler().read(name, defaultValue);
    }

    @TestOnly
    // simulate uninstall.
    public void reset(Context context, boolean simulateUninstall) {
        reset(new AndroidContext(context), simulateUninstall);
    }

    @TestOnly
    public void resetRuntime(Context context) {
        resetRuntime(new AndroidContext(context));
    }

    private void resetRuntime(com.ibm.airlock.common.cache.Context context) {
        try {

            PersistenceHandler sp = cacheManager.getPersistenceHandler();
            if (sp == null) {
                sp = new AndroidPersistenceHandler(context);
                cacheManager.setPersistenceHandler(sp);
            }
            sp.clearRuntimeData();
        } catch (Exception e) {
            Logger.log.d(TAG, AirlockMessages.ERROR_SP_NOT_INIT_CANT_CLEAR);
        }
    }

    @TestOnly
    public void reInitSDK(Context appContext, int defaultFileId, String productVersion) throws AirlockInvalidFileException, IOException {
        reset(appContext, false);
        initSDK(appContext, defaultFileId, productVersion);
    }

    /**
     * Initializes AirlockManager with application information.
     * InitSDK loads the defaults file specified by the defaultFileId and
     * merges it with the current feature set.
     *
     * @param appContext    The current Android context.
     * @param encryptionKey Encryption key will be used to encrypt/decrypt the cached data
     * @throws AirlockInvalidFileException Thrown when the defaults file does not contain the proper content.
     * @throws IOException                 Thrown when the defaults file cannot be opened.
     */
    @SuppressWarnings("DanglingJavadoc")
    public synchronized void initSDKWithRuntime(com.ibm.airlock.common.cache.Context appContext, RuntimeLoader runtimeLoader, String encryptionKey) throws
            AirlockInvalidFileException,
            IOException {
        /**
         * Allows multiple initSDK calls; skip initialization logic if it's already done.
         */
        if (init) {
            return;
        }
        PersistenceHandler persistenceHandler = new AndroidPersistenceHandler(appContext);
        this.streamsManager = new StreamsManager(persistenceHandler, appVersion);
        this.notificationsManager = new AndroidNotificationsManager(appContext, persistenceHandler, appVersion, this.cacheManager.getAirlockContextManager());
        this.cacheManager.init(ANDROID_PRODUCT_NAME, appContext, "{}", appVersion, persistenceHandler, this
                .streamsManager, this.notificationsManager, new ConnectionManager(new AndroidOkHttpClientBuilder(), encryptionKey));
        connectionManager = this.cacheManager.getConnectionManager();
        runtimeLoader.loadRuntimeFilesOnStartup(this);
        init = true;
    }
}
