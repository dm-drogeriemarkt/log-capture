package de.dm.infrastructure.logcapture;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static de.dm.infrastructure.logcapture.LogExpectation.info;
import static de.dm.infrastructure.logcapture.Times.atLeast;
import static de.dm.infrastructure.logcapture.Times.atMost;
import static de.dm.infrastructure.logcapture.Times.once;
import static de.dm.infrastructure.logcapture.Times.times;
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

        assertDoesNotThrow(() -> logCapture.assertLogged(info("hello world"), times(3)));
    }

    @Test
    void assertLoggedWithTimes_loggedTooOften_assertionFails() {
        log.info("hello world");
        log.info("hello world");
        log.info("hello world");

        assertThrows(AssertionError.class, () -> logCapture.assertLogged(info("hello world"), times(2)));
    }

    @Test
    void assertLoggedWithTimes_loggedTooLess_assertionFails() {
        log.info("hello world");
        log.info("hello world");

        assertThrows(AssertionError.class, () -> logCapture.assertLogged(info("hello world"), times(3)));
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

        assertDoesNotThrow(() -> logCapture.assertLogged(info("hello world"), once()));
    }

    @Test
    void assertLoggedWithTimesOnce_loggedTooOften_assertionFails() {
        log.info("hello world");
        log.info("hello world");

        assertThrows(AssertionError.class, () -> logCapture.assertLogged(info("hello world"), once()));
    }

    @Test
    void assertLoggedWithTimesOnce_loggedTooLess_assertionFails() {

        assertThrows(AssertionError.class, () -> logCapture.assertLogged(info("hello world"), once()));
    }

    @Test
    void assertLoggedWithAtLeast_succeeds() {
        log.info("hello world");
        log.info("hello world");
        log.info("hello world");

        assertDoesNotThrow(() -> logCapture.assertLogged(info("hello world"), atLeast(3)));
    }

    @Test
    void assertLoggedWithAtLeast_loggedTooOften_succeeds() {
        log.info("hello world");
        log.info("hello world");
        log.info("hello world");

        assertDoesNotThrow(() -> logCapture.assertLogged(info("hello world"), atLeast(2)));
    }

    @Test
    void assertLoggedWithAtLeast_loggedTooLess_assertionFails() {
        log.info("hello world");
        log.info("hello world");

        assertThrows(AssertionError.class, () -> logCapture.assertLogged(info("hello world"), atLeast(3)));
    }

    @Test
    void assertLoggedWithAtMost_succeeds() {
        log.info("hello world");
        log.info("hello world");
        log.info("hello world");

        assertDoesNotThrow(() -> logCapture.assertLogged(info("hello world"), atMost(3)));
    }

    @Test
    void assertLoggedWithAtMost_loggedTooOften_assertionFails() {
        log.info("hello world");
        log.info("hello world");
        log.info("hello world");

        assertThrows(AssertionError.class, () -> logCapture.assertLogged(info("hello world"), atMost(2)));
    }

    @Test
    void assertLoggedWithAtMost_loggedLessThanSpecified_succeeds() {
        log.info("hello world");
        log.info("hello world");

        assertDoesNotThrow(() -> logCapture.assertLogged(info("hello world"), atMost(3)));
    }
}
