package com.weather.airlock.sdk.manager;

import java.util.concurrent.CountDownLatch;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;
import com.ibm.airlock.common.AirlockCallback;
import com.ibm.airlock.common.AirlockNotInitializedException;
import com.ibm.airlock.common.data.Feature;
import com.weather.airlock.sdk.AirlockManager;
import junit.framework.Assert;


/**
 * Created by iditb on 09/11/2016.
 */
public class NoInitNegativeTest {

    private boolean m_success = false;
    private String m_failMessage = null;

    @Test(expected = AirlockNotInitializedException.class)
    public void noInitPullFeaturesTest() throws AirlockNotInitializedException {

        AirlockManager am = AirlockManager.getInstance();
        am.pullFeatures(new AirlockCallback() {
            @Override
            public void onFailure(Exception e) {

            }

            @Override
            public void onSuccess(String msg) {

            }
        });
    }

    @Test(expected = AirlockNotInitializedException.class)
    public void noInitCalculateFeaturesTest() throws AirlockNotInitializedException, JSONException {
        AirlockManager am = AirlockManager.getInstance();
        JSONObject json = new JSONObject("{a:b}");
        am.calculateFeatures(json, json);
    }

    @Test(expected = AirlockNotInitializedException.class)
    public void noInitSyncFeaturesTest() throws AirlockNotInitializedException {
        AirlockManager am = AirlockManager.getInstance();
        am.syncFeatures();
    }

    @Test
    public void noInitGetFeatureTest() {
        AirlockManager am = AirlockManager.getInstance();
        Feature f = am.getFeature("Multi Location Home Screen");
        Assert.assertTrue("isOn = false is expected when calling getFeature without calling initSDK before.", f.isOn() == false);
        Feature.Source s = f.getSource();
        Assert.assertTrue("MISSING name is expected when calling getFeature without calling initSDK before.", s.name().equals("MISSING"));
    }

    @Test
    public void noInitGetServerUserGroupTest() {
        AirlockManager m = AirlockManager.getInstance();
        final CountDownLatch latch = new CountDownLatch(1);
        m.getServerUserGroups(new AirlockCallback() {
            @Override
            public void onFailure(Exception e) {
                m_failMessage = e.getMessage();
                latch.countDown();
            }

            @Override
            public void onSuccess(String msg) {
                m_success = true;
                latch.countDown();
            }
        });
        try {
            latch.await();
        } catch (InterruptedException e) {
            Assert.fail(e.getMessage());
        }
        if (m_success) {
            Assert.fail("Exception is expected when trying to get the server user group before init sdk.");
        }
        Assert.assertTrue(m_failMessage != null);
        Assert.assertTrue(m_failMessage.contains("Call the InitSdk method before any other calls"));
    }

    @Test
    public void noInitGetSessionAndProcuctIdShouldBeNull() {
        AirlockManager am = AirlockManager.getInstance();
        Assert.assertTrue("Session should be null if call AirlockManager.getSessionId before initSDK", am.getSeasonId() == null);
        Assert.assertTrue("Session should be null if call AirlockManager.getProductId before initSDK", am.getProductId() == null);
    }
}
