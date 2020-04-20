
package org.hapiserver;

import java.util.Iterator;

/**
 * Return only records within startTime to endTime, more specifically
 * records greater than or equal to startTime, and less than endTime.
 * @author jbf
 */
public class TrimHapiRecordIterator implements Iterator<HapiRecord>  {

    String startTime;
    String endTime;
    
    Iterator<HapiRecord> source;
    HapiRecord nextRecord=null;
    
    public TrimHapiRecordIterator( Iterator<HapiRecord> source, String startTime, String endTime )  {
        this.source= source;
        this.startTime= startTime;
        this.endTime= endTime;
    }
    
    private String rewriteIsoTime( String time, String exampleForm ) {
        char c= exampleForm.charAt(8);
        int[] nn= TimeUtil.isoTimeToArray(TimeUtil.normalizeTimeString(time));
        switch (c) {
            case 'T':
                // $Y-$jT
                nn[2]= TimeUtil.dayOfYear( nn[0], nn[1], nn[2] );
                nn[1]= 1;
                time= String.format( "%d-%03dT%02d:%02d:%02d.%09dZ",
                        nn[0], nn[2],
                        nn[3], nn[4], nn[5],
                        nn[6] );
                break;
            case 'Z':
                nn[2]= TimeUtil.dayOfYear( nn[0], nn[1], nn[2] );
                nn[1]= 1;
                time= String.format( "%d-%03dZ",
                        nn[0], nn[2] );
                break;
            default:
                c= exampleForm.charAt(10);
                if ( c=='T' ) { // $Y-$jT
                    time= String.format( "%d-%02d-%02dT%02d:%02d:%02d.%09dZ",
                            nn[0], nn[1], nn[2],
                            nn[3], nn[4], nn[5],
                            nn[6] );
                } else if ( c=='Z' ) {
                    time= String.format( "%d-%02d-%02dZ",
                            nn[0], nn[1], nn[2] );
                }   
                break;
        }
        if ( exampleForm.endsWith("Z") ) {
            return time.substring(0,exampleForm.length()-1)+"Z";
        } else {
            return time.substring(0,exampleForm.length());
        }
    }
    /**
     * rewrite the startTime and endTime to the same format as the source,
     * so simple string comparisons can be made.
     */
    private void initializeFirstRec() {
        startTime= rewriteIsoTime( startTime, nextRecord.getIsoTime(0) );
        endTime= rewriteIsoTime( endTime, nextRecord.getIsoTime(0) );
    }
    
    private void advanceToStartTime() {
        while ( nextRecord.getIsoTime(0).compareTo(startTime)<0 ) {
            if ( source.hasNext() ) {
                nextRecord= source.next();
            } else {
                nextRecord= null;
                return;
            }
        }
        
    }
    @Override
    public boolean hasNext() {
        if ( nextRecord==null ) {
            if ( !source.hasNext() ) {
                return false;
            }
            nextRecord= source.next();
            initializeFirstRec();
            advanceToStartTime();
        }
        if ( nextRecord==null ) {
            // we run off the end of source.
            return false;
        } else {
            return nextRecord.getIsoTime(0).compareTo(endTime)<0;
        }
    }

    @Override
    public HapiRecord next() {
        HapiRecord result= nextRecord;
        if ( source.hasNext() ) {
            nextRecord= source.next();
        } else {
            nextRecord= null;
        }
        return result;
    }
    
}
