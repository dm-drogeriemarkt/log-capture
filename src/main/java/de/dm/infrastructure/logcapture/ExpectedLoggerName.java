package de.dm.infrastructure.logcapture;

import java.util.regex.Pattern;

public class ExpectedLoggerName implements LogEventMatcher {
    private final Pattern expectedName;
    private final String inputRegex;

    ExpectedLoggerName(String loggerNameRegex) {
        inputRegex = loggerNameRegex;
        expectedName = Pattern.compile(".*" + loggerNameRegex + ".*");
    }

    @Override
    public boolean matches(LoggedEvent loggedEvent) {
        return expectedName.matcher(loggedEvent.getLoggerName()).matches();
    }

    @Override
    public String getNonMatchingErrorMessage(LoggedEvent loggedEvent) {
        StringBuilder assertionMessage = new StringBuilder(String.format("  expected logger name (regex): \"%s\"", inputRegex));
        assertionMessage.append(System.lineSeparator());
        assertionMessage.append(String.format("  actual logger name: \"%s\"", loggedEvent.getLoggerName()));

        return assertionMessage.toString();
    }

    @Override
    public String getMatcherDescription() {
        return "logger name";
    }

    public static ExpectedLoggerName logger(String loggerNameRegex) {
        return new ExpectedLoggerName(loggerNameRegex);
    }
}
