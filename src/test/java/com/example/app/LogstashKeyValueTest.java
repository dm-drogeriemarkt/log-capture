package com.example.app;

import de.dm.infrastructure.logcapture.LogCapture;
import lombok.extern.slf4j.Slf4j;
import net.logstash.logback.argument.StructuredArguments;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static de.dm.infrastructure.logcapture.ExpectedKeyValue.keyValue;
import static de.dm.infrastructure.logcapture.LogExpectation.info;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@SuppressWarnings("java:S5778") //this rule does not increase the clarity of these tests
@Slf4j
class LogstashKeyValueTest {
    @RegisterExtension
    LogCapture logCapture = LogCapture.forCurrentPackage();

    @Test
    void worksWithObjectEquality() {
        log.info("hello", StructuredArguments.keyValue("myKey", "myValue"));

        logCapture.assertLogged(info("hello", keyValue("myKey", "myValue")));
    }

    @Test
    void failsWithObjectEquality() {
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
    void worksWithStringMatcher() {
        //TODO
    }

    @Test
    void failsWithStringMatcher() {
        //TODO
    }

    @Test
    void worksWithObjectMatcher() {
        //TODO
    }

    @Test
    void failsWithObjectMatcher() {
        //TODO
    }

    @Test
    void objectEqualityFailsWithAssertNotLogged() {
        //TODO
    }

    @Test
    void stringMatchFailsWithAssertNotLogged() {
        //TODO
    }

    @Test
    void objectMatchFailsWithAssertNotLogged() {
        //TODO
    }
}
