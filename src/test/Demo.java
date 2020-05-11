
package test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import org.hapiserver.HapiClient;
import org.hapiserver.HapiRecord;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Demo of Java client.
 * @author jbf
 */
public class Demo {
    
    public static void main( String[] args ) throws MalformedURLException, IOException, JSONException {
        URL hapiServer=new URL("https://jfaden.net/HapiServerDemo/hapi/");
        
        System.err.println("");
        System.err.println("\n# The catalog can be retrieved as a string array:");
        
        String[] ids= HapiClient.getCatalogIdsArray(hapiServer);
        for ( String s : ids ) {
            System.err.println(s);
        }
        
        System.err.println("");
        System.err.println("# Raw JSON Objects can be retrieved:");
        
        JSONObject info= HapiClient.getInfo(hapiServer,"Iowa+City+Conditions");
        System.err.println(info.toString(4));
        
        System.err.println("");
        System.err.println("# Print each of the parameters:");
        
        JSONArray parameters= info.getJSONArray("parameters");
        for ( int i=0; i<parameters.length(); i++ ) {
            System.err.println(parameters.getJSONObject(i).get("name"));
        }
        
        System.err.println("");
        System.err.println("# Data is retrieved using an iterator of HapiRecords:");
        
        Iterator<HapiRecord> it= HapiClient.getData(hapiServer,"Iowa+City+Conditions","2019-10-21T00:00","2019-10-22T00:00");        
        while( it.hasNext() ) {
            HapiRecord rec= it.next();
            System.err.println( rec.getIsoTime(0) );
        }
    }

}
