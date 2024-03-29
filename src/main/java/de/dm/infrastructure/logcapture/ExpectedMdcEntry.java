package de.dm.infrastructure.logcapture;

import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.regex.Pattern;

import static java.lang.String.format;
import static java.lang.System.lineSeparator;
import static lombok.AccessLevel.PRIVATE;

/**
 * define expected MDC entries with this
 */
@RequiredArgsConstructor(access = PRIVATE)
public final class ExpectedMdcEntry implements LogEventMatcher {

    private final String key;
    private final MdcMatcher matcher;

    /**
     * use this in a log expectation to verify that something has been logged with an MDC value
     *
     * @param key MDC key
     * @param valueRegex regex that must match the expected value. Will be wrapped with .*
     *
     * @return expected Mdc entry to use in log expectation
     */
    public static ExpectedMdcEntry mdc(String key, String valueRegex) {
        return new ExpectedMdcEntry(key, new PatternMatcher(valueRegex));
    }

    /**
     * use this in log expectation to verify that something has been logged with an MDC value using
     * your own {@link MdcMatcher} if MDC contents are structured and the Matcher needs to understand the structure
     *
     * @param key MDC key
     * @param mdcMatcher implementation of {@link MdcMatcher} that checks if the actual MDC content matches the expectations
     *
     * @return expected Mdc entry to use in log expectation
     */
    public static ExpectedMdcEntry mdc(String key, MdcMatcher mdcMatcher) {
        return new ExpectedMdcEntry(key, mdcMatcher);
    }

    @Override
    public boolean matches(LoggedEvent loggedEvent) {
        if (!loggedEvent.getMdcData().containsKey(key)) {
            return false;
        }
        return matcher.matches(loggedEvent.getMdcData().get(key));
    }

    @Override
    public String getNonMatchingErrorMessage(LoggedEvent loggedEvent) {
        StringBuilder assertionMessage = new StringBuilder(format("  captured message: \"%s\"", loggedEvent.getFormattedMessage()));
        assertionMessage.append(lineSeparator());
        assertionMessage.append(format("  expected MDC key: %s", key));
        if (matcher instanceof PatternMatcher patternMatcher) {
            assertionMessage.append(lineSeparator());
            assertionMessage.append(format("  expected MDC value: \"%s\"", patternMatcher.pattern));
        }
        assertionMessage.append(lineSeparator());
        assertionMessage.append("  captured MDC values:");
        for (Map.Entry<String, String> entry : loggedEvent.getMdcData().entrySet()) {
            assertionMessage.append(lineSeparator());
            assertionMessage.append(format("    %s: \"%s\"", entry.getKey(), entry.getValue()));
        }
        return assertionMessage.toString();
    }

    @Override
    public String getMatcherTypeDescription() {
        return "MDC value";
    }

    @Override
    public String getMatcherDetailDescription() {
        return format("MDCValue with key: \"%s\"", key);
    }

    private static class PatternMatcher implements MdcMatcher {

        private final Pattern pattern;

        PatternMatcher(String valueRegex) {
            pattern = Pattern.compile(".*" + valueRegex + ".*", Pattern.DOTALL + Pattern.MULTILINE);
        }

        @Override
        public boolean matches(String mdcValue) {
            return pattern.matcher(mdcValue).matches();
        }
    }

}
