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

public class AnalyzerActivationCandidateLiquibaseTest {

    private static final Path VERSION_ROOT = Path.of("src", "main", "resources", "liquibase", "3.5.x.x");
    private static final Path BASE_CHANGELOG = VERSION_ROOT.resolve("base.xml");
    private static final Path MIGRATION = VERSION_ROOT.resolve("091-analyzer-activation-candidate.xml");

    @Test
    public void versionedChangelogIncludesActivationCandidateMigration() throws Exception {
        assertTrue(attributes(elements(parse(BASE_CHANGELOG), "include"), "file")
                .contains("091-analyzer-activation-candidate.xml"));
    }

    @Test
    public void migrationRetainsCandidatesAndAddsOneActiveReference() throws Exception {
        Document migration = parse(MIGRATION);
        Element table = elements(migration, "createTable").stream()
                .filter(element -> "analyzer_activation_candidate".equals(element.getAttribute("tableName")))
                .findFirst().orElseThrow();

        assertEquals(Set.of("id", "analyzer_id", "site_binding_revision_id", "verification_confirmation_id",
                "candidate_document_json", "bridge_registration_json", "desired_state_fingerprint", "created_by",
                "created_at", "last_updated"), attributes(childColumns(table), "name"));
        assertTrue(attributes(elements(migration, "column"), "name").contains("active_candidate_id"));
        Set<String> foreignKeys = attributes(elements(migration, "addForeignKeyConstraint"), "constraintName");
        assertTrue(foreignKeys.contains("fk_analyzer_activation_candidate_analyzer"));
        assertTrue(foreignKeys.contains("fk_analyzer_activation_candidate_binding_revision"));
        assertTrue(foreignKeys.contains("fk_analyzer_activation_candidate_confirmation"));
        assertTrue(foreignKeys.contains("fk_analyzer_active_candidate"));
        assertFalse("analyzer candidates must remain a retained history",
                attributes(elements(migration, "addUniqueConstraint"), "columnNames").contains("analyzer_id"));
        assertTrue("activation candidate migration requires rollback", !elements(migration, "rollback").isEmpty());
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
        NodeList nodes = document.getElementsByTagName(tagName);
        Set<Element> result = new HashSet<>();
        for (int index = 0; index < nodes.getLength(); index++) {
            result.add((Element) nodes.item(index));
        }
        return result;
    }
}
