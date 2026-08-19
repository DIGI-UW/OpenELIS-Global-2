package org.openelisglobal.analyzer.migration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class AnalyzerSiteBindingLiquibaseTest {

    private static final Path VERSION_ROOT = Path.of("src", "main", "resources", "liquibase", "3.5.x.x");
    private static final Path BASE_CHANGELOG = VERSION_ROOT.resolve("base.xml");
    private static final Path PROFILE_REFERENCE_MIGRATION = VERSION_ROOT.resolve("085-analyzer-profile-binding.xml");
    private static final Path MIGRATION = VERSION_ROOT.resolve("086-analyzer-site-binding.xml");

    @Test
    public void versionedChangelogIncludesAnalyzerSiteBindingMigration() throws Exception {
        Document base = parse(BASE_CHANGELOG);

        assertTrue(elements(base, "include").stream()
                .anyMatch(include -> "086-analyzer-site-binding.xml".equals(include.getAttribute("file"))));
    }

    @Test
    public void migrationDefinesImmutableLocalBindingRevisionsAndIndependentRows() throws Exception {
        Document migration = parse(MIGRATION);

        assertEquals(Set.of("analyzer_site_binding", "analyzer_site_binding_revision", "analyzer_site_binding_test",
                "analyzer_site_binding_result"), attributes(elements(migration, "createTable"), "tableName"));

        Element analyzerColumns = elements(migration, "addColumn").stream()
                .filter(element -> "analyzer".equals(element.getAttribute("tableName"))).findFirst().orElseThrow();
        assertEquals(Set.of("site_binding_revision_id"), childColumnNames(analyzerColumns));

        Set<String> uniqueConstraints = attributes(elements(migration, "addUniqueConstraint"), "constraintName");
        assertTrue(uniqueConstraints.contains("uq_analyzer_site_binding_revision_number"));
        assertTrue(uniqueConstraints.contains("uq_analyzer_site_binding_revision_fingerprint"));

        Set<String> foreignKeys = attributes(elements(migration, "addForeignKeyConstraint"), "constraintName");
        assertTrue(foreignKeys.contains("fk_analyzer_site_binding_profile"));
        assertTrue(foreignKeys.contains("fk_analyzer_site_binding_revision_binding"));
        assertTrue(foreignKeys.contains("fk_analyzer_site_binding_test_revision"));
        assertTrue(foreignKeys.contains("fk_analyzer_site_binding_result_revision"));
        assertTrue(foreignKeys.contains("fk_analyzer_site_binding_analyzer_revision"));
    }

    @Test
    public void migrationStoresReferencesAndLocalDecisionsWithoutCopyingPortableProfileContent() throws Exception {
        Document profileReferenceMigration = parse(PROFILE_REFERENCE_MIGRATION);
        Document migration = parse(MIGRATION);

        Set<String> allColumns = attributes(elements(migration, "column"), "name");
        allColumns.addAll(attributes(elements(profileReferenceMigration, "column"), "name"));
        assertTrue(allColumns.contains("profile_id"));
        assertTrue(allColumns.contains("profile_revision"));
        assertTrue(allColumns.contains("source_row_key"));
        assertTrue(allColumns.contains("raw_value"));
        assertTrue(allColumns.contains("test_id"));
        assertTrue(allColumns.contains("test_result_id"));

        assertFalse(allColumns.contains("profile_json"));
        assertFalse(allColumns.contains("profile_snapshot"));
        assertFalse(allColumns.contains("display_name"));
        assertFalse(allColumns.contains("analyzer_code"));
        assertFalse(allColumns.contains("normalized_coding"));
    }

    @Test
    public void migrationKeepsLegacyInputsForPreflightAndDefinesRollback() throws Exception {
        Document migration = parse(MIGRATION);

        Set<String> droppedTables = attributes(elements(migration, "dropTable"), "tableName");
        assertFalse(droppedTables.contains("analyzer_test_map"));
        assertFalse(droppedTables.contains("qualitative_result_mapping"));
        assertFalse(droppedTables.contains("analyzer_plugin_config"));
        assertTrue("every structural changeset must define rollback", elements(migration, "rollback").size() >= 2);
    }

    private static Document parse(Path path) throws Exception {
        assertTrue("missing changelog " + path, Files.isRegularFile(path));
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        return factory.newDocumentBuilder().parse(path.toFile());
    }

    private static Set<String> attributes(Iterable<Element> elements, String attribute) {
        Set<String> values = new HashSet<>();
        for (Element element : elements) {
            values.add(element.getAttribute(attribute));
        }
        return values;
    }

    private static Set<String> childColumnNames(Element parent) {
        return attributes(childColumns(parent), "name");
    }

    private static Set<Element> childColumns(Element parent) {
        Set<Element> columns = new HashSet<>();
        NodeList children = parent.getChildNodes();
        for (int index = 0; index < children.getLength(); index++) {
            Node child = children.item(index);
            if (child instanceof Element element && "column".equals(element.getTagName())) {
                columns.add(element);
            }
        }
        return columns;
    }

    private static Set<Element> elements(Document document, String tagName) {
        Set<Element> result = new HashSet<>();
        NodeList nodes = document.getElementsByTagName(tagName);
        for (int index = 0; index < nodes.getLength(); index++) {
            result.add((Element) nodes.item(index));
        }
        return result;
    }
}
