package com.weather.airlock.sdk.features;

import com.ibm.airlock.common.data.Feature;
import com.ibm.airlock.common.data.FeaturesList;
import com.weather.airlock.sdk.AirlockManager;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;


/**
 * Created by iditb on 14/03/2017.
 */

public class FeaturesListMultiThreadingTest {

    public FeaturesList m_featuresList;
    public FeaturesList m_toMerge;


    @Before
    public void setUp(){
        // init log
        AirlockManager.getInstance();
    }

    @Test
    public void seqSimpleAddMerge() {
        m_featuresList = new FeaturesList("{\"root\":{\"isON\":false,\"source\":\"SERVER\",\"features\":[{\"fullName\":\"ns.f1\",\"isON\":true,\"source\":\"SERVER\",\"type\":\"FEATURE\",\"defaultIfAirlockSystemIsDown\": false,\"featureAttributes\":{},\"sendToAnalytics\":false},{\"fullName\":\"f2.f3\",\"isON\":true,\"source\":\"SERVER\",\"type\":\"FEATURE\",\"defaultIfAirlockSystemIsDown\": false,\"featureAttributes\":{},\"sendToAnalytics\":false},{\"fullName\":\"f4.f5\",\"isON\":false,\"source\":\"CACHE\",\"type\":\"FEATURE\",\"defaultIfAirlockSystemIsDown\": false,\"sendToAnalytics\":false},{\"fullName\":\"f2.f4\",\"isON\":false,\"source\":\"SERVER\",\"type\":\"FEATURE\",\"defaultIfAirlockSystemIsDown\": false,\"sendToAnalytics\":false},{\"fullName\":\"ns.f2\",\"isON\":true,\"source\":\"SERVER\",\"type\":\"FEATURE\",\"defaultIfAirlockSystemIsDown\": false,\"featureAttributes\":{},\"sendToAnalytics\":false}]}}", Feature.Source.DEFAULT);
        int size = m_featuresList.size();
        m_toMerge = new FeaturesList("{\"root\":{\"isON\":false,\"source\":\"SERVER\",\"features\":[{\"fullName\":\"ns.f12\",\"isON\":true,\"source\":\"SERVER\",\"type\":\"FEATURE\",\"defaultIfAirlockSystemIsDown\": false,\"featureAttributes\":{},\"sendToAnalytics\":false}]}}", Feature.Source.DEFAULT);
        m_featuresList.merge(m_toMerge);
        m_featuresList.put("ThreeUp.V1", new Feature("ThreeUp.V1", true, Feature.Source.DEFAULT));
        Assert.assertTrue("List should have two more elements now", m_featuresList.size() == (size + 2));
    }

    @Test
    public void concSimpleAddMergeEmptyListContructor() {
        m_featuresList = new FeaturesList();
        Feature parent = new Feature("ThreeUp.V1", true, Feature.Source.DEFAULT);
        Feature f = new Feature("ThreeUp.V1.f1", true, Feature.Source.DEFAULT);
        f.setParent(parent);
        parent.addUpdateChild(f);
        m_featuresList.put("ThreeUp.V1", parent);
        m_featuresList.put("ThreeUp.V1.f1", f);
        for (int i = 0; i < 5; i++) {
            m_featuresList.put("f" + i, new Feature("f" + i, true, Feature.Source.DEFAULT));
        }
        int size = m_featuresList.size();
        m_toMerge = new FeaturesList();
        Feature parent2 = new Feature("ThreeUp.V2", true, Feature.Source.DEFAULT);
        Feature f2 = new Feature("ThreeUp.V2.f1", true, Feature.Source.DEFAULT);
        f2.setParent(parent2);
        parent2.addUpdateChild(f2);
        m_toMerge.put("ThreeUp.V2", parent2);
        m_toMerge.put("ThreeUp.V2.f1", f2);
        for (int i = 0; i < 5; i++) {
            m_toMerge.put("f" + i, new Feature("f" + i, true, Feature.Source.SERVER));
        }
        Thread merge = new mergeThread();
        Thread add = new addToListThread(new Feature("ThreeUp.V3", true, Feature.Source.DEFAULT));

        merge.start();
        add.start();

        try {
            merge.join();
            add.join();
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }

        Assert.assertTrue("Verify add thread", m_featuresList.containsKey("ThreeUp.V3"));
        Assert.assertTrue("List should have two more elements now", m_featuresList.size() >= (size + 2));
    }

    public class addToListThread extends Thread {

        Feature m_feature;

        public addToListThread(Feature f) {
            m_feature = f;
        }

        public void run() {
            m_featuresList.put(m_feature.getName(), m_feature);
        }
    }

    public class mergeThread extends Thread {

        public void run() {
            m_featuresList.merge(m_toMerge);
        }
    }

    public class toJsonObjectThread extends Thread {
        public void run() {
            m_featuresList.toJsonObject();
        }
    }
}

