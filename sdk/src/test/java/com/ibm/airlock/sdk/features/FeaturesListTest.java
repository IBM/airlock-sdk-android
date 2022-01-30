package com.ibm.airlock.sdk.features;

import com.ibm.airlock.common.data.Feature;
import com.ibm.airlock.common.data.FeaturesList;

import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Test;


/**
 * Created by iditb on 22/11/2016.
 */
public class FeaturesListTest {



  /*  @Test
    public void createFetureListWithNull(){
    FeaturesList fl = new FeaturesList(null, Feature.Source.DEFAULT);
        Assert.assertTrue(fl!=null);
        Assert.assertTrue(fl.size()==0);
        Assert.assertTrue(fl.getFeatures()!=null);
        Assert.assertTrue(fl.getFeatures().size()==0);
    }*/

    @Test
    public void createEmptyFeaturesListTest() {
        FeaturesList fl = new FeaturesList();
        Assert.assertTrue("getFeatures method returned null value after creating a FeaturesList.", fl.getFeatures() != null);
        Assert.assertTrue("Empty features list is expected.", fl.getFeatures().isEmpty());
        Assert.assertTrue("0 size features list is expected. ", fl.getFeatures().size() == 0);
    }

    @Test
    public void getNotExistFeatureTest() {
        FeaturesList fl = new FeaturesList();
        Feature f = fl.getFeature("notExist");
        Assert.assertTrue("Null was returned when trying to get a non exist feature", f != null);
        Assert.assertTrue("MISSING source is expected when trying to get a non exist feature", f.getSource().name().equals("MISSING"));
        Assert.assertTrue("isOn = false is expected when trying to get a non exist feature", !f.isOn());
    }

    @Test
    public void getNullFeatureTest() {
        FeaturesList fl = new FeaturesList();
        Feature f = fl.getFeature(null);
        Assert.assertTrue("Null returned value is expected when trying to get a null feature", f == null);
    }

    @Test
    public void containsNullFeatureTest() {
        FeaturesList fl = new FeaturesList();
        boolean b = fl.containsKey(null);
        Assert.assertTrue("False returned value is expected when trying to ask for a null contains key", b == false);
    }

    @Test
    public void putNullKeyFeatureTest() {
        FeaturesList fl = new FeaturesList();
        Feature f = new Feature("ThreeUp.V1.NullKey", true, Feature.Source.DEFAULT);
        fl.put(null, f);
        Assert.assertTrue("The features list should not be updated.", fl.size() == 0);
        Feature g = fl.getFeature("ThreeUp.V1.NullKey");
        Assert.assertTrue(g != null);
        Assert.assertTrue(g.getSource().equals(Feature.Source.MISSING));
    }

    @Test
    public void putEmptyKeyFeatureTest() {
        FeaturesList fl = new FeaturesList();
        Feature f = new Feature("ThreeUp.V1.NullKey", true, Feature.Source.DEFAULT);
        fl.put("", f);
        Assert.assertTrue("The features list should not be updated.", fl.size() == 0);
        Feature g = fl.getFeature("ThreeUp.V1.NullKey");
        Assert.assertTrue(g != null);
        Assert.assertTrue(g.getSource().equals(Feature.Source.MISSING));
    }

    @Test
    public void putNullFeatureTest() {
        FeaturesList fl = new FeaturesList();
        fl.put("ThreeUp.V1.NULL", null);
        Assert.assertTrue("The features list should not be updated.", fl.size() == 0);
        Feature g = fl.getFeature("ThreeUp.V1.NULL");
        Assert.assertTrue(g != null);
        Assert.assertTrue(g.getSource().equals(Feature.Source.MISSING));
    }

    @Test
    public void putFeatureWithNoChildrenNoParentTest() {
        FeaturesList fl = new FeaturesList();
        Feature f = new Feature("ThreeUp.V1", true, Feature.Source.DEFAULT);
        fl.put("ThreeUp.V1", f);
        Assert.assertTrue("The features list should have size = 1 now.", fl.size() == 1);
        Feature g = fl.getFeature("ThreeUp.V1");
        Assert.assertTrue("Null was returned when trying to get a feature that was just added", g != null);
        Assert.assertTrue("\"ThreeUp.V1\" feature name is expected", g.getName().equals("ThreeUp.V1"));
        Assert.assertTrue("\"Feature.Source.DEFAULT feature source is expected", g.getSource().equals(Feature.Source.DEFAULT));
        Assert.assertTrue("isOn = true is expected", g.isOn());
    }

