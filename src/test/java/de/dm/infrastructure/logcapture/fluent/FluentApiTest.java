package de.dm.infrastructure.logcapture.fluent;

import de.dm.infrastructure.logcapture.LogCapture;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.MDC;

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

        logCapture.info().assertMessage("hello world");
    }

    @Test
    void inOrderSucceeds() {
        log.info("hello world");
        log.warn("bye world");

        logCapture
                .info().assertMessage("hello world")
                .warn().assertMessage("bye world");
    }

    @Test
    void inOrderFails() {
        log.warn("bye world");
        log.info("hello world");

        AssertionError assertionError = assertThrows(AssertionError.class, () ->
                logCapture
                        .info().assertMessage("hello world")
                        .warn().assertMessage("bye world"));

        assertThat(assertionError).hasMessage("Expected log message has not occurred: Level: WARN, Regex: \"bye world\"");
    }

    @Test
    void singleMdcSucceeds() {
        log.info("hello world");
        MDC.put("key", "value");
        log.warn("bye world");
        MDC.clear();

        logCapture
                .info().assertMessage("hello world")
                .warn().withMdc("key", "value").assertMessage("bye world");
    }

    @Test
    void singleMdcFails() {
        MDC.put("key", "value");
        log.info("hello world");
        MDC.clear();
        log.warn("bye world");

        AssertionError assertionError = assertThrows(AssertionError.class, () ->
                logCapture
                        .info().assertMessage("hello world")
                        .warn().withMdc("key", "value").assertMessage("bye world"));

        assertThat(assertionError).hasMessage("Expected log message has occurred, but never with the expected MDC value: Level: WARN, Regex: \"bye world\"" +
                lineSeparator() + "  Captured message: \"bye world\"" +
                lineSeparator() + "  Captured MDC values:");
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
                        .info().assertMessage("hello world")
                        .warn().assertMessage("bye world"));

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
                .withMdcForAll("key", "value")
                .info().assertMessage("hello world")
                .warn().assertMessage("bye world");
    }

    @Test
    void mdcForAllAndSingleMdcFails() {
        MDC.put("key", "value");
        MDC.put("another_key", "another_value");
        log.info("hello world");
        MDC.remove("another_key");
        log.warn("bye world");
        MDC.clear();

        AssertionError assertionError = assertThrows(AssertionError.class, () -> {
            logCapture
                    .withMdcForAll("key", "value")
                    .info().assertMessage("hello world")
                    .warn().withMdc("another_key", "another_value").assertMessage("bye world");
        });

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
                .withMdcForAll("key", "value")
                .info().assertMessage("hello world")
                .warn().withMdc("another_key", "another_value").assertMessage("bye world")
                .error().assertMessage("hello again");
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
                .error().assertMessage("first error")
                .error().assertMessage("second error");
        logCapture
                .warn().assertMessage("first warn")
                .warn().assertMessage("second warn");
        logCapture
                .info().assertMessage("first info")
                .info().assertMessage("second info");
        logCapture
                .debug().assertMessage("first debug")
                .debug().assertMessage("second debug");
        logCapture
                .trace().assertMessage("first trace")
                .trace().assertMessage("second trace");
    }
}
