/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import org.hapiserver.TimeUtil;

/**
 *
 * @author jbf
 */
public class DemoTimeUtilities {
    
    public static void demoCountOffDays() {
        System.err.println("\n# demoCountOffDays");
        String[] ss= TimeUtil.countOffDays("2015-12-30Z", "2016-03-02Z" );
        for ( String s: ss ) {
            System.err.println(s);
        }        
    }
    
    public static void demoReformatTime() {
        System.err.println("\n# demoReformatTime");
        System.err.println(TimeUtil.reformatIsoTime( "2000-01-01T00:00Z", "2021-12-09T00:00:00.999888Z"));
    }
    
    public static void demoDayOfYear() {
        System.err.println("\n# demoDayOfYear");
        System.err.println(TimeUtil.dayOfYear( 2000, 12, 31 ));
        System.err.println(TimeUtil.dayOfYear( 1970, 1, 1 ));
    }

    public static void demoFloorCeil() {
        System.err.println("\n# demoFloorCeil");
        System.err.println(TimeUtil.floor("2020-05-11T08:31"));
        System.err.println(TimeUtil.ceil("2020-05-11T08:31"));
        System.err.println(TimeUtil.floor("2020-05-11T00:00"));
        System.err.println(TimeUtil.ceil("2020-05-11T00:00"));
    }
        
    public static void demoNextPrevDay() {
        System.err.println("\n# demoNextPrevDay");
        System.err.println(TimeUtil.nextDay("2020-05-11T08:31"));
        System.err.println(TimeUtil.nextDay("2020-05-11T00:00"));
        System.err.println(TimeUtil.nextDay("2020-12-31T00:00"));
        System.err.println(TimeUtil.previousDay("2020-05-11T08:31"));
        System.err.println(TimeUtil.previousDay("2020-05-11T00:00"));
        System.err.println(TimeUtil.previousDay("2020-12-31T00:00"));
    }

    public static void demoToMillisecondsSince1970() {
        System.err.println("\n# demoToMilli");
        System.err.println(TimeUtil.toMillisecondsSince1970("2000-01-01T00:00:00.0Z"));
        System.err.println(TimeUtil.toMillisecondsSince1970("2000-01-01T00:00Z"));
        System.err.println(TimeUtil.toMillisecondsSince1970("1970-01-01T00:00Z"));
        long ms1970= TimeUtil.toMillisecondsSince1970("2000-01-02T00:00:01.00Z");
        System.err.println("days: " + (ms1970/86400000 ) );
        System.err.println("mills: "+ (ms1970%86400000 ) );
        
    }
    
    public static void main( String[] args ) {
        demoCountOffDays();
        demoReformatTime();
        demoDayOfYear();
        demoFloorCeil();
        demoNextPrevDay();
        demoToMillisecondsSince1970();
        
    }
}
