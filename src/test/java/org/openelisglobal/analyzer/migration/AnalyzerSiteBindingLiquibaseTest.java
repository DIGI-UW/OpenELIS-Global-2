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
    private static final Path MIGRATION = VERSION_ROOT.resolve("076-analyzer-site-binding.xml");

    @Test
    public void versionedChangelogIncludesAnalyzerSiteBindingMigration() throws Exception {
        Document base = parse(BASE_CHANGELOG);

        assertTrue(elements(base, "include").stream()
                .anyMatch(include -> "076-analyzer-site-binding.xml".equals(include.getAttribute("file"))));
    }

    @Test
    public void migrationDefinesSharedRevisionedBindingsAndAnalyzerReferences() throws Exception {
        Document migration = parse(MIGRATION);

        Set<String> createdTables = attributes(elements(migration, "createTable"), "tableName");
        assertEquals(Set.of("analyzer_site_binding", "analyzer_site_binding_revision", "analyzer_site_binding_test"),
                createdTables);

        Element analyzerColumns = elements(migration, "addColumn").stream()
                .filter(element -> "analyzer".equals(element.getAttribute("tableName"))).findFirst().orElseThrow();
        assertEquals(Set.of("bridge_profile_id", "bridge_profile_revision", "site_binding_revision_id"),
                childColumnNames(analyzerColumns));

        Set<String> uniqueConstraints = attributes(elements(migration, "addUniqueConstraint"), "constraintName");
        assertTrue(uniqueConstraints.contains("uq_analyzer_site_binding_revision_number"));

        Set<String> foreignKeys = attributes(elements(migration, "addForeignKeyConstraint"), "constraintName");
        assertTrue(foreignKeys.contains("fk_analyzer_site_binding_revision_binding"));
        assertTrue(foreignKeys.contains("fk_analyzer_site_binding_test_revision"));
        assertTrue(foreignKeys.contains("fk_analyzer_site_binding_test_test"));
        assertTrue(foreignKeys.contains("fk_analyzer_site_binding_test_component"));
        assertTrue(foreignKeys.contains("fk_analyzer_site_binding_analyzer_revision"));
    }

    @Test
    public void migrationPreservesLegacyEvidenceAndProvidesRollback() throws Exception {
        Document migration = parse(MIGRATION);

        Set<String> droppedTables = attributes(elements(migration, "dropTable"), "tableName");
        assertFalse(droppedTables.contains("analyzer_test_map"));
        assertFalse(droppedTables.contains("analyzer_plugin_config"));
        assertTrue("each changeset must define rollback", elements(migration, "rollback").size() >= 2);
    }

    @Test
    public void bindingRevisionIsRegisteredForDurableAuditEvents() throws Exception {
        Document migration = parse(MIGRATION);

        boolean registered = elements(migration, "insert").stream()
                .filter(element -> "reference_tables".equals(element.getAttribute("tableName")))
                .flatMap(element -> childColumns(element).stream())
                .anyMatch(column -> "name".equals(column.getAttribute("name"))
                        && "analyzer_site_binding_revision".equals(column.getAttribute("value")));

        assertTrue("site-binding revisions must be registered with the existing audit trail", registered);
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
