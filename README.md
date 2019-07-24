# log-capture

[<img src="https://opensourcelogos.aws.dmtech.cloud/dmTECH_opensource_logo.svg" height="20" width="130">](https://dmtech.de/)
[![Build Status](https://travis-ci.org/dm-drogeriemarkt/log-capture.svg?branch=master)](https://travis-ci.org/dm-drogeriemarkt/log-capture)

Helper for Unit/Integration tests with JUnit 4 to test if something has been logged.

Because this is a library, Checkstyle is used to make sure all public classes/methods have appropriate Javadoc.

## Table of Contents

* [Usage:](#Usage)
  * [Examples:](#Examples)
    * [Unit Test Example:](#Unit-Test-Example)
    * [Integration Test Example:](#Integration-Test-Example)
    * [Example with MDC](#Example-with-MDC)
* [Usage with non-JUnit Runner](#Usage-with-non-JUnit-Runner)
  * [Cucumber example](#Cucumber-example)
    * [Cucumber feature file](#Cucumber-feature-file)
    * [Cucumber stepdefs](#Cucumber-stepdefs)
    * [Cucumber DTOs](#Cucumber-DTOs)

## Usage:

### Examples:

#### Unit Test Example:

```java
package my.company.application;

...

public class MyUnitTest {
    
    Logger logger = LoggerFactory.getLogger(MyUnitTest.class);

    // captures logs from any packages, but only if the log event's call stack 
    // indicates that it has been caused by the test
    @Rule
    public LogCapture logCapture = LogCapture.forUnitTest();

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
...

public class MyIntegrationTest {
    
    Logger logger = LoggerFactory.getLogger(MyIntegrationTest.class);

    // captures only logs from my.company and utility.that.logs and sub-packages
    @Rule
    public LogCapture logCapture = LogCapture.forIntegrationTest("my.company", "utility.that.logs");

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

    @Rule
    public LogCapture logCapture = LogCapture.forUnitTest();

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

If you intend to use LogCapture outside of a JUnit test, you cannot rely on JUnit's `@Rule` annotation and must call LocCapture's `addAppender()` and `removeAppender()` methods manually.

Be aware that this will still cause JUnit to be a dependency.

### Cucumber example

Here's an Example that shows how to use LogCapture with Cucumber 4:

#### Cucumber feature file

```cucumber
And with MDC logging context
  | contextId      | contentRegex                    |
  | verkaufsbon_vo | ^MDC_JSON_VALUE:.*2401219817317 |
* the following messages where logged
  | level | messageRegex   |
  | INFO  | ^Received bon$ |
```

#### Cucumber stepdefs

You can create these stepdefs in your project to use log-capture in feature files.

```java
public class LoggingStepdefs {
    private ExpectedMdcEntry[] expectedMdcEntries;

    public LogCapture logCapture = LogCapture.forIntegrationTest("my.company.app");

    @Before
    public void setupLogCapture() {
        logCapture.addAppender();
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
