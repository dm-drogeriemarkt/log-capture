package de.dm.infrastructure.logcapture;

import org.junit.jupiter.api.Test;

import static ch.qos.logback.classic.Level.INFO;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LogCaptureWrongUsageTest {
    LogCapture logCapture = LogCapture.forCurrentPackage();

    @Test
    void doubleInitializationFails() {
        logCapture.addAppenderAndSetLogLevelToDebug();

        assertThrows(IllegalStateException.class, () -> logCapture.addAppenderAndSetLogLevelToDebug());
    }

    @Test
    void resetWithoutInitializationFails() {
        assertThrows(IllegalStateException.class, () -> logCapture.removeAppenderAndResetLogLevel());
    }

    @Test
    void assertionWithoutInitializationFails() {
        assertThrows(IllegalStateException.class, () -> logCapture.assertLogged(INFO, "something"));
    }
}
