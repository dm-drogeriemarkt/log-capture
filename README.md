# log-capture

[<img src="https://raw.githubusercontent.com/dm-drogeriemarkt/.github/refs/heads/main/assets/dmtech-open-source-badge.svg" height="20" width="130">](https://dmtech.de/)
[![Apache License 2](https://img.shields.io/badge/license-MIT-blue.svg)](LICENSE)
[![Build Status](https://github.com/dm-drogeriemarkt/log-capture/actions/workflows/build.yml/badge.svg?branch=master)](https://github.com/dm-drogeriemarkt/log-capture/actions?query=branch%3Amaster)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/de.dm.infrastructure/log-capture/badge.svg)](https://maven-badges.herokuapp.com/maven-central/de.dm.infrastructure/log-capture)

Simple assertions for log messages. See [Examples](#examples).

> [!NOTE]
> log-capture asserts evaluated log statements. That means it depends on a logging implementation (*logback*), but works with any logging facade (*slf4j* and others)

```java
var name="world";
log.info("hello {}", name);
log.warn("bye {}", name);

logCapture.assertLoggedInOrder(
    info("hello world"),
    warn("bye world")
);
```

It's even simple when there's more than just the message and level to assert:

```java
logCapture.assertLoggedInOrder(
    info("hello world",
        logger("com.acme.helloworld.WorldGreeter"))
    warn("bye world",
        exception().expectedType(WorldNotFoundException.class))
);
```

**Table of Contents**

* [Usage](#usage)
  * [Maven](#maven)
  * [Additional matchers](#additional-matchers)
    * [MDC content](#mdc-content)
    * [Exceptions](#exceptions)
    * [Markers](#markers)
    * [Key-Value](#key-value)
    * [Logger name](#logger-name)
  * [Examples](#examples)
    * [Unit Test Example:](#unit-test-example)
    * [Integration Test Example:](#integration-test-example)
    * [Example with additional MDC matcher](#example-with-additional-mdc-matcher)
    * [More Examples](#more-examples)
* [Usage outside of JUnit 5 (Cucumber example)](#usage-outside-of-junit-5-cucumber-example)
  * [Cucumber example](#cucumber-example)
    * [Cucumber feature file](#cucumber-feature-file)
* [Changes](#changes) 
  * [4.1.0](#410)
  * [4.0.1](#401)
  * [4.0.0](#400)
  * [3.6.2](#362)
  * [3.6.1](#361)
  * [3.6.0](#360)
  * [3.5.0](#350)
  * [3.4.1](#341)
  * [3.4.0](#340)
  * [3.3.0](#330)
  * [3.2.1](#321)
  * [3.2.0](#320)
  * [3.1.0](#310)
  * [3.0.0](#300)
  * [2.0.1](#201)
  * [Updating from Version 1.x.x to 2.x.x](#updating-from-version-1xx-to-2xx)
  * [Updating from Version 3.2.x or lower to Version 3.3.x or higher](#updating-from-version-32x-or-lower-to-version-33x-or-higher)

## Usage

### Maven

Add log-capture as a test dependency to your project. If you use Maven, add this to your pom.xml:

```xml
<dependency>
    <groupId>de.dm.infrastructure</groupId>
    <artifactId>log-capture</artifactId>
    <version>4.1.0</version>
    <scope>test</scope>
</dependency>
```

### Additional matchers

The default matchers can match level and/or message. But there are additional matchers for log messages. See also [the detailed example below](#example-with-additional-mdc-matcher)

#### MDC content

```java
import static de.dm.infrastructure.logcapture.ExpectedMdcEntry.mdc;

...

MDC.put("key", "value");
log.info("did something");

logCapture.assertLogged(info("did something", mdc("key", "value")));`
```

#### Exceptions

```java
import static de.dm.infrastructure.logcapture.ExpectedException.exception;

...

log.warn("oh no!",
    new IllegalArgumentException("shame on you!",
        new NullPointerException("never use null")));

logCapture.assertLogged(
    warn("oh no!",
        exception()
            .expectedType(IllegalArgumentException.class)
            .expectedCause(exception()
                .expectedType(NullPointerException.class)
                .expectedMessageRegex("never use null")
                .build())
            .build()
    ));
```

#### Markers

```java
import static de.dm.infrastructure.logcapture.ExpectedMarker.marker;

...

log.info(MarkerFactory.getMarker("my-marker"), "hello with marker");

logCapture.assertLogged(info("hello with marker", marker("my-marker")));
```

#### Key-Value

```java
import static de.dm.infrastructure.logcapture.ExpectedKeyValue.keyValue;

...

log.atInfo().setMessage("hello").addKeyValue("meaning", 42).log();

logCapture.assertLogged(info("hello", keyValue("meaning", 42)))
```

#### Logger name

```java
import static de.dm.infrastructure.logcapture.ExpectedLoggerName.logger;

...

log.info("did something");

logCapture.assertLogged(info("did something", logger("com.acme.foo")));
```

### Expect a specifiq ammount of times

It is possible to match a LogExpectation multiple times. This can be done by using the `Times` class.

```java
import static de.dm.infrastructure.logcapture.ExpectedLoggerName.logger;

...

log.info("did something");
log.info("did something");

logCapture.assertLogged(info("did something"), times(2));
```

There are also more options for checking the amount of times a log message is logged a little more or less specific.

```java
logCapture.assertLogged(info("did something"), atLeast(1)); // not important if more than once, but at least once
logCapture.assertLogged(info("did something"), atMost(2)); // not important if less than e.g. twice, but at most twice
logCapture.assertLogged(info("did something"), once()); // exactly once
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

#### Example with additional MDC matcher

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

If assertLogged fails because the message is correct, but the MDC value is wrong, the assertion error will contain the actual MDC values of the last captured log event where the log message and level matched.

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

If you intend to use LogCapture outside of a JUnit test, you cannot rely on JUnit's `@RegisterExtension` annotation and must call LogCapture's `addAppenderAndSetLogLevelToTrace()` and `removeAppenderAndResetLogLevel()` methods manually.

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

### 4.1.0

* added Times class which allows matching a LogExpectation multiple times

### 4.0.1

* dependencies updated due to logback vulnerability

### 4.0.0

* **breaking change:** log-capture now requires Java 17
* **breaking change:** all deprecated parts have been removed
* added a new log event matcher [for key-value content](#key-value)
* lots of dependency updates

### 3.6.2

* Fixed an assertion message concerning captured Exceptions.

### 3.6.1

* Fixed a misleading and wrong assertion message. The assertion itself was correct, but the message always said all matchers did not match when only a subset did not match.

### 3.6.0

* Removed ExpectedKeyValue again due to an [API change in Logstash without a workaround](https://github.com/logfellow/logstash-logback-encoder/issues/788)

### 3.5.0

* Added new Log Event Matcher: `ExpectedKeyValue.keyValue(...)` to assert `StructuredArguments.keyValue(...)` from Logstash
* Improved readability for assertion errors when using `assertNotLogged(...)`
* Updated dependencies

### 3.4.1

* Improved Javadoc for deprecated methods

### 3.4.0

* Added `assertNotLogged(...)` for asserting that no matching log message has been logged
* Added more factory methods for `warn(...)`, `error(...)` as a shortcut to ignore the message when matching
* Added `any(...)` factory method for matching any log message regardless of level

### 3.3.0

* Introduced a new fluent API with
    * better readability
    * extensible log message assertions (to assert attached Exceptions, Markers and LoggerName beyond MDC content)
* Deprecated the old API (will be removed in 4.0, [how to update](#updating-from-version-32x-or-lower-to-version-33x-or-higher))

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

### Updating from Version 3.2.x or lower to Version 3.3.x or higher

Since the old API has been deprecated in 3.3.0 existing assertions should be replaced in preparation for 4.0. So for example:

```java
...

import static ch.qos.logback.classic.Level.INFO;
import static de.dm.infrastructure.logcapture.ExpectedMdcEntry.withMdc;

...

// plain assertion
logCapture.assertLogged(INFO, "Something happened.");
// assertion with MDC
logCapture.assertLogged(INFO, "Something with MDC content", 
    withMdc("bookingNumber", "1234"));
// in-order assertion
logCapture
    .assertLogged(INFO, "Step 1")
    .thenLogged(INFO, "Step 2");

```

needs to be replaced with:

```java
...

import static de.dm.infrastructure.logcapture.ExpectedMdcEntry.mdc;
import static de.dm.infrastructure.logcapture.LogExpectation.info;

...

// plain assertion
logCapture.assertLogged(info("Something happened."));
// assertion with MDC
logCapture.assertLogged(info("Something with MDC content",
    mdc("rabattUpdate", "CapturableHeadline")));
// in-order assertion
ogCapture.assertLoggedInOrder(
    info("Step 1")
    info("Step 2")
);

```
