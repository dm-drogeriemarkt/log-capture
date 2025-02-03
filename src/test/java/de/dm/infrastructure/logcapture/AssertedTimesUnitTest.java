package de.dm.infrastructure.logcapture;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static de.dm.infrastructure.logcapture.ExpectedMdcEntry.mdc;
import static de.dm.infrastructure.logcapture.ExpectedTimes.atLeast;
import static de.dm.infrastructure.logcapture.ExpectedTimes.atMost;
import static de.dm.infrastructure.logcapture.ExpectedTimes.once;
import static de.dm.infrastructure.logcapture.ExpectedTimes.times;
import static de.dm.infrastructure.logcapture.LogExpectation.info;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SuppressWarnings({
        "java:S5778", //this rule does not increase the clarity of these tests
        "LoggingSimilarMessage" // not a sensible rule for a logging test
})
@Slf4j
class AssertedTimesUnitTest {
    @RegisterExtension
    LogCapture logCapture = LogCapture.forCurrentPackage();

    @Test
    void assertLoggedWithTimes_succeeds() {
        log.info("hello world");
        log.info("hello world");
        log.info("hello world");

        assertDoesNotThrow(() -> logCapture.assertLogged(times(3), info("hello world")));
    }

    @Test
    void assertLoggedWithTimes_loggedTooOften_assertionFails() {
        log.info("hello world");
        log.info("hello world");
        log.info("hello world");

        var assertionError = assertThrows(AssertionError.class, () -> logCapture.assertLogged(times(2), info("hello world", mdc("foo", "bar"), ExpectedException.exception().expectedMessageRegex("noooo!").build())));

        assertThat(assertionError).hasMessage("""
                Expected log message has not occurred exactly 2 time(s)
                actual occurrences: 0 (3 without additional matchers)
                message: INFO "hello world" (regex)
                  with additional matchers:
                  - MDCValue with key: "foo"
                  - Exception: message (regex): "noooo!"
                """);
    }

    @Test
    void assertLoggedWithTimes_loggedTooFewTimes_assertionFails() {
        log.info("hello world");
        log.info("hello world");

        var assertionError = assertThrows(AssertionError.class, () -> logCapture.assertLogged(times(3), info("hello world")));

        assertThat(assertionError).hasMessage("""
                Expected log message has not occurred exactly 3 time(s)
                actual occurrences: 2
                message: INFO "hello world" (regex)
                """);
    }

    @Test
    void times_lowerThanZero_throwsIllegalArgumentException() {
        var assertionError = assertThrows(IllegalArgumentException.class, () -> times(-1));
        assertThat(assertionError).hasMessage("Number of log message occurrences that are expected must be positive.");
    }

    @Test
    void atMost_lowerThanZero_throwsIllegalArgumentException() {
        var assertionError = assertThrows(IllegalArgumentException.class, () -> atMost(-1));
        assertThat(assertionError).hasMessage("Maximum number of log message occurrences that are expected must be greater than 0.");

    }

    @Test
    void assertLoggedWithTimesOnce_succeeds() {
        log.info("hello world");

        assertDoesNotThrow(() -> logCapture.assertLogged(once(), info("hello world")));
    }

    @Test
    void assertLoggedWithTimesOnce_loggedTooOften_assertionFails() {
        log.info("hello world");
        log.info("hello world");

        var assertionError = assertThrows(AssertionError.class, () -> logCapture.assertLogged(once(), info("hello world")));
        assertThat(assertionError).hasMessage("""
                Expected log message has not occurred exactly 1 time(s)
                actual occurrences: 2
                message: INFO "hello world" (regex)
                """);

    }

    @Test
    void assertLoggedWithTimesOnce_loggedTooLittle_assertionFails() {

        var assertionError = assertThrows(AssertionError.class, () -> logCapture.assertLogged(once(), info("hello world")));
        assertThat(assertionError).hasMessage("""
                Expected log message has not occurred exactly 1 time(s)
                actual occurrences: 0
                message: INFO "hello world" (regex)
                """);
    }

    @Test
    void assertLoggedWithAtLeast_succeeds() {
        log.info("hello world");
        log.info("hello world");
        log.info("hello world");

        assertDoesNotThrow(() -> logCapture.assertLogged(atLeast(3), info("hello world")));
    }

    @Test
    void assertLoggedWithAtLeast_loggedTooOften_succeeds() {
        log.info("hello world");
        log.info("hello world");
        log.info("hello world");

        assertDoesNotThrow(() -> logCapture.assertLogged(atLeast(2), info("hello world")));
    }

    @Test
    void assertLoggedWithAtLeast_loggedTooLittle_assertionFails() {
        log.info("hello world");
        log.info("hello world");

        var assertionError = assertThrows(AssertionError.class, () -> logCapture.assertLogged(atLeast(3), info("hello world")));
        assertThat(assertionError).hasMessage("""
                Expected log message has not occurred at least 3 time(s)
                actual occurrences: 2
                message: INFO "hello world" (regex)
                """);

    }

    @Test
    void assertLoggedWithAtMost_succeeds() {
        log.info("hello world");
        log.info("hello world");
        log.info("hello world");

        assertDoesNotThrow(() -> logCapture.assertLogged(atMost(3), info("hello world")));
    }

    @Test
    void assertLoggedWithAtMost_loggedTooOften_assertionFails() {
        log.info("hello world");
        log.info("hello world");
        log.info("hello world");

        var assertionError = assertThrows(AssertionError.class, () -> logCapture.assertLogged(atMost(2), info("hello world")));
        assertThat(assertionError).hasMessage("""
                Expected log message has not occurred at most 2 time(s)
                actual occurrences: 3
                message: INFO "hello world" (regex)
                """);
    }

    @Test
    void assertLoggedWithAtMost_loggedLessThanSpecified_succeeds() {
        log.info("hello world");
        log.info("hello world");

        assertDoesNotThrow(() -> logCapture.assertLogged(atMost(3), info("hello world")));
    }
}
