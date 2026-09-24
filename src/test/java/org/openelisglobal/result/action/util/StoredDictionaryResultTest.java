package org.openelisglobal.result.action.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.result.valueholder.Result;

/**
 * OGC-1234 — a result entered before its option was removed keeps showing by
 * name: its value is appended to the offered options once, and only for a
 * select-list result.
 */
public class StoredDictionaryResultTest {

    private static List<IdValuePair> activeOptions() {
        List<IdValuePair> options = new ArrayList<>();
        options.add(new IdValuePair("0", ""));
        options.add(new IdValuePair("821", "Negative"));
        return options;
    }

    private static Result result(String value, String type) {
        Result result = new Result();
        result.setValue(value);
        result.setResultType(type);
        return result;
    }

    private static DictionaryService dictionaryWith(String id, String name) {
        Dictionary entry = mock(Dictionary.class);
        when(entry.getLocalizedName()).thenReturn(name);
        DictionaryService service = mock(DictionaryService.class);
        when(service.getDataForId(id)).thenReturn(entry);
        return service;
    }

    @Test
    public void aRemovedOptionsStoredValueIsAppendedByName() {
        List<IdValuePair> options = StoredDictionaryResult.withStoredValue(activeOptions(), result("820", "D"),
                dictionaryWith("820", "Positive"));

        assertEquals(3, options.size());
        assertEquals("820", options.get(2).getId());
        assertEquals("Positive", options.get(2).getValue());
    }

    @Test
    public void aValueStillOfferedIsNotDuplicated() {
        DictionaryService service = dictionaryWith("821", "Negative");
        List<IdValuePair> options = StoredDictionaryResult.withStoredValue(activeOptions(), result("821", "D"),
                service);

        assertEquals(2, options.size());
        verify(service, never()).getDataForId(anyString());
    }

    @Test
    public void aNonSelectListResultOrANonSelectListTestIsLeftAlone() {
        DictionaryService service = dictionaryWith("820", "Positive");

        assertEquals(2, StoredDictionaryResult.withStoredValue(activeOptions(), result("820", "N"), service).size());
        assertNull(StoredDictionaryResult.withStoredValue(null, result("820", "D"), service));
        assertEquals(2,
                StoredDictionaryResult.withStoredValue(activeOptions(), result("not-an-id", "D"), service).size());
    }
}
