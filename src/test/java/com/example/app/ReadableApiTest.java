package com.example.app;

import de.dm.infrastructure.logcapture.LogCapture;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.MDC;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;

import static de.dm.infrastructure.logcapture.ExpectedException.exception;
import static de.dm.infrastructure.logcapture.ExpectedKeyValue.keyValue;
import static de.dm.infrastructure.logcapture.ExpectedLoggerName.logger;
import static de.dm.infrastructure.logcapture.ExpectedMarker.marker;
import static de.dm.infrastructure.logcapture.ExpectedMdcEntry.mdc;
import static de.dm.infrastructure.logcapture.LogExpectation.any;
import static de.dm.infrastructure.logcapture.LogExpectation.debug;
import static de.dm.infrastructure.logcapture.LogExpectation.error;
import static de.dm.infrastructure.logcapture.LogExpectation.info;
import static de.dm.infrastructure.logcapture.LogExpectation.trace;
import static de.dm.infrastructure.logcapture.LogExpectation.warn;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Slf4j
@SuppressWarnings({
        "java:S5778", //this rule does not increase the clarity of these tests
        "LoggingSimilarMessage" // not a sensible rule for a logging test
})
class ReadableApiTest {
    @RegisterExtension
    LogCapture logCapture = LogCapture.forCurrentPackage();

    @Test
    void basicReadableApiSucceeds() {
        log.info("hello world", new IllegalArgumentException("shame on you", new NullPointerException("bah!")));

        logCapture.assertLogged(info("hello world"));
    }

    @Test
    void varargsAssertionsRequireLogExpectations() {
        assertThat(
                assertThrows(IllegalArgumentException.class, () ->
                        logCapture.assertLoggedInAnyOrder()
                ))
                .hasMessage("at least 2 LogExpectations are required for assertLoggedInAnyOrder(). Found none");

        assertThat(
                assertThrows(IllegalArgumentException.class, () ->
                        logCapture.assertLoggedInAnyOrder(info("Hello world"))
                ))
                .hasMessageMatching("at least 2 LogExpectations are required for assertLoggedInAnyOrder\\(\\)\\. Found .*Hello world.*");

        assertThat(
                assertThrows(IllegalArgumentException.class, () ->
                        logCapture.assertLoggedInOrder()
                ))
                .hasMessage("at least 2 LogExpectations are required for assertLoggedInOrder(). Found none");

        assertThat(
                assertThrows(IllegalArgumentException.class, () ->
                        logCapture.assertLoggedInOrder(info("Hello world"))
                ))
                .hasMessageMatching("at least 2 LogExpectations are required for assertLoggedInOrder\\(\\)\\. Found .*Hello world.*");

        assertThat(
                assertThrows(IllegalArgumentException.class, () ->
                        logCapture.assertNotLogged()
                ))
                .hasMessageMatching("at least one LogExpectation is required for assertNotLogged\\(\\)\\. Found none");
    }

    @Test
    void withRequiresAtLeastOneMatcher() {
        assertThat(
                assertThrows(IllegalArgumentException.class, () ->
                        logCapture
                                .with()
                                .assertLogged(info("Hello world"))
                ))
                .hasMessage("with() needs at least one LogEventMatcher");
    }

    @Test
    void allLevelsWork() {
        log.error("first error");
        log.error("second error");
        log.warn("first warn");
        log.warn("second warn");
        log.info("first info");
        log.info("second info");
        log.debug("first debug");
        log.debug("second debug");
        log.trace("first trace");
        log.trace("second trace");

        logCapture.assertLoggedInOrder(
                error("first error"),
                error("second error"));
        logCapture.assertLoggedInOrder(
                warn("first warn"),
                warn("second warn"));
        logCapture.assertLoggedInOrder(
                info("first info"),
                info("second info"));
        logCapture.assertLoggedInOrder(
                debug("first debug"),
                debug("second debug"));
        logCapture.assertLoggedInOrder(
                trace("first trace"),
                trace("second trace"));
        logCapture.assertNotLogged(warn("first error"));
        logCapture.assertNotLogged(any("notlogged error"));
    }

