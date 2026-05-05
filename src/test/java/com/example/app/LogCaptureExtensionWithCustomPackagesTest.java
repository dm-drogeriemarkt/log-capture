package com.example.app;

import de.dm.infrastructure.logcapture.LogCaptureExtension;
import de.dm.infrastructure.logcapture.LogCapturePackages;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static de.dm.infrastructure.logcapture.LogCapture.logCapture;
import static de.dm.infrastructure.logcapture.LogExpectation.info;

@Slf4j
@ExtendWith(LogCaptureExtension.class)
@LogCapturePackages({"com.example.app", "com.capture"})
class LogCaptureExtensionWithCustomPackagesTest {

    @Test
    void capturesLogsFromAnnotatedPackages() {
        log.info("message from test class");
        org.slf4j.LoggerFactory.getLogger("com.capture.SomeService")
                .info("message from captured package");

        logCapture().assertLoggedInOrder(
                info("message from test class"),
                info("message from captured package"));
    }

    @Test
    void doesNotCaptureUnannotatedPackages() {
        org.slf4j.LoggerFactory.getLogger("org.other.Service")
                .info("should not be captured");

        logCapture().assertNotLogged(info("should not be captured"));
    }
}
