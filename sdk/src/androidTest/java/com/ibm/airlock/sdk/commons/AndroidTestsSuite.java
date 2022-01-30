package com.ibm.airlock.sdk.commons;

import com.github.peterwippermann.junit4.parameterizedsuite.ParameterizedSuite;
import com.ibm.airlock.common.test.AbstractBaseTest;
import com.ibm.airlock.common.test.functional.AnalyticsTest;
import com.ibm.airlock.common.test.functional.BasicEntitlementsTest;
import com.ibm.airlock.common.test.functional.DevProdSeparationTest;
import com.ibm.airlock.common.test.functional.FeatureOrderingTest;
import com.ibm.airlock.common.test.functional.FeaturesListTreeTest;
import com.ibm.airlock.common.test.functional.ManagerBasicTest;
import com.ibm.airlock.common.test.functional.MinMaxVersionTest;
import com.ibm.airlock.common.test.functional.NotificationsDevTest;
import com.ibm.airlock.common.test.functional.NotificationsQATest;
import com.ibm.airlock.common.test.functional.RePullProdFeaturesTest;
import com.ibm.airlock.common.test.functional.SetLocaleTest;
import com.ibm.airlock.common.test.functional.StreamsDevTest;
import com.ibm.airlock.common.test.functional.StreamsQATest;
import com.ibm.airlock.common.test.functional.UserGroupsTest;
import com.ibm.airlock.common.test.golds_machine.GoldsTester;
import com.ibm.airlock.common.test.regressions.BranchesDiffBugRegTest;
import com.ibm.airlock.common.test.regressions.PercentageUpgradeRegTest;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Suite;


/**
 * Created by Denis Voloshin on 20/11/17.
 */
@RunWith(ParameterizedSuite.class)
@Suite.SuiteClasses({
        BasicEntitlementsTest.class,
        NotificationsQATest.class,
        NotificationsDevTest.class,
        PercentageUpgradeRegTest.class,
        MinMaxVersionTest.class,
        GoldsTester.class,
        FeatureOrderingTest.class,
        SetLocaleTest.class,
        ManagerBasicTest.class,
        StreamsDevTest.class,
        RePullProdFeaturesTest.class,
        DevProdSeparationTest.class,
        FeaturesListTreeTest.class,
        StreamsQATest.class,
        UserGroupsTest.class,
        AnalyticsTest.class,
        BranchesDiffBugRegTest.class
})
public class AndroidTestsSuite {

    @Parameterized.Parameters(name = "Create test helper")
    public static Object[] params() {
        return new Object[][] {{new AndroidSdkBaseTest()}};
    }

    @Parameterized.Parameter
    public AbstractBaseTest baseTest ;
}

