package de.dm.infrastructure.logcapture;

import ch.qos.logback.classic.Level;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.slf4j.Marker;
import org.slf4j.event.KeyValuePair;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static lombok.AccessLevel.PRIVATE;

/**
 * represents a captured logged event
 */
@Builder
@Getter
public final class LoggedEvent {
    private final Level level;
    private final String formattedMessage;
    private final Map<String, String> mdcData;
    private final Optional<LoggedException> loggedException;
    private final String loggerName;
    private final List<Marker> markers;
    private final List<KeyValuePair> keyValuePairs;

    private LoggedEvent(Level level,
                        String formattedMessage,
                        Map<String, String> mdcData,
                        Optional<LoggedException> loggedException,
                        String loggerName,
                        List<Marker> markers,
                        List<KeyValuePair> keyValuePairs) {
        this.level = level;
        this.formattedMessage = formattedMessage;
        this.mdcData = mdcData;
        this.loggedException = loggedException;
        this.loggerName = loggerName;
        this.markers = markers;
        this.keyValuePairs = keyValuePairs;
    }

    @SuppressWarnings("squid:S2166") // LoggedException is not an Exception, but the name is still appropriate
    @AllArgsConstructor(access = PRIVATE)
    @Builder
    @Getter
    static class LoggedException {
        private final String type; // because IThrowableProxy only offers getClassName() and not getClass()
        private final String message;
        private final Optional<LoggedException> cause;
    }
}
