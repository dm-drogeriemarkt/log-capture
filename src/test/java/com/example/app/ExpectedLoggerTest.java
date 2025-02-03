package com.example.app;

import de.dm.infrastructure.logcapture.LogCapture;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static de.dm.infrastructure.logcapture.ExpectedLoggerName.logger;
import static de.dm.infrastructure.logcapture.LogExpectation.info;
import static de.dm.infrastructure.logcapture.LogExpectation.warn;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Slf4j
@SuppressWarnings({
        "java:S5778", //this rule does not increase the clarity of these tests
        "LoggingSimilarMessage" // not a sensible rule for a logging test
})
class ExpectedLoggerTest {
    @RegisterExtension
    LogCapture logCapture = LogCapture.forCurrentPackage();

    @Test
    void expectedLoggerSucceeds() {
        log.info("hello world");
        log.warn("bye world");

        logCapture.assertLogged(
                warn("bye world",
                        logger("ExpectedLoggerTest"))
        );

        logCapture.with(
                logger("ExpectedLoggerTest")
        ).assertLoggedInAnyOrder(
                warn("bye world"),
                info("hello world")
        );

        logCapture.with(
                logger("^com.example.app.ExpectedLoggerTest")
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
                  actual logger name: "com.example.app.ExpectedLoggerTest"
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
                        logger("ExpectedLoggerTest$"))));
        assertThat(assertionError)
                .hasMessage("""
                        Found a log message that should not be logged.
                        message: INFO "hello on this logger" (regex)
                          with additional matchers:
                          - logger name (regex): "ExpectedLoggerTest$"
                        """);
    }
}
