package com.example.app;

import de.dm.infrastructure.logcapture.LogCapture;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.MDC;

import static de.dm.infrastructure.logcapture.ExpectedException.exception;
import static de.dm.infrastructure.logcapture.ExpectedMdcEntry.mdc;
import static de.dm.infrastructure.logcapture.LogExpectation.any;
import static de.dm.infrastructure.logcapture.LogExpectation.debug;
import static de.dm.infrastructure.logcapture.LogExpectation.error;
import static de.dm.infrastructure.logcapture.LogExpectation.info;
import static de.dm.infrastructure.logcapture.LogExpectation.trace;
import static de.dm.infrastructure.logcapture.LogExpectation.warn;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Slf4j
@SuppressWarnings({
        "java:S5778", //this rule does not increase the clarity of these tests
        "LoggingSimilarMessage" // not a sensible rule for a logging test
})
class BasicReadableApiTest {
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

}