    @Test
    public void putFeatureWithParentTest() {
        FeaturesList fl = new FeaturesList();
        Feature parent = new Feature("ThreeUp.V1", true, Feature.Source.DEFAULT);
        Feature f = new Feature("ThreeUp.V1.f1", true, Feature.Source.DEFAULT);
        f.setParent(parent);
        parent.addUpdateChild(f);
        fl.put("ThreeUp.V1", parent);
        fl.put("ThreeUp.V1.f1", f);
        //Test the feature list was updated
        Assert.assertTrue("The features list should have size = 2 now.", fl.size() == 2);
        Assert.assertTrue("The features list should have a non empty features map now.", !fl.getFeatures().isEmpty());
        Assert.assertTrue("The features list should have a  features map of size = 2 now.", fl.getFeatures().size() == 2);
        //TODO Ask Rachel about lower-upper cases
        Assert.assertTrue("The features list should contain the parent name as a key in the features map", fl.containsKey("ThreeUp.V1"));
        Assert.assertTrue("The features list should contain the child name as a key in the features map", fl.containsKey("ThreeUp.V1.f1"));
        //Test getting the parent feature
        Feature g = fl.getFeature("ThreeUp.V1");
        Assert.assertTrue("Null was returned when trying to get a feature that was just added", g != null);
        Assert.assertTrue("\"ThreeUp.V1\" feature name is expected", g.getName().equals("ThreeUp.V1"));
        Assert.assertTrue("\"Feature.Source.DEFAULT feature source is expected", g.getSource().equals(Feature.Source.DEFAULT));
        Assert.assertTrue("isOn = true is expected", g.isOn());
        Assert.assertTrue("Null was returned when trying to get children of the parent feature.", g.getChildren() != null);
        Assert.assertTrue("A non-empty children list is expected for the parent feature.", !g.getChildren().isEmpty());
        Assert.assertTrue("A size = 1 children list is expected for the parent feature.", g.getChildren().size() == 1);
        /* Test children addition*/
        Feature c = fl.getFeature("ThreeUp.V1.f1");
        Assert.assertTrue("Null was returned when trying to get a feature that was just added", c != null);
        Assert.assertTrue("\"ThreeUp.V1.f1\" feature name is expected", c.getName().equals("ThreeUp.V1.f1"));
        Assert.assertTrue("\"Feature.Source.DEFAULT feature source is expected", c.getSource().equals(Feature.Source.DEFAULT));
        Assert.assertTrue("isOn = true is expected", c.isOn());
        Assert.assertTrue("Null was returned when trying to get parent .", c.getParent() != null);
    }

    @Test
    public void putFeatureWith2ChildrenTest() {
        FeaturesList fl = new FeaturesList();
        Feature parent = new Feature("ThreeUp.V1", true, Feature.Source.DEFAULT);
        Feature f1 = new Feature("ThreeUp.V1.f1", true, Feature.Source.DEFAULT);
        f1.setParent(parent);
        Feature f2 = new Feature("ThreeUp.V1.f2", true, Feature.Source.DEFAULT);
        f2.setParent(parent);
        parent.addUpdateChild(f1);
        parent.addUpdateChild(f2);
        fl.put("ThreeUp.V1", parent);
        fl.put("ThreeUp.V1.f1", f1);
        fl.put("ThreeUp.V1.f2", f2);
        //Test getting the parent feature
        Feature p = fl.getFeature("ThreeUp.V1");
        //Test getting the child via parent
        Assert.assertTrue("Null was returned when trying to get a feature that was just added", p != null);
        Assert.assertTrue("\"ThreeUp.V1\" feature name is expected", p.getName().equals("ThreeUp.V1"));
        Assert.assertTrue("\"Feature.Source.DEFAULT feature source is expected", p.getSource().equals(Feature.Source.DEFAULT));
        Assert.assertTrue("isOn = true is expected", p.isOn());
        Assert.assertTrue("Null was returned when trying to get children of the parent feature.", p.getChildren() != null);
        Assert.assertTrue("A non-empty children list is expected for the parent feature.", !p.getChildren().isEmpty());
        Assert.assertTrue("A size = 2 children list is expected for the parent feature.", p.getChildren().size() == 2);
//        Assert.assertTrue("\"ThreeUp.V1.f1\" feature is expected as a single feature in the children map.",p.getChildren().get(0).getName().equalsIgnoreCase("ThreeUp.V1.f1"));
        /* Test children addition*/
        Feature c = fl.getFeature("ThreeUp.V1.f1");
        Assert.assertTrue("Null was returned when trying to get a feature that was just added", c != null);
        Assert.assertTrue("\"ThreeUp.V1.f1\" feature name is expected", c.getName().equals("ThreeUp.V1.f1"));
        Assert.assertTrue("\"Feature.Source.DEFAULT feature source is expected", c.getSource().equals(Feature.Source.DEFAULT));
        Assert.assertTrue("isOn = true is expected", c.isOn());
        Assert.assertTrue("Null was returned when trying to get parent .", c.getParent() != null);
        Assert.assertTrue("\"ThreeUp.V1\" parent name of the child feature is expected .", c.getParent().getName().equalsIgnoreCase("ThreeUp.V1"));
    }

