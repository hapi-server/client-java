package test;

import org.hapiserver.TimeUtil;

/**
 * The TimeUtilities are needed to implement caching and may be useful to people
 * using the library as well.
 * @author jbf
 */
public class DemoTimeUtilities {
    
    /**
     * demo code which counts off the days in an interval.
     */
    public static void demoCountOffDays() {
        System.err.println("\n# demoCountOffDays");
        String[] ss= TimeUtil.countOffDays("2015-12-30Z", "2016-03-02Z" );
        for ( String s: ss ) {
            System.err.println(s);
        }        
    }
    
    /**
     * demo reformat time which will reformat arbitrary isoTime to standard format.
     */
    public static void demoReformatTime() {
        System.err.println("\n# demoReformatTime");
        System.err.println(TimeUtil.reformatIsoTime( "2000-01-01T00:00Z", "2021-12-09T00:00:00.999888Z"));
    }
    
    /**
     * demo calculate the day of year function.
     */
    public static void demoDayOfYear() {
        System.err.println("\n# demoDayOfYear");
        System.err.println(TimeUtil.dayOfYear( 2000, 12, 31 ));
        System.err.println(TimeUtil.dayOfYear( 1970, 1, 1 ));
    }

    /**
     * demo floor and ceiling functions which round down and round up to the day bounaries.
     */
    public static void demoFloorCeil() {
        System.err.println("\n# demoFloorCeil");
        System.err.println(TimeUtil.floor("2020-05-11T08:31"));
        System.err.println(TimeUtil.ceil("2020-05-11T08:31"));
        System.err.println(TimeUtil.floor("2020-05-11T00:00"));
        System.err.println(TimeUtil.ceil("2020-05-11T00:00"));
    }
     
    /**
     * Demo next and previous day functions, which count off days.
     */
    public static void demoNextPrevDay() {
        System.err.println("\n# demoNextPrevDay");
        System.err.println(TimeUtil.nextDay("2020-05-11T08:31"));
        System.err.println(TimeUtil.nextDay("2020-05-11T00:00"));
        System.err.println(TimeUtil.nextDay("2020-12-31T00:00"));
        System.err.println(TimeUtil.previousDay("2020-05-11T08:31"));
        System.err.println(TimeUtil.previousDay("2020-05-11T00:00"));
        System.err.println(TimeUtil.previousDay("2020-12-31T00:00"));
    }
    
    /**
     * Demo Calculate the time as long milliseconds since 1970-01-01T00:00Z, also known
     * as "Unix Time".
     */
    public static void demoToMillisecondsSince1970() {
        System.err.println("\n# demoToMilli");
        System.err.println(TimeUtil.toMillisecondsSince1970("2000-01-01T00:00:00.0Z"));
        System.err.println(TimeUtil.toMillisecondsSince1970("2000-01-01T00:00Z"));
        System.err.println(TimeUtil.toMillisecondsSince1970("1970-01-01T00:00Z"));
        long ms1970= TimeUtil.toMillisecondsSince1970("2000-01-02T00:00:01.00Z");
        System.err.println("days: " + (ms1970/86400000 ) );
        System.err.println("mills: "+ (ms1970%86400000 ) );
        
    }
    
    /**
     * Demo function for decomposing isoTime into array of 
     * year, month, day, hour, minute, second and nanoseconds.
     */
    public static void demoIsoTimeToArray() {
        System.err.println("\n# demoIsoToTimeArray");
        int[] arr= TimeUtil.isoTimeToArray("2001-02-03T04:05:06:000000007");
        System.err.println("year: "+arr[0]);
        System.err.println("month: "+arr[1]);
        System.err.println("day: "+arr[2]);
        System.err.println("hour: "+arr[3]);
        System.err.println("minute: "+arr[4]);
        System.err.println("second: "+arr[5]);
        System.err.println("nanoseconds: "+arr[6]);
    }
    
    public static void main( String[] args ) {
        demoCountOffDays();
        demoReformatTime();
        demoDayOfYear();
        demoFloorCeil();
        demoNextPrevDay();
        demoToMillisecondsSince1970();
        demoIsoTimeToArray();
    }


}
