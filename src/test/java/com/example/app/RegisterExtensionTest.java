package com.example.app;

import de.dm.infrastructure.logcapture.LogCapture;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static de.dm.infrastructure.logcapture.LogExpectation.info;
import static de.dm.infrastructure.logcapture.LogExpectation.warn;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Slf4j
class RegisterExtensionTest {
    @RegisterExtension
    LogCapture logCapture = LogCapture.forCurrentPackage();

    @Test
    void capturesCurrentPackage() {
        log.info("hello via register extension");
        logCapture.assertLogged(info("hello via register extension"));
    }

    @Test
    void assertionFailsWhenMessageNotLogged() {
        AssertionError assertionError = assertThrows(AssertionError.class, () ->
                logCapture.assertLogged(info("never logged")));

        assertThat(assertionError).hasMessageContaining("never logged");
    }

    @Test
    void inOrderAssertionWorks() {
        log.info("first");
        log.warn("second");

        logCapture.assertLoggedInOrder(
                info("first"),
                warn("second"));
    }

    @Test
    void doesNotCaptureOtherPackages() {
        org.slf4j.LoggerFactory.getLogger("some.other.package.Logger")
                .info("foreign message");

        logCapture.assertNotLogged(info("foreign message"));
    }

    @Test
    void nothingElseLoggedWorks() {
        log.info("only message");
        logCapture.assertLogged(info("only message")).assertNothingElseLogged();
    }
}