    @Nested
    class ExpectedExceptionWorks {
        @Test
        void expectedExceptionSucceeds() {
            log.warn("oh no!", new IllegalArgumentException("shame on you!", new NullPointerException("never use null")));

            logCapture.assertLogged(
                    warn("oh no!",
                            exception()
                                    .expectedMessageRegex("shame on you!")
                                    .expectedType(RuntimeException.class)
                                    .build()
                    ));
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
            logCapture.assertNotLogged(debug(), info(), trace(), error());
            logCapture.assertLogged(warn());
        }

        @Test
        void expectedExceptionFails() {
            log.info("without exception");
            log.warn("oh no!", new IllegalArgumentException("this is illegal", new NullPointerException("never be null!")));

            AssertionError assertionError = assertThrows(AssertionError.class,
                    () -> logCapture.assertLogged(
                            warn("oh no!",
                                    exception()
                                            .expectedMessageRegex("a message never used")
                                            .expectedType(RuntimeException.class)
                                            .build()
                            )));

            assertThat(assertionError).hasMessage("""
                    Expected log message has occurred, but never with the expected Exception:
                    message: WARN "oh no!" (regex)
                      expected exception: message (regex): "a message never used" type: java.lang.RuntimeException
                      actual exception: message: "this is illegal", type: java.lang.IllegalArgumentException, cause: (message: "never be null!", type: java.lang.NullPointerException)
                    """);

            AssertionError withoutExceptionAssertionError = assertThrows(AssertionError.class,
                    () -> logCapture.assertLogged(
                            info("without exception",
                                    exception()
                                            .expectedMessageRegex("a message never used")
                                            .expectedType(RuntimeException.class)
                                            .build()
                            )));

            assertThat(withoutExceptionAssertionError).hasMessage("""
                    Expected log message has occurred, but never with the expected Exception:
                    message: INFO "without exception" (regex)
                      expected exception: message (regex): "a message never used" type: java.lang.RuntimeException
                      actual exception: (null)
                    """);

        }

        @Test
        void assertNotLoggedFails() {
            log.info("testlogmessage");

            final AssertionError exceptionAny = assertThrows(AssertionError.class, () -> logCapture.assertNotLogged(any()));
            assertThat(exceptionAny).hasMessage("""
                    Found a log message that should not be logged.
                    message: <Any log message>
                    """);

            final AssertionError exceptionWithLevel = assertThrows(AssertionError.class, () -> logCapture.assertNotLogged(info()));
            assertThat(exceptionWithLevel).hasMessage("""
                    Found a log message that should not be logged.
                    message: INFO <any message>
                    """);

            final AssertionError exceptionWithRegex = assertThrows(AssertionError.class, () -> logCapture.assertNotLogged(any("testlogmessage")));
            assertThat(exceptionWithRegex).hasMessage("""
                    Found a log message that should not be logged.
                    message: <any level> "testlogmessage" (regex)
                    """);

            final AssertionError exceptionWithRegexAndLevel = assertThrows(AssertionError.class, () -> logCapture.assertNotLogged(info("testlogmessage")));
            assertThat(exceptionWithRegexAndLevel).hasMessage("""
                    Found a log message that should not be logged.
                    message: INFO "testlogmessage" (regex)
                    """);
        }

    }


    @Nested
    class ExpectedMarker {
        @Test
        void markerFailsBecauseOtherMarkerIsPresent() {
            log.info(MarkerFactory.getMarker("unexpected"), "hello with marker");

            AssertionError assertionError = assertThrows(AssertionError.class,
                    () -> logCapture.assertLogged(
                            info("hello with marker",
                                    marker("expected"))
                    ));

            assertThat(assertionError).hasMessage("""
                    Expected log message has occurred, but never with the expected marker name:
                    message: INFO "hello with marker" (regex)
                      expected marker name: "expected"
                      actual marker names: "[unexpected]"
                    """);
        }

