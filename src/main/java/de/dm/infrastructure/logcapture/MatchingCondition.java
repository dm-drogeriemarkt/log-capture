package de.dm.infrastructure.logcapture;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import static lombok.AccessLevel.PRIVATE;

@RequiredArgsConstructor(access = PRIVATE)
@Getter
public final class MatchingCondition {
    private final ExpectedMdcEntry expectedMdcEntry;

    public static MatchingCondition mdc(String key, String valueRegex) {
        return new MatchingCondition(ExpectedMdcEntry.withMdc(key, valueRegex));
    }

    public static MatchingCondition mdc(String key, MdcMatcher mdcMatcher) {
        return new MatchingCondition(ExpectedMdcEntry.withMdc(key, mdcMatcher));
    }
}
