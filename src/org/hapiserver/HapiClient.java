
package org.hapiserver;

import java.awt.EventQueue;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Utility classes for interacting with a HAPI server.  JSON
 * responses are presented using JSON, and data responses are record-by-record.
 * @author jbf
 */
public class HapiClient {
    
    private static final Logger LOGGER= Logger.getLogger("org.hapiserver");
    
    private static final Lock LOCK= new ReentrantLock();

    private HapiClient() {
        // this class is not instanciated.
    }
    
    /**
     * use cache of HAPI responses, to allow for use in offline mode.
     * @return 
     */
    protected static boolean useCache() {
        return ( "true".equals( System.getProperty("hapiServerCache","false") ) );
    }
    
    /**
     * allow cached files to be used for no more than 1 hour.
     * @return 
     */
    protected static long cacheAgeLimitMillis() {
        return 3600000;
    }
    
    protected static int getConnectTimeoutMs() {
        return 5000;
    }

    protected static int getReadTimeoutMs() {
        return 5000;
    }
    
    private static boolean offline= false;

    /**
     * return true if no web interactions, just use previously downloaded data.
     * @return 
     */
    public static boolean isOffline() {
        return offline;
    }

    /**
     * true if no web interactions, just use previously downloaded data.
     * @param offline 
     */
    public static void setOffline( boolean offline ) {
        HapiClient.offline= offline;
    }
    
    /**
     * return the local folder of the cache for HAPI data.  This will end with
     * a slash.
     * @return the local folder of the cache for HAPI data.
     */
    public static String getHapiCache() {
        String hapiCache= System.getProperty("HAPI_DATA");
        if ( hapiCache!=null ) {
            String home=System.getProperty("user.home") ;
            if ( hapiCache.contains("${HOME}") ) { // the filesystem settings used ${}, %{} seems more conventional.
                hapiCache= hapiCache.replace("${HOME}", home );
            } else if ( hapiCache.contains("%{HOME}") ) {
                hapiCache= hapiCache.replace("%{HOME}", home );
            }            
        }
        if ( hapiCache!=null && hapiCache.contains("\\") ) { // Windows path sep
            hapiCache= hapiCache.replaceAll("\\\\", "/" );
        }
        if ( hapiCache==null ) {
            String home=System.getProperty("user.home") ;
            hapiCache= home.replaceAll("\\\\", "/" );
            if ( hapiCache.endsWith("/") ) hapiCache= hapiCache.substring(0,hapiCache.length()-1);
            hapiCache= hapiCache + "/hapi/";
        }
        if ( !hapiCache.endsWith("/") ) hapiCache= hapiCache + "/";
        
        if ( useCache() ) {
            if ( !new File(hapiCache).exists() ) {
                if ( !new File(hapiCache).mkdirs() ) {
                    LOGGER.log(Level.WARNING, "unable to mkdir directories {0}", hapiCache);
                }
            }
        }
        return hapiCache;

    }
    
    /**
     * read the file into a string.  
     * @param f non-empty file
     * @return String containing file contents.
     * @throws IOException 
     */
    public static String readFromFile( File f ) throws IOException {
        StringBuilder builder= new StringBuilder();
        try ( BufferedReader in= new BufferedReader( 
                new InputStreamReader( new FileInputStream(f) ) ) ) {
            String line= in.readLine();
            while ( line!=null ) {
                builder.append(line);
                builder.append("\n");
                line= in.readLine();
            }
        }
        if ( builder.length()==0 ) {
            throw new IOException("file is empty:" + f );
        }
        String result=builder.toString();
        return result;
    }
        