        @Test
        void markerFailsBecauseNoMarkerIsPresent() {
            log.info("hello without marker");

            AssertionError assertionError = assertThrows(AssertionError.class,
                    () -> logCapture.assertLogged(
                            info("hello without marker",
                                    marker("expected"))
                    ));

            assertThat(assertionError).hasMessage("""
                    Expected log message has occurred, but never with the expected marker name:
                    message: INFO "hello without marker" (regex)
                      expected marker name: "expected"
                      but no marker was found
                    """);
        }

        @Test
        void markerFailsWhileNestedMarkersArePresent() {
            Marker marker = MarkerFactory.getMarker("unexpected_top");
            marker.add(MarkerFactory.getMarker("unexpected_nested"));
            log.info(marker, "hello with marker");

            AssertionError assertionError = assertThrows(AssertionError.class,
                    () -> logCapture.assertLogged(
                            info("hello with marker",
                                    marker("expected"))
                    ));

            assertThat(assertionError).hasMessage("""
                    Expected log message has occurred, but never with the expected marker name:
                    message: INFO "hello with marker" (regex)
                      expected marker name: "expected"
                      actual marker names: "[unexpected_top [ unexpected_nested ]]"
                    """);
        }

        @Test
        void markerFailsWhileMultipleMarkersArePresent() {
            log.atInfo()
                    .setMessage("hello with markers")
                    .addMarker(MarkerFactory.getMarker("unexpected1"))
                    .addMarker(MarkerFactory.getMarker("unexpected2"))
                    .log();

            AssertionError assertionError = assertThrows(AssertionError.class,
                    () -> logCapture.assertLogged(
                            info("hello with markers",
                                    marker("expected"))
                    ));

            assertThat(assertionError).hasMessage("""
                    Expected log message has occurred, but never with the expected marker name:
                    message: INFO "hello with markers" (regex)
                      expected marker name: "expected"
                      actual marker names: "[unexpected1, unexpected2]"
                    """);
        }

        @Test
        void markerSucceedsForNestedMarker() {
            Marker marker = MarkerFactory.getMarker("expected_top");
            marker.add(MarkerFactory.getMarker("expected_nested"));
            log.info(marker, "hello with marker");

            logCapture.assertLogged(
                    info("hello with marker",
                            marker("expected_nested")));
        }

        @Test
        void markerSucceeds() {
            log.info(MarkerFactory.getMarker("expected"), "hello with marker");

            logCapture.assertLogged(
                    info("hello with marker",
                            marker("expected")));
        }

        @Test
        void markerWithAssertNotLogged() {
            log.info(MarkerFactory.getMarker("expected"), "hello with marker");

            logCapture.assertNotLogged(
                    info("hello with marker",
                            marker("not expected")));

            final AssertionError assertionError = assertThrows(AssertionError.class, () -> logCapture.assertNotLogged(
                    info("hello with marker",
                            marker("expected"))));
            assertThat(assertionError).hasMessage("""
                    Found a log message that should not be logged.
                    message: INFO "hello with marker" (regex)
                      with additional matchers:
                      - marker name: "expected"
                    """);
        }

    }

    @Nested
    class ExpectedLogger {
        @Test
        void expectedLoggerSucceeds() {
            log.info("hello world");
            log.warn("bye world");

            logCapture.assertLogged(
                    warn("bye world",
                            logger("ReadableApiTest$"))
            );

            logCapture.with(
                    logger("ReadableApiTest$")
            ).assertLoggedInAnyOrder(
                    warn("bye world"),
                    info("hello world")
            );

            logCapture.with(
                    logger("^com.example.app.ReadableApiTest$")
            ).assertLoggedInAnyOrder(
                    info("hello world"),
                    warn("bye world")
            );
        }

