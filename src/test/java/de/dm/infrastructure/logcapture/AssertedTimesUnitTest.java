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
import static java.lang.System.lineSeparator;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

        // TODO: assert all assertion messages in this test files and check tests for completeness
        assertThat(assertionError).hasMessage("Expected log message has occurred, but never with the expected marker name: Level: INFO, Regex: \"hello without marker\"" +
                lineSeparator() + "  expected marker name: \"expected\"" +
                lineSeparator() + "  but no marker was found" +
                lineSeparator());
    }

    @Test
    void assertLoggedWithTimes_loggedTooLess_assertionFails() {
        log.info("hello world");
        log.info("hello world");

        assertThrows(AssertionError.class, () -> logCapture.assertLogged(times(3), info("hello world")));
    }

    @Test
    void times_lowerThan2_throwsIllegalArgumentException() {

        assertThrows(IllegalArgumentException.class, () -> times(1));
        assertThrows(IllegalArgumentException.class, () -> times(0));
    }

    @Test
    void atMost_lowerThan1_throwsIllegalArgumentException() {

        assertThrows(IllegalArgumentException.class, () -> atMost(0));
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

        assertThrows(AssertionError.class, () -> logCapture.assertLogged(once(), info("hello world")));
    }

    @Test
    void assertLoggedWithTimesOnce_loggedTooLess_assertionFails() {

        assertThrows(AssertionError.class, () -> logCapture.assertLogged(once(), info("hello world")));
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
    void assertLoggedWithAtLeast_loggedTooLess_assertionFails() {
        log.info("hello world");
        log.info("hello world");

        assertThrows(AssertionError.class, () -> logCapture.assertLogged(atLeast(3), info("hello world")));
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

        assertThrows(AssertionError.class, () -> logCapture.assertLogged(atMost(2), info("hello world")));
    }

    @Test
    void assertLoggedWithAtMost_loggedLessThanSpecified_succeeds() {
        log.info("hello world");
        log.info("hello world");

        assertDoesNotThrow(() -> logCapture.assertLogged(atMost(3), info("hello world")));
    }
}
