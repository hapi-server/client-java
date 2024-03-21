
package test;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import org.hapiserver.HapiClient;
import org.hapiserver.HapiRecord;
import org.json.JSONException;

/**
 * Demo how cache is built and used by the library when it is offline.
 * @author jbf
 */
public class DemoCaching {
    
    private static int deleteTree( File dr ) {
        int filesDeleted=0;
        File[] ff= dr.listFiles();
        if ( ff==null ) return 0;
        for ( File f : ff ) {
            if ( f.isDirectory() ) {
                filesDeleted+= deleteTree(f);
            } else if ( f.isFile() ) {
                if ( ! f.delete() ) {
                    System.out.println( "unable to delete "+ f );
                } else {
                    filesDeleted+= 1;
                }
            }
        }
        return filesDeleted;
    }
    
    private static void clearCache( ) {
        String cache= new HapiClient().getHapiCache();
        File f= new File( cache, "https/jfaden.net/HapiServerDemo/hapi/data/" );
        int fileCount= deleteTree(f);
        System.out.println("clear cache deletes "+fileCount+ " files.");
    }
    
    public static void main( String[] args ) throws MalformedURLException, IOException, JSONException {
        HapiClient hapiClient= new HapiClient();
        URL hapiServer=new URL("https://jfaden.net/HapiServerDemo/hapi/");
        
        clearCache();
        
        String ID="Iowa+City+Conditions";

        System.out.println("--- "+ID+" ---");
        
        Iterator<HapiRecord> it= hapiClient.getData(hapiServer,
            "Iowa+City+Conditions",
            "Temperature,Humidity",
            "2019-10-20T00:00","2019-10-24T05:00" );
        
        while ( it.hasNext() ) {
            HapiRecord rec= it.next();
            System.out.println( String.format( "%s %9.3f %9.3f", rec.getIsoTime(0), rec.getDouble(1), rec.getDouble(2) ) );
        }
        
        System.out.println("# Demo that we can read data from cache, even though we are offline.");
        hapiClient.setOffline(true);
        
        it= hapiClient.getData(hapiServer,
            "Iowa+City+Conditions",
            "Temperature,Humidity",
            "2019-10-20T12:30","2019-10-21T00:30" );
        
        while ( it.hasNext() ) {
            HapiRecord rec= it.next();
            if ( rec.getIsoTime(0).compareTo("2019-10-20T12:30")<0 ) {
                throw new IllegalArgumentException("first timetag returned is too early");
            }
            System.out.println( String.format( "%s %9.3f %9.3f", rec.getIsoTime(0), rec.getDouble(1), rec.getDouble(2) ) );
        }
        
        System.out.println("# Now clear the cache and demo that no records are found.");
        
        clearCache();

        it= hapiClient.getData(hapiServer,
            "Iowa+City+Conditions",
            "Temperature,Humidity",
            "2019-10-20T12:30","2019-10-21T00:30" );
        
        if ( !it.hasNext() ) {
            System.out.println("(reads no records)");
        }
        while ( it.hasNext() ) {
            HapiRecord rec= it.next();
            System.out.println( String.format( "%s %9.3f %9.3f", rec.getIsoTime(0), rec.getDouble(1), rec.getDouble(2) ) );
        }
                
    }
}