        @Test
        void expectedLoggerFails() {
            log.info("hello world");
            log.warn("bye world");

            AssertionError assertionError = assertThrows(AssertionError.class,
                    () -> logCapture.assertLoggedInOrder(
                            info("hello world"),
                            warn("bye world",
                                    logger("WrongLogger$"))
                    ));

            assertThat(assertionError).hasMessage("""
                    Expected log message has occurred, but never with the expected logger name:
                    message: WARN "bye world" (regex)
                      expected logger name (regex): "WrongLogger$"
                      actual logger name: "com.example.app.ReadableApiTest"
                    """);
        }

        @Test
        void loggerWithAssertNotLogged() {
            log.info("hello on this logger");

            logCapture.assertNotLogged(
                    info("hello on this logger",
                            logger("wrongLogger")));

            final AssertionError assertionError = assertThrows(AssertionError.class, () -> logCapture.assertNotLogged(
                    info("hello on this logger",
                            logger("ReadableApiTest$"))));
            assertThat(assertionError)
                    .hasMessage("""
                            Found a log message that should not be logged.
                            message: INFO "hello on this logger" (regex)
                              with additional matchers:
                              - logger name (regex): "ReadableApiTest$"
                            """);
        }
    }

    @Nested
    class ExpectedMdc {
        @Test
        void singleMdcSucceeds() {
            log.info("hello world");
            MDC.put("key", "value");
            log.warn("bye world");
            MDC.clear();

            logCapture.assertLoggedInOrder(
                    info("hello world"),
                    warn("bye world",
                            mdc("key", "value"))
            );
        }

        @Test
        void singleMdcFails() {
            MDC.put("key", "value");
            log.info("hello world");
            MDC.clear();
            log.warn("bye world");

            AssertionError assertionError = assertThrows(AssertionError.class, () ->
                    logCapture.assertLoggedInOrder(
                            info("hello world"),
                            warn("bye world",
                                    mdc("key", "value"))
                    ));

            assertThat(assertionError).hasMessage("""
                    Expected log message has occurred, but never with the expected MDC value:
                    message: WARN "bye world" (regex)
                      captured message: "bye world"
                      expected MDC key: key
                      expected MDC value: ".*value.*"
                      captured MDC values:
                    """);
        }

        @Test
        void mdcForAllFails() {
            MDC.put("key", "value");
            log.info("hello world");
            MDC.clear();
            log.warn("bye world");

            AssertionError assertionError = assertThrows(AssertionError.class, () ->
                    logCapture
                            .with(
                                    mdc("key", "value"))
                            .assertLoggedInAnyOrder(
                                    info("hello world"),
                                    warn("bye world")
                            ));

            assertThat(assertionError).hasMessage("""
                    Expected log message has occurred, but never with the expected MDC value:
                    message: WARN "bye world" (regex)
                      captured message: "bye world"
                      expected MDC key: key
                      expected MDC value: ".*value.*"
                      captured MDC values:
                    """);
        }

        @Test
        void mdcForAllSucceeds() {
            MDC.put("key", "value");
            log.info("hello world");
            log.warn("bye world");
            MDC.clear();

            logCapture
                    .with(
                            mdc("key", "value"))
                    .assertLoggedInAnyOrder(
                            info("hello world"),
                            warn("bye world"));
        }

        @Test
        void mdcForAllAndSingleMdcFails() {
            MDC.put("key", "value");
            MDC.put("another_key", "another_value");
            log.info("hello world");
            MDC.remove("another_key");
            log.warn("bye world");
            MDC.clear();

            AssertionError assertionError = assertThrows(AssertionError.class, () ->
                    logCapture
                            .with(
                                    mdc("key", "value"))
                            .assertLoggedInAnyOrder(
                                    info("hello world"),
                                    warn("bye world",
                                            mdc("another_key", "another_value"))
                            ));

            assertThat(assertionError).hasMessage("""
                    Expected log message has occurred, but never with the expected MDC value:
                    message: WARN "bye world" (regex)
                      captured message: "bye world"
                      expected MDC key: another_key
                      expected MDC value: ".*another_value.*"
                      captured MDC values:
                        key: "value"
                    """);
        }

