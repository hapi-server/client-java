
package org.hapiserver;

import java.nio.ByteBuffer;
import java.util.Map;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Convert a binary record into a HapiRecord.
 * @author jbf
 */
public class BinaryHapiRecordConverter {
    
    Map<Integer,Integer> indexMap;
    JSONObject info;
    JSONArray params;
    int[] sizes;
    int[] lengths;
    int[] offsets;
    
    public BinaryHapiRecordConverter( JSONObject info ) throws JSONException {
        this.info= info;
        this.params= info.getJSONArray("parameters");
        this.sizes= new int[params.length()];
        this.lengths= new int[params.length()];
        this.offsets= new int[params.length()];
        for ( int i=0; i<params.length(); i++ ) {
            JSONObject jo= params.getJSONObject(i);
            String type= jo.getString("type");
            int length;
            switch (type) {
                case "isotime":
                    length= jo.getInt("length");
                    break;
                case "string":
                    length= jo.getInt("length");
                    break;
                case "integer":
                    length= 4;
                    break;
                case "double":
                    length= 8;
                    break;
                default:
                    throw new IllegalArgumentException("not supported: "+type);
            }
            lengths[i]= length;
            if ( jo.has("size") ) {
                JSONArray size= jo.getJSONArray("size");
                sizes[i]= size.getInt(0);
                for ( int j=1; j<size.length(); j++ ) {
                    sizes[i]*= size.getInt(j);
                }
            } else {
                sizes[i]= 1;
            }
            if ( i==0 ) {
                offsets[i]= 0;
            } else {
                offsets[i]= offsets[i-1] + sizes[i-1]*lengths[i-1];
            }
        }
    }
    
    /**
     * return the length of each record in bytes.
     * @return the length of each record in bytes.
     */
    public int getRecordSizeBytes() {
        int n= offsets.length;
        return offsets[n-1]+sizes[n-1]*lengths[n-1];
    }

    /**
     * convert the line into a HapiRecord, breaking the string on commas.
     * @param bbuf byte buffer containing the binary-encoded data.
     * @return a HapiRecord containing the data.
     */
    public HapiRecord convert( ByteBuffer bbuf ) {
        return new BinaryHapiRecord(info,sizes,lengths,offsets,bbuf);
    }

}
