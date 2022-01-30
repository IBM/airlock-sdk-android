package com.ibm.airlock.sdk.commons;

import com.github.peterwippermann.junit4.parameterizedsuite.ParameterizedSuite;
import com.ibm.airlock.common.test.AbstractBaseTest;
import com.ibm.airlock.common.test.long_run.percentage.NotificationPercentageRealTest;

import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Suite;

/**
 * Created by iditb on 26/12/17.
 */
@RunWith(ParameterizedSuite.class)
@Suite.SuiteClasses({
        NotificationPercentageRealTest.class,
        //FeaturePercentageRealTest.class,
        //StreamPercentageRealTest.class
})

public class AndroidTestsLongRunSuite {

    @Parameterized.Parameters(name = "Create test helper")
    public static Object[] params() {
        return new Object[][] {{new AndroidSdkBaseTest()}};
    }

    @Parameterized.Parameter
    public AbstractBaseTest baseTest ;
}
