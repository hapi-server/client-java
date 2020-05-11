
package test;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import static org.hapiserver.HapiClient.*;
import org.hapiserver.HapiRecord;
import org.hapiserver.TimeUtil;
import org.json.JSONException;

/**
 * Demo a more realistic use of the library.
 * @author jbf
 */
public class DemoCdawebRead {
    
    public static void main( String[] args ) throws MalformedURLException, IOException, JSONException {
        URL hapiServer=new URL("https://cdaweb.gsfc.nasa.gov/hapi");
        
        Iterator<HapiRecord> it= getData( hapiServer,
                "MMS4_EPD-EIS_SRVY_L2_ELECTRONENERGY",
                "mms4_epd_eis_electronenergy_electron_P4_flux_t5",
                "2020-02-03T00Z",
                "2020-02-03T08Z" );
        
        while ( it.hasNext() ) {
            HapiRecord rec= it.next();
            double[] aa= rec.getDoubleArray(1);
            String s= TimeUtil.reformatIsoTime( "2000-01-01T00:00:00.000Z", rec.getIsoTime(0) );
            System.out.println( String.format( "%s %9.3f %9.3f %9.3f %9.3f %9.3f", s, aa[0], aa[1], aa[2], aa[3], aa[4] ) );
        }
    }
}
