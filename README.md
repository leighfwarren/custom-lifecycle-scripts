# custom-lifecycle-scripts

A plugin that allows users to add customisable scripts that run during events in a contents' lifecycle.

Usage
=====

Add the following to your project pom.

```xml
<dependencies>
    ....
    <dependency>
        <groupId>com.atex.plugins</groupId>
        <artifactId>custom-lifecycle-scripts</artifactId>
        <version>1.0-SNAPSHOT</version>
    </dependency>
    <dependency>
        <groupId>com.atex.plugins</groupId>
        <artifactId>custom-lifecycle-scripts</artifactId>
        <version>1.0-SNAPSHOT</version>
        <classifier>contentdata</classifier>
    </dependency>
    ....
</dependencies>
```

Configure
=========

Scripts can be added and configured inside polopoly, with the external id:

```
com.atex.plugins.scripting.configuration
```

Javadoc
=======

Standalone Javadocs can be generated using maven-javadoc-plugin by running:

```bash
mvn javadoc:javadoc
```
