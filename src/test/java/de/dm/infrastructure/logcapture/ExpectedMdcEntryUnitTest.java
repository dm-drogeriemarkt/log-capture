package de.dm.infrastructure.logcapture;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

class ExpectedMdcEntryUnitTest { //TODO: move this test to the right place/unit

    private static final String TEST_KEY = "test_key";

    @Test
    void isContainedIn() {
        ExpectedMdcEntry expectedMdcEntry = ExpectedMdcEntry.withMdc(TEST_KEY, "test value");
        Map<String, String> mdcContents = new HashMap<>();
        mdcContents.put(TEST_KEY, "this is a test value, cool!");

        LoggedEvent loggedEvent = LoggedEvent.builder()
                .mdcData(mdcContents)
                .build();

        Assertions.assertTrue(expectedMdcEntry.matches(loggedEvent));
    }

    @Test
    void isNotContainedIn() {
        ExpectedMdcEntry expectedMdcEntry = ExpectedMdcEntry.withMdc(TEST_KEY, "test value");
        Map<String, String> mdcContents = new HashMap<>();
        mdcContents.put(TEST_KEY, "this is a value, cool!");
        mdcContents.put("some_other_key", "this is a test value, cool!");

        LoggedEvent loggedEvent = LoggedEvent.builder()
                .mdcData(mdcContents)
                .build();

        Assertions.assertFalse(expectedMdcEntry.matches(loggedEvent));
    }

}
