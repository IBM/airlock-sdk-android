package com.weather.airlock.sdk.commons;

import android.content.res.Configuration;
import android.content.res.Resources;
import androidx.test.platform.app.InstrumentationRegistry;

import com.ibm.airlock.common.net.ConnectionManager;
import com.ibm.airlock.common.test.AbstractBaseTest;
import com.ibm.airlock.common.util.Constants;
import com.weather.airlock.sdk.AirlockManager;
import com.weather.airlock.sdk.cache.AndroidContext;
import com.weather.airlock.sdk.net.AndroidOkHttpClientBuilder;

import org.json.JSONException;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.util.Locale;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;

/**
 * Created by iditb on 20/11/17.
 */

public class AndroidSdkBaseTest extends AbstractBaseTest {


    public AndroidSdkBaseTest(){
    }


    @Override
    public void setUpMockups() throws JSONException {
        manager = AirlockManager.getInstance();
        MockitoAnnotations.initMocks(this);
        mockedContext = Mockito.spy(new AndroidContext(InstrumentationRegistry.getInstrumentation().getContext()));

        doReturn(getDefaultFile()).when(mockedContext).openRawResource(any(Integer.class));
        Mockito.when(mockedContext.openRawResource(any(Integer.class))).thenReturn(getDefaultFile());
        mockedContext.getSharedPreferences(Constants.SP_NAME, android.content.Context.MODE_PRIVATE);
    }


    @Override
    public String getDataFileContent(String pathInDataFolder) throws IOException {
        return (new AndroidSdkTestDataManager()).getFileContent(pathInDataFolder);
    }

    @Override
    public String[] getDataFileNames(String directoryPathInDataFolder) throws IOException {
           return (new AndroidSdkTestDataManager()).getFileNamesListFromDirectory(directoryPathInDataFolder);
    }
    @Override
    protected ConnectionManager getConnectionManager(){
        return new ConnectionManager(new AndroidOkHttpClientBuilder(true));
    }
    @Override
    protected ConnectionManager getConnectionManager(String m_key){
        return new ConnectionManager(new AndroidOkHttpClientBuilder(true), m_key);
    }

    @Override
    public void setLocale(Locale locale) {
        Resources resources = InstrumentationRegistry.getInstrumentation().getTargetContext().getResources();
        Locale.setDefault(locale);
        Configuration config = resources.getConfiguration();
        config.locale = locale;
        resources.updateConfiguration(config, resources.getDisplayMetrics());
    }

    @Override
    public String getTestName() {
        return null;
    }
}
