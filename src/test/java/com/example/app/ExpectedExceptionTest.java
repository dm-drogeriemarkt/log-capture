package com.example.app;

import de.dm.infrastructure.logcapture.LogCapture;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static de.dm.infrastructure.logcapture.ExpectedException.exception;
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
class ExpectedExceptionTest {
    @RegisterExtension
    LogCapture logCapture = LogCapture.forCurrentPackage();

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
