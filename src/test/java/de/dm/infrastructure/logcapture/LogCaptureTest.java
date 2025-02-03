package de.dm.infrastructure.logcapture;

import ch.qos.logback.classic.Level;
import com.example.app.LogCaptureCreatorInOtherPackage;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.LoggerFactory;

import static ch.qos.logback.classic.Level.INFO;
import static ch.qos.logback.classic.Level.TRACE;
import static de.dm.infrastructure.logcapture.LogExpectation.info;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Slf4j
// tests that test correct behaviour of logCapture in test context go here.
// tests of actual assertions should go to com.example.app.ReadableApiTest instead.
class LogCaptureTest {

    @RegisterExtension
    public LogCapture logCapture = LogCapture.forPackages("de.dm", "com.capture");

    @RegisterExtension
    public LogCapture logCaptureForCurrentPackage = LogCapture.forCurrentPackage();

    @SuppressWarnings("java:S5778") //this rule does not increase the clarity of this test
    @Test
    void fromCurrentPackageWorks() {
        LogCapture logCaptureFromOtherPackage = LogCaptureCreatorInOtherPackage.getLogCaptureFromCurrentPackage();
        logCaptureFromOtherPackage.addAppenderAndSetLogLevelToTrace();
        log.info("hello from de.dm.infrastructure.logcapture");
        logCaptureFromOtherPackage.removeAppenderAndResetLogLevel();

        assertThat(logCaptureFromOtherPackage.capturedPackages).containsExactly(LogCaptureCreatorInOtherPackage.class.getPackage().getName());
        var thrown = assertThrows(AssertionError.class, () -> logCaptureFromOtherPackage.assertLogged(info("hello from")));
        assertThat(thrown).hasMessage("""
                Expected log message has not occurred.
                message: INFO "hello from" (regex)
                """);
    }

    @Test
    void logLevelIsResetToInfo() {
        logLevelIsResetTo(INFO);
    }

    @Test
    void logLevelIsResetToNull() {
        logLevelIsResetTo(null);
    }

    private void logLevelIsResetTo(Level originalLevel) {
        final ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        final ch.qos.logback.classic.Logger comExampleLogger = rootLogger.getLoggerContext().getLogger("com.example");

        comExampleLogger.setLevel(originalLevel);

        LogCapture logCaptureComExample = LogCapture.forPackages("com.example");
        logCaptureComExample.addAppenderAndSetLogLevelToTrace();

        assertThat(comExampleLogger.getLevel()).isEqualTo(TRACE);

        logCaptureComExample.removeAppenderAndResetLogLevel();

        assertThat(comExampleLogger.getLevel()).isEqualTo(originalLevel);
    }

}