    /**
     * return the resource, if cached, or null if the data is not cached.
     * @param url
     * @param type "json" (the extension) or "" for no additional extension.
     * @return the data or null.
     * @throws IOException 
     */
    public static String readFromCachedURL( URL url, String type ) throws IOException {
        
        String hapiCache= getHapiCache();
        
        String u= url.getProtocol() + "/" + url.getHost() + "/" + url.getPath();
        if ( url.getQuery()!=null ) {
            Pattern p= Pattern.compile("id=(.+)");
            Matcher m= p.matcher(url.getQuery());
            if ( m.matches() ) {
                u= u + "/" + m.group(1);
                if ( type.length()>0 ) u= u+ "." + type;
            } else {
                throw new IllegalArgumentException("query not supported, implementation error");
            }
        } else {
            if ( type.length()>0 ) u= u + "." + type;
        }
        String su= hapiCache + u;
        File f= new File(su);
        if ( f.exists() && f.canRead() ) {
            long age= System.currentTimeMillis() - f.lastModified();
            if ( age < cacheAgeLimitMillis() || isOffline() ) {
                LOGGER.log(Level.FINE, "read from hapi cache: {0}", url);
                String r= readFromFile( f );
                return r;
            } else {
                LOGGER.log(Level.FINE, "old cache item will not be used: {0}", url);
                return null;
            }
        } else {
            return null;
        }
    }

    
    /**
     * write the data (for example, an info response) to a cache file.  This is 
     * called from readFromURL to cache the data.
     * @param url the resource location, query param id is handled specially, 
     *    but others are ignored.
     * @param type "json" (the extension), or "" if no extension should be added.
     * @param data the data.
     * @throws IOException 
     */
    public static void writeToCachedURL( URL url, String type, String data ) 
            throws IOException {
        
        String hapiCache= getHapiCache();
        
        String u= url.getProtocol() + "/" + url.getHost() + "/" + url.getPath();
        String q= url.getQuery();
        if ( q!=null ) {
            if ( q.contains("resolve_references=false&") ) {
                q= q.replace("resolve_references=false&","");
            }
            Pattern p= Pattern.compile("id=(.+)");
            Matcher m= p.matcher(q);
            if ( m.matches() ) {
                u= u + "/" + m.group(1);
                if ( type.length()>0 ) {
                    u= u + "." + type;
                }
            } else {
                throw new IllegalArgumentException("query not supported, implementation error");
            }
        } else {
            if ( type.length()>0 ) {
                u= u + "." + type;
            }
        }
        
        String su= hapiCache + u;
        File f= new File(su);
        if ( f.exists() ) {
            if ( !f.delete() ) {
                throw new IOException("unable to delete file " + f );
            }
        }
        if ( !f.getParentFile().exists() ) {
            if ( !f.getParentFile().mkdirs() ) {
                throw new IOException("unable to make parent directories");
            }
        }
        if ( !f.exists() ) {
            LOGGER.log(Level.FINE, "write to hapi cache: {0}", url);
            try ( BufferedWriter w= new BufferedWriter( new FileWriter(f) ) ) {
                w.write(data);
            }
        } else {
            throw new IOException("unable to write to file: "+f);
        }
    }
    
    /**
     * read data from the URL.  
     * @param url the URL to read from
     * @param type the extension to use for the cache file (JSON, bin, txt).
     * @return non-empty string
     * @throws IOException 
     */
    public static String readFromURL( URL url, String type ) 
            throws IOException {
        
        if ( isOffline() ) {
            String s= readFromCachedURL( url, type );
            if ( s!=null ) return s;
        }
        LOGGER.log(Level.FINE, "GET {0}", new Object[] { url } );
        URLConnection urlc= url.openConnection();
        urlc.setConnectTimeout( getConnectTimeoutMs() );
        urlc.setReadTimeout( getReadTimeoutMs() );
        StringBuilder builder= new StringBuilder();
        try ( BufferedReader in= new BufferedReader( 
                new InputStreamReader( urlc.getInputStream() ) ) ) {
            String line= in.readLine();
            while ( line!=null ) {
                builder.append(line);
                builder.append("\n");
                line= in.readLine();
            }
        } catch ( IOException ex ) {
            if ( urlc instanceof HttpURLConnection ) {
                StringBuilder builder2= new StringBuilder();
                try ( BufferedReader in2= new BufferedReader( 
                        new InputStreamReader( ((HttpURLConnection)urlc).getErrorStream() ) ) ) {
                    String line= in2.readLine();
                    while ( line!=null ) {
                        builder2.append(line);
                        builder2.append("\n");
                        line= in2.readLine();
                    }
                    String s2= builder2.toString().trim();
                    if ( type.equals("json") && s2.length()>0 && s2.charAt(0)=='{' ) {
                        LOGGER.warning("incorrect error code returned, content is JSON");
                        return s2;
                    }
                } catch ( IOException ex2 ) {
                    LOGGER.log( Level.FINE, ex2.getMessage(), ex2 );
                }
            }
            LOGGER.log( Level.FINE, ex.getMessage(), ex );
            LOCK.lock();
            try {
                if ( useCache() ) {
                    String s= readFromCachedURL( url, type );
                    if ( s!=null ) return s;
                } else {
                    throw ex;
                }
            } finally {
                LOCK.unlock();
            }
        }
        
        if ( builder.length()==0 ) {
            throw new IOException("empty response from "+url );
        }
        String result=builder.toString();
        
        LOCK.lock();
        try {
            if ( useCache() ) {
                writeToCachedURL( url, type, result );
            }
        } finally {
            LOCK.unlock();
        }
        return result;
    }

