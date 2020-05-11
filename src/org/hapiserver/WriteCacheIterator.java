
package org.hapiserver;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Iterator which writes to cache file as records come in.  Note JSON
 * exceptions are treated as runtime exceptions.
 * @author jbf
 */
public class WriteCacheIterator implements Iterator<HapiRecord> {

    private static final Logger logger= Logger.getLogger("org.hapiserver");
    
    private final Iterator<HapiRecord> source;
    private String startTime;
    private String endTime;
    
    private boolean doInitialize= true;
    
    /**
     * indicator of the current interval we are writing to, null indicates not writing.
     */
    private String currentTag= null; 
    private String tempEndTime;
    
    private final File cacheRoot;
    private final String pid;
    
    private File channel;
    private PrintWriter fout;
    
    private String[] names;
    private File[] channels;
    private PrintWriter[] fouts;
    private final boolean separateChannels;
    
    JSONObject info;
    
    public WriteCacheIterator( JSONObject info, 
            Iterator<HapiRecord> source, 
            String startTime, 
            String endTime, 
            File cacheRoot, 
            boolean separateChannels ) {
        
        this.source= source;
        // cache complete days.
        this.startTime= TimeUtil.ceil(startTime);
        this.endTime= TimeUtil.floor(endTime);
        this.info= info;
        this.cacheRoot= cacheRoot;
        this.pid= com.cottagesystems.util.Util.getProcessId("99999");
        this.separateChannels = separateChannels;
    }

    /**
     * rewrite the startTime and endTime to the same format as the source,
     * so simple string comparisons can be made.
     * @param record an example record
     */
    private void initializeFirstRec( HapiRecord record ) {
        startTime= TimeUtil.reformatIsoTime(record.getIsoTime(0), startTime );
        endTime= TimeUtil.reformatIsoTime(record.getIsoTime(0), endTime );
        doInitialize= false;
        try {
            JSONArray parameters= info.getJSONArray("parameters");
            names= new String[record.length()];
            for ( int i=0; i<names.length; i++ ) {
                names[i]= parameters.getJSONObject(i).getString("name");
            }
        } catch (JSONException ex) {
            logger.log(Level.SEVERE, null, ex);
        }
    }
    
    @Override
    public boolean hasNext() {
        boolean hasNext=source.hasNext();
        if ( hasNext==false && channel!=null ) { // shutdown
            if ( separateChannels ) {
                for ( PrintWriter fout1 : fouts ) fout1.close();
            } else {
                fout.close();
            }
        }
        return hasNext;
    }

    /**
     * return the name of the file within the cache.
     * @param cacheRoot the root for the HAPI cache
     * @param currentTag the current tag $Y$m$d
     * @param name the parameter name, or empty string
     * @return the file
     */
    private static File getCacheFile( File cacheRoot, String currentTag, String name ) {
        String dirWithinCache= currentTag.substring(0,4)+"/"+currentTag.substring(4,6);
        File dir= new File(cacheRoot,dirWithinCache);
        if ( name.length()>0 ) {
            return new File( dir, currentTag + "." + name + ".csv" );
        } else {
            return new File( dir, currentTag + ".csv" );
        }
    }
    
    
    @Override
    public HapiRecord next() {
        HapiRecord record= source.next();
        if ( doInitialize ) {
            initializeFirstRec(record);
        }
        if ( currentTag!=null && tempEndTime!=null && tempEndTime.compareTo( record.getIsoTime(0) )<=0 ) {
            if ( separateChannels ) {
                for ( int i=0; i<fouts.length; i++ ) {
                    PrintWriter fout1 = fouts[i];
                    fout1.close();
                    install( channels[i] );
                }
                channels=null;
            } else {
                fout.close();
                install( channel );
                channel= null;
            }
            currentTag= null;
        }
        
        if ( currentTag==null &&
                startTime.compareTo( record.getIsoTime(0) )<=0 &&
                endTime.compareTo( record.getIsoTime(0) )>0 ) {
            String tag=  TimeUtil.reformatIsoTime( "2000-01-01Z", record.getIsoTime(0) );
            currentTag= tag.substring(0,4) + tag.substring(5,7) + tag.substring( 8,10 );
            if ( separateChannels ) {
                channels= new File[names.length];
                fouts= new PrintWriter[names.length];
                for ( int i=0; i<names.length; i++ ) {
                    channels[i]= getCacheFile( cacheRoot, currentTag, names[i] );
                    try {
                        if ( !channels[i].getParentFile().exists() ) {
                            if ( !channels[i].getParentFile().mkdirs() ) {
                                logger.log(Level.WARNING, "unable to make directory for {0}", channels[i]);
                            }
                        }
                        fouts[i]= new PrintWriter(channels[i] + ".writing."+pid  );
                    } catch (FileNotFoundException ex) {
                        logger.log(Level.SEVERE, null, ex);
                    }
                }
            } else {
                channel= getCacheFile( cacheRoot, currentTag, "" );
                try {
                    if ( !channel.getParentFile().exists() ) {
                        if ( !channel.getParentFile().mkdirs() ) {
                            logger.log(Level.WARNING, "unable to make directory for {0}", channels);
                        }
                    }
                    fout= new PrintWriter( channel + ".writing."+pid );
                } catch (FileNotFoundException ex) {
                    logger.log(Level.SEVERE, null, ex);
                }
            }
            tempEndTime= TimeUtil.reformatIsoTime( record.getIsoTime(0), TimeUtil.nextDay(record.getIsoTime(0)) );
        }
        
        if ( currentTag!=null ) {
            if ( separateChannels ) {
                for ( int i=0; i<names.length; i++ ) {
                    fouts[i].println(record.getAsString(i));
                }
            } else {
                StringBuilder b= new StringBuilder( record.getAsString(0) );
                for ( int i=1; i<names.length; i++ ) {
                    b.append(",").append( record.getAsString(i) );
                }
                fout.println(b.toString());
            }
        }
        
        return record;
    }
        
    private void install(File channel) {
        String name= channel.getAbsolutePath();
        File ch= new File( name + ".writing."+pid );
        if ( !ch.exists() ) {
            throw new IllegalArgumentException("incorrect name, should have found "+ch);
        } 
        if ( !ch.renameTo( new File(name) ) ) {
            if ( !ch.delete() ) {
                logger.info("temporary file left in cache");
            } else {
                logger.info("Unable to install file in cache");
            }
        }
    }

}
