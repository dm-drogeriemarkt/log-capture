package com.example.app;

import de.dm.infrastructure.logcapture.LogCaptureExtension;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static de.dm.infrastructure.logcapture.LogCapture.logCapture;
import static de.dm.infrastructure.logcapture.LogExpectation.info;
import static de.dm.infrastructure.logcapture.LogExpectation.warn;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Slf4j
@ExtendWith(LogCaptureExtension.class)
class LogCaptureExtensionTest {

    @Test
    void capturesCurrentPackageByDefault() {
        log.info("hello from extension");
        logCapture().assertLogged(info("hello from extension"));
    }

    @Test
    void assertionFailsWhenMessageNotLogged() {
        AssertionError assertionError = assertThrows(AssertionError.class, () ->
                logCapture().assertLogged(info("never logged")));

        assertThat(assertionError).hasMessageContaining("never logged");
    }

    @Test
    void eachTestGetsIndependentLogCapture() {
        log.info("independent message");
        logCapture().assertLogged(info("independent message"));
        logCapture().assertLogged(info("independent message")).assertNothingElseLogged();
    }

    @Test
    void inOrderAssertionWorksWithExtension() {
        log.info("first");
        log.warn("second");

        logCapture().assertLoggedInOrder(
                info("first"),
                warn("second"));
    }

    @Test
    void doesNotCaptureOtherPackages() {
        org.slf4j.LoggerFactory.getLogger("some.other.package.Logger")
                .info("foreign message");

        logCapture().assertNotLogged(info("foreign message"));
    }

}