    /**
     * return the catalog as a JSONObject.  For example:
     * <code><pre>
     * jo= getCatalog( URL( "https://jfaden.net/HapiServerDemo/hapi/catalog" ) )
     * print jo.get('HAPI') # "2.0"
     * catalog= jo.getJSONArray( 'catalog' )
     * for i in range(catalog.length()):
     *    print catalog.getJSONObject(i).get('id')
     * </pre></code>
     * @param server
     * @return 
     * @throws java.io.IOException 
     * @throws org.json.JSONException 
     * @see #getCatalogIdsArray
     */
    public static org.json.JSONObject getCatalog( URL server ) 
            throws IOException, JSONException {
        if ( EventQueue.isDispatchThread() ) {
            LOGGER.warning("HAPI network call on event thread");
        }        
        URL url;
        url= new URL( server, "catalog" );
        String s= readFromURL(url, "json");
        JSONObject o= new JSONObject(s);
        return o;
    }
    
    /**
     * return the catalog as a String array.
     * <code><pre>
     * catalog= getCatalogIdsArray( URL( "https://jfaden.net/HapiServerDemo/hapi/catalog" ) )
     * for s in catalog:
     *    print s
     * </pre></code>
     * @param server
     * @return
     * @throws IOException
     * @throws JSONException 
     */
    public static String[] getCatalogIdsArray( URL server ) 
            throws IOException, JSONException {
        JSONObject jo= getCatalog(server);
        JSONArray joa= jo.getJSONArray("catalog");
        
        String[] result= new String[joa.length()];
        for ( int i=0; i<joa.length(); i++ ) {
            result[i]= joa.getJSONObject(i).getString("id");
        }
        return result;
    }
    
    /**
     * get the info for the id
     * @param server
     * @param id HAPI dataset identifier, matching [a-zA-Z_]+[a-zA-Z0-9_/]*
     * @return the JSON for info
     * @throws java.io.IOException 
     * @throws org.json.JSONException 
     */
    public static org.json.JSONObject getInfo( URL server, String id ) 
            throws IOException, JSONException {
        if ( EventQueue.isDispatchThread() ) {
            LOGGER.warning("HAPI network call on event thread");
        }        
        URL url;
        try {
            url= new URL( server, "info?id="+id );
        } catch (MalformedURLException ex) {
            throw new IllegalArgumentException(ex);
        }
        
        String s= readFromURL(url, "json");
        
        JSONObject o= new JSONObject(s);
        return o;
    }
    
    /**
     * get the info for the id, for a subset of the parameters.
     * @param server
     * @param id HAPI dataset identifier, matching [a-zA-Z_]+[a-zA-Z0-9_/]*
     * @param parameters comma-separated list of parameter names.
     * @return the JSON for info
     * @throws java.io.IOException 
     * @throws org.json.JSONException 
     */
    public static JSONObject getInfo(URL server, String id, String parameters) 
            throws IOException, JSONException {
        if ( EventQueue.isDispatchThread() ) {
            LOGGER.warning("HAPI network call on event thread");
        }        
        URL url;
        try {
            url= new URL( server, "info?id="+id + "&parameters="+parameters );
        } catch (MalformedURLException ex) {
            throw new IllegalArgumentException(ex);
        }
        
        String s= readFromURL(url, "json");
        
        JSONObject o= new JSONObject(s);
        String[] ss= parameters.split(",",-2);
        
        JSONArray joa= o.getJSONArray("parameters");
        
        if ( ss.length==joa.length() || ss.length==joa.length()-1 ) {
            int ioff= joa.length()-ss.length;
            StringBuilder sb= new StringBuilder(joa.getJSONObject(ioff).getString("name"));
            for ( int i=1+ioff; i<joa.length(); i++ ) {
                sb.append(",").append(joa.getJSONObject(i).get("name"));
            }
            String sbs= sb.toString();
            if ( !sbs.equals(parameters) ) {
                throw new IllegalArgumentException("parameters must be requested in order, use instead "+sbs);
            }
        } else {
            throw new IllegalArgumentException("number of parameters in result doesn't jibe with request");
        }
        
        return o;
    }
    
