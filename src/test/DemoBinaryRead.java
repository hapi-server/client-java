
package test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.hapiserver.HapiClient;
import org.hapiserver.HapiRecord;
import org.hapiserver.TimeUtil;
import org.json.JSONException;

/**
 * Codes used to test and demonstrate binary reads.
 * @author jbf
 */
public class DemoBinaryRead {
    public static void main( String[] args ) throws MalformedURLException, IOException, JSONException {
        {
            Logger.getLogger( "org.hapiserver" ).setLevel( Level.ALL );
            ConsoleHandler h= new ConsoleHandler();
            h.setLevel(Level.ALL);
            Logger.getLogger( "org.hapiserver" ).addHandler( h );
        }
        
        testArray();
        testAll();
        testMultiple();
        testTemp();
    }

    private static void testTemp() throws IOException, MalformedURLException, JSONException {
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
            //System.out.println( String.format( "%s %10.3e", s, t ) );
        }
    }

    private static void testAll() throws IOException, MalformedURLException, JSONException {
        URL hapiServer=new URL("https://jfaden.net/HapiServerDemo/hapi");
        
        Iterator<HapiRecord> it= new HapiClient().getDataBinary( hapiServer,
                "atticTemperature",
                "2020-08-25T00Z",
                "2020-09-17T08Z" );
        
        while ( it.hasNext() ) {
            HapiRecord rec= it.next();
            String s= TimeUtil.reformatIsoTime( "2000-01-01T00:00:00.000Z", rec.getIsoTime(0) );
            double t= rec.getDouble(1);
            //System.out.println( String.format( "%s %10.3e", s, t ) );
        }
    }
    
    
    private static void testMultiple() throws IOException, MalformedURLException, JSONException {
        URL hapiServer=new URL("http://jfaden.net/HapiServerDemo/hapi");
        
        Iterator<HapiRecord> it= new HapiClient().getDataBinary( hapiServer,
                "Iowa+City+Forecast",
                "Time,Temperature,DewPoint,PrecipProbability",
                "2019-07-01",
                "2019-07-03" );
        
        while ( it.hasNext() ) {
            HapiRecord rec= it.next();

            String s= TimeUtil.reformatIsoTime( "2000-01-01T00:00:00.000Z", rec.getIsoTime(0) );
            double t= rec.getDouble(1);
            double d= rec.getDouble(2);
            double p= rec.getDouble(3);
            System.out.println( String.format( "%s %f %f %f", s, t, d, p ) );
        }
    }
    
    /**
     * TODO: this shows a strange problem where data is mishandled. 
     * @throws IOException
     * @throws MalformedURLException
     * @throws JSONException 
     */
    private static void testArray() throws IOException, MalformedURLException, JSONException {
        //GET https://cdaweb.gsfc.nasa.gov/hapi/data?id=PO_H0_HYD&time.min=2008-03-31T02:00:00Z&time.max=2008-03-31T04:00:00Z&parameters=Time,ELECTRON_DIFFERENTIAL_ENERGY_FLUX&format=binary
        URL hapiServer=new URL("https://cdaweb.gsfc.nasa.gov/hapi");
        
        Iterator<HapiRecord> it= new HapiClient().getDataBinary( hapiServer,
                "PO_H0_HYD",
                "Time,ELECTRON_DIFFERENTIAL_ENERGY_FLUX",
                "2008-03-31T02:00:00Z",
                "2008-03-31T04:00:00Z" );
        
        int irec=0;
        while ( it.hasNext() ) {
            HapiRecord rec= it.next();
            irec++;
            try {
                String s= TimeUtil.reformatIsoTime( "2000-01-01T00:00:00.000Z", rec.getIsoTime(0) );
                double[] t= rec.getDoubleArray(1);
                System.out.print( String.format( "%s ", s ) );
                for ( int i=0; i<t.length; i++ ) {
                    System.out.print( String.format( "%.2e ", t[i] ) );
                }
                System.out.println("");
            } catch ( RuntimeException ex ) {
                ex.printStackTrace();
            }
        }
    }    
}
