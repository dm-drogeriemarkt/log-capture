package de.dm.infrastructure.logcapture;

import org.junit.jupiter.api.Test;

import static de.dm.infrastructure.logcapture.LogExpectation.info;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LogCaptureWrongUsageTest {
    LogCapture logCapture = LogCapture.forCurrentPackage();

    @Test
    void doubleInitializationFails() {
        logCapture.addAppenderAndSetLogLevelToTrace();

        assertThrows(IllegalStateException.class, () -> logCapture.addAppenderAndSetLogLevelToTrace());
    }

    @Test
    void resetWithoutInitializationFails() {
        assertThrows(IllegalStateException.class, () -> logCapture.removeAppenderAndResetLogLevel());
    }

    @SuppressWarnings("java:S5778") //this rule does not increase the clarity of this test
    @Test
    void assertionWithoutInitializationFails() {
        assertThrows(IllegalStateException.class, () -> logCapture.assertLogged(info("something")));
    }

}