        @Test
        void mdcForAllAndSingleMdcSucceeds() {
            MDC.put("key", "value");
            log.info("hello world");
            MDC.put("another_key", "another_value");
            log.warn("bye world");
            MDC.remove("another_key");
            log.error("hello again");
            MDC.clear();

            logCapture
                    .with(
                            mdc("key", "value")
                    )
                    .assertLoggedInOrder(
                            info("hello world"),
                            warn("bye world",
                                    mdc("another_key", "another_value")),
                            error("hello again"));
        }

        @Test
        void customMdcMatcherFails() {
            MDC.put("key", "value");
            log.info("hello");
            MDC.clear();

            AssertionError assertionError = assertThrows(AssertionError.class,
                    () -> logCapture.assertLogged(
                            info("hello",
                                    mdc("key", mdcValue -> !mdcValue.equals("value"))
                            )));

            assertThat(assertionError).hasMessage("""
                    Expected log message has occurred, but never with the expected MDC value:
                    message: INFO "hello" (regex)
                      captured message: "hello"
                      expected MDC key: key
                      captured MDC values:
                        key: "value"
                    """);
        }

        @Test
        void customMdcMatcherSucceeds() {
            MDC.put("key", "value");
            log.info("hello");
            MDC.clear();

            logCapture.assertLogged(
                    info("hello",
                            mdc("key", mdcValue -> mdcValue.equals("value"))));
        }

        @Test
        void notLoggedWithMdc() {
            MDC.put("key", "value");
            MDC.put("another_key", "another_value");
            log.info("hello world");
            MDC.clear();

            logCapture.assertNotLogged(info("helloWorld", mdc("thirdKey", "value")));

            final AssertionError oneKeyMatches = assertThrows(AssertionError.class, () -> logCapture.assertNotLogged(info("hello world", mdc("key", "value"))));
            assertThat(oneKeyMatches).hasMessage("""
                    Found a log message that should not be logged.
                    message: INFO "hello world" (regex)
                      with additional matchers:
                      - MDCValue with key: "key"
                    """);

            final AssertionError bothKeysMatches = assertThrows(AssertionError.class, () -> logCapture.assertNotLogged(info("hello world", mdc("key", "value"), mdc("another_key", "another_value"))));
            assertThat(bothKeysMatches).hasMessage("""
                    Found a log message that should not be logged.
                    message: INFO "hello world" (regex)
                      with additional matchers:
                      - MDCValue with key: "key"
                      - MDCValue with key: "another_key"
                    """);
        }

    }

    @Nested
    class InOrder {
        @Test
        void assertionWithoutOrderSucceeds() {
            log.info("hello 1");
            log.info("hello 2");
            log.info("hello 3");

            logCapture.assertLoggedInAnyOrder(
                    info("hello 3"),
                    info("hello 1"),
                    info("hello 2")
            );
        }

        @Test
        void assertionWithoutOrderFails() {
            log.info("hello 1");
            log.info("hello 3");

            AssertionError assertionError = assertThrows(AssertionError.class, () ->
                    logCapture.assertLoggedInAnyOrder(
                            info("hello 3"),
                            info("hello 1"),
                            info("hello 2")
                    ));

            assertThat(assertionError).hasMessage("""
                    Expected log message has not occurred.
                    message: INFO "hello 2" (regex)
                    """);
        }

        @Test
            // this behavior is necessary because
            // 1. otherwise nothingElseLogged() would be a lot more complicated
            // 2. is is reasonable to assume that is it not the user's intention to verify the same message twice
        void assertionWithoutOrderMatchingSameMessageFails() {
            log.info("hello 1");
            log.info("hello 3");

            AssertionError assertionError = assertThrows(AssertionError.class, () ->
                    logCapture.assertLoggedInAnyOrder(
                            info("hello"),
                            info("1")
                    ));

            assertThat(assertionError).hasMessage("""
                    Imprecise matching: Two log expectations have matched the same message. Use more precise matching or in-order matching.
                    -- First match:
                    message: INFO "hello" (regex)
                    -- Second match:
                    message: INFO "1" (regex)
                    """);
        }

