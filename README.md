# log-capture

[<img src="https://opensourcelogos.aws.dmtech.cloud/dmTECH_opensource_logo.svg" height="20" width="130">](https://dmtech.de/)
[![Build Status](https://travis-ci.org/dm-drogeriemarkt/log-capture.svg?branch=master)](https://travis-ci.org/dm-drogeriemarkt/log-capture)

Helper for Unit/Integration tests with JUnit 4/5 to test if something has been logged.

Because this is a library, Checkstyle is used to make sure all public classes/methods have appropriate Javadoc.

## Table of Contents

* [Table of Contents](#table-of-contents)
* [Changes](#changes)
  * [3.0.0](#300)
  * [2.0.1](#201)
  * [Updating from Version 1.x.x to 2.x.x](#updating-from-version-1xx-to-2xx)
* [Usage](#usage)
  * [Junit 4 vs 5](#junit-4-vs-5)
  * [Maven](#maven)
  * [Examples](#examples)
    * [Unit Test Example:](#unit-test-example)
    * [Integration Test Example:](#integration-test-example)
    * [Example with MDC](#example-with-mdc)
* [Usage with non-JUnit Runner](#usage-with-non-junit-runner)
  * [Cucumber example](#cucumber-example)
    * [Cucumber feature file](#cucumber-feature-file)
    * [Cucumber stepdefs](#cucumber-stepdefs)
    * [Cucumber DTOs](#cucumber-dtos)


## Changes

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
* `logCapture.addAppender()` has been replaced with `logCapture.addAppenderAndSetLogLevelToDebug()`
* `logCapture.removeAppender()` has been replaced with `logCapture.removeAppenderAndResetLogLevel()`

## Usage

### Junit 4 vs 5

If you still use Junit 4, you need to use LogCapture 2.x.x

There is no guarantee, however, if and how long 2.x.x will be maintained. We plan to maintain it as long as it is needed, though.

### Maven

Add log-capture as a test dependency to your project. If you use Maven, add this to your pom.xml:

```pom.xml
<dependency>
    <groupId>de.dm.infrastructure</groupId>
    <artifactId>log-capture</artifactId>
    <version>3.0.0</version>
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

        logCapture
                .assertLogged(Level.INFO, "^something interesting$") //second parameter is a regular expression
                .thenLogged(Level.ERROR, "terrible");
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
                .assertLogged(Level.INFO, "^something interesting")
                .assertLogged(Level.INFO, "^info from utility.that.logs")
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


## Usage with non-JUnit Runner

If you intend to use LogCapture outside of a JUnit test, you cannot rely on JUnit's `@Rule` annotation and must call LocCapture's `addAppenderAndSetLogLevelToDebug()` and `removeAppenderAndResetLogLevel()` methods manually.

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
        logCapture.addAppenderAndSetLogLevelToDebug();
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
