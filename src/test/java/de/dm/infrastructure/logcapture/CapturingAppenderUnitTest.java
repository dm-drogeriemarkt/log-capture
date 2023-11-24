package de.dm.infrastructure.logcapture;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.filter.Filter;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.Set;

import static ch.qos.logback.core.spi.FilterReply.ACCEPT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class CapturingAppenderUnitTest {

    @Mock
    LoggerContext loggerContext;

    CapturingAppender sut = new CapturingAppender(loggerContext, Set.of(
            "a.package",
            "another.package")
    );

    @SuppressWarnings("unchecked") // filter types are irrelevant
    @Test
    void neverFiltersAndNeverHasAnyFilters() {
        assertThat(sut.getFilterChainDecision(mock(ILoggingEvent.class))).isEqualTo(ACCEPT);

        sut.addFilter(mock(Filter.class));

        assertThat(sut.getFilterChainDecision(mock(ILoggingEvent.class))).isEqualTo(ACCEPT);
        assertThat(sut.getCopyOfAttachedFiltersList()).isEmpty();

        sut.clearAllFilters();

        assertThat(sut.getFilterChainDecision(mock(ILoggingEvent.class))).isEqualTo(ACCEPT);
    }

    @Test
    void startStop() {
        assertThat(sut.isStarted()).isFalse();
        sut.start();
        assertThat(sut.isStarted()).isTrue();
        sut.stop();
        assertThat(sut.isStarted()).isFalse();
    }

}
