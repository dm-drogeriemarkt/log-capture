package de.dm.infrastructure.logcapture;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static de.dm.infrastructure.logcapture.ExpectedMdcEntry.mdc;

class LogAsserterIntegrationTest {

    @Test
    void containsMdcEntries() {
        String testKey = "test_key";
        String otherKey = "test_key_2";

        ExpectedMdcEntry expectedMdcEntry1 = mdc(testKey, "test value");
        ExpectedMdcEntry expectedMdcEntry2 = mdc(otherKey, "good value");
        List<ExpectedMdcEntry> expectedMdcEntries = new LinkedList<>();

        expectedMdcEntries.add(expectedMdcEntry1);
        expectedMdcEntries.add(expectedMdcEntry2);

        Map<String, String> mdcContents = new HashMap<>();
        mdcContents.put(testKey, "this is a test value, cool!");
        mdcContents.put(otherKey, "this is a good value, cool!");

        LoggedEvent loggedEvent = LoggedEvent.builder()
                .mdcData(mdcContents)
                .build();

        Assertions.assertTrue(LogAsserter.isMatchedByAll(loggedEvent, expectedMdcEntries));
    }

    @Test
    void nullEntriesShouldNotThrowNullPointerException() {
        Map<String, String> mdcContents = new HashMap<>();

        LoggedEvent loggedEvent = LoggedEvent.builder()
                .mdcData(mdcContents)
                .build();

        Assertions.assertTrue(LogAsserter.isMatchedByAll(loggedEvent, null));
    }
}
