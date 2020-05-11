
package org.hapiserver;

import com.cottagesystems.util.AbstractLineReader;
import com.cottagesystems.util.ConcatenateBufferedReader;
import com.cottagesystems.util.PasteBufferedReader;
import com.cottagesystems.util.SingleFileBufferedReader;
import java.awt.EventQueue;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.GZIPInputStream;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Utility classes for interacting with a HAPI server.  JSON
 * responses are presented using JSON, and data responses are record-by-record.
 * @author jbf
 */
public class HapiClient {
    
    private static final Logger logger= Logger.getLogger("org.hapiserver");
    
    /**
     * special logger used for http connections.
     */
    private static Logger loggerUrl= Logger.getLogger("org.hapiserver.url");
    
    private static final Lock LOCK= new ReentrantLock();

    private HapiClient() {
        // this class is not instanciated.
    }
    
    /**
     * use cache of HAPI responses, to allow for use in offline mode.
     * @return true if the cache can be used.
     */
    protected static boolean useCache() {
        return ( "true".equals( System.getProperty("hapiServerCache","false") ) );
    }
    
    /**
     * allow cached files to be used for no more than 1 hour.
     * @return the number of milliseconds a cached resource should be used.
     */
    protected static long cacheAgeLimitMillis() {
        return 3600000;
    }
    
    /**
     * number of milliseconds before assuming a web connection is not available.
     * @return number of milliseconds before assuming a web connection is not available.
     */
    protected static int getConnectTimeoutMs() {
        return 5000;
    }

    /**
     * number of milliseconds allowed during a download.
     * @return number of milliseconds before assuming a web connection is not available.
     */    
    protected static int getReadTimeoutMs() {
        return 5000;
    }
    
    private static boolean offline= false;

    /**
     * return true if there should be no web interactions, and only previously downloaded data should be used.
     * @return true if there should be no web interactions
     */
    public static boolean isOffline() {
        return offline;
    }

