
package test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import org.hapiserver.HapiClient;
import org.hapiserver.HapiRecord;
import org.json.JSONException;

/**
 * Demo reading a subset of the parameters.
 * @author jbf
 */
public class DemoReadSubset {
    
    public static void main( String[] args ) throws MalformedURLException, IOException, JSONException {
        URL hapiServer=new URL("https://jfaden.net/HapiServerDemo/hapi/");
        
        Iterator<HapiRecord> it= new HapiClient().getData( hapiServer, "Iowa+City+Conditions", "Temperature,Humidity", "2019-10-21T00:00", "2019-10-22T00:00" );
        
        while ( it.hasNext() ) {
            HapiRecord rec= it.next();
            System.out.println( String.format( "%s %9.3f %9.3f", rec.getIsoTime(0), rec.getDouble(1), rec.getDouble(2) ) );
        }
    }
}