    /**
     * return a list of the parameters for the id, as a String array.
     * @param server
     * @param id
     * @return
     * @throws IOException
     * @throws JSONException 
     * @see #getInfo(java.net.URL, java.lang.String) 
     */
    public static String[] getInfoParametersArray( URL server, String id ) 
            throws IOException, JSONException {
        JSONObject jo= getInfo(server, id);
        JSONArray joa= jo.getJSONArray("parameters");
        String[] result= new String[joa.length()];
        for ( int i=0; i<joa.length(); i++ ) {
            result[i]= joa.getJSONObject(i).getString("name");
        }
        return result;
    }
    
    /**
     * return the data record-by-record, using the CSV response.
     * @param server
     * @param id
     * @param startTime
     * @param endTime
     * @return Iterator, which will return records until the stream is empty.
     * @throws java.io.IOException  
     * @throws org.json.JSONException should the server return an invalid response.
     */
    public static Iterator<HapiRecord> getDataCSV( 
            URL server, 
            String id, 
            String startTime,
            String endTime ) throws IOException, JSONException {
        
        JSONObject info= getInfo( server, id );
        
        URL dataURL= new URL( server, 
                "data?id="+id 
                + "&time.min="+startTime 
                + "&time.max="+endTime );
        
        InputStream ins= dataURL.openStream();
        BufferedReader reader= new BufferedReader( new InputStreamReader(ins) );
        return new HapiClientIterator( info, reader );
        
    }
    
    /**
     * return the next day boundary.
     * @param day
     * @return 
     */
    public static String nextDay( String day ) {
        int[] nn= isoTimeToArray( day );
        nn[2]= nn[2]+1;
        normalizeTime(nn);
        return String.format( "%04d-%02d-%02dZ", 
                nn[0], nn[1], nn[2] );
                
    }
    
    /**
     * 
     * @param startTime must be $Y-$m-$dT...
     * @param stopTime must be $Y-$m-$dT...
     * @return array of times.
     */
    public static String[] countOffDays( String startTime, String stopTime ) {
        if ( stopTime.length()<10 || Character.isDigit(stopTime.charAt(10)) ) {
            throw new IllegalArgumentException("arguments must be $Y-$m-$dZ");
        }
        ArrayList<String> result= new ArrayList();
        String time= startTime.substring(0,11);
        stopTime= stopTime.substring(0,11);
        while ( time.compareTo(stopTime)<0 ) {
            result.add( time );
            time= nextDay(time);
        }
        return result.toArray( new String[result.size()] );
    }
    
//    private static void checkMissingRange( String[] trs, boolean[][] hits, String timeRange) throws IllegalArgumentException {
//        String missingRange=null;
//        for ( int i=0; i<trs.length; i++ ) {
//            for ( int j=0; j<hits[i].length; j++ ) {
//                if ( hits[i][j]==false ) {
//                    if ( missingRange==null ) {
//                        missingRange= trs[i];
//                    } else {
//                        missingRange= DatumRangeUtil.union( missingRange, trs.get(i) );
//                    }
//                }
//            }
//        }
//        LOGGER.log(Level.FINE, "missingRange={0}", missingRange);
//        if ( missingRange!=null ) {
//            if ( missingRange.min().equals(timeRange.min()) || missingRange.max().equals(timeRange.max()) ) {
//                LOGGER.log(Level.FINE, "candidate for new partial cache, only {0} needs to be loaded.", missingRange);
//            }
//        }
//    }

    private static long getEarliestTimeStamp( File[][] files ) {
        // digest all this into a single timestamp.  
        // For each day, what is the oldest any of the granules was created?
        // For each interval, what was the oldest of any granule?
        long timeStamp= Long.MAX_VALUE;
        for (File[] files1 : files) {
            for (File file1 : files1 ) {
                timeStamp = Math.min(timeStamp, file1.lastModified());
            }
        }
        return timeStamp;
    }    
    
