
package test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import org.hapiserver.HapiClient;
import org.json.JSONException;

/**
 * Demo of Java client.
 * @author jbf
 */
public class DemoAllData {
    
    public static void test2( String[] args ) throws MalformedURLException, IOException, JSONException {
        URL hapiServer=new URL("http://hapi-server.org/servers/TestData/hapi");

        HapiClient hc= new HapiClient();
        
        System.err.println("");
        System.err.println("# Data is retrieved using Java arrays of native types:");
        
        Map<String,Object> data= hc.getAllData(hapiServer,"dataset1","vectorstr,vectorcats","1970-01-01Z","1970-01-01T00:00:11Z");
        
        
    }
    public static void test1( String[] args ) throws MalformedURLException, IOException, JSONException {
        URL hapiServer=new URL("https://jfaden.net/HapiServerDemo/hapi/");

        HapiClient hc= new HapiClient();
        
        System.err.println("");
        System.err.println("# Data is retrieved using Java arrays of native types:");
        
        long t0= System.currentTimeMillis();
        
        Map<String,Object> data= hc.getAllData(hapiServer,"Spectrum","Spectra","2016-01-01T00:00","2016-01-01T24:00");

        System.err.println( data.get("Time") );
        System.err.println( data.get("Spectra") );
        System.err.println( "data read in millis: "+(System.currentTimeMillis()-t0) );        
        
        String[] time= (String[])data.get("Time");
        double[][] spectra= (double[][])data.get("Spectra");
        for ( int i=0; i<time.length; i++ ) {
            double total=0;
            for ( int j=0; j<spectra[i].length; j++ ) {
                total+= spectra[i][j];
            }
            System.out.println( String.format("%s: %.2e",time[i],total) );
        }

    }

    public static void main( String[] args ) throws IOException, MalformedURLException, JSONException {
        test1( args );
    }
}
