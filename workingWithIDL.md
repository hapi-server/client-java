With the IDL-Java bridge, this can be used to read data into IDL as well.  This experiments with the interface.

First create an IDL object to call the static methods.  This assumes that json-2011-01-27-gitrelease.jar and client-java.jar
are in the current directory.

~~~~~
IDL> setenv, 'CLASSPATH=json-2011-01-27-gitrelease.jar:client-java.jar'
IDL> hc= OBJ_NEW('IDLjavaObject$Static$ClientJava', 'org.hapiserver.HapiClient')
~~~~~

The symbol 'hc' is now used to call methods.  We will also need to get a Java URL for the server we wish to use.

~~~~~
URL= OBJ_NEW('IDLjavaObject$URL', 'java.net.URL', 'http://jfaden.net/HapiServerDemo/hapi/' )
js= hc.getCatalogIdsArray(URL)
~~~~~

Note the URL must end in a slash.

~~~~~
IDL> js= hc.getInfo(URL,'poolTemperature') 
IDL> js.toString(4)
{
    "HAPI": "2.1",
    "cadence": "PT1M",
    "modificationDate": "2020-04-23T00:00:00.000Z",
    "parameters": [
        {
            "fill": null,
            "length": 24,
            "name": "Time",
            "type": "isotime",
            "units": "UTC"
...
}
IDL> js= hc.getInfoParametersArray(URL,'poolTemperature')
IDL> js
Time
Temperature
~~~~~

Now let's get some data.

~~~~~
IDL> js= hc.getData(URL,'poolTemperature', '2020-04-23T00:00Z', '2020-04-24T00:00Z')
IDL> while ( js.hasNext() ) do print, (js.next()).toString()
~~~~~

And here's a full program:
~~~~~
pro demoHapi
   hc= OBJ_NEW('IDLjavaObject$Static$ClientJava', 'org.hapiserver.HapiClient')
   URL= OBJ_NEW('IDLjavaObject$URL', 'java.net.URL', 'http://jfaden.net/HapiServerDemo/hapi/' )
   js= hc.getData(URL,'poolTemperature', '2020-04-23T00:00Z', '2020-04-24T00:00Z')
   while ( js.hasNext() ) do begin
      rec= js.next()
      print, rec.getIsoTime(0), rec.getDouble(1)
   endwhile
~~~~~
