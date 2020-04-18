
package org.hapiserver;

import com.cottagesystems.util.AbstractLineReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.NoSuchElementException;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * read records from the AbstractLineReader and convert them into HapiRecords.
 * @author jbf
 */
public class LineReaderHapiRecordIterator implements Iterator<HapiRecord> {
    
    String nextLine;
    JSONObject info;
    AbstractLineReader reader;
    CSVHapiRecordConverter converter;
    
    /**
     * create an iterator for the CSV stream.
     * @param info
     * @param reader
     * @throws IOException 
     * @throws org.json.JSONException 
     */
    public LineReaderHapiRecordIterator(JSONObject info, AbstractLineReader reader) throws IOException, JSONException {
        this.info= info;
        this.reader = reader;
        this.nextLine = this.reader.readLine();
        this.converter= new CSVHapiRecordConverter(info);
    }

    @Override
    public boolean hasNext() {
        boolean result = nextLine != null;
        return result;
    }

    @Override
    public HapiRecord next() {
        if (this.nextLine == null) {
            throw new NoSuchElementException("No more records");
        }        
        HapiRecord result = this.converter.convert(this.nextLine);
        try {
            nextLine = reader.readLine();
            if ( nextLine==null ) {
                reader.close();
            }
        } catch (IOException ex) {
            throw new IllegalArgumentException(ex);
        }
        return (HapiRecord) result;
    }
    
}
