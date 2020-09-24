/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.hapiserver;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Iterator;
import java.util.NoSuchElementException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 *
 * @author jbf
 */
public class HapiClientBinaryIterator implements Iterator<HapiRecord> {
    
    ByteBuffer nextRecord;
    JSONObject info;
    InputStream ins;
    BinaryHapiRecordConverter converter;
    byte[] buff;
    int recSize;
    
    public HapiClientBinaryIterator(JSONObject info, InputStream ins) throws IOException, JSONException {
        this.ins= ins;
        converter= new BinaryHapiRecordConverter(info);
        this.recSize= converter.getRecordSizeBytes();
        this.buff= new byte[recSize];
        this.nextRecord= readNextRecord( buff, recSize );
//        if ( this.nextLine!=null && this.nextLine.startsWith("{") ) {
//            StringBuilder b= new StringBuilder(this.nextLine);
//            this.nextLine= readJSON( reader, b );
//            if ( this.nextLine==null ) {
//                reader.close();
//            }
//            JSONObject o= new JSONObject(b.toString());
//            if ( !o.has("status") ) {
//                throw new IllegalArgumentException("expected to see status in JSON response.");
//            } else {
//                JSONObject status= o.getJSONObject("status");
//                int statusCode= status.getInt("code");
//                if ( statusCode==1201 ) {
//                    logger.fine("do nothing, this.nextLine is already set.");
//                } else {
//                    throw new IOException("error from server: "+statusCode+ " " +status.getString("message") );
//                }
//            }
//        }
        
        
    }

    private ByteBuffer readNextRecord( byte[] buff, int recSize ) throws IOException {
        int bytesRead= ins.read( buff, 0, recSize );
        int totalBytesRead= bytesRead;
        if ( bytesRead==-1 ) {
            return null;
        } else {
            while ( totalBytesRead<recSize ) {
                bytesRead= ins.read( buff, bytesRead, recSize-bytesRead );
                if ( bytesRead==-1 ) {
                    throw new IllegalArgumentException("partial record read");
                }
                totalBytesRead+= bytesRead;
            }
            ByteBuffer result= ByteBuffer.wrap(buff);
            result.order(ByteOrder.LITTLE_ENDIAN);
            return result;
        }
        
    }
    
    @Override
    public boolean hasNext() {
        boolean result = nextRecord != null;
        return result;
    }

    @Override
    public HapiRecord next() {
        if (this.nextRecord == null) {
            throw new NoSuchElementException("No more records");
        }
        
        HapiRecord result = this.converter.convert(this.nextRecord);
        result.getDouble(1);
        try {
            this.nextRecord= readNextRecord( buff, recSize );

        } catch (IOException ex) {
            throw new IllegalArgumentException(ex);
        }

        return result;

    }
    
}
