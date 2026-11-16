package org.openelisglobal.pathology;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.openelisglobal.program.service.PathologySampleService;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Seeds and removes the dictionary rows a pathology test needs, the tissue
 * types and stains a case's rows name, directly in the tables.
 *
 * <p>
 * They are written here rather than in a dataset, because naming dictionary or
 * dictionary_category in a dataset truncates them for every later test class in
 * the same Surefire fork. Each test class keeps its own ids, outside the ranges
 * fixtures use.
 */
final class DictionaryRows {

    private DictionaryRows() {
    }

    /**
     * Adds a category of that name unless one exists, which Liquibase or the
     * configuration loader may already have done. Answers whether it added it, so
     * the caller removes only what it created.
     */
    static boolean insertCategory(JdbcTemplate jdbcTemplate, int id, String name, String description,
            String abbreviation) {
        return jdbcTemplate.update(
                "INSERT INTO clinlims.dictionary_category (id, description, local_abbrev, name, lastupdated)"
                        + " SELECT ?, ?, ?, ?, now()"
                        + " WHERE NOT EXISTS (SELECT 1 FROM clinlims.dictionary_category WHERE name = ?)",
                id, description, abbreviation, name, name) == 1;
    }

    /** The id of the category of that name, whoever added it. */
    static int idOfCategory(JdbcTemplate jdbcTemplate, String name) {
        return jdbcTemplate.queryForObject("SELECT id FROM clinlims.dictionary_category WHERE name = ?", Integer.class,
                name);
    }

    /** An entry of the category, in use or retired; it sorts by its id. */
    static void insertEntry(JdbcTemplate jdbcTemplate, int id, int categoryId, String name, String abbreviation,
            boolean active) {
        insertEntry(jdbcTemplate, id, categoryId, name, abbreviation, active, id);
    }

    /** An entry in use, with the sort order given; null leaves it unsorted. */
    static void insertEntry(JdbcTemplate jdbcTemplate, int id, int categoryId, String name, String abbreviation,
            boolean active, Integer sortOrder) {
        jdbcTemplate.update(
                "INSERT INTO clinlims.dictionary (id, is_active, dict_entry, local_abbrev,"
                        + " dictionary_category_id, sort_order, lastupdated) VALUES (?, ?, ?, ?, ?, ?, now())",
                id, active ? "Y" : "N", name, abbreviation, categoryId, sortOrder);
    }

    /**
     * Removes the entries; rows that still name one have to be cleared first.
     */
    static void removeEntries(JdbcTemplate jdbcTemplate, int... ids) {
        if (ids.length == 0) {
            return;
        }
        String placeholders = Arrays.stream(ids).mapToObj(id -> "?").collect(Collectors.joining(", "));
        jdbcTemplate.update("DELETE FROM clinlims.dictionary WHERE id IN (" + placeholders + ")",
                Arrays.stream(ids).boxed().toArray());
    }

    static void removeCategory(JdbcTemplate jdbcTemplate, int id) {
        jdbcTemplate.update("DELETE FROM clinlims.dictionary_category WHERE id = ?", id);
    }

    /**
     * Seeds stains under the stain category for one test class, adding the category
     * with the class's own id unless the loader or Liquibase made it.
     */
    static StainEntries stainEntries(JdbcTemplate jdbcTemplate, int ownCategoryId) {
        return new StainEntries(jdbcTemplate, ownCategoryId);
    }

    /**
     * The stains a test class seeded, removed on close with every slide reference
     * to them, and the category with them when the class added it.
     */
    static final class StainEntries implements AutoCloseable {

        private final JdbcTemplate jdbcTemplate;
        private final int ownCategoryId;
        private final boolean addedTheCategory;
        private final int categoryId;
        private final List<Integer> ids = new ArrayList<>();

        private StainEntries(JdbcTemplate jdbcTemplate, int ownCategoryId) {
            this.jdbcTemplate = jdbcTemplate;
            this.ownCategoryId = ownCategoryId;
            this.addedTheCategory = insertCategory(jdbcTemplate, ownCategoryId, PathologySampleService.STAIN_CATEGORY,
                    PathologySampleService.STAIN_CATEGORY, "PathStain");
            this.categoryId = idOfCategory(jdbcTemplate, PathologySampleService.STAIN_CATEGORY);
        }

        /** Adds a stain, replacing a row of that id an earlier run left behind. */
        StainEntries with(int id, String name, boolean active) {
            forget(id);
            insertEntry(jdbcTemplate, id, categoryId, name, "PS" + id, active);
            ids.add(id);
            return this;
        }

        @Override
        public void close() {
            ids.forEach(this::forget);
            if (addedTheCategory) {
                removeCategory(jdbcTemplate, ownCategoryId);
            }
        }

        private void forget(int id) {
            jdbcTemplate.update("UPDATE clinlims.pathology_slide SET stain_id = NULL WHERE stain_id = ?", id);
            removeEntries(jdbcTemplate, id);
        }
    }
}
