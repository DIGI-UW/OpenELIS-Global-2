package org.openelisglobal.analyzer.migration;

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

public class AnalyzerProfileMigrationLiquibaseTest {

    private static final Path VERSION_ROOT = Path.of("src", "main", "resources", "liquibase", "3.5.x.x");
    private static final Path BASE_CHANGELOG = VERSION_ROOT.resolve("base.xml");
    private static final Path MIGRATION = VERSION_ROOT.resolve("077-analyzer-profile-migration-anomaly.xml");

    @Test
    public void versionedChangelogIncludesDurableMigrationAnomalies() throws Exception {
        Document base = parse(BASE_CHANGELOG);

        assertTrue(elements(base, "include").stream().anyMatch(
                include -> "077-analyzer-profile-migration-anomaly.xml".equals(include.getAttribute("file"))));
    }

    @Test
    public void anomalyRowsPreserveDeterministicLegacyEvidenceAndAudit() throws Exception {
        Document migration = parse(MIGRATION);
        Element table = elements(migration, "createTable").stream()
                .filter(element -> "analyzer_profile_migration_anomaly".equals(element.getAttribute("tableName")))
                .findFirst().orElseThrow();

        Set<String> columns = childColumnNames(table);
        assertTrue(columns.containsAll(Set.of("id", "analyzer_id", "code", "evidence_key", "legacy_source_key",
                "legacy_test_id", "detail", "status", "detected_by", "detected_at", "resolved_by", "resolved_at")));
        assertTrue(elements(migration, "addUniqueConstraint").stream()
                .anyMatch(element -> "uq_analyzer_profile_migration_anomaly_evidence"
                        .equals(element.getAttribute("constraintName"))));
        assertTrue(elements(migration, "addForeignKeyConstraint").stream()
                .anyMatch(element -> "fk_analyzer_profile_migration_anomaly_analyzer"
                        .equals(element.getAttribute("constraintName"))));
        assertTrue(elements(migration, "insert").stream()
                .filter(element -> "reference_tables".equals(element.getAttribute("tableName")))
                .flatMap(element -> childColumns(element).stream())
                .anyMatch(column -> "name".equals(column.getAttribute("name"))
                        && "analyzer_profile_migration_anomaly".equals(column.getAttribute("value"))));
    }

    @Test
    public void rollbackDoesNotMutateLegacyMigrationEvidence() throws Exception {
        Document migration = parse(MIGRATION);

        Set<String> droppedTables = attributes(elements(migration, "dropTable"), "tableName");
        assertFalse(droppedTables.contains("analyzer_test_map"));
        assertFalse(droppedTables.contains("analyzer_plugin_config"));
        assertTrue(elements(migration, "rollback").size() >= 2);
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
