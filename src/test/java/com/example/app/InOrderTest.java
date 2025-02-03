package com.example.app;

import de.dm.infrastructure.logcapture.LogCapture;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static de.dm.infrastructure.logcapture.LogExpectation.info;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Slf4j
@SuppressWarnings({
        "java:S5778", //this rule does not increase the clarity of these tests
        "LoggingSimilarMessage" // not a sensible rule for a logging test
})
class InOrderTest {
    @RegisterExtension
    LogCapture logCapture = LogCapture.forCurrentPackage();

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
