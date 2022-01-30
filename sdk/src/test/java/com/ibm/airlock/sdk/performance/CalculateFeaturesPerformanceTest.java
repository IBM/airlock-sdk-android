package com.ibm.airlock.sdk.performance;

import com.ibm.airlock.common.engine.FeaturesCalculator;
import com.ibm.airlock.common.engine.ScriptInitException;
import com.ibm.qautils.FileUtils;
import com.ibm.qautils.PropertiesFileUtils;

import org.json.JSONArray;
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

//import com.ibm.airlock.sdk.EngineInstrumentalTests;

/**
 * Created by iditb on 26/10/2016.
 */

public class CalculateFeaturesPerformanceTest {

    /*
    DEFAULTS
     */
    //The test is usually run from the parent of the source folder
    private static final String DEFAULT_DATA_PATH = "." + File.separator + "src" + File.separator + "test" + File
            .separator + "java" + File.separator + "com" + File.separator + "weather" + File.separator + "airlock" + File.separator + "sdk" + File.separator + "performance";
    private static final int DEFAULT_NUM_OF_FEATURES = 100;
    private static final int DEFAULT_NUM_OF_SIMPLE_KEYS = 20;
    private static final int DEFAULT_NUM_OF_ARR_KEYS = 10;
    private static final int DEFAULT_NUM_OF_OBJECT_KEYS = 10;
    private static final int DEFAULT_RULE_LENGTH = 20;
    /*
    TEST CONFIGURATION VALUES
     */
    private static HashMap<String, String> m_settings;
    private static String m_dataPath;
    private static int m_numberOfFeatures;
    private static int m_numberOfFeatureSimpleKeys;
    private static int m_numberOfFeatureArrayKeys;
    private static int m_numberOfFeatureObjectKeys;
    private static int m_numberOfContextSimpleKeys;
    private static int m_numberOfContextArrayKeys;
    private static int m_numberOfContextObjectKeys;
    private static int m_ruleLength;
    private static int m_numberOfRules;
    /*
    READ DATA FROM INPUT FILE
     */
    private static JSONObject m_features;
    private static JSONObject m_context;
    private static String m_functions;
    private static JSONObject m_translations;
    private static Map<String, FeaturesCalculator.Fallback> m_fallback;
    private static List<String> m_profileGroups;
    private static String m_productVersion;
    private static int m_userRandomNumber;


