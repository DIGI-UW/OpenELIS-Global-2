package org.openelisglobal.pathology;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.program.service.PathologyConclusionSearchService;
import org.openelisglobal.program.service.PathologySampleService;
import org.openelisglobal.program.valueholder.pathology.ConclusionEntries;
import org.openelisglobal.program.valueholder.pathology.ConclusionEntry;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * The diagnosis search reads the active entries of the diagnosis category from
 * the dictionary on each call, by name or code, ignoring case and accents.
 */
public class PathologyConclusionSearchTest extends BaseWebContextSensitiveTest {

    private static final int FIRST_EXTRA_ENTRY = 9400;
    private static final int EXTRA_ENTRIES = 56;
    private static final int SEEDED_ACTIVE_DIAGNOSES = 3;

    @Autowired
    private PathologyConclusionSearchService searchService;

    private PathologyFindingsDictionary dictionary;
    private int diagnosesBefore;

    @Before
    public void seedTheDictionary() {
        diagnosesBefore = searchService.search("").total();
        dictionary = PathologyFindingsDictionary.seed(jdbcTemplate);
        int category = DictionaryRows.idOfCategory(jdbcTemplate, PathologySampleService.CONCLUSION_CATEGORY);
        for (int i = 1; i <= EXTRA_ENTRIES; i++) {
            DictionaryRows.insertEntry(jdbcTemplate, FIRST_EXTRA_ENTRY + i, category, String.format("Entry %02d", i),
                    null, true);
        }
    }

    @After
    public void removeTheDictionary() {
        int[] extra = new int[EXTRA_ENTRIES];
        for (int i = 0; i < EXTRA_ENTRIES; i++) {
            extra[i] = FIRST_EXTRA_ENTRY + i + 1;
        }
        DictionaryRows.removeEntries(jdbcTemplate, extra);
        dictionary.close();
    }

    @Test
    public void findsByPartOfTheName_ignoringCaseAndAccents() {
        assertEquals("an unaccented, lower case part finds the accented name",
                List.of(PathologyFindingsDictionary.CARCINOME), ids(searchService.search("carcinome")));
        assertEquals("an accented, upper case part finds it too", List.of(PathologyFindingsDictionary.CARCINOME),
                ids(searchService.search("  CARCINÔME ")));
        assertEquals("the part of a name in the middle of it", List.of(PathologyFindingsDictionary.CHRONIC_GASTRITIS),
                ids(searchService.search("gastr")));
    }

    @Test
    public void findsByPartOfTheCode() {
        assertEquals(List.of(PathologyFindingsDictionary.ADENOCARCINOMA), ids(searchService.search("c80")));
        assertEquals("punctuation is kept, so a slash is part of the code",
                List.of(PathologyFindingsDictionary.CARCINOME), ids(searchService.search("8500")));
        assertEquals(List.of(PathologyFindingsDictionary.CARCINOME), ids(searchService.search("8500/3")));
        assertEquals("a dot that is not in the code finds nothing", 0, searchService.search("c80,1").total());
    }

    @Test
    public void leavesOutRetiredEntries_andOtherCategories() {
        assertEquals("a retired diagnosis is not offered", 0, searchService.search("old term").total());
        assertEquals("a technique is not a diagnosis", 0, searchService.search("frozen").total());
        assertEquals("a retired technique neither", 0, searchService.search("old technique").total());
    }

    @Test
    public void answersTheFirstFiftyByName_withTheCountOfAll() {
        ConclusionEntries entries = searchService.search("entry ");

        assertEquals("the count is of every match", EXTRA_ENTRIES, entries.total());
        assertEquals("the items are the first fifty", 50, entries.items().size());
        assertEquals("by name", "Entry 01", entries.items().get(0).value());
        assertEquals("up to the fiftieth", "Entry 50", entries.items().get(49).value());
    }

    @Test
    public void aBlankQuery_isTheFirstFifty() {
        for (String blank : new String[] { null, "", "   " }) {
            ConclusionEntries entries = searchService.search(blank);

            assertEquals("every active diagnosis counts", diagnosesBefore + SEEDED_ACTIVE_DIAGNOSES + EXTRA_ENTRIES,
                    entries.total());
            assertEquals(50, entries.items().size());
            List<String> names = entries.items().stream().map(ConclusionEntry::value).collect(Collectors.toList());
            List<String> sorted = new ArrayList<>(names);
            sorted.sort(Comparator.comparing((String name) -> name.toLowerCase(Locale.ROOT)));
            assertEquals("sorted by name ignoring case", sorted, names);
            assertTrue("a retired entry is not among them", entries.items().stream()
                    .noneMatch(entry -> entry.id().equals(PathologyFindingsDictionary.OLD_TERM)));
        }
    }

    private static List<String> ids(ConclusionEntries entries) {
        return entries.items().stream().map(ConclusionEntry::id).collect(Collectors.toList());
    }
}
