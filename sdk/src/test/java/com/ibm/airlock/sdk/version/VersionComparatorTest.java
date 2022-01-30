package com.ibm.airlock.sdk.version;

import org.junit.Test;
import com.ibm.airlock.common.util.AirlockVersionComparator;
import junit.framework.Assert;


/**
 * @author Rachel Levy
 */

public class VersionComparatorTest {


    @Test
    public void SimpleString() {
        AirlockVersionComparator vc = new AirlockVersionComparator();
        String v1 = "a.b.c";
        String v2 = "a.b.d";
        int compareResult = vc.compare(v1, v2); // should be -1 because second param is bigger
        Assert.assertTrue(compareResult < 0);
        compareResult = vc.compare(v2, v1);// should be 1 because first param is bigger
        Assert.assertTrue(compareResult > 0);

        v1 = "a.b.c";
        v2 = "a.b.c";
        compareResult = vc.compare(v2, v1);// should be 0
        Assert.assertEquals(compareResult, 0);
    }

    @Test
    public void SimpleInt() {
        AirlockVersionComparator vc = new AirlockVersionComparator();
        String v1 = "1.1.0";
        String v2 = "1.1.1";
        int compareResult = vc.compare(v1, v2); // should be -1 because second param is bigger
        Assert.assertTrue(compareResult < 0);
        compareResult = vc.compare(v2, v1);// should be 1 because first param is bigger
        Assert.assertTrue(compareResult > 0);

        v1 = "1.1.0";
        v2 = "1.1.0";
        compareResult = vc.compare(v2, v1);// should be 0
        Assert.assertTrue(compareResult == 0);
    }

    @Test
    public void OneIsLongerWithInsignificantString() {
        AirlockVersionComparator vc = new AirlockVersionComparator();
        String v1 = "1.1";
        String v2 = "1.1..";
        int compareResult = vc.compare(v1, v2); // should be 0
        Assert.assertTrue(compareResult == 0);
    }

    @Test
    public void OneIsLongerWithInsignificantInt() {
        int aaa = "a".compareTo("1");
        AirlockVersionComparator vc = new AirlockVersionComparator();
        String v1 = "1.1";
        String v2 = "1.1.0.0";
        int compareResult = vc.compare(v1, v2); // should be 0
        Assert.assertTrue(compareResult == 0);
    }

    @Test
    public void twoZeros() {
        AirlockVersionComparator vc = new AirlockVersionComparator();
        String v1 = "1.1";
        String v2 = "1.1.00";
        int compareResult = vc.compare(v1, v2); // should be 0
        Assert.assertTrue(compareResult == 0);
    }

    @Test
    public void OneIsLongerWithSignificantString() {
        AirlockVersionComparator vc = new AirlockVersionComparator();
        String v1 = "1.1";
        String v2 = "1.1.A";
        int compareResult = vc.compare(v1, v2); // should be -1 because second param is bigger
        Assert.assertTrue(compareResult < 0);
    }

    @Test
    public void OneIsLongerWithSignificantInt() {
        AirlockVersionComparator vc = new AirlockVersionComparator();
        String v1 = "1.1";
        String v2 = "1.1.1";
        int compareResult = vc.compare(v1, v2); // should be -1 because second param is bigger
        Assert.assertTrue(compareResult < 0);
    }

    @Test
    public void digitAndString() {
        AirlockVersionComparator vc = new AirlockVersionComparator();
        String v1 = "1.0";
        String v2 = "ngh";
        int compareResult = vc.compare(v1, v2); // should be >0 because second param is bigger
        Assert.assertTrue(compareResult < 0);
    }

    @Test
    public void combineIntWithString() {
        AirlockVersionComparator vc = new AirlockVersionComparator();
        String v1 = "1.1.1";
        String v2 = "1.1.a";
        int compareResult = vc.compare(v1, v2); // should be -1 because second param is bigger
        Assert.assertTrue(compareResult < 0);
    }

    private boolean isV2greaterThanV1(String v1, String v2) {
        AirlockVersionComparator vc = new AirlockVersionComparator();
        int compareResult = vc.compare(v1, v2);
        return (compareResult < 0);
    }

    private boolean areVersionsEqual(String v1, String v2) {
        AirlockVersionComparator vc = new AirlockVersionComparator();
        int compareResult = vc.compare(v1, v2);
        return (compareResult == 0);
    }

    @Test
    public void oneMoreDotTest() {
        Assert.assertTrue("1.1.1 should be greater than 1.1", isV2greaterThanV1("1.1", "1.1.1"));
    }

    @Test
    public void oneMoreDotWithZeroTest() {
        Assert.assertTrue("1.1.0 should be equal to 1.1", areVersionsEqual("1.1.0", "1.1"));
    }

    @Test
    public void twoMoreDotsTest() {
        Assert.assertTrue("1.1.1.1 should be greater than 1.1", isV2greaterThanV1("1.1", "1.1.1.1"));
    }

    @Test
    public void twoMoreDotsWithZeroTest() {
        Assert.assertTrue("1.1.1.0 should be greater than 1.1", isV2greaterThanV1("1.1", "1.1.1.0"));
    }

    @Test
    public void oneMoreDotButSmallerNumberTest() {
        Assert.assertTrue("2.0 should be greater than 1.1.3", isV2greaterThanV1("1.1.3", "2.0"));
    }

    @Test
    public void oneMoreDotButSmallerNumberWithZeroTest() {
        Assert.assertTrue("2.0 should be greater than 1.2.0", isV2greaterThanV1("1.2.0", "2.0"));
    }

    @Test
    public void twoMoreDotsButSmallerNumberTest() {
        Assert.assertTrue("2.0 should be greater than 1.2.0.1", isV2greaterThanV1("1.2.0.1", "2.0"));
    }

    @Test
    public void oneMoreDotWithStringTest() {
        Assert.assertTrue("beta.1 should be greater than beta", isV2greaterThanV1("beta", "beta.1"));
    }

    @Test
    public void twoMoreDotsWithStringTest() {
        Assert.assertTrue("beta.1.2 should be greater than beta.1", isV2greaterThanV1("beta.1", "beta.1.2"));
    }

    /*
    @Test
    public void versionStandardsTest1(){
        Assert.assertTrue("1.2.0.1 should be equal to 1.2-a1", areVersionsEqual("1.2.0.1", "1.2-a1"));
    }

    @Test
    public void versionStandardsTest2(){
        Assert.assertTrue("1.2.1.2 should be equal to 1.2-b2", areVersionsEqual("1.2.1.2", "1.2-b2"));
    }

    @Test
    public void versionStandardsTest3(){
        Assert.assertTrue("1.2.2.3 should be equal to 1.2-rc3", areVersionsEqual("1.2.2.3", "1.2-rc3"));
    }

    @Test
    public void versionStandardsTest4(){
        Assert.assertTrue("1.2.3.0 should be equal to 1.2-r", areVersionsEqual("1.2.3.0", "1.2-r"));
    }

    @Test
    public void versionStandardsTest5(){
        Assert.assertTrue("1.2.3.5 should be equal to 1.2-r5", areVersionsEqual("1.2.3.5", "1.2-r5"));
    }
    */
}
