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
import org.json.JSONException;

/**
 * unit tests are too much overhead, so here are some simple end-to-end tests.
 * @author jbf
 */
public class HapiClientTests {
    public static void main( String[] args ) throws MalformedURLException, IOException, JSONException {
        new HapiClient().getCatalog(new URL("https://jfaden.net/HapiServerDemo/hapi") );
        URL u= new URL("https://jfaden.net/HapiServerDemo/hapi/");
        Iterator<HapiRecord> js= new HapiClient().getData( u, "poolTemperature", "2020-04-23T00:00Z", "2020-04-24T00:00Z");
        while ( js.hasNext() ) {
            HapiRecord rec= js.next();
            System.out.println( String.format( "%s %9.2f", rec.getIsoTime(0), rec.getDouble(1) ) );
        }
    }
        
}