    @Test
    public void clearFeaturesListTest() {
        FeaturesList fl = new FeaturesList();
        Feature parent = new Feature("ThreeUp.V1", true, Feature.Source.DEFAULT);
        Feature f1 = new Feature("ThreeUp.V1.f1", true, Feature.Source.DEFAULT);
        f1.setParent(parent);
        Feature f2 = new Feature("ThreeUp.V1.f2", true, Feature.Source.DEFAULT);
        f2.setParent(parent);
        parent.addUpdateChild(f1);
        parent.addUpdateChild(f2);
        fl.put("ThreeUp.V1", parent);
        fl.put("ThreeUp.V1.f1", f1);
        fl.put("ThreeUp.V1.f1", f2);
        //Test getting the parent feature
        Feature p = fl.getFeature("ThreeUp.V1");
        //Test getting the child via parent
        Assert.assertTrue("Null was returned when trying to get a feature that was just added", p != null);
        Assert.assertTrue("A size = 2 children list is expected for the parent feature.", p.getChildren().size() == 2);
        //Try clear the list
        fl.clear();
        Assert.assertTrue("A features list of size = 0 is expected after clear.", fl.size() == 0);
        Assert.assertTrue("A features map of size = 0 is expected after clear.", fl.getFeatures().size() == 0);
        Feature f = fl.getFeature("ThreeUp.V1");
        Assert.assertTrue("Null was returned when trying to get a non exist feature", f != null);
        Assert.assertTrue("MISSING source is expected when trying to get a non exist feature", f.getSource().name().equals("MISSING"));
        Assert.assertTrue("isOn = false is expected when trying to get a non exist feature", !f.isOn());
    }

    @Test
    public void simpleMergeTest() {
        FeaturesList fl1 = new FeaturesList();
        Feature parent = new Feature("ThreeUp.V1", true, Feature.Source.DEFAULT);
        Feature f = new Feature("ThreeUp.V1.f1", true, Feature.Source.DEFAULT);
        f.setParent(parent);
        parent.addUpdateChild(f);
        fl1.put("ThreeUp.V1", parent);
        fl1.put("ThreeUp.V1.f1", f);
        FeaturesList fl2 = new FeaturesList();
        Feature parent2 = new Feature("ThreeUp.V1", true, Feature.Source.DEFAULT);
        Feature f2 = new Feature("ThreeUp.V1.f2", true, Feature.Source.DEFAULT);
        f2.setParent(parent2);
        parent2.addUpdateChild(f2);
        fl2.put("ThreeUp.V1", parent2);
        fl2.put("ThreeUp.V1.f2", f2);
        //Now merge
        fl1.merge(fl2);
        System.out.println(fl1);
        Assert.assertTrue("A size = 3 features list is expected.", fl1.size() == 3);
        Assert.assertTrue("ThreeUp.V1.f2 feature should appear in the merged list.", fl1.getFeature("ThreeUp.V1.f2") != null);
        Assert.assertTrue("ThreeUp.V1 feature should appear in the merged list.", fl1.containsKey("ThreeUp.V1"));
        Assert.assertTrue("ThreeUp.V1.f1 feature should appear in the merged list.", fl1.containsKey("ThreeUp.V1.f1"));
    }

