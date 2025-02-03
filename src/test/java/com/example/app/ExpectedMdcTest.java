package com.example.app;

import de.dm.infrastructure.logcapture.LogCapture;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.MDC;

import static de.dm.infrastructure.logcapture.ExpectedMdcEntry.mdc;
import static de.dm.infrastructure.logcapture.LogExpectation.error;
import static de.dm.infrastructure.logcapture.LogExpectation.info;
import static de.dm.infrastructure.logcapture.LogExpectation.warn;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Slf4j
@SuppressWarnings({
        "java:S5778", //this rule does not increase the clarity of these tests
        "LoggingSimilarMessage" // not a sensible rule for a logging test
})
class ExpectedMdcTest {
    @RegisterExtension
    LogCapture logCapture = LogCapture.forCurrentPackage();

    @Test
    void singleMdcSucceeds() {
        log.info("hello world");
        MDC.put("key", "value");
        log.warn("bye world");
        MDC.clear();

        logCapture.assertLoggedInOrder(
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
                logCapture.assertLoggedInOrder(
                        info("hello world"),
                        warn("bye world",
                                mdc("key", "value"))
                ));

        assertThat(assertionError).hasMessage("""
                Expected log message has occurred, but never with the expected MDC value:
                message: WARN "bye world" (regex)
                  captured message: "bye world"
                  expected MDC key: key
                  expected MDC value: ".*value.*"
                  captured MDC values:
                """);
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
                        .assertLoggedInAnyOrder(
                                info("hello world"),
                                warn("bye world")
                        ));

        assertThat(assertionError).hasMessage("""
                Expected log message has occurred, but never with the expected MDC value:
                message: WARN "bye world" (regex)
                  captured message: "bye world"
                  expected MDC key: key
                  expected MDC value: ".*value.*"
                  captured MDC values:
                """);
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
                .assertLoggedInAnyOrder(
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
                        .assertLoggedInAnyOrder(
                                info("hello world"),
                                warn("bye world",
                                        mdc("another_key", "another_value"))
                        ));

        assertThat(assertionError).hasMessage("""
                Expected log message has occurred, but never with the expected MDC value:
                message: WARN "bye world" (regex)
                  captured message: "bye world"
                  expected MDC key: another_key
                  expected MDC value: ".*another_value.*"
                  captured MDC values:
                    key: "value"
                """);
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

    @Test
    void customMdcMatcherFails() {
        MDC.put("key", "value");
        log.info("hello");
        MDC.clear();

        AssertionError assertionError = assertThrows(AssertionError.class,
                () -> logCapture.assertLogged(
                        info("hello",
                                mdc("key", mdcValue -> !mdcValue.equals("value"))
                        )));

        assertThat(assertionError).hasMessage("""
                Expected log message has occurred, but never with the expected MDC value:
                message: INFO "hello" (regex)
                  captured message: "hello"
                  expected MDC key: key
                  captured MDC values:
                    key: "value"
                """);
    }

    @Test
    void customMdcMatcherSucceeds() {
        MDC.put("key", "value");
        log.info("hello");
        MDC.clear();

        logCapture.assertLogged(
                info("hello",
                        mdc("key", mdcValue -> mdcValue.equals("value"))));
    }

    @Test
    void notLoggedWithMdc() {
        MDC.put("key", "value");
        MDC.put("another_key", "another_value");
        log.info("hello world");
        MDC.clear();

        logCapture.assertNotLogged(info("helloWorld", mdc("thirdKey", "value")));

        final AssertionError oneKeyMatches = assertThrows(AssertionError.class, () -> logCapture.assertNotLogged(info("hello world", mdc("key", "value"))));
        assertThat(oneKeyMatches).hasMessage("""
                Found a log message that should not be logged.
                message: INFO "hello world" (regex)
                  with additional matchers:
                  - MDCValue with key: "key"
                """);

        final AssertionError bothKeysMatches = assertThrows(AssertionError.class, () -> logCapture.assertNotLogged(info("hello world", mdc("key", "value"), mdc("another_key", "another_value"))));
        assertThat(bothKeysMatches).hasMessage("""
                Found a log message that should not be logged.
                message: INFO "hello world" (regex)
                  with additional matchers:
                  - MDCValue with key: "key"
                  - MDCValue with key: "another_key"
                """);
    }

}
