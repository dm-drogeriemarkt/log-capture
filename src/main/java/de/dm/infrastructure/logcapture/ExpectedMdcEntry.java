package de.dm.infrastructure.logcapture;

import lombok.RequiredArgsConstructor;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * define expected MDC entries with this
 */
@RequiredArgsConstructor
public final class ExpectedMdcEntry {

    private final String key;
    private final MdcMatcher matcher;

    boolean isContainedIn(Map<String, String> mdcContents) {
        if (!mdcContents.containsKey(key)) {
            return false;
        }
        return matcher.matches(mdcContents.get(key));
    }

    /**
     * use this in LogCapture.assertLogged(...) to verify that something has been logged with an MDC value
     *
     * @param key MDC key
     * @param valueRegex regex that must match the expected value. Will be wrapped with .*
     *
     * @return expected entry
     */
    public static ExpectedMdcEntry withMdc(String key, String valueRegex) {
        return new ExpectedMdcEntry(key, new PatternMatcher(valueRegex));
    }

    /**
     * use this in LogCapture.assertLogged(...) to verify that something has been logged with an MDC value
     *
     * @param key MDC key
     * @param matcher implementation of {@link MdcMatcher} that checks if the actual MDC content matches the expectations
     *
     * @return expected entry
     */
    public static ExpectedMdcEntry withMdc(String key, MdcMatcher matcher) {
        return new ExpectedMdcEntry(key, matcher);
    }

    private static class PatternMatcher implements MdcMatcher {

        private final Pattern pattern;

        PatternMatcher(String valueRegex) {
            pattern = Pattern.compile(".*" + valueRegex + ".*");
        }

        @Override
        public boolean matches(String mdcValue) {
            return pattern.matcher(mdcValue).matches();
        }
    }

}
