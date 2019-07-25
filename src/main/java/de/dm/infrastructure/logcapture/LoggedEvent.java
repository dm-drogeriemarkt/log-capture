package de.dm.infrastructure.logcapture;

import ch.qos.logback.classic.Level;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.Set;

@RequiredArgsConstructor
@Getter
class LoggedEvent {

    private final Level level;
    private final String formattedMessage;
    private final Set<String> methodsInCallStack;
    private final Map<String, String> mdcData;
}
