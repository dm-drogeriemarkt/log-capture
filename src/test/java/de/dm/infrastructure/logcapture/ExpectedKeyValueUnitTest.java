package de.dm.infrastructure.logcapture;

import net.logstash.logback.marker.SingleFieldAppendingMarker;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ExpectedKeyValueUnitTest {
    @Test
    @SuppressWarnings("squid:S3415")
        // assertion arguments are in the right order, despite Sonar thinking otherwise
    void logstashMarkerClassCanonicalNameIsCorrect() {
        assertThat(ExpectedKeyValue.LOGSTASH_MARKER_CLASS).isEqualTo(SingleFieldAppendingMarker.class.getCanonicalName());
    }

}