    @BeforeClass
    public static void readSettingsFileAndMakeTheFirstCall() {
        try {
            m_settings = PropertiesFileUtils.pfileToHashMap(DEFAULT_DATA_PATH + File.separator + "settings.txt");
        } catch (IOException e) {
            Assert.fail("IOException was thrown when trying to access the settings file. Message: " + e.getMessage());
        }
        //1. Data path
        m_dataPath = m_settings.get("dataPath");
        if (m_dataPath == null) {
            m_dataPath = DEFAULT_DATA_PATH;
        }
        //2. Number of features
        try {
            m_numberOfFeatures = Integer.parseInt(m_settings.get("numberOfFeatures"));
        } catch (NumberFormatException e) {
            m_numberOfFeatures = DEFAULT_NUM_OF_FEATURES;
        }
        //2.1 Feature size
        try {
            m_numberOfFeatureSimpleKeys = Integer.parseInt(m_settings.get("numberOfFeatureSimpleKeys"));
        } catch (NumberFormatException e) {
            m_numberOfFeatureSimpleKeys = DEFAULT_NUM_OF_SIMPLE_KEYS;
        }
        try {
            m_numberOfFeatureArrayKeys = Integer.parseInt(m_settings.get("numberOfFeatureArrayKeys"));
        } catch (NumberFormatException e) {
            m_numberOfFeatureArrayKeys = DEFAULT_NUM_OF_ARR_KEYS;
        }
        try {
            m_numberOfFeatureObjectKeys = Integer.parseInt(m_settings.get("numberOfFeatureObjectKeys"));
        } catch (NumberFormatException e) {
            m_numberOfFeatureObjectKeys = DEFAULT_NUM_OF_OBJECT_KEYS;
        }

        //4. Context size
        try {
            m_numberOfContextSimpleKeys = Integer.parseInt(m_settings.get("numberOfContextSimpleKeys"));
        } catch (NumberFormatException e) {
            m_numberOfContextSimpleKeys = DEFAULT_NUM_OF_SIMPLE_KEYS;
        }
        try {
            m_numberOfContextArrayKeys = Integer.parseInt(m_settings.get("numberOfContextArrayKeys"));
        } catch (NumberFormatException e) {
            m_numberOfContextArrayKeys = DEFAULT_NUM_OF_ARR_KEYS;
        }
        try {
            m_numberOfContextObjectKeys = Integer.parseInt(m_settings.get("numberOfContextObjectKeys"));
        } catch (NumberFormatException e) {
            m_numberOfContextObjectKeys = DEFAULT_NUM_OF_OBJECT_KEYS;
        }
        //5. Rule length
        try {
            m_ruleLength = Integer.parseInt(m_settings.get("ruleLength"));
        } catch (NumberFormatException e) {
            m_ruleLength = DEFAULT_RULE_LENGTH;
        }
        //5. Number of rules
        try {
            m_numberOfRules = Integer.parseInt(m_settings.get("numberOfRules"));
        } catch (NumberFormatException e) {
            m_numberOfRules = DEFAULT_NUM_OF_FEATURES;
        }

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
        //Make the first call to calculate features since it is the longest call and should not be taken into account when calculating performance
        long start = System.currentTimeMillis();
        try {
            new FeaturesCalculator().calculate(null, m_features, m_context, m_functions, m_translations, m_profileGroups, m_fallback, m_productVersion, null);
        } catch (ScriptInitException e) {
            Assert.fail("An exception was thrown when trying to execute calculate features method of the client engine. Message: " + e.getMessage());
        } catch (JSONException e) {
            Assert.fail("An exception was thrown when trying to execute calculate features method of the client engine. Message: " + e.getMessage());
        }
    }

