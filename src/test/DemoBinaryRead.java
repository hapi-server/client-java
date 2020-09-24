/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import org.hapiserver.HapiClient;
import org.hapiserver.HapiRecord;
import org.hapiserver.TimeUtil;
import org.json.JSONException;

/**
 *
 * @author jbf
 */
public class DemoBinaryRead {
    public static void main( String[] args ) throws MalformedURLException, IOException, JSONException {
        URL hapiServer=new URL("https://jfaden.net/HapiServerDemo/hapi");
        
        Iterator<HapiRecord> it= new HapiClient().getDataBinary( hapiServer,
                "atticTemperature",
                "Temperature",
                "2020-08-25T00Z",
                "2020-09-17T08Z" );
        
        while ( it.hasNext() ) {
            HapiRecord rec= it.next();

            String s= TimeUtil.reformatIsoTime( "2000-01-01T00:00:00.000Z", rec.getIsoTime(0) );
            double t= rec.getDouble(1);
            System.out.println( String.format( "%s %9.3f", s, t ) );
        }
    }
}
