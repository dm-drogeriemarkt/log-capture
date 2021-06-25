package de.dm.infrastructure.logcapture;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.MDC;

import java.util.Optional;

import static java.lang.System.lineSeparator;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Slf4j
class FluentApiTest {
    @RegisterExtension
    LogCapture logCapture = LogCapture.forCurrentPackage();


    @Test
    void basicFluentApi() {
        log.info("hello world");

        logCapture.info().assertLogged("hello world");
    }

    @Test
    void inOrderSucceeds() {
        log.info("hello world");
        log.warn("bye world");

        logCapture
                .info().assertLogged("hello world")
                .warn().assertLogged("bye world");
    }

    @Test
    void inOrderFails() {
        log.warn("bye world");
        log.info("hello world");

        AssertionError assertionError = assertThrows(AssertionError.class, () ->
                logCapture
                        .info().assertLogged("hello world")
                        .warn().assertLogged("bye world"));

        assertThat(assertionError).hasMessage("Expected log message has not occurred: Level: WARN, Regex: \"bye world\"");
    }

    @Test
    void nothingElseLoggedSucceeds() {
        log.info("hello world");

        logCapture
                .info().assertLogged("hello world")
                .assertNothingElseLogged();
    }

    @Test
    void nothingElseLoggedFails() {
        log.info("hello world");
        log.info("hello universe");


        AssertionError assertionError = assertThrows(AssertionError.class, () ->
                logCapture
                        .info().assertLogged("hello world")
                        .assertNothingElseLogged());

        assertThat(assertionError).hasMessage("There have been other log messages than the asserted ones.");
    }

    @Test
    void wrongUsageThrowsIllegalState() {
        // this constructor is package private, so it should be effectively impossible to actually cause this IllegalStateException
        FluentLogAssertion fluentLogAssertion = new FluentLogAssertion(null, Optional.empty());

        IllegalStateException illegalStateException = assertThrows(IllegalStateException.class, () -> fluentLogAssertion.assertNothingElseLogged());

        assertThat(illegalStateException).hasMessage("assertNothingElseLogged() must be called with a previous log assertion");
    }

    @Test
    void singleMdcSucceeds() {
        log.info("hello world");
        MDC.put("key", "value");
        log.warn("bye world");
        MDC.clear();

        logCapture
                .info().assertLogged("hello world")
                .warn().withMdc("key", "value").assertLogged("bye world");
    }

    @Test
    void singleMdcFails() {
        MDC.put("key", "value");
        log.info("hello world");
        MDC.clear();
        log.warn("bye world");

        AssertionError assertionError = assertThrows(AssertionError.class, () ->
                logCapture
                        .info().assertLogged("hello world")
                        .warn().withMdc("key", "value").assertLogged("bye world"));

        assertThat(assertionError).hasMessage("Expected log message has occurred, but never with the expected MDC value: Level: WARN, Regex: \"bye world\"" +
                lineSeparator() + "  captured message: \"bye world\"" +
                lineSeparator() + "  expected MDC key: key" +
                lineSeparator() + "  expected MDC value: \".*value.*\"" +
                lineSeparator() + "  captured MDC values:" +
                lineSeparator());
    }

    @Test
    void mdcForAllFails() {
        MDC.put("key", "value");
        log.info("hello world");
        MDC.clear();
        log.warn("bye world");

        AssertionError assertionError = assertThrows(AssertionError.class, () ->
                logCapture
                        .withMdcForAll("key", "value")
                        .info().assertLogged("hello world")
                        .warn().assertLogged("bye world"));

        assertThat(assertionError).hasMessage("Expected log message has occurred, but never with the expected MDC value: Level: WARN, Regex: \"bye world\"" +
                lineSeparator() + "  captured message: \"bye world\"" +
                lineSeparator() + "  expected MDC key: key" +
                lineSeparator() + "  expected MDC value: \".*value.*\"" +
                lineSeparator() + "  captured MDC values:" +
                lineSeparator());
    }

    @Test
    void mdcForAllSucceeds() {
        MDC.put("key", "value");
        log.info("hello world");
        log.warn("bye world");
        MDC.clear();

        logCapture
                .withMdcForAll("key", "value")
                .info().assertLogged("hello world")
                .warn().assertLogged("bye world");
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
                        .withMdcForAll("key", "value")
                        .info().assertLogged("hello world")
                        .warn().withMdc("another_key", "another_value").assertLogged("bye world")
        );

        assertThat(assertionError).hasMessage(
                "Expected log message has occurred, but never with the expected MDC value: Level: WARN, Regex: \"bye world\"" +
                        lineSeparator() + "  captured message: \"bye world\"" +
                        lineSeparator() + "  expected MDC key: key" +
                        lineSeparator() + "  expected MDC value: \".*value.*\"" +
                        lineSeparator() + "  captured MDC values:" +
                        lineSeparator() + "    key: \"value\"" +
                        lineSeparator() + "Expected log message has occurred, but never with the expected MDC value: Level: WARN, Regex: \"bye world\"" +
                        lineSeparator() + "  captured message: \"bye world\"" +
                        lineSeparator() + "  expected MDC key: another_key" +
                        lineSeparator() + "  expected MDC value: \".*another_value.*\"" +
                        lineSeparator() + "  captured MDC values:" +
                        lineSeparator() + "    key: \"value\"" +
                        lineSeparator());
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
                .withMdcForAll("key", "value")
                .info().assertLogged("hello world")
                .warn().withMdc("another_key", "another_value").assertLogged("bye world")
                .error().assertLogged("hello again");
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

        logCapture
                .error().assertLogged("first error")
                .error().assertLogged("second error");
        logCapture
                .warn().assertLogged("first warn")
                .warn().assertLogged("second warn");
        logCapture
                .info().assertLogged("first info")
                .info().assertLogged("second info");
        logCapture
                .debug().assertLogged("first debug")
                .debug().assertLogged("second debug");
        logCapture
                .trace().assertLogged("first trace")
                .trace().assertLogged("second trace");
    }
}
