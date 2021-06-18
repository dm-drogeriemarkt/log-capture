package de.dm.infrastructure.logcapture;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.filter.Filter;
import ch.qos.logback.core.spi.ContextAwareBase;
import ch.qos.logback.core.spi.FilterReply;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

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
                    new LoggedEvent(
                            loggingEvent.getLevel(),
                            loggingEvent.getFormattedMessage(),
                            loggingEvent.getMDCPropertyMap()
                    ));
        }
    }

    private boolean eventIsRelevant(ILoggingEvent loggingEvent) {
        for (String packageName : capturedPackages) {
            if (loggingEvent.getLoggerName().startsWith(packageName)) {
                return true;
            }
        }
        return false;
    }

    Integer assertCapturedNext(Level level, String regex, int startIndex, ExpectedMdcEntry... expectedMdcEntries) {
        Pattern pattern = Pattern.compile(".*" + regex + ".*", Pattern.DOTALL + Pattern.MULTILINE);
        LoggedEvent eventWithWrongMdcContents = null;
        for (int i = startIndex; i < loggedEvents.size(); i++) {
            LoggedEvent event = loggedEvents.get(i);
            if (eventMatchesWithoutMdc(event, level, pattern)) {
                if (containsMdcEntries(event.getMdcData(), expectedMdcEntries)) {
                    return i;
                }
                eventWithWrongMdcContents = event;
            }
        }
        if (eventWithWrongMdcContents != null) {
            throwAssertionForFoundMessageWithWrongMdcContents(level, regex, eventWithWrongMdcContents);
        }
        throw new AssertionError(String.format("Expected log message has not occurred: Level: %s, Regex: \"%s\"", level, regex));
    }

    int getNumberOfLoggedMessages() {
        return loggedEvents.size();
    }

    private boolean eventMatchesWithoutMdc(LoggedEvent event, Level level, Pattern pattern) {
        return eventHasLevel(event, level) && eventMatchesPattern(event, pattern);
    }

    private void throwAssertionForFoundMessageWithWrongMdcContents(Level level, String regex, LoggedEvent eventWithWrongMdcContents) {
        StringBuilder assertionMessage = new StringBuilder(String.format("Expected log message has occurred, but never with the expected MDC value: Level: %s, Regex: \"%s\"", level, regex));
        assertionMessage.append(System.lineSeparator());
        assertionMessage.append(String.format("  Captured message: \"%s\"", eventWithWrongMdcContents.getFormattedMessage()));
        assertionMessage.append(System.lineSeparator());
        assertionMessage.append("  Captured MDC values:");
        for (Map.Entry<String, String> entry : eventWithWrongMdcContents.getMdcData().entrySet()) {
            assertionMessage.append(System.lineSeparator());
            assertionMessage.append(String.format("    %s: \"%s\"", entry.getKey(), entry.getValue()));
        }
        throw new AssertionError(assertionMessage.toString());
    }

    private boolean eventMatchesPattern(LoggedEvent event, Pattern pattern) {
        return pattern.matcher(event.getFormattedMessage()).matches();
    }

    private boolean eventHasLevel(LoggedEvent event, Level level) {
        return event.getLevel().equals(level);
    }

    static boolean containsMdcEntries(Map<String, String> mdcData, ExpectedMdcEntry[] expectedMdcEntries) {
        if (expectedMdcEntries == null) {
            return true;
        }
        for (ExpectedMdcEntry expectedMdcEntry : expectedMdcEntries) {
            if (!expectedMdcEntry.isContainedIn(mdcData)) {
                return false;
            }
        }
        return true;
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