    /**
     * 
     * @param trs list of times in $Y-$m-$dZ.
     * @param parameters parameters to read.
     * @param fs
     * @param format
     * @param hits
     * @param files
     * @param offline
     * @param lastModified
     * @return
     * @throws IOException
     * @throws IllegalArgumentException 
     */
    private static boolean getCacheFilesWithTime( 
            String[] trs, 
            String[] parameters, 
            String fs, 
            String format, 
            boolean[][] hits, 
            File[][] files, 
            boolean offline, 
            long lastModified ) throws IOException, IllegalArgumentException {
        boolean staleCacheFiles;
        long timeNow= System.currentTimeMillis();
        staleCacheFiles= false;
        for ( int i=0; i<trs.length; i++ ) {
            String tr= trs[i];
            for ( int j=0; j<parameters.length; j++ ) {
                String parameter= parameters[j];
                String sf= String.format( "%s/%s/%s%s%s.%s.%s", 
                        tr.substring(0,4), 
                        tr.substring(5,7),
                        tr.substring(0,4),
                        tr.substring(5,7),
                        tr.substring(8,10),
                        parameter, 
                        format );
                
                File f= new File( fs + sf );
                if ( !f.exists() ) {
                    f= new File( fs + sf + ".gz" );
                }
                
                if ( !f.exists() ) {
                    hits[i][j]= false;
                } else {
                    long ageMillis= timeNow - f.lastModified();
                    boolean isStale= ( ageMillis > cacheAgeLimitMillis() );
                    if ( lastModified>0 ) {
                        isStale= f.lastModified() < lastModified; // Note FAT32 only has 4sec resolution, which could cause problems.
                        if ( !isStale ) {
                            LOGGER.fine("server lastModified indicates the cache file can be used");
                        } else {
                            LOGGER.fine("server lastModified indicates the cache file should be updated");
                        }
                    }
                    if ( offline || !isStale ) {
                        hits[i][j]= true;
                        files[i][j]= f;
                    } else {
                        LOGGER.log(Level.FINE, "cached file is too old to use: {0}", f);
                        hits[i][j]= false;
                        staleCacheFiles= true;
                    }
                }
            }
        }
        return staleCacheFiles;
    }
    
    /**
     * return the data from the cache, or null if the data is not cached.
     * @param url
     * @param id
     * @param sparameters
     * @param startTime
     * @param endTime
     * @return
     * @throws IOException
     * @throws JSONException 
     */
    public static Iterator<HapiRecord> checkCache( URL url, 
            String id, 
            String sparameters,
            String startTime,
            String endTime ) throws IOException, JSONException {
        
        String[] parameters= sparameters.split(",",-2);
        
        String s= getHapiCache();
        if ( s.endsWith("/") ) s= s.substring(0,s.length()-1);
        
        StringBuilder ub= new StringBuilder( url.getProtocol() + "/" + url.getHost() + "/" + url.getPath() );
        if ( url.getQuery()!=null ) {
            String[] querys= url.getQuery().split("\\&");
            Pattern p= Pattern.compile("id=(.+)");
            for ( String q : querys ) {
                Matcher m= p.matcher(q);
                if ( m.matches() ) {
                    ub.append("/").append(m.group(1));
                    break;
                }
            }
        } else {
            throw new IllegalArgumentException("query must be specified, implementation error");
        }
        
        String aday= normalizeTimeString(startTime);
        String lday= normalizeTimeString(endTime);
        
        String[] days= countOffDays( aday, lday );
                
        // which granules are available for all parameters?
        boolean[][] hits= new boolean[days.length][parameters.length];
        File[][] files= new File[days.length][parameters.length];
        
        boolean staleCacheFiles;
        
        String u= ub.toString();
        
        if ( ! new File( s + "/" + u  ).exists() ) {
            return null;
        }
        
        staleCacheFiles= getCacheFilesWithTime( days, parameters, "csv", s, hits, files, offline, 0 );
            
        if ( staleCacheFiles && !offline ) {
            LOGGER.fine("old cache files found, but new data is available and accessible");
            return null;
        }
    
        boolean haveSomething= false;
        boolean haveAll= true;
        for ( int i=0; i<days.length; i++ ) {
            for ( int j=0; j<parameters.length; j++ ) {
                if ( hits[i][j]==false ) {
                    haveAll= false;
                }
            }
            if ( haveAll ) {
                haveSomething= true;
            }
        }
        
        if ( !haveAll ) {
            //checkMissingRange(days, hits, timeRange);
        }
        
        if ( !offline && !haveAll ) {
            LOGGER.fine("some cache files missing, but we are on-line and should retrieve all of them");
            return null;
        }
        
        if ( !haveSomething ) {
            LOGGER.fine("no cached data found");
            return null;
        }
        
        return null;
//        
//        AbstractLineReader result;
//        
//        long timeStamp= getEarliestTimeStamp(files);
//        
//        try {
//            result= maybeGetCsvCacheReader( url, files, timeStamp );
//            if ( result!=null ) return result;
//        } catch ( IOException ex ) {
//            logger.log( Level.WARNING, null, ex );
//        }
//            
//        AbstractLineReader cacheReader= calculateCsvCacheReader( files );
//        return cacheReader;
        
    }
    
