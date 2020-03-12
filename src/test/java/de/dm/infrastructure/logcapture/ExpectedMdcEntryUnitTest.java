package de.dm.infrastructure.logcapture;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

public class ExpectedMdcEntryUnitTest {

    private static final String TEST_KEY = "test_key";

    @Test
    public void isContainedIn() {
        ExpectedMdcEntry expectedMdcEntry = ExpectedMdcEntry.withMdc(TEST_KEY, "test value");
        Map<String, String> mdcContents = new HashMap<>();
        mdcContents.put(TEST_KEY, "this is a test value, cool!");
        Assertions.assertTrue(expectedMdcEntry.isContainedIn(mdcContents));
    }

    @Test
    public void isNotContainedIn() {
        ExpectedMdcEntry expectedMdcEntry = ExpectedMdcEntry.withMdc(TEST_KEY, "test value");
        Map<String, String> mdcContents = new HashMap<>();
        mdcContents.put(TEST_KEY, "this is a value, cool!");
        mdcContents.put("some_other_key", "this is a test value, cool!");
        Assertions.assertFalse(expectedMdcEntry.isContainedIn(mdcContents));
    }

}
