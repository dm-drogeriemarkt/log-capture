package de.dm.infrastructure.logcapture;

import ch.qos.logback.classic.Level;

import java.util.Arrays;
import java.util.List;


public final class LogAssertion {
    final Level level;
    final String regex;
    final List<LogEventMatcher> logEventMatchers;

    private LogAssertion(Level level, String regex, LogEventMatcher... logEventMatchersForThisMessage) {
        this.level = level;
        this.regex = regex;
        logEventMatchers = Arrays.asList(logEventMatchersForThisMessage);
    }

    public static LogAssertion trace(String regex, LogEventMatcher... logEventMatchersForThisMessage) {
        return new LogAssertion(Level.TRACE, regex, logEventMatchersForThisMessage);
    }

    public static LogAssertion debug(String regex, LogEventMatcher... logEventMatchersForThisMessage) {
        return new LogAssertion(Level.DEBUG, regex, logEventMatchersForThisMessage);
    }

    public static LogAssertion info(String regex, LogEventMatcher... logEventMatchersForThisMessage) {
        return new LogAssertion(Level.INFO, regex, logEventMatchersForThisMessage);
    }

    public static LogAssertion warn(String regex, LogEventMatcher... logEventMatchersForThisMessage) {
        return new LogAssertion(Level.WARN, regex, logEventMatchersForThisMessage);
    }

    public static LogAssertion error(String regex, LogEventMatcher... logEventMatchersForThisMessage) {
        return new LogAssertion(Level.ERROR, regex, logEventMatchersForThisMessage);
    }

}
