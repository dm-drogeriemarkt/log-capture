package com.acme;

import de.dm.infrastructure.logcapture.LogCapture;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static de.dm.infrastructure.logcapture.ExpectedKeyValue.keyValue;
import static de.dm.infrastructure.logcapture.LogExpectation.info;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Slf4j
public class LogCaptureTest {
    @RegisterExtension
    LogCapture logCapture = LogCapture.forCurrentPackage();

    @Test
    void assertionWorksDespiteLogstashNotBeingInTheClasspath() {
        log.info("hello");
        logCapture.assertLogged(info("hello"));
    }

    @Test
    void errorMessageForKeyValueAssertion() {
        log.info("hello");

        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, () ->
                logCapture.assertLogged(info("hello", keyValue("key", "value"))));

        assertThat(thrown).hasMessage("keyValue cannot be used for log assertions if logstash-logback-encoder " +
                "that provides StructuredArguments.keyValue(...) is not in the classpath.");
    }
}
