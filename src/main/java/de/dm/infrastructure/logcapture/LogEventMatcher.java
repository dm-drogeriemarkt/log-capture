package de.dm.infrastructure.logcapture;

/**
 * interface for matching captured log events
 */
public interface LogEventMatcher {
    /**
     * should return true if the logged event is matched
     *
     * @param loggedEvent logged event to check for match
     *
     * @return true if the event is matched
     */
    boolean matches(LoggedEvent loggedEvent);

    /**
     * returns an error message describing why a logged event does not match
     *
     * @param loggedEvent logged event that does not match
     *
     * @return description why the event does not match
     */
    String getNonMatchingErrorMessage(LoggedEvent loggedEvent);

    /**
     * short description of what aspect of the log event matcher matches, for example "marker name" or "Exception"
     *
     * @return matched aspect
     */
    String getMatcherTypeDescription();

    /**
     * returns an error message describing the concrete matcher, including as much of its expectations as possible
     *
     * @return matched aspect
     */
    String getMatcherDetailDescription();


}
