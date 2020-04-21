
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
    
    /**
     * rewrite the startTime and endTime to the same format as the source,
     * so simple string comparisons can be made.
     */
    private void initializeFirstRec() {
        startTime= TimeUtil.reformatIsoTime(nextRecord.getIsoTime(0), startTime );
        endTime= TimeUtil.reformatIsoTime(nextRecord.getIsoTime(0), endTime );
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
