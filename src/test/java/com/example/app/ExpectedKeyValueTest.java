package com.example.app;

import de.dm.infrastructure.logcapture.LogCapture;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;

import static de.dm.infrastructure.logcapture.ExpectedKeyValue.keyValue;
import static de.dm.infrastructure.logcapture.LogExpectation.info;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Slf4j
@SuppressWarnings({
        "java:S5778", //this rule does not increase the clarity of these tests
        "LoggingSimilarMessage" // not a sensible rule for a logging test
})
class ExpectedKeyValueTest {
    @RegisterExtension
    LogCapture logCapture = LogCapture.forCurrentPackage();


    @Test
    void failsWithDetailsNotMatchingExistingKeyValues() {
        log.atInfo().setMessage("hello")
                .addKeyValue("key1", 1)
                .addKeyValue("key2", "value2")
                .log();

        var assertionError = assertThrows(AssertionError.class, () ->
                logCapture.assertLogged(info("hello", keyValue("key", "a value")))
        );
        assertThat(assertionError).hasMessage("""
                Expected log message has occurred, but never with the expected key-value pair:
                message: INFO "hello" (regex)
                  expected key-value pair (key, a value)
                  actual pairs: [(key1, 1), (key2, value2)]
                """);
    }

    @Test
    void failsWithDetailsWithNoExistingKeyValues() {
        log.atInfo().setMessage("hello")
                .log();

        var assertionError = assertThrows(AssertionError.class, () ->
                logCapture.assertLogged(info("hello", keyValue("key", "a value")))
        );
        assertThat(assertionError).hasMessage("""
                Expected log message has occurred, but never with the expected key-value pair:
                message: INFO "hello" (regex)
                  expected key-value pair (key, a value)
                  actual pairs: []
                """);
    }

    @Test
    void requiresKey() {
        var assertionError = assertThrows(IllegalArgumentException.class, () ->
                logCapture.assertLogged(info("hello", keyValue(null, "a value")))
        );
        assertThat(assertionError).hasMessage("key and value are required for key-value log assertion");
    }

    @Test
    void requiresValue() {
        var assertionError = assertThrows(IllegalArgumentException.class, () ->
                logCapture.assertLogged(info("hello", keyValue("a_key", null)))
        );
        assertThat(assertionError).hasMessage("key and value are required for key-value log assertion");
    }

    @Test
    void succeedsWithString() {
        log.atInfo().setMessage("hello")
                .addKeyValue("name", "Frederick")
                .log();

        Assertions.assertDoesNotThrow(() ->
                logCapture.assertLogged(info("hello", keyValue("name", "Frederick")))
        );
    }

    @Test
    void succeedsWithLong() {
        log.atInfo().setMessage("hello")
                .addKeyValue("meaning", 42L)
                .log();

        Assertions.assertDoesNotThrow(() ->
                logCapture.assertLogged(info("hello", keyValue("meaning", 42L)))
        );
    }

    @Test
    void succeedsWithLongAndInt() {
        log.atInfo().setMessage("hello")
                .addKeyValue("meaning", 42)
                .log();

        Assertions.assertDoesNotThrow(() ->
                logCapture.assertLogged(info("hello", keyValue("meaning", 42L)))
        );
    }

    @Test
    void succeedsWithIntAndLong() {
        log.atInfo().setMessage("hello")
                .addKeyValue("meaning", 42L)
                .log();

        Assertions.assertDoesNotThrow(() ->
                logCapture.assertLogged(info("hello", keyValue("meaning", 42)))
        );
    }

    @Test
    void succeedsWithIntAndAtomicInt() {
        log.atInfo().setMessage("hello")
                .addKeyValue("meaning", new AtomicInteger(42))
                .log();

        Assertions.assertDoesNotThrow(() ->
                logCapture.assertLogged(info("hello", keyValue("meaning", 42)))
        );
    }

    @Test
    void failsWithBigDecimalAndPrecisionAndInt() {
        log.atInfo().setMessage("hello")
                .addKeyValue("meaning", new BigDecimal("42.00"))
                .log();

        var assertionError = assertThrows(AssertionError.class, () ->
                logCapture.assertLogged(info("hello", keyValue("meaning", 42)))
        );
        assertThat(assertionError).hasMessage("""
                Expected log message has occurred, but never with the expected key-value pair:
                message: INFO "hello" (regex)
                  expected key-value pair (meaning, 42)
                  actual pairs: [(meaning, 42.00)]
                """);
    }

    @Test
    void succeedsWithBigDecimalAndInt() {
        log.atInfo().setMessage("hello")
                .addKeyValue("meaning", new BigDecimal("42"))
                .log();

        Assertions.assertDoesNotThrow(() ->
                logCapture.assertLogged(info("hello", keyValue("meaning", 42)))
        );
    }
}
