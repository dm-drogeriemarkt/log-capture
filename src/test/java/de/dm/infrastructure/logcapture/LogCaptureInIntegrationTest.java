package de.dm.infrastructure.logcapture;

import ch.qos.logback.classic.Level;
import lombok.extern.slf4j.Slf4j;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Slf4j
public class LogCaptureInIntegrationTest {

    @Rule
    public LogCapture logCapture = LogCapture.forIntegrationTest("de.dm");

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
        exception.expect(AssertionError.class);
        logCapture.assertLogged(Level.INFO, "something that has not been logged");
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
}
