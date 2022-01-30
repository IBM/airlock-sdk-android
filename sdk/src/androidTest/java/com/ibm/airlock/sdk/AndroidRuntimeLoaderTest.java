package com.ibm.airlock.sdk;

import androidx.test.platform.app.InstrumentationRegistry;

import com.ibm.airlock.common.cache.Context;
import com.ibm.airlock.common.util.Constants;
import com.ibm.airlock.sdk.AirlockManager;
import com.ibm.airlock.sdk.AndroidRuntimeLoader;
import com.ibm.airlock.sdk.cache.AndroidContext;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

public class AndroidRuntimeLoaderTest {

    @Test
    public void readRuntimeFile() {


        AirlockManager manager = AirlockManager.getInstance();
        MockitoAnnotations.initMocks(this);
        Context mockedContext = Mockito.spy(new AndroidContext(InstrumentationRegistry.getInstrumentation().getContext()));

        mockedContext.getSharedPreferences(Constants.SP_NAME, android.content.Context.MODE_PRIVATE);
        AndroidRuntimeLoader androidRuntimeLoader = new AndroidRuntimeLoader("runtime","",InstrumentationRegistry.getInstrumentation().getContext());
        try {
            manager.initSDK(mockedContext, androidRuntimeLoader, "");
        }catch (Exception ex){
            Assert.fail(ex.getMessage());
        }

    }

    @Test
    public void readEncryptedRuntimeFile() {

        AirlockManager manager = AirlockManager.getInstance();
        MockitoAnnotations.initMocks(this);
        Context mockedContext = Mockito.spy(new AndroidContext(InstrumentationRegistry.getInstrumentation().getContext()));

        mockedContext.getSharedPreferences(Constants.SP_NAME, android.content.Context.MODE_PRIVATE);
        AndroidRuntimeLoader androidRuntimeLoader = new AndroidRuntimeLoader("runtime/encrypted","TNHI3XTLNXCMDIZ6",InstrumentationRegistry.getInstrumentation().getContext());
        try{
        manager.initSDK(mockedContext, androidRuntimeLoader, "TNHI3XTLNXCMDIZ6");
        }catch (Exception ex){
            Assert.fail(ex.getMessage());
        }
    }
}