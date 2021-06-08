package com.weather.airlock.sdk.engine;

import com.ibm.airlock.common.cache.CacheManager;
import com.ibm.airlock.common.cache.PersistenceHandler;
import com.ibm.airlock.common.data.Feature;
import com.ibm.airlock.common.data.FeaturesList;
import com.ibm.airlock.common.data.Entitlement;
import com.ibm.airlock.common.engine.EntitlementsCalculator;
import com.ibm.airlock.common.engine.FeaturesCalculator;
import com.ibm.airlock.common.notifications.NotificationsManager;
import com.ibm.airlock.common.util.Constants;
import com.ibm.airlock.common.util.RawEntitlementsJsonParser;
import com.ibm.qautils.FileUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Created by Denis Voloshin on 21/01/2019.
 */
public class CalculationWithPurchasesTest extends EngineNullValuesTest {


    CacheManager cacheManager;
    PersistenceHandler persistenceHandler;
    Collection<String> m_productIds;
    NotificationsManager notificationsManager;

    //The test is usually run from the parent of the source folder
    private static final String DEFAULT_DATA_PATH = "." + File.separator + "src" + File.separator + "test" + File
            .separator + "java" + File.separator + "com" + File.separator + "weather" + File.separator + "airlock" + File.separator + "sdk" + File.separator + "purchase";


    @BeforeClass
    public static void readSettingsFile() {
        //1. Data path
        m_dataPath = DEFAULT_DATA_PATH;

        //Read input data and create other data
        try {
            m_features = new JSONObject(FileUtils.fileToString(m_dataPath + File.separator + "features.json", "UTF-8", false));
            m_context = new JSONObject((FileUtils.fileToString(m_dataPath + File.separator + "context.json", "UTF-8", false)));
            m_translations = new JSONObject((FileUtils.fileToString(m_dataPath + File.separator + "translations.json", "UTF-8", false)));
            m_functions = FileUtils.fileToString(m_dataPath + File.separator + "js-functions.txt", "UTF-8", false);
            m_profileGroups = new ArrayList<String>();
            m_profileGroups.add("QA");
            m_fallback = new HashMap<String, FeaturesCalculator.Fallback>();
            m_userRandomNumber = 100;
            m_productVersion = "3.0";
        } catch (JSONException e) {
            Assert.fail("Exception was thrown when reading a json input file. Message: " + e.getMessage());
        } catch (IOException e) {
            Assert.fail("Exception was thrown when reading an input file. Message: " + e.getMessage());
        }
    }


    @Before
    public void setup() {
        cacheManager = Mockito.mock(CacheManager.class);
        persistenceHandler = Mockito.mock(PersistenceHandler.class);
        notificationsManager = Mockito.mock(NotificationsManager.class);
        Mockito.when(persistenceHandler.getDevelopBranchName()).thenReturn("");
        Mockito.when(persistenceHandler.getLastBranchName()).thenReturn("");
        Mockito.when(persistenceHandler.read(Mockito.anyString(), Mockito.anyString())).thenReturn("{}");
        Mockito.when(cacheManager.getPersistenceHandler()).thenReturn(persistenceHandler);
        Mockito.when(cacheManager.getNotificationsManager()).thenReturn(notificationsManager);
        m_productIds = new ArrayList<>();
        m_productIds.add("free.ads");
        m_productIds.add("975");
    }


    @Test
    public void parsePurchaseCalculationResults() {

        try {
            EntitlementsCalculator entitlementsCalculator = new EntitlementsCalculator();
            JSONObject purchasesAsJson = entitlementsCalculator.calculate(
                    cacheManager,
                    m_features,
                    m_context,
                    m_functions,
                    m_translations,
                    m_profileGroups,
                    m_fallback,
                    m_productVersion,
                    null,
                    null,
                    null);
            Assert.assertNotNull(purchasesAsJson);

            FeaturesList<Entitlement> purchases = RawEntitlementsJsonParser.getInstance().getFeatures(purchasesAsJson, Feature.Source.SERVER);
            Assert.assertNotNull(purchases);
            Assert.assertEquals(true, purchases.getFeature("ns1.inAppPurchase1").isOn());
            Assert.assertEquals(true,  purchases.getFeature("ns1.inAppPurchase1").getPurchaseOptions().size() > 0);
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("An exception was thrown when trying to execute calculate features method of the client engine. Message: " + e.getMessage());
        }

    }


    @Test
    public void testBasicPurchaseCalculation() {
        calculatePurchases();
    }


    @Test
    public void testFeaturesCalculationWithPurchases() {
        Map<String, List<String>> purchases = calculatePurchases();

        m_productVersion = "3.0";
        try {
            JSONObject calculatedFeatures = new FeaturesCalculator().calculate(
                    cacheManager,
                    m_features,
                    m_context,
                    m_functions,
                    m_translations,
                    m_profileGroups,
                    m_fallback,
                    m_productVersion,
                    null,
                    m_productIds,
                    purchases
            );
            Assert.assertNotNull(calculatedFeatures);
            Assert.assertEquals(1, calculatedFeatures.getJSONArray(Constants.JSON_FEATURE_FIELD_FEATURES).length());
            JSONObject feature = calculatedFeatures.getJSONArray(Constants.JSON_FEATURE_FIELD_FEATURES).getJSONObject(0);
            Assert.assertNotNull(feature);
            Assert.assertNotNull(feature.optString(Constants.JSON_FIELD_STORE_PRODUCT_ID));
            Assert.assertEquals("free.ads", feature.optString(Constants.JSON_FIELD_STORE_PRODUCT_ID));
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("An exception was thrown when trying to execute calculate features method of the client engine. Message: " + e.getMessage());
        }
    }


    private Map<String, List<String>> calculatePurchases() {
        m_productVersion = "3.0";
        try {

            EntitlementsCalculator entitlementsCalculator = new EntitlementsCalculator();
            JSONObject purchases = entitlementsCalculator.calculate(
                    cacheManager,
                    m_features,
                    m_context,
                    m_functions,
                    m_translations,
                    m_profileGroups,
                    m_fallback,
                    m_productVersion,
                    null,
                    null,
                    null);
            Assert.assertNotNull(purchases);

            Map<String, List<String>> purchaseToProductIdMap = entitlementsCalculator.getPurchaseToProductIdsMap(purchases);
            Assert.assertEquals("[free.ads]",
                    purchaseToProductIdMap.get("ns1.inAppPurchase1").toString());
            return purchaseToProductIdMap;
        } catch (Exception e) {
            e.printStackTrace();
            Assert.fail("An exception was thrown when trying to execute calculate features method of the client engine. Message: " + e.getMessage());
        }
        Assert.fail("EntitlementsCalculator().calculate method returned null");
        return null;
    }

}
