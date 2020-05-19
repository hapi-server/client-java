This can be used to read data into Matlab as well.  This experiments with the interface.

First create an Java object for the client.

~~~~~
>> javaaddpath('/home/jbf/Linux/hapi-client-java.jar')
>> hc= org.hapiserver.HapiClient()
~~~~~

The symbol 'hc' is now used to call methods.  We will also need to get a Java URL for the server we wish to use.

~~~~~
>> URL= java.net.URL('http://jfaden.net/HapiServerDemo/hapi');
>> js= hc.getCatalogIdsArray(URL);
~~~~~

Note all HAPI servers end in "hapi".

~~~~~
>> js= hc.getInfo(URL,'poolTemperature') 
>> js.toString(4)
ans =

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
>> js= hc.getData(URL,'poolTemperature', '2020-04-23T00:00Z', '2020-04-24T00:00Z')
>> while ( js.hasNext() ) do print, (js.next()).toString()
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
end
~~~~~
