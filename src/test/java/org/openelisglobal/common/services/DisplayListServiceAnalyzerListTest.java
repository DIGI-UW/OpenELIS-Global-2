package org.openelisglobal.common.services;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.analyzer.service.AnalyzerService;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.common.services.DisplayListService.ListType;
import org.openelisglobal.common.util.IdValuePair;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Results Entry names each result's analyzer from the analyzer list, asked for
 * fresh on every load. The lists are built once at startup, so an analyzer set
 * up later must still be named.
 */
public class DisplayListServiceAnalyzerListTest {

    private Object listsBuiltAtStartup;

    @Before
    public void keepTheSharedLists() {
        listsBuiltAtStartup = ReflectionTestUtils.getField(DisplayListService.class, "typeToListMap");
    }

    @After
    public void restoreTheSharedLists() {
        ReflectionTestUtils.setField(DisplayListService.class, "typeToListMap", listsBuiltAtStartup);
    }

    @Test
    public void aFreshAnalyzerListNamesAnAnalyzerSetUpAfterStartup() {
        Map<ListType, List<IdValuePair>> builtAtStartup = new HashMap<>();
        builtAtStartup.put(ListType.ANALYZER_LIST, List.of());
        ReflectionTestUtils.setField(DisplayListService.class, "typeToListMap", builtAtStartup);
        AnalyzerService analyzers = mock(AnalyzerService.class);
        when(analyzers.getAll()).thenReturn(List.of(analyzer("65", "Results GeneXpert")));
        DisplayListService lists = new DisplayListService();
        ReflectionTestUtils.setField(lists, "analyzerService", analyzers);

        List<IdValuePair> listed = lists.getFreshList(ListType.ANALYZER_LIST);

        assertEquals(List.of("65 Results GeneXpert"),
                listed.stream().map(pair -> pair.getId() + " " + pair.getValue()).toList());
    }

    private static Analyzer analyzer(String id, String name) {
        Analyzer analyzer = new Analyzer();
        analyzer.setId(id);
        analyzer.setName(name);
        return analyzer;
    }
}
