# client-java
Java client for accessing HAPI servers.

This can be checked out and used immediately with Netbeans, or just download 
the .jar file.  Note a jar file from json.org is needed as well.

## Building with Ant
You can also download and build this using Ant (like make but for Java).  

~~~~~
unix> git clone git@github.com:hapi-server/client-java.git
unix> cd client-java
unix> ant jar
~~~~~

The jar file will be created in dist/client-java.jar

## Building with javac
~~~~~
unix> git clone git@github.com:hapi-server/client-java.git
unix> cd client-java/src
unix> javac -cp ../lib/json-2011-01-27-gitrelease.jar org/hapiserver/*.java com/cottagesystems/util/*.java
unix> jar cvf client-java.jar org/hapiserver/*.class com/cottagesystems/util/*.class
