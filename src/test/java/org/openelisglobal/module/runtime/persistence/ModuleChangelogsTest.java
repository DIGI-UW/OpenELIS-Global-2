package org.openelisglobal.module.runtime.persistence;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.nio.file.Path;
import liquibase.changelog.ChangeLogParameters;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.parser.ChangeLogParserFactory;
import liquibase.resource.ResourceAccessor;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.openelisglobal.module.testkit.ModuleJarBuilder;

public class ModuleChangelogsTest {

    private static final String MANIFEST = "{\"schemaVersion\":1,\"id\":\"alpha\",\"kind\":\"module\",\"name\":\"Alpha\","
            + "\"version\":\"1.0.0\",\"requires\":{\"openelis\":\">=3.4.0\"}}";
    private static final String ROOT = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
            + "<databaseChangeLog xmlns=\"http://www.liquibase.org/xml/ns/dbchangelog\""
            + " xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\""
            + " xsi:schemaLocation=\"http://www.liquibase.org/xml/ns/dbchangelog"
            + " http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-4.8.xsd\">\n";
    private static final String BASE = ROOT + "  <include relativeToChangelogFile=\"true\" file=\"001-table.xml\"/>\n"
            + "  <include relativeToChangelogFile=\"true\" file=\"002-index.xml\"/>\n" + "</databaseChangeLog>\n";
    private static final String CHANGELOG = ROOT
            + "  <changeSet id=\"alpha-%s\" author=\"alpha\"><sql>select 1</sql></changeSet>\n"
            + "</databaseChangeLog>\n";

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private Path jar() throws Exception {
        return ModuleJarBuilder.module().manifest(MANIFEST).resource("liquibase/base.xml", BASE)
                .resource("liquibase/001-table.xml", String.format(CHANGELOG, "001"))
                .resource("liquibase/002-index.xml", String.format(CHANGELOG, "002"))
                .build(folder.newFolder().toPath().resolve("alpha.jar"));
    }

    @Test
    public void baseChangelogIncludesTheModuleFilesInOrderFromTheJar() throws Exception {
        ResourceAccessor accessor = ModuleChangelogs.accessor(jar());

        DatabaseChangeLog parsed = ChangeLogParserFactory.getInstance().getParser("liquibase/base.xml", accessor)
                .parse("liquibase/base.xml", new ChangeLogParameters(), accessor);

        assertEquals(2, parsed.getChangeSets().size());
        assertEquals("alpha-001", parsed.getChangeSets().get(0).getId());
        assertEquals("liquibase/001-table.xml", parsed.getChangeSets().get(0).getFilePath());
        assertEquals("alpha-002", parsed.getChangeSets().get(1).getId());
        assertEquals("liquibase/002-index.xml", parsed.getChangeSets().get(1).getFilePath());
    }

    @Test
    public void theAccessorSeesTheModuleJarAndNothingElse() throws Exception {
        ResourceAccessor accessor = ModuleChangelogs.accessor(jar());

        assertNotNull(getClass().getClassLoader().getResource("liquibase/base-changelog.xml"));
        assertTrue(accessor.openStreams(null, "liquibase/base-changelog.xml").isEmpty());
        assertFalse(accessor.openStreams(null, "liquibase/base.xml").isEmpty());
    }

    @Test
    public void existsAnswersForFilesInTheJarOnly() throws Exception {
        Path jar = jar();

        assertTrue(ModuleChangelogs.exists(jar, "liquibase/base.xml"));
        assertTrue(ModuleChangelogs.exists(jar, "liquibase/002-index.xml"));
        assertFalse(ModuleChangelogs.exists(jar, "liquibase/base-changelog.xml"));
        assertFalse(ModuleChangelogs.exists(jar, "liquibase"));
    }
}
