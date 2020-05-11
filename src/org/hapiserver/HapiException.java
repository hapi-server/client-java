
package org.hapiserver;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * exceptions thrown by HAPI servers when not used properly.
 * @author jbf
 */
public class HapiException extends RuntimeException {
    
    int code;
    
    public HapiException( JSONObject status ) throws JSONException {
        super( status.getString("message") );
        this.code= status.getInt("code");
    }
    
    public HapiException( String s ) {
        super(s);
        this.code= -1;
    }
}
