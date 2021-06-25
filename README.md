# log-capture

[<img src="https://opensourcelogos.aws.dmtech.cloud/dmTECH_opensource_logo.svg" height="20" width="130">](https://dmtech.de/)
[![Build Status](https://travis-ci.org/dm-drogeriemarkt/log-capture.svg?branch=master)](https://travis-ci.org/dm-drogeriemarkt/log-capture)

Simple assertions for log messages. See [Examples](#examples).

```java
logCapture
        .info().assertLogged("hello world")
        .warn().assertLogged("bye world");
```

**Table of Contents**

* [Usage](#usage)
  * [Maven](#maven)
  * [Examples](#examples)
    * [Unit Test Example:](#unit-test-example)
    * [Integration Test Example:](#integration-test-example)
    * [Example with MDC](#example-with-mdc)
* [Usage outside of JUnit 5 (Cucumber example)](#usage-outside-of-junit-5-cucumber-example)
  * [Cucumber example](#cucumber-example)
    * [Cucumber feature file](#cucumber-feature-file)
    * [Cucumber stepdefs](#cucumber-stepdefs)
    * [Cucumber DTOs](#cucumber-dtos)
* [Changes](#changes)
  * [3.2.1](#321)
  * [3.2.0](#320)
  * [3.1.0](#310)
  * [3.0.0](#300)
  * [2.0.1](#201)
  * [Updating from Version 1.x.x to 2.x.x](#updating-from-version-1xx-to-2xx)

## Usage

### Maven

Add log-capture as a test dependency to your project. If you use Maven, add this to your pom.xml:

```xml
<dependency>
    <groupId>de.dm.infrastructure</groupId>
    <artifactId>log-capture</artifactId>
    <version>3.2.1</version>
    <scope>test</scope>
</dependency>
```

### Examples

#### Unit Test Example:

```java
package my.company.application;

import de.dm.infrastructure.logcapture.LogCapture;
...

public class MyUnitTest {
    
    Logger logger = LoggerFactory.getLogger(MyUnitTest.class);

    @RegisterExtension //use @Rule for LogCapture 2/JUnit 4
    public LogCapture logCapture = LogCapture.forCurrentPackage();

    @Test
    public void twoLogMessagesInOrder() {
        log.info("something interesting"); 
        log.error("something terrible");

        //assert that the messages have been logged
        //expected log message is a regular expression
        logCapture
                .info().assertLogged("^something interesting$")
                .error().assertLogged("terrible")

        // alterative using the old, non-fluent API
        logCapture
                .assertLogged(Level.INFO, "^something interesting$") //second parameter is a regular expression
                .thenLogged(Level.ERROR, "terrible")
                .assertNothingElseLogged();
    }
}
```

#### Integration Test Example:

```Java
package my.company.application;

import utility.that.logs.Tool;
import irrelevant.utility.Irrelevant;
import de.dm.infrastructure.logcapture.LogCapture;
...

public class MyIntegrationTest {
    
    Logger logger = LoggerFactory.getLogger(MyIntegrationTest.class);

    // captures only logs from my.company and utility.that.logs and sub-packages
    @RegisterExtension //use @Rule for LogCapture 2/JUnit 4
    public LogCapture logCapture = LogCapture.forPackages("my.company", "utility.that.logs");

    @Test
    public void twoLogMessagesInOrder() {
        Irrelevant.doSomething();
        Tool.doSomething();

        log.info("something interesting");
        log.error("something terrible");

        logCapture
                .info().assertLogged("^something interesting$")
                .info().assertLogged("^start of info from utility.that.logs") // no $ at the end to only match the start of the message
                .error().("terrible");

        // alterative using the old, non-fluent API
        logCapture
                .assertLogged(Level.INFO, "^something interesting")
                .assertLogged(Level.INFO, "^start of info from utility.that.logs")
                .thenLogged(Level.ERROR, "terrible");
    }
}
```

#### Example with MDC

```Java
package my.company.application;

...

import static de.dm.prom.logcapture.ExpectedMdcEntry.withMdc;

...

public class MyUnitTest {
    Logger logger = LoggerFactory.getLogger(MyUnitTest.class);

    @RegisterExtension
    public LogCapture logCapture = LogCapture.forCurrentPackage();

    @Test
    public void logMessageWithMdcInformation() {
        MDC.put("my_mdc_key", "this is the MDC value");
        MDC.put("other_mdc_key", "this is the other MDC value");
        log.info("this message has some MDC information attached");
        log.info("this message has some MDC information attached, too");

        logCapture
                .info()
                .withMdc("my_mdc_key", "^this is the MDC value$")
                .withMdc("other_mdc_key", "^this is the other MDC value$")
                .assertLogged("information attached")

        // to assert MDC content in both messages
        logCapture
                .withMdcForAll("my_mdc_key", "^this is the MDC value$")
                .withMdcForAll("other_mdc_key", "^this is the other MDC value$")
                .info().assertLogged("information attached")
                .info().assertLogged("information attached, too")

        // non-fluent API (no equivalent to withMdcForAll here)
        logCapture
            .assertLogged(Level.INFO, "information attached", 
                withMdc("my_mdc_key", "^this is the MDC value$"),
                withMdc("other_mdc_key", "^this is the other MDC value$")
            );
    }
}
```

If assertLogged fails because the message is correct, but the MDC value is wrong, the assertion error will contain the actual MDC values of the last captured log event where the log message and level matched.

This can be useful for debugging purposes. For example, this test code:

```java
    MDC.put("my_mdc_key", "this is the wrong MDC value");
    MDC.put("other_mdc_key", "this is the other MDC value");
    log.info("this message has some MDC information attached");

    logCapture
        .assertLogged(Level.INFO, "information attached", 
            withMdc("my_mdc_key", "^something expected that never occurs$"),
            withMdc("other_mdc_key", "^this is the other MDC value$")
        );
```

will output:

```text
java.lang.AssertionError: Expected log message has occurred, but never with the expected MDC value: Level: INFO, Regex: "information attached"
  Captured message: "this message has some MDC information attached"
  Captured MDC values:
    my_mdc_key: "this is the wrong MDC value"
    other_mdc_key: "this is the other MDC value"
```

## Usage outside of JUnit 5 (Cucumber example)

If you intend to use LogCapture outside of a JUnit test, you cannot rely on JUnit's `@Rule` annotation and must call LocCapture's `addAppenderAndSetLogLevelToTrace()` and `removeAppenderAndResetLogLevel()` methods manually.

Be aware that this will still cause JUnit to be a dependency.

### Cucumber example

Here's an Example that shows how to use LogCapture with Cucumber 4:

#### Cucumber feature file

```cucumber
And with MDC logging context
  | contextId      | contentRegex                    |
  | mdc_key | ^some mdc value |
* the following messages were logged
  | level | messageRegex   |
  | INFO  | ^Something happened$ |
  | INFO  | ^Something else happened with the same mdc context$ |
```

#### Cucumber stepdefs

You can create these stepdefs in your project to use log-capture in feature files.

```java
public class LoggingStepdefs {
    private ExpectedMdcEntry[] expectedMdcEntries;

    public LogCapture logCapture = LogCapture.forPackages("my.company.app");

    @Before
    public void setupLogCapture() {
        logCapture.addAppenderAndSetLogLevelToTrace();
    }
    
    @After
    public void stopLogCapture() {
        logCapture.removeAppenderAndResetLogLevel();
    }

    @And("with MDC logging context")
    public void withMdcLoggingContext(List<LogContext> logContexts) {
        List<ExpectedMdcEntry> expectedMdcEntries = new LinkedList<>();
        logContexts.forEach(logContext -> expectedMdcEntries.add(ExpectedMdcEntry.withMdc(logContext.getContextId(), logContext.getContentRegex())));
        this.expectedMdcEntries = expectedMdcEntries.toArray(new ExpectedMdcEntry[0]);
    }

    @And("the following messages where logged")
    public void followingMessagedLogged(List<LogEntry> logEntries) {
        LogEntry firstEntry = logEntries.get(0);
        logEntries.remove(0);
        LogCapture.LastCapturedLogEvent lastCapturedLogEvent = logCapture.assertLogged(Level.toLevel(firstEntry.getLevel()), firstEntry.getMessageRegex(), expectedMdcEntries);
        for (LogEntry logEntry : logEntries) {
            lastCapturedLogEvent = lastCapturedLogEvent.thenLogged(Level.toLevel(logEntry.getLevel()), logEntry.getMessageRegex(), expectedMdcEntries);
        }
    }
}
```

#### Cucumber DTOs

You need to register these DTOs with Cucumber's [TypeRegistryConfigurer](https://cucumber.io/docs/cucumber/configuration/#type-registry):

```java
import lombok.Data;

@Data
public class LogContext {
    private String contextId;
    private String contentRegex;
}
```

```java
import lombok.Data;

@Data
public class LogEntry {
    private String level;
    private String messageRegex;
}
```

## Future Plans

LocCapture is currently complete for our (dmTECH) needs. A LogEventMatcher for [Markers](http://www.slf4j.org/apidocs/org/slf4j/Marker.html) would be an obviously useful addition, but is currently not needed by us.

## Changes

### 3.2.1

Added deprecated `addAppenderAndSetLogLevelToDebug()` again for compatibility

### 3.2.0

Added fluent API

### 3.1.0

Added `assertNothingElseLogged()`

### 3.0.0

Updated from JUnit 4 to JUnit 5.

To update from 2.x.x to 3.x.x:

* Use JUnit 5 instead of JUnit 4
* Replace `@Rule` in logging tests with `@RegisterExtension`

### 2.0.1

Fixed a bug where multiline log messages (for example Messages that contain a stack trace) could not be matched.

### Updating from Version 1.x.x to 2.x.x

* `LogCapture.forUnitTest()` has been replaced with `LogCapture.forCurrentPackage()`
* `LogCapture.forIntegrationTest(...)` has been replaced with `LogCapture.forPackages(...)`
* `logCapture.addAppender()` has been replaced with `logCapture.addAppenderAndSetLogLevelToTrace()`
* `logCapture.removeAppender()` has been replaced with `logCapture.removeAppenderAndResetLogLevel()`

