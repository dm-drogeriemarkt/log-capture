package de.dm.infrastructure.logcapture;

import ch.qos.logback.classic.Level;

import java.util.Arrays;
import java.util.List;

/**
 * an assertion
 */
public final class LogExpectation {
    final Level level;
    final String regex;
    final List<LogEventMatcher> logEventMatchers;

    private LogExpectation(Level level, String regex, LogEventMatcher... logEventMatchersForThisMessage) {
        this.level = level;
        this.regex = regex;
        logEventMatchers = Arrays.asList(logEventMatchersForThisMessage);
    }

    public static LogExpectation trace(String regex, LogEventMatcher... logEventMatchersForThisMessage) {
        return new LogExpectation(Level.TRACE, regex, logEventMatchersForThisMessage);
    }

    public static LogExpectation debug(String regex, LogEventMatcher... logEventMatchersForThisMessage) {
        return new LogExpectation(Level.DEBUG, regex, logEventMatchersForThisMessage);
    }

    public static LogExpectation info(String regex, LogEventMatcher... logEventMatchersForThisMessage) {
        return new LogExpectation(Level.INFO, regex, logEventMatchersForThisMessage);
    }

    public static LogExpectation warn(String regex, LogEventMatcher... logEventMatchersForThisMessage) {
        return new LogExpectation(Level.WARN, regex, logEventMatchersForThisMessage);
    }

    public static LogExpectation error(String regex, LogEventMatcher... logEventMatchersForThisMessage) {
        return new LogExpectation(Level.ERROR, regex, logEventMatchersForThisMessage);
    }

}
