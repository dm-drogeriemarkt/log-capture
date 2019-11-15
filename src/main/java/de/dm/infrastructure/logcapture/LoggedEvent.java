package de.dm.infrastructure.logcapture;

import ch.qos.logback.classic.Level;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@RequiredArgsConstructor
@Getter
class LoggedEvent {

    private final Level level;
    private final String formattedMessage;
    private final Map<String, String> mdcData;
}
