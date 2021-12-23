package de.dm.infrastructure.logcapture;

import ch.qos.logback.classic.Level;
import com.example.app.LogCaptureCreatorInOtherPackage;
import de.dm.infrastructure.logcapture.LogCapture.LastCapturedLogEvent;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import static ch.qos.logback.classic.Level.DEBUG;
import static ch.qos.logback.classic.Level.INFO;
import static ch.qos.logback.classic.Level.TRACE;
import static de.dm.infrastructure.logcapture.ExpectedMdcEntry.withMdc;
import static java.lang.System.lineSeparator;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Slf4j
class LogCaptureTest {

    @RegisterExtension
    public LogCapture logCapture = LogCapture.forPackages("de.dm", "com.capture");

    @RegisterExtension
    public LogCapture logCaptureForCurrentPackage = LogCapture.forCurrentPackage();

    @Test
    void twoLogMessagesInOrderAndNothingElse() {
        log.info("something interesting");
        log.error("something terrible");

        logCapture
                .assertLogged(Level.INFO, "^something interesting")
                .thenLogged(Level.ERROR, "terrible")
                .assertNothingElseLogged();
    }

    @Test
    void twoLogMessagesInOrderAndSomethingElseFails() {
        log.info("something interesting");
        log.info("something unexpected");
        log.error("something terrible");

        LastCapturedLogEvent lastCapturedLogEvent = logCapture
                .assertLogged(INFO, "^something interesting")
                .thenLogged(Level.ERROR, "terrible");

        AssertionError assertionError = assertThrows(AssertionError.class, () -> {
            lastCapturedLogEvent.assertNothingElseLogged();
        });

        assertThat(assertionError).hasMessage("There have been other log messages than the asserted ones.");
    }

    @Test
    void captureMultilineMessages() {
        log.info("something interesting\nwith something else in other lines, for example exception details");

        logCapture.assertLogged(Level.INFO, "something interesting");
    }

    @Test
    void captureLogsForCurrentPackage() {
        log.info("Hello from logcapture");

        final Logger acmeLogger = LoggerFactory.getLogger("com.acme");
        acmeLogger.info("Hello from com.acme");

        logCaptureForCurrentPackage
                .assertLogged(INFO, "^Hello from logcapture$");

        AssertionError assertionError = assertThrows(AssertionError.class, () -> {
            logCaptureForCurrentPackage.assertLogged(INFO, "Hello from com.acme");
        });

        assertThat(assertionError).hasMessage("Expected log message has not occurred: Level: INFO, Regex: \"Hello from com.acme\"");
    }

    @Test
    void captureLogsForMultiplePackages() {
        log.info("something interesting");
        log.error("something terrible");

        final Logger comCaptureLogger = LoggerFactory.getLogger("com.capture.foo.bar");
        comCaptureLogger.info("some info");

        logCapture
                .assertLogged(Level.INFO, "^something interesting")
                .thenLogged(Level.ERROR, "terrible")
                .thenLogged(Level.INFO, "some info");
    }

    @Test
    void twoLogMessagesOutOfOrder() {
        log.error("something terrible");
        log.info("something interesting");
        LastCapturedLogEvent lastCapturedLogEvent = logCapture
                .assertLogged(INFO, "^something interesting");

        AssertionError assertionError = assertThrows(AssertionError.class, () -> {
            lastCapturedLogEvent.thenLogged(Level.ERROR, "terrible");
        });

        assertThat(assertionError).hasMessage("Expected log message has not occurred: Level: ERROR, Regex: \"terrible\"");
    }

    @Test
    void catchMissingLogMessage() {
        AssertionError assertionError = assertThrows(AssertionError.class, () -> logCapture.assertLogged(INFO, "something that has not been logged"));

        assertThat(assertionError).hasMessage("Expected log message has not occurred: Level: INFO, Regex: \"something that has not been logged\"");
    }

    @Test
    void filterOutIrrelevantLogMessagesInIntegrationTest() {
        ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        rootLogger.getLoggerContext().getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).setLevel(DEBUG);
        Logger logger = LoggerFactory.getLogger("com.acme.whatever");
        logger.info("something from another package");

        AssertionError assertionError = assertThrows(AssertionError.class, () -> {
            logCapture.assertLogged(INFO, "something from another package");
        });

        assertThat(assertionError).hasMessage("Expected log message has not occurred: Level: INFO, Regex: \"something from another package\"");
    }

    @Test
    void logMessagesWithMdc() {
        final String MDC_KEY = "mdc_key";

        MDC.put(MDC_KEY, "an mdc value here");
        log.info("this should have an mdc value");
        MDC.clear();

        logCapture
                .assertLogged(Level.INFO, "mdc value", withMdc(MDC_KEY, "mdc value"));
    }

    @Test
    void correctLogMessagesWithMissingMdc() {
        final String MDC_KEY = "mdc_key";
        final String actualMdcValue = "a wrong value here";

        MDC.put(MDC_KEY, actualMdcValue);
        log.info("some message");
        MDC.clear();

        AssertionError assertionError = assertThrows(AssertionError.class, () ->
                logCapture.assertLogged(INFO, "some message", withMdc(MDC_KEY, "mdc value")));

        assertThat(assertionError).hasMessage("Expected log message has occurred, but never with the expected MDC value: Level: INFO, Regex: \"some message\"" +
                lineSeparator() + "  captured message: \"some message\"" +
                lineSeparator() + "  expected MDC key: mdc_key" +
                lineSeparator() + "  expected MDC value: \".*mdc value.*\"" +
                lineSeparator() + "  captured MDC values:" +
                lineSeparator() + "    " + MDC_KEY + ": \"" + actualMdcValue + "\"" +
                lineSeparator());
    }

    @Test
    void fromCurrentPackageWorks() {
        LogCapture logCapture = LogCaptureCreatorInOtherPackage.getLogCaptureFromCurrentPackage();
        assertThat(logCapture.capturedPackages).containsExactly(LogCaptureCreatorInOtherPackage.class.getPackage().getName());
    }

    @Test
    void logLevelIsResetToInfo() {
        logLevelIsResetTo(INFO);
    }

    @Test
    void logLevelIsResetToNull() {
        logLevelIsResetTo(null);
    }

    @Test
    void doesNotFailForNullArrayInMdcEntried() {
        log.info("something interesting");

        logCapture.assertLogged(Level.INFO, "^something interesting", null);
    }

    private void logLevelIsResetTo(Level originalLevel) {
        final ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        final ch.qos.logback.classic.Logger comExampleLogger = rootLogger.getLoggerContext().getLogger("com.example");

        comExampleLogger.setLevel(originalLevel);

        LogCapture logCapture = LogCapture.forPackages("com.example");
        logCapture.addAppenderAndSetLogLevelToTrace();

        assertThat(comExampleLogger.getLevel()).isEqualTo(TRACE);

        logCapture.removeAppenderAndResetLogLevel();

        assertThat(comExampleLogger.getLevel()).isEqualTo(originalLevel);
    }

    private void logLevelIsResetWithDeprecatedMedhod() {
        final ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        final ch.qos.logback.classic.Logger comExampleLogger = rootLogger.getLoggerContext().getLogger("com.example");

        comExampleLogger.setLevel(INFO);

        LogCapture logCapture = LogCapture.forPackages("com.example");
        logCapture.addAppenderAndSetLogLevelToDebug();

        assertThat(comExampleLogger.getLevel()).isEqualTo(TRACE);

        logCapture.removeAppenderAndResetLogLevel();

        assertThat(comExampleLogger.getLevel()).isEqualTo(INFO);
    }
}
