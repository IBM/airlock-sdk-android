package com.weather.airlock.sdk.commons;

import android.content.Context;
import android.content.res.AssetManager;
import androidx.test.platform.app.InstrumentationRegistry;

import com.ibm.airlock.common.test.AbstractTestDataManager;
import com.weather.airlock.sdk.commons.utils.AndroidTestUtils;

import java.io.IOException;

/**
 * Created by iditb on 20/11/17.
 */

public class AndroidSdkTestDataManager implements AbstractTestDataManager {

    public AndroidSdkTestDataManager(){}

    @Override
    public String getFileContent(String pathInDataFolder) throws IOException {
        return AndroidTestUtils.readFromAssets(pathInDataFolder);
    }

    @Override
    public String[] getFileNamesListFromDirectory(String dirPathUnderDataFolder) throws IOException {
        Context testContext = InstrumentationRegistry.getInstrumentation().getContext();
        AssetManager mng = testContext.getAssets();
        String[] list = mng.list(dirPathUnderDataFolder);
        return  list ;
    }

}
