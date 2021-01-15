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

## Configure

Scripts can be added to and configured inside polopoly, with the external id:

```
com.atex.plugins.scripting.configuration
```

## Using the Engine in Java

Scripts are stored as content represented by `LifecycleScript` beans, and should have an external-id of
`com.atex.script.<unique-id>`. Each script is added to a `ScriptList` with the external-id `com.atex.plugins.scripting.configuration`.
The pScriptListWidget widget handles all of this, and can be used as an example of script management.

### Get an instance of the engine

The engine is a singleton instance that can be acquired by:

```java
LifecycleScriptingEngine engine = LifecycleScriptingEngine.getInstance(cm);
```

Here `cm` is a reference to a ContentManager that the engine can use to fetch and cache scripts.

### Run a specific script

A script can be run by calling `run` and passing its external-id and context:

```java
ContextMap context = engine.run("com.atex.script.my-script", new ContextMap());
```

### Script Contexts

Each script is run in isolation, so there are no runtime conflicts. To inject data into a script, so it can
be manipulated by JavaScript, use a `ContextMap`. Any method that runs scripts such as `run`, `runDetached`, 
or `trigger` accept and return `ContextMap` objects. For example:

```java
ContentWrite contentWrite; // existing ContentWrite
        
// This creates a ContextMap containing a variable with the identifier 'content'.
ContextMap context = new ContextMap("content", new ContentWriteFacade(contentWrite, false));

// Get a handle to the script engine.
LifecycleScriptingEngine engine = LifecycleScriptingEngine.getInstance(cm);

// Run a script, passing our context.
// resultContext is a new context that matches the one passed to run, with the modifications
// of scripts applied.
ContextMap resultContext = engine.run("com.atex.script.my-script", context); 
```

Context variables can then be accessed in JavaScript as a global variable:

```javascript
// 'content' doesn't need declaring because it was injected into the 
// global scope by the engine.
print(content.aspects['atex.WFContentStatus'].data.status.statusID);
```

If a script fails or throws an error, the context returned by the engine is in the same state as the one
passed to it. Script executions are atomic in this way, they either work entirely or not at all.

### JavaScript Wrappers

When the engine runs a script that manipulates any variables, what it's really operating on Java objects.
The Nashorn engine that the scripting engine is built on can map calls to Java Bean methods, and can
call constructors. For example: 

```java
// This class will be passed to the engine.
public class Person {
    
    private String name;
    
    public Person(String name) {
        this.name = name;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
}
```

Since the `Person` class is a valid Java Bean, the engine will map standard JavaScript syntax to the
necessary method calls:

```javascript
// person.name is automaticlly mapped to the person.getName() method.
var name = person.name;

// person.name is automatically mapped to the person.setName(String) method.
person.name = 'Bob';
```

When more complexity is required the `BaseJSObject` class can be extended to make the engine treat any
wrapped class as a JavaScript object, with support for more standard ES5 syntax such as the `delete` 
keyword, and `valueOf` and `toString` methods. Objects can also be made fully immutable to prevent
writers of scripts from modifying them in ways you don't want.

The `ContentResultFacade` and `ContentWriteFacade` classes are examples of this that should be used
whenever a `ContentWrite` or `ContentResult` are passed to a context. For example:

```java
// This creates a context containing a wrapped ContentWrite that is mutable and ContentResult that is immutable.
ContextMap context = new ContextMap("contentWrite", new ContentWriteFacade(contentWrite, false),
                                    "contentResult", new ContentResultFacade(contentResult, true));

// Accessing the context variables in Java returns the wrapped value, not the facade:
ContentWrite contextContent = context.get("contentWrite");
```
### JavaScript API

When writing scripts, some useful functions have been provided in `src/main/resources/com/atex/onecms/scripting/script-util.js`. 
This script is loaded into the engine as a library every time a script is run,
so all scripts have access to the functions it declares in their global scope.

Javadoc
=======

Standalone Javadocs can be generated using maven-javadoc-plugin by running:

```bash
mvn javadoc:javadoc
```
