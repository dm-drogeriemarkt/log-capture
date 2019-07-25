package de.dm.infrastructure.logcapture;

import ch.qos.logback.classic.Level;
import lombok.extern.slf4j.Slf4j;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import static de.dm.infrastructure.logcapture.ExpectedMdcEntry.withMdc;

@Slf4j
public class LogCaptureInUnitTest {

    private static final String MDC_KEY = "mdc_key";
    @Rule
    public LogCapture logCapture = LogCapture.forUnitTest();

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
    public void dontFilterOutMessagesFromOtherPackagesInUnitTests() {
        Logger logger = LoggerFactory.getLogger("com.acme.whatever");
        logger.info("something from another package");
        logCapture.assertLogged(Level.INFO, "something from another package");
    }

    @Test
    public void logMessagesWithMdc() {
        MDC.put(MDC_KEY, "an mdc value here");
        log.info("this should have an mdc value");

        logCapture
                .assertLogged(Level.INFO, "mdc value", withMdc(MDC_KEY, "mdc value"));
    }

    @Test
    public void correctLogMessagesWithMissingMdc() {
        String actualMdcValue = "a wrong value here";
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
}
