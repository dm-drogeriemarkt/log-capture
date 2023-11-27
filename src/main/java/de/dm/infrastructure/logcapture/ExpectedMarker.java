package de.dm.infrastructure.logcapture;

import static java.lang.String.format;
import static java.lang.System.lineSeparator;

/**
 * define expected markers on log messages with this
 */
public final class ExpectedMarker implements LogEventMatcher {
    private final String expectedName;

    private ExpectedMarker(String expectedName) {
        this.expectedName = expectedName;
    }

    @Override
    public boolean matches(LoggedEvent loggedEvent) {
        return loggedEvent.getMarkers() != null && loggedEvent.getMarkers().stream().anyMatch(marker -> marker.contains(expectedName));
    }

    @Override
    public String getNonMatchingErrorMessage(LoggedEvent loggedEvent) {
        String expected = format("  expected marker name: \"%s\"", expectedName) + lineSeparator();
        if (loggedEvent.getMarkers() == null) {
            return expected + "  but no marker was found";
        }
        return expected + format("  actual marker names: \"%s\"", loggedEvent.getMarkers());
    }

    @Override
    public String getMatcherTypeDescription() {
        return "marker name";
    }

    @Override
    public String getMatcherDetailDescription() {
        return format("marker name: \"%s\"", expectedName);
    }

    /**
     * use this in a log expectation to verify that something has been logged with a certain marker
     *
     * @param expectedName expected marker name
     *
     * @return expected marker to use in log expectation
     */
    public static ExpectedMarker marker(String expectedName) {
        return new ExpectedMarker(expectedName);
    }
}
