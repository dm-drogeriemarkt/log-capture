package de.dm.infrastructure.logcapture;

import ch.qos.logback.classic.Level;

import java.util.Arrays;
import java.util.List;


public final class LogAssertion {
    final Level level;
    final String regex;
    final List<MatchingCondition> matchingConditions;

    private LogAssertion(Level level, String regex, MatchingCondition... matchingConditionsForThisMessage) {
        this.level = level;
        this.regex = regex;
        matchingConditions = Arrays.asList(matchingConditionsForThisMessage);
    }

    public static LogAssertion trace(String regex, MatchingCondition... matchingConditionsForThisMessage) {
        return new LogAssertion(Level.TRACE, regex, matchingConditionsForThisMessage);
    }

    public static LogAssertion debug(String regex, MatchingCondition... matchingConditionsForThisMessage) {
        return new LogAssertion(Level.DEBUG, regex, matchingConditionsForThisMessage);
    }

    public static LogAssertion info(String regex, MatchingCondition... matchingConditionsForThisMessage) {
        return new LogAssertion(Level.INFO, regex, matchingConditionsForThisMessage);
    }

    public static LogAssertion warn(String regex, MatchingCondition... matchingConditionsForThisMessage) {
        return new LogAssertion(Level.WARN, regex, matchingConditionsForThisMessage);
    }

    public static LogAssertion error(String regex, MatchingCondition... matchingConditionsForThisMessage) {
        return new LogAssertion(Level.ERROR, regex, matchingConditionsForThisMessage);
    }

}
