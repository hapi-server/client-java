/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import org.hapiserver.HapiClient;
import org.hapiserver.TimeUtil;

/**
 *
 * @author jbf
 */
public class DemoCountOffDays {
    public static void main( String[] args ) {
        String[] ss= TimeUtil.countOffDays("2015-12-30Z", "2016-03-02Z" );
        for ( String s: ss ) {
            System.err.println(s);
        }
    }
}
