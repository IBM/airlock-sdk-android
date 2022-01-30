package com.ibm.airlock.sdk.engine;

import com.ibm.airlock.common.engine.FeaturesCalculator;
import com.ibm.airlock.common.engine.ScriptInitException;
import com.ibm.qautils.FileUtils;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 * Created by iditb on 01/05/17.
 */

public class EngineNullValuesTest {

    /*
    DEFAULTS
     */
    //The test is usually run from the parent of the source folder
    protected static String DEFAULT_DATA_PATH = "." + File.separator + "src" + File.separator + "test" + File
            .separator + "java" + File.separator + "com" + File.separator + "weather" + File.separator + "airlock" + File.separator + "sdk" + File.separator + "performance";
    /*
    TEST CONFIGURATION VALUES
     */
    protected static String m_dataPath;
    /*
    READ DATA FROM INPUT FILE
     */
    protected static JSONObject m_features;
    protected static JSONObject m_context;
    protected static String m_functions;
    protected static JSONObject m_translations;
    protected static Map<String, FeaturesCalculator.Fallback> m_fallback;
    protected static List<String> m_profileGroups;
    protected static String m_productVersion;
    protected static int m_userRandomNumber;


    public EngineNullValuesTest() {
    }


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
            m_productVersion = "1.0";
        } catch (JSONException e) {
            Assert.fail("Exception was thrown when reading a json input file. Message: " + e.getMessage());
        } catch (IOException e) {
            Assert.fail("Exception was thrown when reading an input file. Message: " + e.getMessage());
        }
    }

    @Test
    public void nullFeaturesFeaturesCalculator() throws JSONException {
        calculateFeaturesTest(null, m_context, m_functions, m_translations, m_profileGroups, m_fallback, m_productVersion, m_userRandomNumber);
    }

    @Test
    public void nullContextFeaturesCalculator() throws JSONException {
        calculateFeaturesTest(m_features, null, m_functions, m_translations, m_profileGroups, m_fallback, m_productVersion, m_userRandomNumber);
    }

    @Test
    public void nullFunctionsFeaturesCalculator() throws JSONException {
        //todo calculateFeaturesTest(m_features,m_context,null,m_translations,m_profileGroups,m_fallback,m_productVersion,m_userRandomNumber);
    }

    @Test
    public void nullTranslationsFeaturesCalculator() throws JSONException {
        calculateFeaturesTest(m_features, m_context, m_functions, null, m_profileGroups, m_fallback, m_productVersion, m_userRandomNumber);
    }

    @Test
    public void nullProfileGroupsFeaturesCalculator() throws JSONException {
        calculateFeaturesTest(m_features, m_context, m_functions, m_translations, null, m_fallback, m_productVersion, m_userRandomNumber);
    }

    @Test
    public void nullFallbackFeaturesCalculator() throws JSONException {
        calculateFeaturesTest(m_features, m_context, m_functions, m_translations, m_profileGroups, null, m_productVersion, m_userRandomNumber);
    }

    @Test
    public void nullProductVersionFeaturesCalculator() throws JSONException {
        calculateFeaturesTest(m_features, m_context, m_functions, m_translations, m_profileGroups, m_fallback, null, m_userRandomNumber);
    }

    public long calculateFeaturesTest(JSONObject features, JSONObject deviceContext, String functions, JSONObject translations,
                                      List<String> profileGroups, Map<String, FeaturesCalculator.Fallback> fallback, String productVersion, int userRandomNumber) throws JSONException {
        long start = System.currentTimeMillis();
        try {
            new FeaturesCalculator().calculate(null, features, deviceContext, functions, translations, profileGroups, fallback, productVersion, null);
        } catch (ScriptInitException e) {
            Assert.fail("An exception was thrown when trying to execute calculate features method of the client engine. Message: " + e.getMessage());
        }
        long time = System.currentTimeMillis() - start;
        return time;
    }
}
