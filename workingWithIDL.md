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

~~~~~
