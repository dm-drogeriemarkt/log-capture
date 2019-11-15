package de.dm.infrastructure.logcapture;

import ch.qos.logback.classic.Level;
import com.example.app.LogCaptureCreatorInOtherPackage;
import lombok.extern.slf4j.Slf4j;
import org.assertj.core.api.Assertions;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import static de.dm.infrastructure.logcapture.ExpectedMdcEntry.withMdc;

@Slf4j
public class LogCaptureInIntegrationTest {

    @Rule
    public LogCapture logCapture = LogCapture.forPackages("de.dm");

    @Rule //to be replaced with Assertions.assertThrows() in JUnit 5
    public final ExpectedException exception = ExpectedException.none();

    @Test
    public void twoLogMessagesInOrder() {
        log.info("something interesting");
        log.error("something terrible");

        logCapture
                .assertLogged(Level.INFO, "^something interesting")
                .thenLogged(Level.ERROR, "terrible");
    }

    @Test
    public void twoLogMessagesOutOfOrder() {
        log.error("something terrible");
        log.info("something interesting");

        exception.expect(AssertionError.class);

        logCapture
                .assertLogged(Level.INFO, "^something interesting")
                .thenLogged(Level.ERROR, "terrible");
    }

    @Test
    public void catchMissingLogMessage() {
        boolean assertionErrorThrown = false;
        try {
            logCapture.assertLogged(Level.INFO, "something that has not been logged");
        } catch (AssertionError e) {
            String expectedMessage = "Expected log message has not occurred: Level: INFO, Regex: \"something that has not been logged\"";
            Assert.assertEquals(expectedMessage, e.getMessage());
            assertionErrorThrown = true;
        }
        if (!assertionErrorThrown) {
            throw new AssertionError("Assertion Error has not been thrown for missing log message.");
        }
    }

    @Test
    public void filterOutIrrelevantLogMessagesInIntegrationTest() {
        ch.qos.logback.classic.Logger rootLogger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(ch.qos.logback.classic.Logger.ROOT_LOGGER_NAME);
        rootLogger.getLoggerContext().getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME).setLevel(Level.DEBUG);
        Logger logger = LoggerFactory.getLogger("com.acme.whatever");
        logger.info("something from another package");
        exception.expect(AssertionError.class);
        logCapture.assertLogged(Level.INFO, "something from another package");
    }

    @Test
    public void logMessagesWithMdc() {
        final String MDC_KEY = "mdc_key";

        MDC.put(MDC_KEY, "an mdc value here");
        log.info("this should have an mdc value");

        logCapture
                .assertLogged(Level.INFO, "mdc value", withMdc(MDC_KEY, "mdc value"));
    }

    @Test
    public void correctLogMessagesWithMissingMdc() {
        final String MDC_KEY = "mdc_key";
        final String actualMdcValue = "a wrong value here";

        MDC.put(MDC_KEY, actualMdcValue);
        log.info("some message");

        boolean assertionErrorThrown = false;
        try {
            logCapture
                    .assertLogged(Level.INFO, "some message", withMdc(MDC_KEY, "mdc value"));
        } catch (AssertionError e) {
            String expectedMessage = "Expected log message has occurred, but never with the expected MDC value: Level: INFO, Regex: \"some message\""
                    + System.lineSeparator() + "  Captured message: \"some message\""
                    + System.lineSeparator() + "  Captured MDC values:"
                    + System.lineSeparator() + "    " + MDC_KEY + ": \"" + actualMdcValue + "\"";

            Assert.assertEquals(expectedMessage, e.getMessage());
            assertionErrorThrown = true;
        }
        if (!assertionErrorThrown) {
            throw new AssertionError("Assertion Error has not been thrown for missing log message.");
        }
    }

    @Test
    public void fromCurrentPackageWorks() {
        LogCapture logCapture = LogCaptureCreatorInOtherPackage.getLogCapturaFromCurrentPackage();
        Assertions.assertThat(logCapture.capturedPackages).containsExactly(LogCaptureCreatorInOtherPackage.class.getPackage().getName());
    }
}