    @Test
    public void simpleMergeServerSourceTest() {
        FeaturesList fl1 = new FeaturesList();
        Feature parent = new Feature("ThreeUp.V1", true, Feature.Source.DEFAULT);
        Feature f = new Feature("ThreeUp.V1.f1", true, Feature.Source.DEFAULT);
        f.setParent(parent);
        parent.addUpdateChild(f);
        fl1.put("ThreeUp.V1", parent);
        fl1.put("ThreeUp.V1.f1", f);
        FeaturesList fl2 = new FeaturesList();
        Feature parent2 = new Feature("ThreeUp.V1", true, Feature.Source.DEFAULT);
        Feature f2 = new Feature("ThreeUp.V1.f2", true, Feature.Source.DEFAULT);
        f2.setParent(parent2);
        parent2.addUpdateChild(f2);
        Feature f3 = new Feature("ThreeUp.V1.f3", true, Feature.Source.DEFAULT);
        f3.setParent(parent2);
        parent2.addUpdateChild(f3);
        fl2.put("ThreeUp.V1", parent2);
        fl2.put("ThreeUp.V1.f2", f2);
        fl2.put("ThreeUp.V1.f3", f3);
        f.setSource(Feature.Source.SERVER);
        fl2.put("ThreeUp.V1.f1", f);
        //Now merge
        fl1.merge(fl2);
        Assert.assertTrue("A size = 4 features list is expected.", fl1.size() == 4);
        Assert.assertTrue("ThreeUp.V1.f2 feature should appear in the merged list.", fl1.getFeature("ThreeUp.V1.f2") != null);
        Assert.assertTrue("ThreeUp.V1.f2 feature should appear in the merged list.", fl1.getFeature("ThreeUp.V1.f3") != null);
        Assert.assertTrue("ThreeUp.V1 feature should appear in the merged list.", fl1.containsKey("ThreeUp.V1"));
        Assert.assertTrue("ThreeUp.V1.f1 feature should appear in the merged list.", fl1.containsKey("ThreeUp.V1.f1"));
        Assert.assertTrue("ThreeUp.V1.f1 feature source = SERVER should appear in the merged list.", fl1.getFeature("ThreeUp.V1.f1").getSource().equals(Feature.Source.SERVER));
    }

    @Test
    public void mergeChangeParentTest() {
        FeaturesList fl1 = new FeaturesList();
        Feature parent = new Feature("ThreeUp.V1", true, Feature.Source.DEFAULT);
        Feature f = new Feature("ThreeUp.V1.f1", true, Feature.Source.DEFAULT);
        f.setParent(parent);
        parent.addUpdateChild(f);
        fl1.put("ThreeUp.V1", parent);
        fl1.put("ThreeUp.V1.f1", f);
        //Feature parent2 = new Feature("ThreeUp.V1", true, Feature.Source.DEFAULT) ;
        Feature f2 = new Feature("ThreeUp.V1.f2", true, Feature.Source.DEFAULT);
        f2.setParent(parent);
        parent.addUpdateChild(f2);
        Feature f3 = new Feature("ThreeUp.V1.f3", true, Feature.Source.DEFAULT);
        f3.setParent(f2);
        f2.addUpdateChild(f3);
        fl1.put("ThreeUp.V1.f2", f2);
        fl1.put("ThreeUp.V1.f3", f3);
        //change f1 parent
        Feature f1 = new Feature("ThreeUp.V1.f1", true, Feature.Source.DEFAULT);
        f1.setParent(f2);
        f2.addUpdateChild(f1);
        FeaturesList fl2 = new FeaturesList();
        fl2.put("ThreeUp.V1", parent);
        fl2.put("ThreeUp.V1.f2", f2);
        fl2.put("ThreeUp.V1.f3", f3);
        fl2.put("ThreeUp.V1.f1", f1);
        //Now merge
        fl1.merge(fl2);
        System.out.println(fl1);
        System.out.println(fl2);
        Assert.assertTrue("A size = 4 features list is expected.", fl1.size() == 4);
        Assert.assertTrue("ThreeUp.V1.f2 feature should appear in the merged list.", fl1.getFeature("ThreeUp.V1.f2") != null);
        Assert.assertTrue("ThreeUp.V1.f2 feature should appear in the merged list.", fl1.getFeature("ThreeUp.V1.f3") != null);
        Assert.assertTrue("ThreeUp.V1 feature should appear in the merged list.", fl1.containsKey("ThreeUp.V1"));
        Assert.assertTrue("ThreeUp.V1.f1 feature should appear in the merged list.", fl1.containsKey("ThreeUp.V1.f1"));
        Assert.assertTrue("ThreeUp.V1.f1 parent = f2 is expected.", fl1.getFeature("ThreeUp.V1.f1").getParent().getName().equalsIgnoreCase("ThreeUp.V1.f2"));
    }