/*
DRAFTS


1.______________________________________
public class addToListThread extends Thread {

        String name ;
        public addToListThread(String fName){
            name = fName ;
        }
        public void run(){
            Feature f = new Feature(name, true, Feature.Source.DEFAULT) ;
            Feature parent = new Feature("p_"+name,true, Feature.Source.DEFAULT);
            parent.addChild(f);
            Feature[] fs = {
                    new Feature(name+"_1", true, Feature.Source.DEFAULT),
                    new Feature(name+"_2", true, Feature.Source.DEFAULT),
                    new Feature(name+"_3", true, Feature.Source.DEFAULT),
                    new Feature(name+"_4", true, Feature.Source.DEFAULT),
                    new Feature(name+"_5", true, Feature.Source.DEFAULT),
                    new Feature(name+"_6", true, Feature.Source.DEFAULT),
                    new Feature(name+"_7", true, Feature.Source.DEFAULT),
                    new Feature(name+"_8", true, Feature.Source.DEFAULT),
                    new Feature(name+"_9", true, Feature.Source.DEFAULT),
                    new Feature(name+"_10", true, Feature.Source.DEFAULT),
                    new Feature(name+"_11", true, Feature.Source.DEFAULT),
                    new Feature(name+"_12", true, Feature.Source.DEFAULT)
            };
            m_featuresList.put(name,f);
            m_featuresList.put(parent.getName(),parent);
            for(int i=0;i<fs.length;i++)
                m_featuresList.put(fs[i].getName(),fs[i]);
        }

    }

2.______________________________________________
@Test
    public void concMergeAddTest(){
        m_featuresList = new FeaturesList("{\"root\":{\"isON\":false,\"source\":\"SERVER\",\"features\":[{\"fullName\":\"ns.f1\",\"isON\":true,\"source\":\"SERVER\",\"type\":\"FEATURE\",\"featureAttributes\":{},\"sendToAnalytics\":false},{\"fullName\":\"f2.f3\",\"isON\":true,\"source\":\"SERVER\",\"type\":\"FEATURE\",\"featureAttributes\":{},\"sendToAnalytics\":false},{\"fullName\":\"f4.f5\",\"isON\":false,\"source\":\"CACHE\",\"type\":\"FEATURE\",\"sendToAnalytics\":false},{\"fullName\":\"f2.f4\",\"isON\":false,\"source\":\"SERVER\",\"type\":\"FEATURE\",\"sendToAnalytics\":false},{\"fullName\":\"ns.f2\",\"isON\":true,\"source\":\"SERVER\",\"type\":\"FEATURE\",\"featureAttributes\":{},\"sendToAnalytics\":false}]}}", Feature.Source.DEFAULT);
        m_toMerge = new FeaturesList();
        Feature parent2 = new Feature("ThreeUp.V1", true, Feature.Source.DEFAULT) ;
        Feature f2 = new Feature("ThreeUp.V1.f2", true, Feature.Source.DEFAULT) ;
        f2.setParent(parent2);
        parent2.addChild(f2);
        m_toMerge.put("ThreeUp.V1.f2",f2);
        Feature[] fs = {
                new Feature("ThreeUp.V1.f3", true, Feature.Source.DEFAULT),
                new Feature("ThreeUp.V1.f4", true, Feature.Source.DEFAULT),
                new Feature("ThreeUp.V1.f5", true, Feature.Source.DEFAULT),
                new Feature("ThreeUp.V1.f6", true, Feature.Source.DEFAULT),
                new Feature("ThreeUp.V1.f7", true, Feature.Source.DEFAULT),
                new Feature("ThreeUp.V1.f8", true, Feature.Source.DEFAULT),
                new Feature("ThreeUp.V1.f9", true, Feature.Source.DEFAULT),
                new Feature("ThreeUp.V1.f10", true, Feature.Source.DEFAULT),
                new Feature("ThreeUp.V1.f11", true, Feature.Source.DEFAULT),
                new Feature("ThreeUp.V1.f12", true, Feature.Source.DEFAULT),
                new Feature("ThreeUp.V1.f13", true, Feature.Source.DEFAULT),
                new Feature("ThreeUp.V1.f14", true, Feature.Source.DEFAULT),
                new Feature("ThreeUp.V1.f15", true, Feature.Source.DEFAULT)
        };

        for (int i=0;i<fs.length;i++)
            m_toMerge.put(fs[i].getName(),fs[i]);


        Thread merge = new mergeThread();
        Thread[] add ={
                new addToListThread("fAdd1"),
                new addToListThread("fAdd2"),
                new addToListThread("fAdd3"),
                new addToListThread("fAdd4"),
                new addToListThread("fAdd5"),
                new addToListThread("fAdd6"),
                new addToListThread("fAdd7"),
                new addToListThread("fAdd8"),
                new addToListThread("fAdd9"),
                new addToListThread("fAdd10"),
                new addToListThread("fAdd11"),
                new addToListThread("fAdd12"),
                new addToListThread("fAdd13"),
                new addToListThread("fAdd14")
        } ;

        merge.start();
        for (int i=0;i<add.length;i++){
            add[i].start();
        }

        Thread[] toJson = {
                new toJsonObjectThread(),
                new toJsonObjectThread(),
                new toJsonObjectThread(),
                new toJsonObjectThread(),
                new toJsonObjectThread(),
                new toJsonObjectThread(),
                new toJsonObjectThread(),
                new toJsonObjectThread(),
                new toJsonObjectThread(),
                new toJsonObjectThread(),
                new toJsonObjectThread()
        };
        for (int i=0;i<toJson.length;i++)
            toJson[i].start();

        try {
            merge.join();
            for (int i=0;i<add.length;i++){
                add[i].join();
            }
            for (int i=0;i<toJson.length;i++){
                toJson[i].join();
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        System.out.println("DEBUG: "+m_featuresList.toString());
    }
 */