    /**
     * return the data record-by-record, using the CSV response.
     * @param server
     * @param id
     * @param parameters
     * @param startTime
     * @param endTime
     * @return Iterator, which will return records until the stream is empty.
     * @throws java.io.IOException  
     * @throws org.json.JSONException should the server return an invalid response.
     */
    public static Iterator<HapiRecord> getDataCSV( 
            URL server, 
            String id, 
            String parameters,
            String startTime,
            String endTime ) throws IOException, JSONException {
        
        JSONObject info= getInfo( server, id, parameters );
        
        URL dataURL= new URL( server, 
                "data?id="+id 
                + "&parameters="+parameters 
                + "&time.min="+startTime 
                + "&time.max="+endTime );
        
        InputStream ins= dataURL.openStream();
        BufferedReader reader= new BufferedReader( new InputStreamReader(ins) );
        return new HapiClientIterator( info, reader );
        
    }
        
    /**
     * return the data record-by-record
     * @param server the HAPI server
     * @param id the dataset id
     * @param startTime the start time
     * @param endTime the end time
     * @return the records in an iterator
     * @throws java.io.IOException
     * @throws org.json.JSONException should the server return an invalid response
     */
    public static Iterator<HapiRecord> getData( 
            URL server, 
            String id, 
            String startTime,
            String endTime ) throws IOException, JSONException {
        
        //Iterator<HapiRecord> result= checkCache( server, id, startTime, endTime );
        Iterator<HapiRecord> result= null;
        
        if ( result!=null ) {
            return result;
        } else {
            return getDataCSV(server, id, startTime, endTime);
        }
    }
    
    /**
     * return the data record-by-record
     * @param server the HAPI server
     * @param id the dataset id
     * @param parameters a comma-separated list of parameter names
     * @param startTime the start time
     * @param endTime the end time
     * @return the records in an iterator
     * @throws java.io.IOException
     * @throws org.json.JSONException should the server return an invalid response
     */
    public static Iterator<HapiRecord> getData( 
            URL server, 
            String id, 
            String parameters,
            String startTime,
            String endTime ) throws IOException, JSONException {
        return getDataCSV(server, id, parameters, startTime, endTime);
    }
    
    /**
     * return the time as milliseconds since 1970-01-01T00:00Z.  This
     * does not include leap seconds.  For example, in Jython:
     * <pre>
     * {@code
     * from org.hapiserver.HapiClient import *
     * x= toMillisecondsSince1970('2000-01-02T00:00:00.0Z')
     * print x / 86400000   # 10958.0 days
     * print x % 86400000   # and no milliseconds
     * }
     * </pre>
     * @param time
     * @return number of non-leap-second milliseconds since 1970-01-01T00:00Z.
     */
    public static long toMillisecondsSince1970( String time ) {
        TemporalAccessor ta = DateTimeFormatter.ISO_INSTANT.parse(time);
        Instant i = Instant.from(ta);
        Date d = Date.from(i);
        return d.getTime();
    }
    
    /**
     * fast parser requires that each character of string is a digit.
     * @param s
     * @return 
     */
    private static int parseInt( String s ) {
        int result;
        switch ( s.length() ) {
            case 2:
                result= 10 * ( s.charAt(0)-48 ) + (s.charAt(1)-48);
                return result;
            case 3:
                result= 100 * ( s.charAt(0)-48 )
                        + 10 * ( s.charAt(1)-48 ) + (s.charAt(2)-48);
                return result;
            default:        
                result=0;
                for ( int i=0; i<s.length(); i++ ) {
                    result= 10 * result + (s.charAt(i)-48);
                }
                return result;
        }
    }

    private final static int[][] DAYS_IN_MONTH = {
        {0, 31, 28, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31, 0},
        {0, 31, 29, 31, 30, 31, 30, 31, 31, 30, 31, 30, 31, 0}
    };
    
