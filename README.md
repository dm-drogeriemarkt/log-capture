# log-capture

[<img src="https://opensourcelogos.aws.dmtech.cloud/dmTECH_opensource_logo.svg" height="20" width="130">](https://dmtech.de/)
[![Build Status](https://travis-ci.org/dm-drogeriemarkt/log-capture.svg?branch=master)](https://travis-ci.org/dm-drogeriemarkt/log-capture)

Simple assertions for log messages. See [Examples](#examples).

```java
logCapture.assertLogged(
        info("hello world"),
        warn("bye world")
        );
```

It's even simple when there's more than just the message and level to assert:

```java
logCapture.assertLogged(
        info("hello world",
        logger("com.acme.helloworld.WorldGreeter")
        )
        warn("bye world",
        exception().expectedType(WorldNotFoundException.class)
        )
        );
```

**Table of Contents**

* [Usage](#usage)
    * [Maven](#maven)
    * [Examples](#examples)
        * [Unit Test Example:](#unit-test-example)
        * [Integration Test Example:](#integration-test-example)
        * [Example with MDC](#example-with-mdc)
        * [More Examples](#more-examples)
* [Usage outside of JUnit 5 (Cucumber example)](#usage-outside-of-junit-5-cucumber-example)
    * [Cucumber example](#cucumber-example)
        * [Cucumber feature file](#cucumber-feature-file)
* [Changes](#changes)
    * [3.3.0](#330)
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
    <version>3.3.0</version>
    <scope>test</scope>
</dependency>
```

### Examples

#### Unit Test Example:

```java
package my.company.application;

import de.dm.infrastructure.logcapture.LogCapture;
import de.dm.infrastructure.logcapture.LogExpectation;
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
        logCapture.assertLogged(
                info("^something interesting$"),
                error("terrible")
        );
        
        //is the same assertion, but also asserts the order of these log messages
        logCapture.assertLoggedInOrder(
                info("^something interesting$"),
                error("terrible")
        );

        //assert that no log message containing "something unwanted" with any log level exists 
        //and that no log message with level DEBUG exists
        logCapture.assertNotLogged(
                any("something unwanted"),
                debug()
        );
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

        logCapture.assertLogged(
                info("^something interesting$"),
                info("^start of info from utility.that.logs"),
                error("terrible"));
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
        log.info("this message has some MDC information attached");
        MDC.put("other_mdc_key", "this is the other MDC value");
        log.info("this message has some additional MDC information attached");

        // asserts my_mdc_key for both message and other_mdc_key for the second one
        logCapture
                .with(mdc("my_mdc_key", "^this is the MDC value$"))
                .assertLoggedInOrder(
                        info("information attached"),
                        info("additional MDC information attached",
                                mdc("other_mdc_key", "^this is the other MDC value$")));
    }
}
```

If assertLogged fails because the message is correct, but the MDC value is wrong, the assertion error will contain the
actual MDC values of the last captured log event where the log message and level matched.

This can be useful for debugging purposes. For example, this test code:

```java
    MDC.put("my_mdc_key","this is the wrong MDC value");
        MDC.put("other_mdc_key","this is the other MDC value");
        log.info("this message has some MDC information attached");

        logCapture.assertLogged().info("information attached",
        mdc("my_mdc_key","^something expected that never occurs$"),
        mdc("other_mdc_key","^this is the other MDC value$"));
```

will output:

```text
java.lang.AssertionError: Expected log message has occurred, but never with the expected MDC value: Level: INFO, Regex: "information attached"
  Captured message: "this message has some MDC information attached"
  Captured MDC values:
    my_mdc_key: "this is the wrong MDC value"
    other_mdc_key: "this is the other MDC value"
```

#### More Examples

See [ReadableApiTest.java](src/test/java/com/example/app/ReadableApiTest.java) for more usage examples.

## Usage outside of JUnit 5 (Cucumber example)

If you intend to use LogCapture outside of a JUnit test, you cannot rely on JUnit's `@RegisterExtension` annotation and
must call LogCapture's `addAppenderAndSetLogLevelToTrace()` and `removeAppenderAndResetLogLevel()` methods manually.

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

## Changes

### 3.3.0

* Introduced a new fluent API with
    * better readability
    * extensible log message assertions (to assert attached Exceptions, Markers and LoggerName beyond MDC content)
* Deprecated the old API (will be removed in 4.0)

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
