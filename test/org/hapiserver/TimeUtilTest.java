
package org.hapiserver;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Tests for TimeUtil
 * @author jbf
 */
public class TimeUtilTest {
    
    public TimeUtilTest() {
    }

    /**
     * Test of reformatIsoTime method, of class TimeUtil.
     */
    @Test
    public void testReformatIsoTime() {
        System.out.println("reformatIsoTime");
        String expResult = "2020-04-21T00:00Z";
        String result = TimeUtil.reformatIsoTime( "2020-01-01T00:00Z", "2020-112Z");
        assertEquals(expResult, result);
    }

    /**
     * Test of countOffDays method, of class TimeUtil.
     */
    @Test
    public void testCountOffDays() {
        System.out.println("countOffDays");
        String startTime = "2020-09-25T12:00";
        String stopTime = "2020-09-27T12:00";
        String[] expResult = new String[] { "2020-09-25Z", "2020-09-26Z", "2020-09-27Z" };
        String[] result = TimeUtil.countOffDays(startTime, stopTime);
        assertArrayEquals(expResult, result);
    }

    /**
     * Test of nextDay method, of class TimeUtil.
     */
    @Test
    public void testNextDay() {
        System.out.println("nextDay");
        String day = "2020-09-25T08:38Z";
        String expResult = "2020-09-26Z";
        String result = TimeUtil.nextDay(day);
        assertEquals(expResult, result);
    }

    /**
     * Test of previousDay method, of class TimeUtil.
     */
    @Test
    public void testPreviousDay() {
        System.out.println("previousDay");
        String day = "2020-09-25T08:38Z";
        String expResult = "2020-09-24Z";
        String result = TimeUtil.previousDay(day);
        assertEquals(expResult, result);
    }

    /**
     * Test of ceil method, of class TimeUtil.
     */
    @Test
    public void testCeil() {
        System.out.println("ceil");
        String time = "2020-09-25T08:38Z";
        String expResult = "2020-09-26T00:00:00.000000000Z";
        String result = TimeUtil.ceil(time);
        assertEquals(expResult, result);
    }

    /**
     * Test of floor method, of class TimeUtil.
     */
    @Test
    public void testFloor() {
        System.out.println("floor");
        String time = "2020-09-25T08:38Z";
        String expResult = "2020-09-25T00:00:00.000000000Z";
        String result = TimeUtil.floor(time);
        assertEquals(expResult, result);
    }

    /**
     * Test of normalizeTimeString method, of class TimeUtil.
     */
    @Test
    public void testNormalizeTimeString() {
        System.out.println("normalizeTimeString");
        String time = "2020-09-25";
        String expResult = "2020-09-25T00:00:00.000000000Z";
        String result = TimeUtil.normalizeTimeString(time);
        assertEquals(expResult, result);
    }

    /**
     * Test of isoTimeFromArray method, of class TimeUtil.
     */
    @Test
    public void testIsoTimeFromArray() {
        System.out.println("isoTimeFromArray");
        int[] nn = new int[] { 2020, 9, 25, 8, 56, 34, 3045 };
        String expResult = "2020-09-25T08:56:34.000003045Z";
        String result = TimeUtil.isoTimeFromArray(nn);
        assertEquals(expResult, result);
    }

    /**
     * Test of isoTimeToArray method, of class TimeUtil.
     */
    @Test
    public void testIsoTimeToArray() {
        System.out.println("isoTimeToArray");
        String time;
        int[] expResult;
        int[] result;

        time = "2020-09-25T01:02Z";
        expResult = new int[] { 2020, 9, 25, 1, 2, 0, 0 };
        result = TimeUtil.isoTimeToArray(time);
        assertArrayEquals(expResult, result);
        
        time= "2020-033";
        expResult = new int[] { 2020, 2, 2, 0, 0, 0, 0 };
        result = TimeUtil.isoTimeToArray(time);
        assertArrayEquals(expResult, result);

        time= "2020-02-02";
        expResult = new int[] { 2020, 2, 2, 0, 0, 0, 0 };
        result = TimeUtil.isoTimeToArray(time);
        assertArrayEquals(expResult, result);
        
        time= "2020-02-02Z";
        expResult = new int[] { 2020, 2, 2, 0, 0, 0, 0 };
        result = TimeUtil.isoTimeToArray(time);
        assertArrayEquals(expResult, result);        
        
        time= "2020-02-02T01:02:03.0406";
        expResult = new int[] { 2020, 2, 2, 1, 2, 3, 40600000 };
        result = TimeUtil.isoTimeToArray(time);
        assertArrayEquals(expResult, result);                
    }

    /**
     * Test of dayOfYear method, of class TimeUtil.
     */
    @Test
    public void testDayOfYear() {
        System.out.println("dayOfYear");
        int year = 2000;
        int month = 5;
        int day = 29;
        int expResult = 150;
        int result = TimeUtil.dayOfYear(year, month, day);
        assertEquals(expResult, result);
    }

    /**
     * Test of toMillisecondsSince1970 method, of class TimeUtil.
     */
    @Test
    public void testToMillisecondsSince1970() {
        System.out.println("toMillisecondsSince1970");
        String time = "2000-01-02T00:00:00.0Z";
        long result = TimeUtil.toMillisecondsSince1970(time);
        assertEquals( result/ 86400000, 10958 );
        assertEquals( result % 86400000, 0 );
        
    }
    
}
