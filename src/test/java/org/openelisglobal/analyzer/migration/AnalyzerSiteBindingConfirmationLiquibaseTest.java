package org.openelisglobal.analyzer.migration;

import static org.junit.Assert.assertEquals;
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

public class AnalyzerSiteBindingConfirmationLiquibaseTest {

    private static final Path VERSION_ROOT = Path.of("src", "main", "resources", "liquibase", "3.5.x.x");
    private static final Path BASE_CHANGELOG = VERSION_ROOT.resolve("base.xml");
    private static final Path MIGRATION = VERSION_ROOT.resolve("089-analyzer-site-binding-confirmation.xml");
    private static final Path EVIDENCE_MIGRATION = VERSION_ROOT.resolve("090-analyzer-verification-evidence.xml");

    @Test
    public void versionedChangelogIncludesConfirmationMigration() throws Exception {
        Set<String> includes = attributes(elements(parse(BASE_CHANGELOG), "include"), "file");
        assertTrue(includes.contains("089-analyzer-site-binding-confirmation.xml"));
        assertTrue(includes.contains("090-analyzer-verification-evidence.xml"));
    }

    @Test
    public void migrationRecordsTheExactConfirmedCandidateAndDurableAudit() throws Exception {
        Document migration = parse(MIGRATION);
        Element table = elements(migration, "createTable").stream()
                .filter(element -> "analyzer_site_binding_confirmation".equals(element.getAttribute("tableName")))
                .findFirst().orElseThrow();

        assertEquals(Set.of("id", "site_binding_revision_id", "profile_id", "profile_revision", "binding_fingerprint",
                "recognition_fingerprint", "confirmed_rows_json", "excluded_rows_json", "confirmed_by", "confirmed_at",
                "last_updated"), attributes(childColumns(table), "name"));
        assertTrue(attributes(elements(migration, "addUniqueConstraint"), "constraintName")
                .contains("uq_analyzer_site_binding_confirmation_revision"));
        assertTrue(attributes(elements(migration, "addForeignKeyConstraint"), "constraintName")
                .contains("fk_analyzer_site_binding_confirmation_revision"));
        assertTrue(attributes(elements(migration, "column"), "value").contains("analyzer_site_binding_confirmation"));
        assertTrue("confirmation migration requires rollback", !elements(migration, "rollback").isEmpty());
    }

    @Test
    public void evidenceMigrationPreservesHistoryAndAddsDurableCandidateEvidence() throws Exception {
        Document migration = parse(EVIDENCE_MIGRATION);

        assertTrue("verification history must not be deleted", elements(migration, "delete").isEmpty());
        assertTrue(attributes(elements(migration, "column"), "name").contains("profile_revision_fingerprint"));
        assertTrue(attributes(elements(migration, "column"), "name").contains("audit_event_id"));
        assertTrue(attributes(elements(migration, "addForeignKeyConstraint"), "constraintName")
                .contains("fk_analyzer_confirmation_audit_event"));
        assertTrue("verification evidence migration requires rollback", !elements(migration, "rollback").isEmpty());
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
