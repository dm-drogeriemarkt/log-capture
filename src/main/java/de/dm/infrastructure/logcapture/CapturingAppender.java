package de.dm.infrastructure.logcapture;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.ContextAwareBase;
import ch.qos.logback.core.spi.FilterReply;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;

import static java.lang.String.format;
import static java.lang.System.lineSeparator;

@Getter
@Setter
class CapturingAppender extends ContextAwareBase implements Appender<ILoggingEvent> {

    private List<LoggedEvent> loggedEvents = new ArrayList<>();
    private final Set<String> capturedPackages;

    private String name;
    private boolean started;

    CapturingAppender(LoggerContext loggerContext, Set<String> capturedPackages) {
        this.capturedPackages = capturedPackages;
        setName("CAPTURE-" + Thread.currentThread().getId());
        setContext(loggerContext);
    }

    @Override
    public synchronized void doAppend(ILoggingEvent loggingEvent) {
        if (eventIsRelevant(loggingEvent)) {
            loggedEvents.add(
                    LoggedEvent.builder()
                            .loggerName(loggingEvent.getLoggerName())
                            .level(loggingEvent.getLevel())
                            .formattedMessage(loggingEvent.getFormattedMessage())
                            .mdcData(loggingEvent.getMDCPropertyMap())
                            .loggedException(getLoggedException(loggingEvent.getThrowableProxy()))
                            .build());
        }
    }

    private Optional<LoggedEvent.LoggedException> getLoggedException(IThrowableProxy throwableProxy) {
        if (throwableProxy == null) {
            return Optional.empty();
        }

        return Optional.of(LoggedEvent.LoggedException.builder()
                .type(throwableProxy.getClassName())
                .message(throwableProxy.getMessage())
                .cause(getLoggedException(throwableProxy.getCause()))
                .build());
    }

    private boolean eventIsRelevant(ILoggingEvent loggingEvent) {
        for (String packageName : capturedPackages) {
            if (loggingEvent.getLoggerName().startsWith(packageName)) {
                return true;
            }
        }
        return false;
    }

    Integer assertCapturedNext(Level level, String regex, int startIndex, List<LogEventMatcher> logEventMatchers) {
        Pattern pattern = Pattern.compile(".*" + regex + ".*", Pattern.DOTALL + Pattern.MULTILINE);
        LoggedEvent eventMatchingWithoutAdditionalMatchers = null;
        for (int i = startIndex; i < loggedEvents.size(); i++) {
            LoggedEvent event = loggedEvents.get(i);
            if (eventMatchesWithoutAdditionalMatchers(event, level, pattern)) {
                if (isMatchedByAll(event, logEventMatchers)) {
                    return i;
                }
                eventMatchingWithoutAdditionalMatchers = event;
            }
        }
        if (eventMatchingWithoutAdditionalMatchers != null) {
            throwAssertionForPartiallyMatchingLoggedEvent(level, regex, eventMatchingWithoutAdditionalMatchers, logEventMatchers);
        }
        throw new AssertionError(format("Expected log message has not occurred: Level: %s, Regex: \"%s\"", level, regex));
    }

    int getNumberOfLoggedMessages() {
        return loggedEvents.size();
    }

    private boolean eventMatchesWithoutAdditionalMatchers(LoggedEvent event, Level level, Pattern pattern) {
        return eventHasLevel(event, level) && eventMatchesPattern(event, pattern);
    }

    //TODO: cleanup (potentially) static methods like this - CapturingAppender is doing too much here
    private static void throwAssertionForPartiallyMatchingLoggedEvent(Level level, String regex, LoggedEvent partiallyMatchingLoggedEvent, List<LogEventMatcher> logEventMatchers) {
        StringBuilder assertionMessage = new StringBuilder();

        for (LogEventMatcher logEventMatcher : logEventMatchers) {
            assertionMessage.append(format("Expected log message has occurred, but never with the expected %s: Level: %s, Regex: \"%s\"",
                    logEventMatcher.getMatcherDescription(), level, regex));
            assertionMessage.append(lineSeparator());
            assertionMessage.append(logEventMatcher.getNonMatchingErrorMessage(partiallyMatchingLoggedEvent));
            assertionMessage.append(lineSeparator());
        }
        throw new AssertionError(assertionMessage.toString());
    }

    private boolean eventMatchesPattern(LoggedEvent event, Pattern pattern) {
        return pattern.matcher(event.getFormattedMessage()).matches();
    }

    private boolean eventHasLevel(LoggedEvent event, Level level) {
        return event.getLevel().equals(level);
    }

    static boolean isMatchedByAll(LoggedEvent loggedEvent, List<? extends LogEventMatcher> logEventMatchers) {
        if (logEventMatchers == null) {
            return true;
        }
        return logEventMatchers.stream().allMatch(matcher -> matcher.matches(loggedEvent));
    }

    @Override
    public void addFilter(Filter<ILoggingEvent> newFilter) {
        //do nothing
    }

    @Override
    public void clearAllFilters() {
        // do nothing
    }

    @Override
    public List<Filter<ILoggingEvent>> getCopyOfAttachedFiltersList() {
        return new LinkedList<>();
    }

    @Override
    public FilterReply getFilterChainDecision(ILoggingEvent event) {
        return FilterReply.ACCEPT; //never filter
    }

    @Override
    public void start() {
        started = true;
    }

    @Override
    public void stop() {
        started = false;
    }
}