    private final static int[][] DAY_OFFSET = {
        {0, 0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334, 365},
        {0, 0, 31, 60, 91, 121, 152, 182, 213, 244, 274, 305, 335, 366}
    };
    
    private static boolean isLeapYear( int year ) {
        if ( year<1800 || year>2400 ) {
            throw new IllegalArgumentException("year must be between 1800 and 2400");
        }
        return (year % 4)==0 && ( year%400==0 || year%100!=0 );
    }
    
    /**
     * return the doy of year of the month and day for the year.  For example,
     * <pre>
     * {@code
     * from org.hapiserver.HapiClient import *
     * print dayOfYear( 2000, 5, 29 ) # 150
     * }
     * </pre>
     * @param year the year
     * @param month the month, from 1 to 12.
     * @param day the day in the month.
     * @return the day of year.
     */
    public static int dayOfYear( int year, int month, int day ) {
        if ( month==1 ) {
            return day;
        }
        if ( month<1 ) throw new IllegalArgumentException("month must be greater than 0.");
        if ( month>12 ) throw new IllegalArgumentException("month must be less than 12.");
        int leap= isLeapYear(year) ? 1: 0;
        return DAY_OFFSET[leap][month] + day;
    }
    
    /**
     * normalize the decomposed time by expressing day of year and month
     * and day of month, and moving hour="24" into the next day.
     * @param time 
     */
    private static void normalizeTime( int[] time ) {
        if ( time[3]==24 ) {
            time[2]+= 1;
            time[3]= 0;
        }
        
        if ( time[3]>24 ) throw new IllegalArgumentException("time[3] is greater than 24 (hours)");
        if ( time[1]>12 ) throw new IllegalArgumentException("time[1] is greater than 12 (months)");
        
        int leap= isLeapYear(time[0]) ? 1: 0;

        if ( time[1]==12 && time[2]==32 ) {
            time[0]= time[0]+1;
            time[1]= 1;
            time[2]= 1;
            return;
        }
        
        int d= DAYS_IN_MONTH[leap][time[1]];
        while ( time[2]>d ) {
            time[1]++;
            time[2]-= d;
            d= DAYS_IN_MONTH[leap][time[1]];
            if ( time[1]>12 ) throw new IllegalArgumentException("time[2] is too big");
        }
        
    }
    
    /**
     * return $Y-$m-$dT$H:$M:$S.$(subsec,places=9)
     * @param time any ISO8601 string.
     * @return the time in standard form.
     */
    private static String normalizeTimeString( String time ) {
        int[] nn= isoTimeToArray( time );
        return String.format( "%d-%d-%dT%d:%d:%d.%09d", 
                nn[0], nn[1], nn[2], 
                nn[3], nn[4], nn[5], 
                nn[6] );
    }
    
    /**
     * return array [ year, months, days, hours, minutes, seconds, nanoseconds ]
     * @param time
     * @return the decomposed time
     */
    public static int[] isoTimeToArray( String time ) {
        int[] result;
        if ( time.length()==4 ) {
            result= new int[] { Integer.parseInt(time), 1, 1, 0, 0, 0, 0 };
        } else {
            if ( time.length()<8 ) throw new IllegalArgumentException("time must have 4 or greater than 7 elements");
            if ( time.charAt(8)=='T' ) {
                result= new int[] { 
                    parseInt(time.substring(0,4)),
                    1, 
                    parseInt(time.substring(5,8)),  // days
                    0, 
                    0, 
                    0,
                    0 
                };
                time= time.substring(9);
            } else {
                result= new int[] { 
                    parseInt(time.substring(0,4)), 
                    parseInt(time.substring(5,7)),
                    parseInt(time.substring(8,10)),
                    0,
                    0,
                    0,
                    0
                };
                time= time.substring(11);
            }
            if ( time.endsWith("Z") ) time= time.substring(0,time.length()-1);
            if ( time.length()>=2 ) {
                result[3]= parseInt(time.substring(0,2));
            }
            if ( time.length()>=5 ) {
                result[4]= parseInt(time.substring(3,5));
            }
            if ( time.length()>=8 ) {
                result[5]= parseInt(time.substring(6,8));
            } 
            if ( time.length()>9 ) {
                result[6]= (int)( Math.pow( 10, 18-time.length() ) ) * parseInt(time.substring(9));
            }
            normalizeTime(result);
        }
        return result;        
    }
    
    
}