        @Test
        void assertionWithOrderSucceeds() {
            log.info("hello 1");
            log.info("hello 2");
            log.info("hello 3");

            logCapture.assertLoggedInOrder(
                    info("hello 1"),
                    info("hello 2"),
                    info("hello 3")
            );
        }

        @Test
        void assertionWithOrderFails() {
            log.info("hello 1");
            log.info("hello 2");
            log.info("hello 3");

            AssertionError assertionError = assertThrows(AssertionError.class, () ->
                    logCapture.assertLoggedInOrder(
                            info("hello 1"),
                            info("hello 3"),
                            info("hello 2")
                    ));

            assertThat(assertionError).hasMessage("""
                    Expected log message has not occurred.
                    message: INFO "hello 2" (regex)
                    """);
        }

        @Test
        void nothingElseLoggedInOrderSucceeds() {
            log.info("hello world");
            log.info("hello universe");

            logCapture
                    .assertLoggedInOrder(
                            info("hello world"),
                            info("hello universe"))
                    .assertNothingElseLogged();
        }

        @Test
        void nothingElseLoggedInOrderFails() {
            log.info("hello world");
            log.info("hello universe");
            log.info("hello multiverse");

            AssertionError assertionError = assertThrows(AssertionError.class, () ->
                    logCapture
                            .assertLoggedInOrder(
                                    info("hello world"),
                                    info("hello universe"))
                            .assertNothingElseLogged());

            assertThat(assertionError).hasMessage("There have been other log messages than the asserted ones.");
        }

        @Test
        void nothingElseLoggedOutOfOrderSucceeds() {
            log.info("hello world");
            log.info("hello universe");

            logCapture
                    .assertLoggedInAnyOrder(
                            info("hello universe"),
                            info("hello world"))
                    .assertNothingElseLogged();
        }

        @Test
        void nothingElseLoggedOutOfOrderFails() {
            log.info("hello world");
            log.info("hello multiverse");
            log.info("hello universe");

            AssertionError assertionError = assertThrows(AssertionError.class, () ->
                    logCapture
                            .assertLoggedInAnyOrder(
                                    info("hello universe"),
                                    info("hello world"))
                            .assertNothingElseLogged());

            assertThat(assertionError).hasMessage("There have been other log messages than the asserted ones.");
        }

        @Test
        void nothingElseLoggedSingleLogMessageSucceeds() {
            log.info("hello world");

            logCapture
                    .assertLogged(info("hello world"))
                    .assertNothingElseLogged();
        }

        @Test
        void nothingElseLoggedSingleLogMessageFails() {
            log.info("hello world");
            log.info("hello universe");

            AssertionError assertionError = assertThrows(AssertionError.class, () ->
                    logCapture
                            .assertLogged(info("hello world"))
                            .assertNothingElseLogged());

            assertThat(assertionError).hasMessage("There have been other log messages than the asserted ones.");
        }
    }

    @Test
    void combinedLogExpectationsOnlyOutputMismatch() {
        MDC.put("key", "a value");
        log.error("some error", new RuntimeException("an exception that was logged"));
        MDC.clear();

        AssertionError assertionError = assertThrows(AssertionError.class, () ->
                logCapture.assertLogged(
                        error("some error",
                                mdc("key", "a value"),
                                exception().expectedMessageRegex("an exception that was not logged").build())
                ));

        assertThat(assertionError).hasMessage("""
                Expected log message has occurred, but never with the expected Exception:
                message: ERROR "some error" (regex)
                  expected exception: message (regex): "an exception that was not logged"
                  actual exception: message: "an exception that was logged", type: java.lang.RuntimeException
                """);
    }

