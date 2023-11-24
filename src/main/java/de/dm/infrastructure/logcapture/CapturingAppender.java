package de.dm.infrastructure.logcapture;

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

class CapturingAppender extends ContextAwareBase implements Appender<ILoggingEvent> {
    @Getter
    List<LoggedEvent> loggedEvents = new ArrayList<>();
    private final Set<String> capturedPackages;

    @Getter
    @Setter
    private String name;
    @Getter
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
                            .markers(loggingEvent.getMarkerList())
                            .argumentArray(loggingEvent.getArgumentArray())
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