    //1. Simple performance test with predefined data (get the path from the settings file)
    @Test
    public void simplePerformanceTestWithPredefinedData() {
        System.out.println("1. Simple performance test with predefined data. Data path: " + m_dataPath);
        try {
            long time = calculateFeaturesTest(m_features, m_context, m_functions, m_translations, m_profileGroups, m_fallback, m_productVersion, m_userRandomNumber);
            System.out.println("Time: " + time + " milliseconds.");
            Assert.assertTrue("1. Simple performance test with predefined data should take less than 16 ms.", time < 16);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //2. Performance depends on the number of features in the system
    @Test
    public void increaseFeaturesNumPerformanceTest() {
        System.out.println("2. Performance test depends on the number of features in the system. Number of features: " + m_numberOfFeatures);
        try {
            JSONObject features = new JSONObject(m_features.toString());
            JSONArray featuresArray = new JSONArray();
            JSONObject feature = (JSONObject) features.getJSONObject("root").getJSONArray("features").get(0);
            for (int i = 0; i < m_numberOfFeatures; i++) {
                JSONObject cloneFeature = new JSONObject(feature.toString());
                cloneFeature.put("name", feature.getString("name") + i);
                cloneFeature.put("uniqueId", feature.getString("uniqueId") + i);
                featuresArray.put(cloneFeature);
            }
            features.getJSONObject("root").put("features", featuresArray);

            long time = calculateFeaturesTest(features, m_context, m_functions, m_translations, m_profileGroups, m_fallback, m_productVersion, m_userRandomNumber);
            System.out.println("Time: " + time + " milliseconds.");
            Assert.assertTrue("2. Performance depends on the number of features in the system should take less than " + m_numberOfFeatures * 1.5 + " ms.", time < (m_numberOfFeatures * 1.5));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //2.1 Performance depends on the features size
    @Test
    public void increaseFeatureSizePerformanceTest() {
        System.out.println("2.1 Performance test depends on the size of a feature in the system.");
        System.out.println("Number of simple keys: " + m_numberOfFeatureSimpleKeys);
        System.out.println("Number of array keys: " + m_numberOfFeatureArrayKeys);
        System.out.println("Number of object keys: " + m_numberOfFeatureObjectKeys);
        try {
            JSONObject features = new JSONObject(m_features.toString());
            JSONObject feature = (JSONObject) features.getJSONObject("root").getJSONArray("features").get(0);
            feature = addSimpleKeys(feature, m_numberOfFeatureSimpleKeys);
            feature = addArrayKeys(feature, m_numberOfFeatureArrayKeys);
            feature = addObjectKeys(feature, m_numberOfFeatureObjectKeys);
            features.getJSONObject("root").getJSONArray("features").put(0, feature);

            long time = calculateFeaturesTest(features, m_context, m_functions, m_translations, m_profileGroups, m_fallback, m_productVersion, m_userRandomNumber);
            System.out.println("Time: " + time + " milliseconds.");
            Assert.assertTrue("2.1 Performance test depends on the size of a feature in the system should take less than 16 ms.", time < 16);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private JSONObject addSimpleKeys(JSONObject toAdd, int numOfKeys) throws JSONException {
        for (int i = 0; i < numOfKeys; i++) {
            toAdd.put("simpleKey" + i, "simpleValue");
        }
        return toAdd;
    }

    private JSONObject addArrayKeys(JSONObject toAdd, int numOfKeys) throws JSONException {
        for (int i = 0; i < numOfKeys; i++) {
            JSONArray arr = new JSONArray("[\"one\",\"two\",\"three\",\"four\",\"five\"]");
            toAdd.put("ArrayKey" + i, arr);
        }
        return toAdd;
    }

    private JSONObject addObjectKeys(JSONObject toAdd, int numOfKeys) throws JSONException {
        for (int i = 0; i < numOfKeys; i++) {
            JSONObject obj = new JSONObject("{\"obj-key1\": \"obj-val1\"," +
                    "\"obj-key2\": \"obj-val2\"," +
                    "\"obj-key3\": \"obj-val3\"}");
            toAdd.put("ObjectKey" + i, obj);
        }
        return toAdd;
    }


    //3.1 Performance depends on the context size.
    @Test
    public void increaseContextSizePerformanceTest() {
        System.out.println("3.1 Performance test depends on the context size.");
        System.out.println("Number of simple keys: " + m_numberOfContextSimpleKeys);
        System.out.println("Number of array keys: " + m_numberOfContextArrayKeys);
        System.out.println("Number of object keys: " + m_numberOfContextObjectKeys);
        try {
            JSONObject context = new JSONObject(m_context.toString());
            context = addSimpleKeys(context, m_numberOfContextSimpleKeys);
            context = addArrayKeys(context, m_numberOfContextArrayKeys);
            context = addObjectKeys(context, m_numberOfContextObjectKeys);
            File f = new File("./temp.txt");
            FileUtils.stringToFile(context.toString(), "./temp.txt");
            System.out.println("Context file size: " + (f.length() / 1024) + " KB");
            f.delete();
            long time = calculateFeaturesTest(m_features, context, m_functions, m_translations, m_profileGroups, m_fallback, m_productVersion, m_userRandomNumber);
            System.out.println("Time: " + time + " milliseconds.");
            Assert.assertTrue("3.1 Performance depends on the context size. should take less than 16 ms.", time < 16);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //4. Performance depends on the rule length (number of conditions)
    @Test
    public void ruleLengthPerformanceTest() {
        System.out.println("4. Performance depends on the rule length (number of conditions). Rule length: " + m_ruleLength);
        try {
            JSONObject context = new JSONObject(m_context.toString());
            context = addSimpleKeys(context, m_ruleLength);
            context = addArrayKeys(context, m_ruleLength);
            context = addObjectKeys(context, m_ruleLength);
            String rule = "";
            for (int i = 0; i < m_ruleLength; i++) {
                if (i != (m_ruleLength - 1)) {
                    rule += "context.simpleKey" + i + " ==\"simpleValue" + i + "\" |";
                } else {
                    rule += "context.simpleKey" + i + " ==\"simpleValue" + i + "\"";
                }
            }
            //put the rule in the feature
            JSONObject features = new JSONObject(m_features.toString());
            JSONObject feature = (JSONObject) features.getJSONObject("root").getJSONArray("features").get(0);
            feature.getJSONObject("rule").put("ruleString", rule);
            features.getJSONObject("root").getJSONArray("features").put(0, feature);

            long time = calculateFeaturesTest(features, context, m_functions, m_translations, m_profileGroups, m_fallback, m_productVersion, m_userRandomNumber);
            System.out.println("Time: " + time + " milliseconds.");
            Assert.assertTrue("4. Performance depends on the rule length (number of conditions) should take less than " + m_ruleLength * 2 + " ms.", time < (m_ruleLength * 2));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //TODO
    //5. Performance depends on the rule complexity (regex for example)

    //6. Performance depends on the profile size and the number of long rules in the system (many features with a significant rule)
    @Test
    public void increaseRulesNumPerformanceTest() {
        System.out.println("6. Performance depends on the profile size and the number of long rules in the system (many features with a significant rule). Number of rules: " + m_numberOfRules);
        try {
            JSONObject context = new JSONObject(m_context.toString());
            context = addSimpleKeys(context, m_ruleLength);
            context = addArrayKeys(context, m_ruleLength);
            context = addObjectKeys(context, m_ruleLength);
            String rule = "";
            for (int i = 0; i < m_ruleLength; i++) {
                if (i != (m_ruleLength - 1)) {
                    rule += "context.simpleKey" + i + " ==\"simpleValue" + i + "\" |";
                } else {
                    rule += "context.simpleKey" + i + " ==\"simpleValue" + i + "\"";
                }
            }

            //put the rule in the feature
            JSONObject features = new JSONObject(m_features.toString());
            JSONArray featuresArray = new JSONArray();
            JSONObject feature = (JSONObject) features.getJSONObject("root").getJSONArray("features").get(0);
            features.getJSONObject("root").getJSONArray("features").put(0, feature);

            for (int i = 0; i < m_numberOfFeatures; i++) {
                JSONObject cloneFeature = new JSONObject(feature.toString());
                cloneFeature.put("name", feature.getString("name") + i);
                cloneFeature.put("uniqueId", feature.getString("uniqueId") + i);
                cloneFeature.getJSONObject("rule").put("ruleString", rule);
                featuresArray.put(cloneFeature);
            }

            long time = calculateFeaturesTest(features, context, m_functions, m_translations, m_profileGroups, m_fallback, m_productVersion, m_userRandomNumber);
            System.out.println("Time: " + time + " milliseconds.");
            Assert.assertTrue("6. Performance depends on the profile size and the number of long rules in the system (many features with a significant rule) should take less than " + m_ruleLength * 2 + " ms.", time < (m_ruleLength * 2));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public long calculateFeaturesTest(JSONObject features, JSONObject deviceContext, String functions, JSONObject translations,
                                      List<String> profileGroups, Map<String, FeaturesCalculator.Fallback> fallback, String productVersion, int userRandomNumber) throws JSONException {
        long start = System.currentTimeMillis();
        // Map<String, Result> results = FeaturesCalculator.calculate(features, profile, deviceContext, profileGroups, fallback, productVersion, userRandomNumber);
        try {
            new FeaturesCalculator().calculate(null, features, deviceContext, functions, translations, profileGroups, fallback, productVersion, null);
        } catch (ScriptInitException e) {
            Assert.fail("An exception was thrown when trying to execute calculate features method of the client engine. Message: " + e.getMessage());
        }
        long time = System.currentTimeMillis() - start;
        return time;
    }
}
