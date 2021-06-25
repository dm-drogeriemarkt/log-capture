package de.dm.infrastructure.logcapture;

public interface LogEventMatcher {
    boolean matches(LoggedEvent loggedEvent);

    String getNonMatchingErrorMessage(LoggedEvent loggedEvent);

    String getMatcherDescription();
}
