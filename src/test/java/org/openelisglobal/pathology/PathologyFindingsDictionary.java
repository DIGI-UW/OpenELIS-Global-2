package org.openelisglobal.pathology;

import org.openelisglobal.program.service.PathologySampleService;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * The diagnoses, techniques and stain of the slice's fixture rows, added for a
 * test and removed after it; no dataset names the dictionary.
 */
public final class PathologyFindingsDictionary implements AutoCloseable {

    public static final String ADENOCARCINOMA = "9101";
    public static final String CHRONIC_GASTRITIS = "9102";
    public static final String OLD_TERM = "9103";
    public static final String CARCINOME = "9104";
    public static final String FROZEN_SECTION = "9201";
    public static final String OLD_TECHNIQUE = "9202";
    public static final String ANY_STAIN = "9301";
    public static final String NO_ENTRY = "99999";

    private static final int CONCLUSION_CATEGORY_ID = 9111;
    private static final int TECHNIQUE_CATEGORY_ID = 9211;
    private static final int STAIN_CATEGORY_ID = 9311;
    private static final int[] ENTRIES = { 9101, 9102, 9103, 9104, 9201, 9202 };

    private final JdbcTemplate jdbcTemplate;
    private final boolean addedConclusionCategory;
    private final boolean addedTechniqueCategory;
    private final DictionaryRows.StainEntries stains;

    private PathologyFindingsDictionary(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        DictionaryRows.removeEntries(jdbcTemplate, ENTRIES);
        addedConclusionCategory = DictionaryRows.insertCategory(jdbcTemplate, CONCLUSION_CATEGORY_ID,
                PathologySampleService.CONCLUSION_CATEGORY, "Pathologist conclusions", "PathConc");
        addedTechniqueCategory = DictionaryRows.insertCategory(jdbcTemplate, TECHNIQUE_CATEGORY_ID,
                PathologySampleService.TECHNIQUE_CATEGORY, "Pathology techniques", "PathTech");
        int conclusions = DictionaryRows.idOfCategory(jdbcTemplate, PathologySampleService.CONCLUSION_CATEGORY);
        int techniques = DictionaryRows.idOfCategory(jdbcTemplate, PathologySampleService.TECHNIQUE_CATEGORY);
        DictionaryRows.insertEntry(jdbcTemplate, 9101, conclusions, "Adenocarcinoma", "C80.1", true);
        DictionaryRows.insertEntry(jdbcTemplate, 9102, conclusions, "Chronic gastritis", "K29.5", true);
        DictionaryRows.insertEntry(jdbcTemplate, 9103, conclusions, "Old term", null, false);
        DictionaryRows.insertEntry(jdbcTemplate, 9104, conclusions, "Carcinôme canalaire", "8500/3", true);
        DictionaryRows.insertEntry(jdbcTemplate, 9201, techniques, "Frozen section", null, true);
        DictionaryRows.insertEntry(jdbcTemplate, 9202, techniques, "Old technique", null, false);
        stains = DictionaryRows.stainEntries(jdbcTemplate, STAIN_CATEGORY_ID).with(9301, "Any stain", true);
    }

    public static PathologyFindingsDictionary seed(JdbcTemplate jdbcTemplate) {
        return new PathologyFindingsDictionary(jdbcTemplate);
    }

    @Override
    public void close() {
        stains.close();
        DictionaryRows.removeEntries(jdbcTemplate, ENTRIES);
        if (addedConclusionCategory) {
            DictionaryRows.removeCategory(jdbcTemplate, CONCLUSION_CATEGORY_ID);
        }
        if (addedTechniqueCategory) {
            DictionaryRows.removeCategory(jdbcTemplate, TECHNIQUE_CATEGORY_ID);
        }
    }
}
