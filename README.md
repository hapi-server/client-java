# client-java
Java client for accessing HAPI servers.

This can be checked out and used immediately with Netbeans, or just download 
the .jar file.  Note a jar file from json.org is found in the lib folder.

## Building with Ant
You can also download and build this using Ant (like Make but for Java).  

~~~~~
unix> git clone git@github.com:hapi-server/client-java.git
unix> cd client-java
unix> ant jar
~~~~~

The jar file will be created in dist/client-java.jar

## Building with javac
It's assumed that typically ant would be used to build the library, but in cases where it is not available, javac can be used.
~~~~~
unix> git clone git@github.com:hapi-server/client-java.git
unix> cd client-java
unix> mkdir build
unix> javac -cp lib/json-2011-01-27-gitrelease.jar -sourcepath src/ -d build `find src -name *.java`
unix> cd build
unix> jar xvf ../lib/json-2011-01-27-gitrelease.jar
unix> mkdir dist
unix> jar cvf ../dist/client-java.jar `find . -name '*.class'`
~~~~~
This will create dist/client-java.jar.
