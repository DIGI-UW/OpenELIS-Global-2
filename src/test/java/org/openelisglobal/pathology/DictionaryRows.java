package org.openelisglobal.pathology;

import java.util.Arrays;
import java.util.stream.Collectors;
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
        jdbcTemplate.update(
                "INSERT INTO clinlims.dictionary (id, is_active, dict_entry, local_abbrev,"
                        + " dictionary_category_id, sort_order, lastupdated) VALUES (?, ?, ?, ?, ?, ?, now())",
                id, active ? "Y" : "N", name, abbreviation, categoryId, id);
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
}
