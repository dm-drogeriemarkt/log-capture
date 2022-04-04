package com.example.app;

import de.dm.infrastructure.logcapture.LogCapture;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.argument.StructuredArguments;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static de.dm.infrastructure.logcapture.ExpectedKeyValue.keyValue;
import static de.dm.infrastructure.logcapture.LogExpectation.info;
import static java.lang.System.lineSeparator;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SuppressWarnings("java:S5778") //this rule does not increase the clarity of these tests
@Slf4j
class LogstashKeyValueTest {
    @RegisterExtension
    LogCapture logCapture = LogCapture.forCurrentPackage();

    @Test
    void worksWithString() {
        log.info("hello", StructuredArguments.keyValue("myKey", "myValue"));

        logCapture.assertLogged(info("hello", keyValue("myKey", "myValue")));
    }

    @Test
    void failsWithString() {
        log.info("hello", StructuredArguments.keyValue("myKey", "actualValue"));

        AssertionError assertionError = assertThrows(AssertionError.class, () ->
                logCapture.assertLogged(info("hello", keyValue("myKey", "expectedValue"))));

        assertThat(assertionError).hasMessageFindingMatch(
                "Expected log message has occurred, but never with the expected key-value content: Level: INFO, Regex: \"hello\"" + ".*" +
                        " expected key-value content: key: \"myKey\", value: \"expectedValue\"" + ".*" +
                        "  actual key-value content:" + ".*" +
                        "    key: \"myKey\", value: \"actualValue\"");
    }

    @Test
    void worksWithInteger() {
        log.info("hello", StructuredArguments.keyValue("myKey", 100000));

        logCapture.assertLogged(info("hello", keyValue("myKey", 100000)));
    }

    @Test
    void failsWithInteger() {
        log.info("hello", StructuredArguments.keyValue("myKey", 100001));

        AssertionError assertionError = assertThrows(AssertionError.class, () ->
                logCapture.assertLogged(info("hello", keyValue("myKey", 100000))));

        assertThat(assertionError).hasMessageFindingMatch(
                "Expected log message has occurred, but never with the expected key-value content: Level: INFO, Regex: \"hello\"" + ".*" +
                        " expected key-value content: key: \"myKey\", value: \"100000\"" + ".*" +
                        "  actual key-value content:" + ".*" +
                        "    key: \"myKey\", value: \"100001\"");
    }

    @Test
    void failsWithIntegerThatIsNoInteger() {
        log.info("hello", StructuredArguments.keyValue("myKey", "not an int"));

        AssertionError assertionError = assertThrows(AssertionError.class, () ->
                logCapture.assertLogged(info("hello", keyValue("myKey", 100000))));

        assertThat(assertionError).hasMessageFindingMatch(
                "Expected log message has occurred, but never with the expected key-value content: Level: INFO, Regex: \"hello\"" + ".*" +
                        " expected key-value content: key: \"myKey\", value: \"100000\"" + ".*" +
                        "  actual key-value content:" + ".*" +
                        "    key: \"myKey\", value: \"not an int\"");
    }


    @Test
    void worksWithLong() {
        log.info("hello", StructuredArguments.keyValue("myKey", 1000000000000L));

        logCapture.assertLogged(info("hello", keyValue("myKey", 1000000000000L)));
    }

    @Test
    void failsWithLong() {
        log.info("hello", StructuredArguments.keyValue("myKey", 1000000000001L));

        AssertionError assertionError = assertThrows(AssertionError.class, () ->
                logCapture.assertLogged(info("hello", keyValue("myKey", 1000000000000L))));

        assertThat(assertionError).hasMessageFindingMatch(
                "Expected log message has occurred, but never with the expected key-value content: Level: INFO, Regex: \"hello\"" + ".*" +
                        " expected key-value content: key: \"myKey\", value: \"1000000000000\"" + ".*" +
                        "  actual key-value content:" + ".*" +
                        "    key: \"myKey\", value: \"1000000000001\"");
    }

    @Test
    void failsWithLongThatIsNoLong() {
        log.info("hello", StructuredArguments.keyValue("myKey", "not a long"));

        AssertionError assertionError = assertThrows(AssertionError.class, () ->
                logCapture.assertLogged(info("hello", keyValue("myKey", 1000000000000L))));

        assertThat(assertionError).hasMessageFindingMatch(
                "Expected log message has occurred, but never with the expected key-value content: Level: INFO, Regex: \"hello\"" + ".*" +
                        " expected key-value content: key: \"myKey\", value: \"1000000000000\"" + ".*" +
                        "  actual key-value content:" + ".*" +
                        "    key: \"myKey\", value: \"not a long\"");
    }

    @Test
    void assertNotLoggedSucceeds() {
        log.info("info", StructuredArguments.keyValue("key", "actualValue"));

        logCapture.assertNotLogged(info("info", keyValue("key", "forbiddenValue")));
    }

    @Test
    void assertNotLoggedFailsWithProperMessage() {
        log.info("info", StructuredArguments.keyValue("key", "forbiddenValue"));

        AssertionError actual = assertThrows(AssertionError.class, () ->
                logCapture.assertNotLogged(info("info", keyValue("key", "forbiddenValue"))));

        assertThat(actual).hasMessage("Found a log message that should not be logged: Level: INFO, Regex: \"info\", with matchers:" +
                lineSeparator() + "  keyValue content with key: \"key\" and value: \"forbiddenValue\"");
    }
}
