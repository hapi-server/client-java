
package com.cottagesystems.util;

import java.io.BufferedReader;
import java.io.IOException;

/**
 * Wrap an ordinary reader to provide AbstractLineReader interface.
 * @author jbf
 */
public class SingleFileBufferedReader implements AbstractLineReader {

    BufferedReader reader;
    
    public SingleFileBufferedReader(BufferedReader reader) {
        this.reader= reader;
    }
            
    @Override
    public String readLine() throws IOException {
        return reader.readLine();
    }

    @Override
    public void close() throws IOException {
        reader.close();
    }
    
}