    @Test
    public void mergeMissingElementInNewListTest() {
        FeaturesList fl1 = new FeaturesList();
        Feature p = new Feature("ThreeUp.V1", true, Feature.Source.DEFAULT);
        Feature f = new Feature("ThreeUp.V1.f1", true, Feature.Source.DEFAULT);
        fl1.put("ThreeUp.V1", p);
        fl1.put("ThreeUp.V1.f1", f);
        FeaturesList fl2 = new FeaturesList();
        //   Feature f2 = new Feature("ThreeUp.V1.f2", true, Feature.Source.DEFAULT) ;
        fl2.put("ThreeUp.V1", p);
        //  fl2.put("ThreeUp.V1.f2",f2);
        //Now merge
        fl1.merge(fl2);
        Assert.assertTrue("A size = 2 features list is expected.", fl1.size() == 2);
        Assert.assertTrue("ThreeUp.V1.f1 feature should appear in the merged list.", fl1.getFeature("ThreeUp.V1.f1") != null);
        Assert.assertTrue("ThreeUp.V1.f1 feature should appear in the merged list.", fl1.getFeature("ThreeUp.V1.f1").getSource() != Feature.Source.MISSING);
        Assert.assertTrue("ThreeUp.V1.f1 feature should appear in the merged list.", fl1.getFeature("ThreeUp.V1") != null);
        Assert.assertTrue("ThreeUp.V1.f1 feature should appear in the merged list.", fl1.getFeature("ThreeUp.V1").getSource() != Feature.Source.MISSING);
        Assert.assertTrue("ThreeUp.V1 feature should appear in the merged list.", fl1.containsKey("ThreeUp.V1"));
        Assert.assertTrue("ThreeUp.V1.f1 feature should appear in the merged list.", fl1.containsKey("ThreeUp.V1.f1"));
    }

    @Test
    public void mergeWithEmptyListTest() {
        FeaturesList fl1 = new FeaturesList();
        Feature p = new Feature("ThreeUp.V1", true, Feature.Source.DEFAULT);
        Feature f = new Feature("ThreeUp.V1.f1", true, Feature.Source.DEFAULT);
        fl1.put("ThreeUp.V1", p);
        fl1.put("ThreeUp.V1.f1", f);
        FeaturesList fl2 = new FeaturesList();
        //Now merge
        fl1.merge(fl2);
        Assert.assertTrue("A size = 2 features list is expected.", fl1.size() == 2);
        Assert.assertTrue("ThreeUp.V1.f1 feature should appear in the merged list.", fl1.getFeature("ThreeUp.V1.f1") != null);
        Assert.assertTrue("ThreeUp.V1.f1 feature should appear in the merged list.", fl1.getFeature("ThreeUp.V1.f1").getSource() != Feature.Source.MISSING);
        Assert.assertTrue("ThreeUp.V1.f1 feature should appear in the merged list.", fl1.getFeature("ThreeUp.V1") != null);
        Assert.assertTrue("ThreeUp.V1.f1 feature should appear in the merged list.", fl1.getFeature("ThreeUp.V1").getSource() != Feature.Source.MISSING);
        Assert.assertTrue("ThreeUp.V1 feature should appear in the merged list.", fl1.containsKey("ThreeUp.V1"));
        Assert.assertTrue("ThreeUp.V1.f1 feature should appear in the merged list.", fl1.containsKey("ThreeUp.V1.f1"));
    }

