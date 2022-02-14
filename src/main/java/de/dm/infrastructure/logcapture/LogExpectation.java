package de.dm.infrastructure.logcapture;

import ch.qos.logback.classic.Level;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * class for describing an expected log message
 */
public final class LogExpectation {
    final Optional<Level> level;
    final String regex;
    final List<LogEventMatcher> logEventMatchers;

    private LogExpectation(Level level, String regex, LogEventMatcher... logEventMatchersForThisMessage) {
        this.level = Optional.of(level);
        this.regex = regex;
        logEventMatchers = Arrays.asList(logEventMatchersForThisMessage);
    }

    private LogExpectation(String regex, LogEventMatcher... logEventMatchersForThisMessage) {
        level = Optional.empty();
        this.regex = regex;
        logEventMatchers = Arrays.asList(logEventMatchersForThisMessage);
    }

    /**
     * use this to describe an expected TRACE log message
     *
     * <p>
     * there are multiple additional matchers you can use to describe a log message beyond the message itself:
     * <ul>
     *     <li>@{@link ExpectedMdcEntry}</li>
     *     <li>@{@link ExpectedException}</li>
     *     <li>@{@link ExpectedLoggerName}</li>
     *     <li>@{@link ExpectedMarker}</li>
     * </ul>
     *
     * @param regex regular expression describing the expected message. Will be padded with .* - so use ^hello world$ for an exact match.
     * @param logEventMatchersForThisMessage additional matchers for the log event.
     *
     * @return the log expectation
     */
    public static LogExpectation trace(String regex, LogEventMatcher... logEventMatchersForThisMessage) {
        return new LogExpectation(Level.TRACE, regex, logEventMatchersForThisMessage);
    }

    /**
     * use this to describe an expected DEBUG log message
     *
     * <p>
     * there are multiple additional matchers you can use to describe a log message beyond the message itself:
     * <ul>
     *     <li>@{@link ExpectedMdcEntry}</li>
     *     <li>@{@link ExpectedException}</li>
     *     <li>@{@link ExpectedLoggerName}</li>
     *     <li>@{@link ExpectedMarker}</li>
     * </ul>
     *
     * @param regex regular expression describing the expected message. Will be padded with .* - so use ^hello world$ for an exact match.
     * @param logEventMatchersForThisMessage additional matchers for the log event.
     *
     * @return the log expectation
     */
    public static LogExpectation debug(String regex, LogEventMatcher... logEventMatchersForThisMessage) {
        return new LogExpectation(Level.DEBUG, regex, logEventMatchersForThisMessage);
    }

    /**
     * use this to describe an expected INFO log message
     *
     * <p>
     * there are multiple additional matchers you can use to describe a log message beyond the message itself:
     * <ul>
     *     <li>@{@link ExpectedMdcEntry}</li>
     *     <li>@{@link ExpectedException}</li>
     *     <li>@{@link ExpectedLoggerName}</li>
     *     <li>@{@link ExpectedMarker}</li>
     * </ul>
     *
     * @param regex regular expression describing the expected message. Will be padded with .* - so use ^hello world$ for an exact match.
     * @param logEventMatchersForThisMessage additional matchers for the log event.
     *
     * @return the log expectation
     */
    public static LogExpectation info(String regex, LogEventMatcher... logEventMatchersForThisMessage) {
        return new LogExpectation(Level.INFO, regex, logEventMatchersForThisMessage);
    }

    /**
     * use this to describe an expected WARN log message
     *
     * <p>
     * there are multiple additional matchers you can use to describe a log message beyond the message itself:
     * <ul>
     *     <li>@{@link ExpectedMdcEntry}</li>
     *     <li>@{@link ExpectedException}</li>
     *     <li>@{@link ExpectedLoggerName}</li>
     *     <li>@{@link ExpectedMarker}</li>
     * </ul>
     *
     * @param regex regular expression describing the expected message. Will be padded with .* - so use ^hello world$ for an exact match.
     * @param logEventMatchersForThisMessage additional matchers for the log event.
     *
     * @return the log expectation
     */
    public static LogExpectation warn(String regex, LogEventMatcher... logEventMatchersForThisMessage) {
        return new LogExpectation(Level.WARN, regex, logEventMatchersForThisMessage);
    }

    /**
     * use this to describe an expected ERROR log message
     *
     * <p>
     * there are multiple additional matchers you can use to describe a log message beyond the message itself:
     * <ul>
     *     <li>@{@link ExpectedMdcEntry}</li>
     *     <li>@{@link ExpectedException}</li>
     *     <li>@{@link ExpectedLoggerName}</li>
     *     <li>@{@link ExpectedMarker}</li>
     * </ul>
     *
     * @param regex regular expression describing the expected message. Will be padded with .* - so use ^hello world$ for an exact match.
     * @param logEventMatchersForThisMessage additional matchers for the log event.
     *
     * @return the log expectation
     */
    public static LogExpectation error(String regex, LogEventMatcher... logEventMatchersForThisMessage) {
        return new LogExpectation(Level.ERROR, regex, logEventMatchersForThisMessage);
    }

    /**
     * use this to describe an expected log message with any level
     *
     * <p>
     * there are multiple additional matchers you can use to describe a log message beyond the message itself:
     * <ul>
     *     <li>@{@link ExpectedMdcEntry}</li>
     *     <li>@{@link ExpectedException}</li>
     *     <li>@{@link ExpectedLoggerName}</li>
     *     <li>@{@link ExpectedMarker}</li>
     * </ul>
     *
     * @param regex regular expression describing the expected message. Will be padded with .* - so use ^hello world$ for an exact match.
     * @param logEventMatchersForThisMessage additional matchers for the log event.
     *
     * @return the log expectation
     */
    public static LogExpectation any(String regex, LogEventMatcher... logEventMatchersForThisMessage) {
        return new LogExpectation(regex, logEventMatchersForThisMessage);
    }

    /**
     * use this to describe any log message with any level
     *
     * <p>
     * there are multiple additional matchers you can use to describe a log message beyond the message itself:
     * <ul>
     *     <li>@{@link ExpectedMdcEntry}</li>
     *     <li>@{@link ExpectedException}</li>
     *     <li>@{@link ExpectedLoggerName}</li>
     *     <li>@{@link ExpectedMarker}</li>
     * </ul>
     *
     * @param logEventMatchersForThisMessage additional matchers for the log event.
     *
     * @return the log expectation
     */
    public static LogExpectation any(LogEventMatcher... logEventMatchersForThisMessage) {
        return new LogExpectation("", logEventMatchersForThisMessage);
    }


}
