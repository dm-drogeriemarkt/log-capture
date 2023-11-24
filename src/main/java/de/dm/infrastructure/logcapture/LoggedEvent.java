package de.dm.infrastructure.logcapture;

import ch.qos.logback.classic.Level;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.slf4j.Marker;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static lombok.AccessLevel.PRIVATE;

/**
 * represents a captured logged event
 */
@AllArgsConstructor(access = PRIVATE)
@Builder
@Getter
public class LoggedEvent {
    private final Level level;
    private final String formattedMessage;
    private final Map<String, String> mdcData;
    private final Optional<LoggedException> loggedException;
    private final String loggerName;
    private final List<Marker> markers;

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
