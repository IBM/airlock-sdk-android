package com.ibm.airlock.sdk;

import org.junit.Test;
import com.ibm.airlock.common.util.AirlockVersionComparator;
import junit.framework.Assert;


/**
 * @author Rachel Levy
 */

public class VersionComparatorTest {

    AirlockVersionComparator comparator = new AirlockVersionComparator();

    @Test
    public void SimpleString() {
        String v1 = "a.b.c";
        String v2 = "a.b.d";
        int compareResult = comparator.compare(v1, v2); // should be -1 because second param is bigger
        Assert.assertTrue(compareResult < 0);
        compareResult = comparator.compare(v2, v1);// should be 1 because first param is bigger
        Assert.assertTrue(compareResult > 0);

        v1 = "a.b.c";
        v2 = "a.b.c";
        compareResult = comparator.compare(v2, v1);// should be 0
        Assert.assertEquals(compareResult, 0);
    }

    @Test
    public void SimpleInt() {
        String v1 = "1.1.0";
        String v2 = "1.1.1";
        int compareResult = comparator.compare(v1, v2); // should be -1 because second param is bigger
        Assert.assertTrue(compareResult < 0);
        compareResult = comparator.compare(v2, v1);// should be 1 because first param is bigger
        Assert.assertTrue(compareResult > 0);

        v1 = "1.1.0";
        v2 = "1.1.0";
        compareResult = comparator.compare(v2, v1);// should be 0
        Assert.assertTrue(compareResult == 0);
    }

    @Test
    public void OneIsLongerWithInsignificantString() {
        String v1 = "1.1";
        String v2 = "1.1..";
        int compareResult = comparator.compare(v1, v2); // should be 0
        Assert.assertTrue(compareResult == 0);
    }

    @Test
    public void OneIsLongerWithInsignificantInt() {
        int aaa = "a".compareTo("1");
        String v1 = "1.1";
        String v2 = "1.1.0.0";
        int compareResult = comparator.compare(v1, v2); // should be 0
        Assert.assertTrue(compareResult == 0);
    }

    @Test
    public void OneIsNull() {
        String v1 = "1.1";
        String v2 = null;
        int compareResult = comparator.compare(v1, v2); // should be > 0
        Assert.assertTrue(compareResult > 0);
        compareResult = comparator.compare(v2, v1); // should be < 0
        Assert.assertTrue(compareResult < 0);
    }


    @Test
    public void twoZeros() {
        String v1 = "1.1";
        String v2 = "1.1.00";
        int compareResult = comparator.compare(v1, v2); // should be 0
        Assert.assertTrue(compareResult == 0);
    }

    @Test
    public void OneIsLongerWithSignificantString() {
        String v1 = "1.1";
        String v2 = "1.1.A";
        int compareResult = comparator.compare(v1, v2); // should be -1 because second param is bigger
        Assert.assertTrue(compareResult < 0);
    }

    @Test
    public void OneIsLongerWithSignificantInt() {
        String v1 = "1.1";
        String v2 = "1.1.1";
        int compareResult = comparator.compare(v1, v2); // should be -1 because second param is bigger
        Assert.assertTrue(compareResult < 0);
    }

    @Test
    public void digitAndString() {
        String v1 = "1.0";
        String v2 = "ngh";
        int compareResult = comparator.compare(v1, v2); // should be > 0 because second param is bigger
        Assert.assertTrue(compareResult < 0);
    }

    @Test
    public void combineIntWithString() {
        String v1 = "1.1.1";
        String v2 = "1.1.a";
        int compareResult = comparator.compare(v1, v2); // should be -1 because second param is bigger
        Assert.assertTrue(compareResult < 0);
    }

    @Test
    public void twoDigits() {
        String v1 = "8.9.0";
        String v2 = "8.11.0";
        int compareResult = comparator.compare(v1, v2); // should be -1 because second param is bigger
        Assert.assertTrue(compareResult < 0);
    }

}
