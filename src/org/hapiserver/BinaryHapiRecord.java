
package org.hapiserver;

import java.nio.ByteBuffer;
import org.json.JSONObject;

/**
 *
 * @author jbf
 */
public class BinaryHapiRecord implements HapiRecord {

    int[] sizes;
    int[] lengths;
    int[] offsets;
    ByteBuffer bbuf;
    
    private static final int LEN_DOUBLE=8;
    private static final int LEN_INT=4;
    
    public BinaryHapiRecord( JSONObject info, int[] sizes, int[] lengths, int[] offsets, ByteBuffer bbuf ) {
        this.bbuf= bbuf;
        this.sizes= sizes;
        this.lengths= lengths;
        this.offsets= offsets;
    }
    
    @Override
    public String getIsoTime(int i) {
        return getString(i);
    }

    @Override
    public String[] getIsoTimeArray(int i) {
        return getStringArray(i);
    }

    @Override
    public String getString(int i) {
        int offs= this.offsets[i];
        int lens= this.lengths[i];
        return new String( this.bbuf.array(), offs, lens );
    }

    @Override
    public String[] getStringArray(int i) {
        int n= this.sizes[i];
        int offs= this.offsets[i];
        int lens= this.lengths[i];
        String[] result= new String[n];
        for ( int j=0; j<n; j++ ) {
            String s= new String( this.bbuf.array(), offs + j*lens, lens );
            result[j]= s;
        }
        return result;
    }

    @Override
    public double getDouble(int i) {
        return bbuf.getDouble(offsets[i]);
    }

    @Override
    public double[] getDoubleArray(int i) {
        int n= this.sizes[i];
        double[] result= new double[n];
        for ( int j=0; j<n; j++ ) {
            result[j]= bbuf.getDouble(offsets[i]+j*LEN_DOUBLE);
        }
        return result;
    }

    @Override
    public int getInteger(int i) {
        return bbuf.getInt(offsets[i]);
    }

    @Override
    public int[] getIntegerArray(int i) {
        int n= this.sizes[i];
        int[] result= new int[n];
        for ( int j=0; j<n; j++ ) {
            result[j]= bbuf.getInt(offsets[i]+j*LEN_INT);
        }
        return result;
    }

    @Override
    public String getAsString(int i) {
        throw new UnsupportedOperationException("this should only be used with CSV");
    }

    /**
     * get the value as a byte buffer.
     * @param i the index of the column
     * @return the string value.
     */
    public ByteBuffer getAsByteBuffer(int i) {
        int offs= this.offsets[i];
        int lens= this.lengths[i];
        bbuf.position(offs);
        bbuf.limit(offs+lens);
        return bbuf.slice();        
    }
    
    @Override
    public int length() {
        return offsets.length;
    }
}