    @Test
    public void mergeWithConfigurationTest() {
        FeaturesList fl1 = new FeaturesList();
        Feature parent = new Feature("ThreeUp.V1", true, Feature.Source.DEFAULT);
        Feature f = new Feature("ThreeUp.V1.f1", true, Feature.Source.DEFAULT);
        f.setParent(parent);
        parent.addUpdateChild(f);
        fl1.put("ThreeUp.V1", parent);
        fl1.put("ThreeUp.V1.f1", f);
        FeaturesList fl2 = new FeaturesList();
        Feature parent2 = new Feature("ThreeUp.V1", true, Feature.Source.DEFAULT);
        Feature f2 = new Feature("ThreeUp.V1.f2", true, Feature.Source.DEFAULT);
        f2.setParent(parent2);
        parent2.addUpdateChild(f2);
        fl2.put("ThreeUp.V1", parent2);
        fl2.put("ThreeUp.V1.f2", f2);
        //Now add the a feature from fl1 but with configuration
        JSONObject configuration = null;
        try {
            configuration = new JSONObject("{"
                    + "\"id\":\"road-condition\","
                    + "\"name\":\"road condition\","
                    + "\"type\":\"native\","
                    + "\"version\":\"1.0\","
                    + "\"regions\":[\"all\"],"
                    + "\"languages\":[\"all\"]"
                    + "}");
        } catch (JSONException e) {
            Assert.fail(e.getMessage());
        }
        f.setConfiguration(configuration);
        fl2.put("ThreeUp.V1.f1", f);
        //Now merge
        fl1.merge(fl2);
        System.out.println(fl1);
        Assert.assertTrue("A size = 3 features list is expected.", fl1.size() == 3);
        Assert.assertTrue("ThreeUp.V1.f2 feature should appear in the merged list.", fl1.getFeature("ThreeUp.V1.f2") != null);
        Assert.assertTrue("ThreeUp.V1 feature should appear in the merged list.", fl1.containsKey("ThreeUp.V1"));
        Assert.assertTrue("ThreeUp.V1.f1 feature should appear in the merged list.", fl1.containsKey("ThreeUp.V1.f1"));
        Assert.assertTrue("ThreeUp.V1.f1 feature should have a non empty configuration object.", fl1.getFeature("ThreeUp.V1.f1").getConfiguration().toString().trim().length() > 2);
    }

    @Test
    public void mergeWithDeletedConfigurationTest() {
        FeaturesList fl1 = new FeaturesList();
        Feature parent = new Feature("ThreeUp.V1", true, Feature.Source.DEFAULT);
        Feature f = new Feature("ThreeUp.V1.f1", true, Feature.Source.DEFAULT);
        //Now add a configuration
        JSONObject configuration = null;
        try {
            configuration = new JSONObject("{"
                    + "\"id\":\"road-condition\","
                    + "\"name\":\"road condition\","
                    + "\"type\":\"native\","
                    + "\"version\":\"1.0\","
                    + "\"regions\":[\"all\"],"
                    + "\"languages\":[\"all\"]"
                    + "}");
        } catch (JSONException e) {
            Assert.fail(e.getMessage());
        }
        f.setConfiguration(configuration);
        f.setParent(parent);
        parent.addUpdateChild(f);
        fl1.put("ThreeUp.V1", parent);
        fl1.put("ThreeUp.V1.f1", f);
        //Create another list with the same features but f contains no configuration
        FeaturesList fl2 = new FeaturesList();
        Feature parent2 = new Feature("ThreeUp.V1", true, Feature.Source.DEFAULT);
        Feature f2 = new Feature("ThreeUp.V1.f1", true, Feature.Source.DEFAULT);
        f2.setParent(parent2);
        parent2.addUpdateChild(f2);
        fl2.put("ThreeUp.V1", parent2);
        fl2.put("ThreeUp.V1.f1", f2);
        //Now merge
        fl1.merge(fl2);
        System.out.println(fl1);
        Assert.assertTrue("A size = 2 features list is expected.", fl1.size() == 2);
        Assert.assertTrue("ThreeUp.V1.f2 feature should appear in the merged list.", fl1.getFeature("ThreeUp.V1.f2") != null);
        Assert.assertTrue("ThreeUp.V1 feature should appear in the merged list.", fl1.containsKey("ThreeUp.V1"));
        Assert.assertTrue("ThreeUp.V1.f1 feature should appear in the merged list.", fl1.containsKey("ThreeUp.V1.f1"));
        Assert.assertTrue("ThreeUp.V1.f1 feature should have an empty configuration object.", fl1.getFeature("ThreeUp.V1.f1").getConfiguration().toString().trim().equals("{}"));
    }
}
