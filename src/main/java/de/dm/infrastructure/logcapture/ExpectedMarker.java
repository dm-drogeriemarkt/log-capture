package de.dm.infrastructure.logcapture;

import static java.lang.String.format;
import static java.lang.System.lineSeparator;

public class ExpectedMarker implements LogEventMatcher {
    private final String expectedName;

    ExpectedMarker(String expectedName) {
        this.expectedName = expectedName;
    }

    @Override
    public boolean matches(LoggedEvent loggedEvent) {
        return loggedEvent.getMarker() != null &&
                (loggedEvent.getMarker().getName().equals(expectedName) || loggedEvent.getMarker().contains(expectedName));
    }

    @Override
    public String getNonMatchingErrorMessage(LoggedEvent loggedEvent) {
        String expected = format("  expected marker name: \"%s\"", expectedName) + lineSeparator();
        if (loggedEvent.getMarker() == null) {
            return expected + "  but no marker was found";
        }
        return expected + format("  actual marker names: \"%s\"", loggedEvent.getMarker());
    }

    @Override
    public String getMatcherDescription() {
        return "marker name";
    }

    public static ExpectedMarker marker(String expectedName) {
        return new ExpectedMarker(expectedName);
    }
}
