# client-java

A Java client for accessing HAPI servers.

This can be checked out and used immediately with Netbeans, or just download 
the .jar file in the `store` folder.  Note a jar file from json.org is found in the `lib` folder.

## Building with Ant

You can also download and build using Ant (like Make but for Java).  

~~~~~
git clone git@github.com:hapi-server/client-java.git
cd client-java
ant jar
~~~~~

The jar file will be created in dist/client-java.jar

## Building with javac

It's assumed that typically `ant` would be used to build the library, but in cases where it is not available, `javac` can be used.
~~~~~
git clone git@github.com:hapi-server/client-java.git
cd client-java
mkdir build  # compiled classes will go here
mkdir dist   # jar file will go here
javac -cp lib/json-2011-01-27-gitrelease.jar -sourcepath src/ -d build `find src -name *.java`
cd build
jar xvf ../lib/json-2011-01-27-gitrelease.jar
jar cvf ../dist/client-java.jar `find . -name '*.class'`
~~~~~
This will create dist/client-java.jar.

## Example Use
Java examples can be found in https://github.com/hapi-server/client-java/tree/master/src/test.

