package com.example.app;

import de.dm.infrastructure.logcapture.LogCapture;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

import static de.dm.infrastructure.logcapture.ExpectedMarker.marker;
import static de.dm.infrastructure.logcapture.LogExpectation.info;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@Slf4j
@SuppressWarnings({
        "java:S5778", //this rule does not increase the clarity of these tests
        "LoggingSimilarMessage" // not a sensible rule for a logging test
})
class ExpectedMarkerTest {
    @RegisterExtension
    LogCapture logCapture = LogCapture.forCurrentPackage();

    @Test
    void markerFailsBecauseOtherMarkerIsPresent() {
        log.info(MarkerFactory.getMarker("unexpected"), "hello with marker");

        AssertionError assertionError = assertThrows(AssertionError.class,
                () -> logCapture.assertLogged(
                        info("hello with marker",
                                marker("expected"))
                ));

        assertThat(assertionError).hasMessage("""
                Expected log message has occurred, but never with the expected marker name:
                message: INFO "hello with marker" (regex)
                  expected marker name: "expected"
                  actual marker names: "[unexpected]"
                """);
    }

    @Test
    void markerFailsBecauseNoMarkerIsPresent() {
        log.info("hello without marker");

        AssertionError assertionError = assertThrows(AssertionError.class,
                () -> logCapture.assertLogged(
                        info("hello without marker",
                                marker("expected"))
                ));

        assertThat(assertionError).hasMessage("""
                Expected log message has occurred, but never with the expected marker name:
                message: INFO "hello without marker" (regex)
                  expected marker name: "expected"
                  but no marker was found
                """);
    }

    @Test
    void markerFailsWhileNestedMarkersArePresent() {
        Marker marker = MarkerFactory.getMarker("unexpected_top");
        marker.add(MarkerFactory.getMarker("unexpected_nested"));
        log.info(marker, "hello with marker");

        AssertionError assertionError = assertThrows(AssertionError.class,
                () -> logCapture.assertLogged(
                        info("hello with marker",
                                marker("expected"))
                ));

        assertThat(assertionError).hasMessage("""
                Expected log message has occurred, but never with the expected marker name:
                message: INFO "hello with marker" (regex)
                  expected marker name: "expected"
                  actual marker names: "[unexpected_top [ unexpected_nested ]]"
                """);
    }

    @Test
    void markerFailsWhileMultipleMarkersArePresent() {
        log.atInfo()
                .setMessage("hello with markers")
                .addMarker(MarkerFactory.getMarker("unexpected1"))
                .addMarker(MarkerFactory.getMarker("unexpected2"))
                .log();

        AssertionError assertionError = assertThrows(AssertionError.class,
                () -> logCapture.assertLogged(
                        info("hello with markers",
                                marker("expected"))
                ));

        assertThat(assertionError).hasMessage("""
                Expected log message has occurred, but never with the expected marker name:
                message: INFO "hello with markers" (regex)
                  expected marker name: "expected"
                  actual marker names: "[unexpected1, unexpected2]"
                """);
    }

    @Test
    void markerSucceedsForNestedMarker() {
        Marker marker = MarkerFactory.getMarker("expected_top");
        marker.add(MarkerFactory.getMarker("expected_nested"));
        log.info(marker, "hello with marker");

        logCapture.assertLogged(
                info("hello with marker",
                        marker("expected_nested")));
    }

    @Test
    void markerSucceeds() {
        log.info(MarkerFactory.getMarker("expected"), "hello with marker");

        logCapture.assertLogged(
                info("hello with marker",
                        marker("expected")));
    }

    @Test
    void markerWithAssertNotLogged() {
        log.info(MarkerFactory.getMarker("expected"), "hello with marker");

        logCapture.assertNotLogged(
                info("hello with marker",
                        marker("not expected")));

        final AssertionError assertionError = assertThrows(AssertionError.class, () -> logCapture.assertNotLogged(
                info("hello with marker",
                        marker("expected"))));
        assertThat(assertionError).hasMessage("""
                Found a log message that should not be logged.
                message: INFO "hello with marker" (regex)
                  with additional matchers:
                  - marker name: "expected"
                """);
    }

}