    @Nested
    class ExpectedKeyValues {
        @Test
        void failsWithDetailsNotMatchingExistingKeyValues() {
            log.atInfo().setMessage("hello")
                    .addKeyValue("key1", 1)
                    .addKeyValue("key2", "value2")
                    .log();

            var assertionError = assertThrows(AssertionError.class, () ->
                    logCapture.assertLogged(info("hello", keyValue("key", "a value")))
            );
            assertThat(assertionError).hasMessage("""
                    Expected log message has occurred, but never with the expected key-value pair:
                    message: INFO "hello" (regex)
                      expected key-value pair (key, a value)
                      actual pairs: [(key1, 1), (key2, value2)]
                    """);
        }

        @Test
        void failsWithDetailsWithNoExistingKeyValues() {
            log.atInfo().setMessage("hello")
                    .log();

            var assertionError = assertThrows(AssertionError.class, () ->
                    logCapture.assertLogged(info("hello", keyValue("key", "a value")))
            );
            assertThat(assertionError).hasMessage("""
                    Expected log message has occurred, but never with the expected key-value pair:
                    message: INFO "hello" (regex)
                      expected key-value pair (key, a value)
                      actual pairs: []
                    """);
        }

        @Test
        void requiresKey() {
            var assertionError = assertThrows(IllegalArgumentException.class, () ->
                    logCapture.assertLogged(info("hello", keyValue(null, "a value")))
            );
            assertThat(assertionError).hasMessage("key and value are required for key-value log assertion");
        }

        @Test
        void requiresValue() {
            var assertionError = assertThrows(IllegalArgumentException.class, () ->
                    logCapture.assertLogged(info("hello", keyValue("a_key", null)))
            );
            assertThat(assertionError).hasMessage("key and value are required for key-value log assertion");
        }

        @Test
        void succeedsWithString() {
            log.atInfo().setMessage("hello")
                    .addKeyValue("name", "Frederick")
                    .log();

            assertDoesNotThrow(() ->
                    logCapture.assertLogged(info("hello", keyValue("name", "Frederick")))
            );
        }

        @Test
        void succeedsWithLong() {
            log.atInfo().setMessage("hello")
                    .addKeyValue("meaning", 42L)
                    .log();

            assertDoesNotThrow(() ->
                    logCapture.assertLogged(info("hello", keyValue("meaning", 42L)))
            );
        }

        @Test
        void succeedsWithLongAndInt() {
            log.atInfo().setMessage("hello")
                    .addKeyValue("meaning", 42)
                    .log();

            assertDoesNotThrow(() ->
                    logCapture.assertLogged(info("hello", keyValue("meaning", 42L)))
            );
        }

        @Test
        void succeedsWithIntAndLong() {
            log.atInfo().setMessage("hello")
                    .addKeyValue("meaning", 42L)
                    .log();

            assertDoesNotThrow(() ->
                    logCapture.assertLogged(info("hello", keyValue("meaning", 42)))
            );
        }

        @Test
        void succeedsWithIntAndAtomicInt() {
            log.atInfo().setMessage("hello")
                    .addKeyValue("meaning", new AtomicInteger(42))
                    .log();

            assertDoesNotThrow(() ->
                    logCapture.assertLogged(info("hello", keyValue("meaning", 42)))
            );
        }

        @Test
        void failsWithBigDecimalAndPrecisionAndInt() {
            log.atInfo().setMessage("hello")
                    .addKeyValue("meaning", new BigDecimal("42.00"))
                    .log();

            var assertionError = assertThrows(AssertionError.class, () ->
                    logCapture.assertLogged(info("hello", keyValue("meaning", 42)))
            );
            assertThat(assertionError).hasMessage("""
                    Expected log message has occurred, but never with the expected key-value pair:
                    message: INFO "hello" (regex)
                      expected key-value pair (meaning, 42)
                      actual pairs: [(meaning, 42.00)]
                    """);
        }

        @Test
        void succeedsWithBigDecimalAndInt() {
            log.atInfo().setMessage("hello")
                    .addKeyValue("meaning", new BigDecimal("42"))
                    .log();

            assertDoesNotThrow(() ->
                    logCapture.assertLogged(info("hello", keyValue("meaning", 42)))
            );
        }
    }
}
