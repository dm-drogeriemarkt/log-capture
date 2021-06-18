package de.dm.infrastructure.logcapture;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.MDC;

import static de.dm.infrastructure.logcapture.LogAssertion.debug;
import static de.dm.infrastructure.logcapture.LogAssertion.error;
import static de.dm.infrastructure.logcapture.LogAssertion.info;
import static de.dm.infrastructure.logcapture.LogAssertion.trace;
import static de.dm.infrastructure.logcapture.LogAssertion.warn;
import static de.dm.infrastructure.logcapture.MatchingCondition.mdc;
import static java.lang.System.lineSeparator;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Slf4j
class ReadableApiTest {
    @RegisterExtension
    LogCapture logCapture = LogCapture.forCurrentPackage();

    @Test
    void basicReadableApiSucceeds() {
        log.info("hello world");

        logCapture.assertLogged(info("hello world"));
    }

    @Test
    void assertionWithoutOrderSucceeds() {
        log.info("hello 1");
        log.info("hello 2");
        log.info("hello 3");

        logCapture.assertLogged(
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
                logCapture.assertLogged(
                        info("hello 3"),
                        info("hello 1"),
                        info("hello 2")
                ));

        assertThat(assertionError).hasMessage("Expected log message has not occurred: Level: INFO, Regex: \"hello 2\"");
    }

    @Test
        // this behavior is necessary because
        // 1. otherwise nothingElseLogged() would be a lot more complicated
        // 2. is is reasonable to assume that is it not the user's intention to verify the same message twice
    void assertionWithoutOrderMatchingSameMessageFails() {
        log.info("hello 1");
        log.info("hello 3");

        AssertionError assertionError = assertThrows(AssertionError.class, () ->
                logCapture.assertLogged(
                        info("hello"),
                        info("1")
                ));

        assertThat(assertionError).hasMessage("Imprecise matching: Two log assertions have matched the same message. " +
                "Use more precise matching or in-order matching. " +
                "(First match: Level: INFO, Regex: \"hello\" | Second match: Level: INFO, Regex: \"1\"");
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

        assertThat(assertionError).hasMessage("Expected log message has not occurred: Level: INFO, Regex: \"hello 2\""); //TODO: output that it has occurred before?
    }

    @Test
    void singleMdcSucceeds() {
        log.info("hello world");
        MDC.put("key", "value");
        log.warn("bye world");
        MDC.clear();

        logCapture.assertLogged(
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
                logCapture.assertLogged(
                        info("hello world"),
                        warn("bye world",
                                mdc("key", "value"))
                ));

        assertThat(assertionError).hasMessage("Expected log message has occurred, but never with the expected MDC value: Level: WARN, Regex: \"bye world\"" +
                lineSeparator() + "  Captured message: \"bye world\"" +
                lineSeparator() + "  Captured MDC values:");
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
                .assertLogged(
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
                        .assertLogged(
                                info("hello universe"),
                                info("hello world"))
                        .assertNothingElseLogged());

        assertThat(assertionError).hasMessage("There have been other log messages than the asserted ones.");
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
                        .assertLoggedMessage(
                                info("hello world"),
                                warn("bye world")
                        ));

        assertThat(assertionError).hasMessage("Expected log message has occurred, but never with the expected MDC value: Level: WARN, Regex: \"bye world\"" +
                lineSeparator() + "  Captured message: \"bye world\"" +
                lineSeparator() + "  Captured MDC values:");
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
                .assertLoggedMessage(
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
                        .assertLoggedMessage(
                                info("hello world"),
                                warn("bye world",
                                        mdc("another_key", "another_value"))
                        ));

        assertThat(assertionError).hasMessage("Expected log message has occurred, but never with the expected MDC value: Level: WARN, Regex: \"bye world\""
                + lineSeparator() + "  Captured message: \"bye world\""
                + lineSeparator() + "  Captured MDC values:"
                + lineSeparator() + "    key: \"value\"");
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

    //TODO: check if all necessary MDC combinations are tested

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
    }
}
