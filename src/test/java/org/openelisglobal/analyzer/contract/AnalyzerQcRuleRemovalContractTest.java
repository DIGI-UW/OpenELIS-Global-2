package org.openelisglobal.analyzer.contract;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class AnalyzerQcRuleRemovalContractTest {

    private static final Path JAVA_ROOT = Path.of("src", "main", "java", "org", "openelisglobal", "analyzer");
    private static final Path FRONTEND_ROOT = Path.of("frontend", "src");
    private static final Path LIQUIBASE_ROOT = Path.of("src", "main", "resources", "liquibase");
    private static final Path WESTGARD_SPEC_ROOT = Path.of("specs", "OGC-41-westgard-qc");
    private static final Path REMOVAL_CHANGELOG = LIQUIBASE_ROOT.resolve("3.5.x.x")
            .resolve("088-remove-analyzer-qc-rule.xml");

    @Test
    public void supersededClassifierRuntimeIsAbsent() throws Exception {
        for (Path path : List.of(JAVA_ROOT.resolve("valueholder/AnalyzerQcRule.java"),
                JAVA_ROOT.resolve("dao/AnalyzerQcRuleDAO.java"), JAVA_ROOT.resolve("dao/AnalyzerQcRuleDAOImpl.java"),
                JAVA_ROOT.resolve("service/AnalyzerQcRuleService.java"),
                JAVA_ROOT.resolve("service/AnalyzerQcRuleServiceImpl.java"),
                JAVA_ROOT.resolve("service/QcRuleDto.java"),
                JAVA_ROOT.resolve("controller/AnalyzerQcRuleRestController.java"),
                FRONTEND_ROOT.resolve("components/analyzers/QcRules/QcRuleBuilderModal.jsx"),
                FRONTEND_ROOT.resolve("components/analyzers/QcRules/QcRuleRow.jsx"))) {
            assertFalse("superseded runtime file remains: " + path, Files.exists(path));
        }

        assertDoesNotContain(JAVA_ROOT.resolve("service/AnalyzerStatusTransitionServiceImpl.java"),
                "hasAtLeastOneActiveQcRule");
        assertDoesNotContain(JAVA_ROOT.resolve("service/AnalyzerServiceImpl.java"), "createQcRulesFromProfile");
        assertDoesNotContain(JAVA_ROOT.resolve("controller/AnalyzerRestController.java"), "qcRules");
        assertDoesNotContain(JAVA_ROOT.resolve("controller/AnalyzerRestController.java"), "controlLots");
        assertDoesNotContain(FRONTEND_ROOT.resolve("App.jsx"), "/analyzers/:id/qc-rules");
        assertDoesNotContain(FRONTEND_ROOT.resolve("components/analyzers/AnalyzersList/AnalyzersList.tsx"),
                "/qc-rules");
        assertDoesNotContain(FRONTEND_ROOT.resolve("services/analyzerService.ts"), "getQcRules");
        assertDoesNotContain(Path.of("src", "main", "resources", "persistence", "persistence.xml"), "AnalyzerQcRule");
    }

    @Test
    public void versionedMigrationRemovesTheClassifierTable() throws Exception {
        assertDoesNotContain(LIQUIBASE_ROOT.resolve("base-changelog.xml"), "liquibase/analyzer/base.xml");
        assertFalse(Files.exists(LIQUIBASE_ROOT.resolve("analyzer/004-012-create-analyzer-qc-rule.xml")));
        assertFalse(Files.exists(LIQUIBASE_ROOT.resolve("analyzer/004-013-seed-default-qc-rules.xml")));
        assertTrue(Files.readString(LIQUIBASE_ROOT.resolve("3.5.x.x/base.xml"))
                .contains("088-remove-analyzer-qc-rule.xml"));

        Document migration = parse(REMOVAL_CHANGELOG);
        assertTrue(hasElement(migration, "dropTable", "tableName", "analyzer_qc_rule"));
        assertTrue(hasElement(migration, "tableExists", "tableName", "analyzer_qc_rule"));
        assertTrue(hasElement(migration, "createTable", "tableName", "analyzer_qc_rule"));
    }

    @Test
    public void operationalQcSpecificationsDoNotRestoreTheSupersededClassifier() throws Exception {
        Path specification = WESTGARD_SPEC_ROOT.resolve("spec.md");
        Path plan = WESTGARD_SPEC_ROOT.resolve("plan.md");
        Path tasks = WESTGARD_SPEC_ROOT.resolve("tasks.md");

        assertDoesNotContain(specification, "Admin Configures Per-Analyzer QC Rules");
        assertDoesNotContain(specification, "pulls the active rule set from OpenELIS");
        assertDoesNotContain(specification, "seeded from the analyzer's profile");
        assertDoesNotContain(plan, "Backend (analyzer QC rules)");
        assertDoesNotContain(plan, "AnalyzerQcRuleRestController");
        assertDoesNotContain(plan, "pulls qcRules from OE");
        assertDoesNotContain(tasks, "QC rules are populated from profiles");
    }

    private static void assertDoesNotContain(Path path, String text) throws Exception {
        assertTrue("missing source file: " + path, Files.isRegularFile(path));
        assertFalse(path + " contains " + text, Files.readString(path).contains(text));
    }

    private static Document parse(Path path) throws Exception {
        assertTrue("missing changelog: " + path, Files.isRegularFile(path));
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        return factory.newDocumentBuilder().parse(path.toFile());
    }

    private static boolean hasElement(Document document, String tagName, String attribute, String value) {
        NodeList elements = document.getElementsByTagName(tagName);
        for (int index = 0; index < elements.getLength(); index++) {
            Element element = (Element) elements.item(index);
            if (value.equals(element.getAttribute(attribute))) {
                return true;
            }
        }
        return false;
    }
}