    /**
     * true if no web interactions, just use previously downloaded data.
     * @param offline true if there should be no web interactions.
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
                    logger.log(Level.WARNING, "unable to mkdir directories {0}", hapiCache);
                }
            }
        }
        return hapiCache;

    }
    
    /**
     * read the file into a string.  
     * @param f non-empty file
     * @return String containing file contents.
     * @throws IOException if there is an issue reading the file.
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
     * @param url the URL to read from the cache, with query parameters like "id".
     * @param type "json" (the extension) or "" for no additional extension.
     * @return the data or null.
     * @throws IOException if there is an issue reading data from the file cache.
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
                logger.log(Level.FINE, "read from hapi cache: {0}", url);
                String r= readFromFile( f );
                return r;
            } else {
                logger.log(Level.FINE, "old cache item will not be used: {0}", url);
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
     * @throws IOException if there is an issue writing data to the file cache.
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
            logger.log(Level.FINE, "write to hapi cache: {0}", url);
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
     * @throws IOException if there is an issue reading data to the file cache.
     */
    public static String readFromURL( URL url, String type ) 
            throws IOException {
        
        if ( isOffline() ) {
            String s= readFromCachedURL( url, type );
            if ( s!=null ) return s;
        }
        logger.log(Level.FINE, "GET {0}", new Object[] { url } );
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
                // attempt to read the error stream so that can be indicated.
                InputStream err= ((HttpURLConnection)urlc).getErrorStream(); 
                if ( err!=null ) {
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
                            logger.warning("incorrect error code returned, content is JSON");
                            return s2;
                        }
                    } catch ( IOException ex2 ) {
                        logger.log( Level.FINE, ex2.getMessage(), ex2 );
                    }
                } else {
                    throw ex;
                }
            }
            logger.log( Level.FINE, ex.getMessage(), ex );
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
    
    private static Iterator<HapiRecord> calculateCsvCacheReader( 
            JSONObject info, File[][] filess) throws IOException, JSONException {
        
        if ( info.getJSONArray("parameters").length()!=filess[0].length ) {
            throw new IllegalArgumentException("parameters length doesn't equal filess length, something has gone wrong.");
        }
        ConcatenateBufferedReader cacheReader= new ConcatenateBufferedReader();
        for (File[] files : filess) {
            boolean haveAllForDay= true;
            if (haveAllForDay) {
                PasteBufferedReader r1= new PasteBufferedReader();
                r1.setDelim(',');
                for (File file : files) {
                    try {
                        InputStreamReader oneDayOneParam;
                        if ( file.getName().endsWith(".gz") ) {
                            oneDayOneParam= new InputStreamReader( 
                                            new GZIPInputStream( 
                                                    new FileInputStream(file)));
                        } else {
                            oneDayOneParam= new FileReader(file);
                        }
                        r1.pasteBufferedReader( 
                                new SingleFileBufferedReader( 
                                        new BufferedReader(oneDayOneParam) ) );
                    }catch ( IOException ex ) {
                        logger.log( Level.SEVERE, ex.getMessage(), ex );
                        return null;
                    }
                }
                cacheReader.concatenateBufferedReader(r1);
            }   
        }
        return new LineReaderHapiRecordIterator( info, cacheReader );
    }
        
    
    /**
     * connect to the server, letting it know that you have cached files which
     * were last modified as of a certain date.  If the server suggests these
     * be used with a 304 response, then return an implementation based on 
     * these.  This might also simply return the data (200), and an iterator is
     * returned for that, possibly decompressing the data.
     * @param url
     * @param files
     * @param timeStamp
     * @return
     * @throws IOException 
     */
    private static Iterator<HapiRecord> maybeGetDataFromCache(
            JSONObject info,
            URL url, 
            File[][] files, 
            long lastModified) throws IOException, JSONException {
        
        if ( info.getJSONArray("parameters").length()!=files[0].length ) {
            throw new IllegalArgumentException("parameters length doesn't equal files length, something has gone wrong.");
        }
        
        HttpURLConnection httpConnect;
        if ( isOffline() ) {
            return null;
            //throw new FileSystem.FileSystemOfflineException("file system is offline");
        } else {
            loggerUrl.log(Level.FINE, "GET {0}", new Object[] { url } );            
            httpConnect= (HttpURLConnection)url.openConnection();
            httpConnect.setConnectTimeout(getConnectTimeoutMs());
            httpConnect.setReadTimeout(getReadTimeoutMs());
            httpConnect.setRequestProperty( "Accept-Encoding", "gzip" );
            String s= new SimpleDateFormat("EEE, d MMM yyyy HH:mm:ss z").format(new Date(lastModified));
            httpConnect.setRequestProperty( "If-Modified-Since", s );
            //TODOhttpConnect= (HttpURLConnection)HttpUtil.checkRedirect(httpConnect);
            httpConnect.connect();
            loggerUrl.log(Level.FINE, "--> {0} {1}", 
                    new Object[]{httpConnect.getResponseCode(), 
                        httpConnect.getResponseMessage()});
        }
        if ( httpConnect.getResponseCode()==304 ) {
            logger.fine("using cache files because server says nothing has changed (304)");
            return calculateCsvCacheReader( info, files );
        }
        boolean gzip= "gzip".equals( httpConnect.getContentEncoding() );
        BufferedReader reader= new BufferedReader( 
            new InputStreamReader( gzip ? 
                    new GZIPInputStream( httpConnect.getInputStream() ) : 
                    httpConnect.getInputStream() ) );
        AbstractLineReader result= new SingleFileBufferedReader( reader );
        return new LineReaderHapiRecordIterator( info, result );
    }        
    
    /**
     * concat the functional area knowing that hapi is a directory not a file.
     * @param server the base URL, for example URL('https://cdaweb.gsfc.nasa.gov/hapi')
     * @param function the function to append, for example "info"
     * @return the URL properly built.
     */
    private static URL url( URL server, String function ) {
        try {
            if ( server.getFile().endsWith("/") ) {
                return new URL( server.toString() + function );
            } else {
                return new URL( server.toString() + "/" + function );
            }
        } catch ( MalformedURLException ex ) {
            throw new RuntimeException(ex);
        }
    }
    
    /**
     * return the catalog as a JSONObject.  For example:
     * <pre>
     * {@code
     * jo= getCatalog( URL( "https://jfaden.net/HapiServerDemo/hapi/catalog" ) )
     * print jo.get('HAPI') # "2.0"
     * catalog= jo.getJSONArray( 'catalog' )
     * for i in range(catalog.length()):
     *    print catalog.getJSONObject(i).get('id')
     * }
     * </pre>
     * @param server the server URL, ending with "hapi".
     * @return the catalog as a JSON response.
     * @throws java.io.IOException IOException when there is an issue reading the data.
     * @throws org.json.JSONException should the server return an invalid response
     * @see #getCatalogIdsArray
     */
    public static org.json.JSONObject getCatalog( URL server ) 
            throws IOException, JSONException {
        if ( EventQueue.isDispatchThread() ) {
            logger.warning("HAPI network call on event thread");
        }        
        URL url;
        url= url( server, "catalog" );
        String s= readFromURL(url, "json");
        JSONObject o= new JSONObject(s);
        return o;
    }
    
    /**
     * return the catalog as a String array.
     * <pre>
     * {@code
     * catalog= getCatalogIdsArray( URL( "https://jfaden.net/HapiServerDemo/hapi/catalog" ) )
     * for s in catalog:
     *    print s
     * }
     * </pre>
     * @param server the server URL, ending with "hapi".
     * @return the dataset ids for the server.
     * @throws java.io.IOException IOException when there is an issue reading the data.
     * @throws org.json.JSONException should the server return an invalid response
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
     * @param server the server URL, ending with "hapi".
     * @param id HAPI dataset identifier, matching [a-zA-Z_]+[a-zA-Z0-9_/]*
     * @return the JSON for info
     * @throws java.io.IOException IOException when there is an issue reading the data.
     * @throws org.json.JSONException should the server return an invalid response
     */
    public static org.json.JSONObject getInfo( URL server, String id ) 
            throws IOException, JSONException {
        if ( EventQueue.isDispatchThread() ) {
            logger.warning("HAPI network call on event thread");
        }        
        URL url;
        url= url( server, "info?id="+id );
        
        String s= readFromURL(url, "json");
        
        JSONObject o= new JSONObject(s);
        
        JSONObject status= o.getJSONObject("status");
        if ( status.getInt("code")!=1200 ) {
            throw new HapiException(status);
        }
                
        return o;
    }
    
    /**
     * get the info for the id, for a subset of the parameters.
     * @param server the server URL, ending with "hapi".
     * @param id HAPI dataset identifier, matching [a-zA-Z_]+[a-zA-Z0-9_/]*
     * @param parameters comma-separated list of parameter names.
     * @return the JSON for info
     * @throws java.io.IOException IOException when there is an issue reading the data.
     * @throws org.json.JSONException should the server return an invalid response
     */
    public static JSONObject getInfo(URL server, String id, String parameters) 
            throws IOException, JSONException {
        if ( EventQueue.isDispatchThread() ) {
            logger.warning("HAPI network call on event thread");
        }        
        URL url;
        url= url( server, "info?id="+id + "&parameters="+parameters );
        
        String s= readFromURL(url, "json");
        
        JSONObject o= new JSONObject(s);
        
        JSONObject status= o.getJSONObject("status");
        if ( status.getInt("code")!=1200 ) {
            throw new HapiException(status);
        }
        
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
                throw new IllegalArgumentException(
                        "parameters must be requested in order, use instead "+sbs);
            }
        } else {
            throw new IllegalArgumentException(
                    "number of parameters in result doesn't jibe with request");
        }
        
        return o;
    }
    
    /**
     * return a list of the parameters for the id, as a String array.
     * @param server the server URL, ending with "hapi".
     * @param id the dataset id.
     * @return the parameters identifying each column of the dataset.
     * @throws java.io.IOException IOException when there is an issue reading the data.
     * @throws org.json.JSONException should the server return an invalid response
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
     * Figure out which files from the cache can be used.
     * @param trs list of times in $Y-$m-$dZ.
     * @param parameters parameters to read.
     * @param cacheRootForDataset file system which contains the cached data.
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
            String id,
            String[] parameters, 
            String cacheRootForDataset, 
            String format, 
            boolean[][] hits, 
            File[][] files, 
            boolean offline, 
            long lastModified ) throws IOException, IllegalArgumentException {
        
        if ( !cacheRootForDataset.endsWith("/") ) {
            throw new IllegalArgumentException("fs must end in slash (/)");
        }
        
        if ( trs.length>0 && trs[0].charAt(10)!='Z' ) {
            throw new IllegalArgumentException("times must be $Y-$m-$dZ");
        }
        
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
                
                File f= new File( cacheRootForDataset + "/" + sf );
                if ( !f.exists() ) {
                    f= new File( cacheRootForDataset + "/" + sf + ".gz" );
                }
                
                if ( !f.exists() ) {
                    hits[i][j]= false;
                } else {
                    long ageMillis= timeNow - f.lastModified();
                    boolean isStale= ( ageMillis > cacheAgeLimitMillis() );
                    if ( lastModified>0 ) {
                        isStale= f.lastModified() < lastModified; // Note FAT32 only has 4sec resolution, which could cause problems.
                        if ( !isStale ) {
                            logger.fine("server lastModified indicates the cache file can be used");
                        } else {
                            logger.fine("server lastModified indicates the cache file should be updated");
                        }
                    }
                    if ( offline || !isStale ) {
                        hits[i][j]= true;
                        files[i][j]= f;
                    } else {
                        logger.log(Level.FINE, "cached file is too old to use: {0}", f);
                        hits[i][j]= false;
                        staleCacheFiles= true;
                    }
                }
            }
        }
        return staleCacheFiles;
    }
    
    /**
     * return the data from the cache, or null if the data is not cached.  There
     * are three modes of caching:
     * <ul>
     * <li> no cache file exists in our cache and we need to download it (populating the cache).  
     * <li> the file exists and we need to request the data from the server with an if-modified-since keyword.
     * <li> the file exists and we don't have internet access, so use it.
     * <li> the have checked recently and we can trivially use what's in the cache.
     * </ul>
     * @param info the info response for the data request.
     * @param url the URL which will be used to load data, not just the HAPI server location.
     * @param id the dataset id.
     * @param startTime the start time
     * @param endTime then end time.
     * @return null or an iterator
     * @throws IOException
     * @throws JSONException 
     */
    private static Iterator<HapiRecord> checkCache( 
            JSONObject info,
            URL url, 
            String id, 
            String startTime,
            String endTime ) throws IOException, JSONException {
        
        String[] parameters;
        JSONArray array= info.getJSONArray("parameters");
        parameters= new String[array.length()];
        for ( int i=0; i<array.length(); i++ ) {
            parameters[i]= array.getJSONObject(i).getString("name");
        }
        
        String s= getHapiCache();
        
        StringBuilder ub= new StringBuilder( 
                url.getProtocol() + "/" + url.getHost() + "/" + url.getPath() );
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
                
        String[] days= TimeUtil.countOffDays( startTime, endTime );
                
        // which granules are available for all parameters?
        boolean[][] hits= new boolean[days.length][parameters.length];
        File[][] files= new File[days.length][parameters.length];
        
        boolean staleCacheFiles;
        
        String u= ub.toString();
        
        String cacheRootForDataset= s + u + "/";
        if ( ! new File( cacheRootForDataset ).exists() ) {
            // nothing in the cache, give up immediately.
            return null;
        }
        
        staleCacheFiles= getCacheFilesWithTime( days, id, parameters, 
                cacheRootForDataset, "csv", hits, files, offline, 0 );
            
        if ( staleCacheFiles && !offline ) {
            logger.fine("old cache files found, but new data is available and accessible");
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
            logger.fine("some cache files missing, but we are on-line and should retrieve all of them");
            return null;
        }
        
        if ( !haveSomething ) {
            logger.fine("no cached data found");
            return null;
        }
        
        Iterator<HapiRecord> result;

        long timeStamp= getEarliestTimeStamp(files);
        
        try {
            result= maybeGetDataFromCache( info, url, files, timeStamp );
            if ( result!=null ) {
                result= new TrimHapiRecordIterator(result,startTime,endTime);
                return result;
            }
        } catch ( IOException ex ) {
            logger.log( Level.WARNING, null, ex );
        }
        
        result= calculateCsvCacheReader( info, files );
        return result;
        
    }
    
    /**
     * return the data record-by-record, using the CSV response.
     * @param server the server URL, ending with "hapi".
     * @param id the dataset id to read.
     * @param startTime the start time, in isoTime.
     * @param endTime the end time, in isoTime.
     * @return Iterator, which will return records until the stream is empty.
     * @throws IOException when there is an issue reading the data.
     * @throws org.json.JSONException should the server return an invalid response.
     */
    public static Iterator<HapiRecord> getDataCSV( 
            URL server, 
            String id, 
            String startTime,
            String endTime ) throws IOException, JSONException {
        
        JSONObject info= getInfo( server, id );

        URL dataURL= url( server, 
            "data?id="+id 
            + "&time.min="+startTime 
            + "&time.max="+endTime );
        
        Iterator<HapiRecord> result= checkCache( info, dataURL, id, startTime, endTime );
        if ( result!=null ) {
            return result;
        } else {
            InputStream ins= dataURL.openStream();
            BufferedReader reader= new BufferedReader( new InputStreamReader(ins) );
            result= new HapiClientIterator( info, reader );
            File cache= Paths.get( getHapiCache(), 
                    dataURL.getProtocol(),
                    dataURL.getHost(),
                    dataURL.getPath(), 
                    id ).toFile();
            result= new WriteCacheIterator( info, result, startTime, endTime, cache, false );
            return result;
        }
    }
    
    /**
     * return the data record-by-record, using the CSV response.
     * @param server the server URL, ending with "hapi".
     * @param id the dataset id to read.
     * @param parameters the parameters, comma separated to read.
     * @param startTime the start time, in isoTime.
     * @param endTime the end time, in isoTime.
     * @return Iterator, which will return records until the stream is empty.
     * @throws IOException when there is an issue reading the data.
     * @throws org.json.JSONException should the server return an invalid response.
     */
    public static Iterator<HapiRecord> getDataCSV( 
            URL server, 
            String id, 
            String parameters,
            String startTime,
            String endTime ) throws IOException, JSONException {
        
        JSONObject info= getInfo( server, id, parameters );

        URL dataURL= url( server, 
                "data?id="+id 
                + "&parameters="+parameters 
                + "&time.min="+startTime 
                + "&time.max="+endTime );
                
        Iterator<HapiRecord> result= checkCache( info, dataURL, id, startTime, endTime );
        if ( result!=null ) {
            return result;
        } else {
            InputStream ins= dataURL.openStream();
            BufferedReader reader= new BufferedReader( new InputStreamReader(ins) );
            result= new HapiClientIterator( info, reader );
            File cache= Paths.get( getHapiCache(), 
                    dataURL.getProtocol(), 
                    dataURL.getHost(), 
                    dataURL.getPath(), 
                    id ).toFile();
            result= new WriteCacheIterator( info, result, startTime, endTime, cache, true );
            return result;
        }
        
    }
        
    /**
     * return the data record-by-record
     * @param server the HAPI server
     * @param id the dataset id
     * @param startTime the start time
     * @param endTime the end time
     * @return the records in an iterator
     * @throws java.io.IOException IOException when there is an issue reading the data.
     * @throws org.json.JSONException should the server return an invalid response
     */
    public static Iterator<HapiRecord> getData( 
            URL server, 
            String id, 
            String startTime,
            String endTime ) throws IOException, JSONException {
                
        return getDataCSV(server, id, startTime, endTime);
       
    }
    
    /**
     * return the data record-by-record
     * @param server the HAPI server
     * @param id the dataset id
     * @param parameters a comma-separated list of parameter names
     * @param startTime the start time
     * @param endTime the end time
     * @return the records in an iterator
     * @throws IOException when there is an issue reading the data.
     * @throws org.json.JSONException when the JSON is mis-formatted.
     */
    public static Iterator<HapiRecord> getData( 
            URL server, 
            String id, 
            String parameters,
            String startTime,
            String endTime ) throws IOException, JSONException {
        return getDataCSV(server, id, parameters, startTime, endTime);
    }
    
    
    
}
