package org.openelisglobal.dictionary.service;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.BaseWebContextSensitiveTest;
import org.openelisglobal.configuration.service.DomainConfigurationHandler;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.dictionarycategory.service.DictionaryCategoryService;
import org.openelisglobal.dictionarycategory.valueholder.DictionaryCategory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Regression test for the legacy-category lookup path in
 * DictionaryConfigurationHandler. A distro author dropping a CSV row into a
 * category whose name differs from its description (i.e. any pre-existing
 * OE2-master-seed category — marital status, nationality, education level,
 * etc.) was silently dropped by the loader before the
 * getDictionaryEntryByNameAndCategoryName switch. This test exercises that
 * exact shape to lock the behavior.
 */
public class DictionaryConfigurationHandlerLegacyCategoryTest extends BaseWebContextSensitiveTest {

    @Autowired
    @Qualifier("dictionaryConfigurationHandler")
    private DomainConfigurationHandler handler;

    @Autowired
    private DictionaryService dictionaryService;

    @Autowired
    private DictionaryCategoryService dictionaryCategoryService;

    @Before
    public void setUp() throws Exception {
        executeDataSetWithStateManagement("testdata/dictionary.xml");
    }

    @Test
    public void csvRowTargetingLegacyCategoryByName_deactivatesExistingEntry() throws Exception {
        // Pre-condition: from dictionary.xml, category 1 has name="Category Name 1"
        // and description="Category Description 1" (name != description, mirrors
        // the OE2 master seed legacy categories). Dictionary id=1 lives in that
        // category and is_active='Y'.
        Dictionary existingBefore = dictionaryService.getDictionaryEntryByNameAndCategoryName("Dictionary Entry 1",
                "Category Name 1");
        assertNotNull("Pre-condition: existing dictionary row must be findable by name lookup", existingBefore);
        assertEquals("Y", existingBefore.getIsActive());

        String csv = "category,dictEntry,localAbbreviation,isActive,sortOrder,loincCode\n"
                + "Category Name 1,Dictionary Entry 1,,N,,\n";
        InputStream stream = new ByteArrayInputStream(csv.getBytes(StandardCharsets.UTF_8));

        handler.processConfiguration(stream, "test-legacy-category-deactivation.csv");

        // The existing dictionary row must now be deactivated, not a new orphan
        // row created.
        Dictionary existingAfter = dictionaryService.getDictionaryEntryByNameAndCategoryName("Dictionary Entry 1",
                "Category Name 1");
        assertNotNull(existingAfter);
        assertEquals("Existing row must be deactivated by the CSV import", "N", existingAfter.getIsActive());
        assertEquals("Existing row ID must be preserved (no insert/duplicate)", existingBefore.getId(),
                existingAfter.getId());

        // No orphan duplicate-name category created.
        List<DictionaryCategory> categories = dictionaryCategoryService.getAll();
        long matchingCategoryCount = categories.stream().filter(c -> "Category Name 1".equals(c.getCategoryName()))
                .count();
        assertEquals("CSV import must not create a duplicate category with the same name", 1L, matchingCategoryCount);
    }
}
