package org.openelisglobal.liquibase;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import liquibase.changelog.ChangeLogParameters;
import liquibase.changelog.ChangeSet;
import liquibase.changelog.DatabaseChangeLog;
import liquibase.parser.ChangeLogParser;
import liquibase.parser.ChangeLogParserFactory;
import liquibase.resource.ClassLoaderResourceAccessor;
import liquibase.resource.FileSystemResourceAccessor;
import org.junit.Test;

/**
 * Validates that the full application changelog and the 3.5.x.x version
 * changelog parse cleanly, that includeAll for changes/ is properly wired, and
 * that changelogs in changes/ execute in alphabetical order.
 */
public class LiquibaseMigrationConfigurationTest {

    private static final String ROOT_CHANGELOG = "liquibase/base-changelog.xml";
    private static final String VERSION_3_5_CHANGELOG = "liquibase/3.5.x.x/base.xml";

    @Test
    public void rootChangelogParsesWithoutErrors() throws Exception {
        ClassLoaderResourceAccessor resourceAccessor = new ClassLoaderResourceAccessor();
        ChangeLogParser parser = ChangeLogParserFactory.getInstance().getParser(ROOT_CHANGELOG, resourceAccessor);
        assertNotNull("Parser should exist for root changelog", parser);

        DatabaseChangeLog changeLog = parser.parse(ROOT_CHANGELOG, new ChangeLogParameters(), resourceAccessor);
        assertNotNull("Parsed root changelog should not be null", changeLog);

        List<ChangeSet> changeSets = changeLog.getChangeSets();
        assertFalse("Root changelog should contain changesets", changeSets.isEmpty());

        Set<String> seenChangeSetKeys = new HashSet<>();
        for (ChangeSet changeSet : changeSets) {
            String key = changeSet.getId() + ":" + changeSet.getAuthor() + ":" + changeSet.getFilePath();
            boolean isUnique = seenChangeSetKeys.add(key);
            assertTrue("Duplicate changeset key found in root changelog: " + key, isUnique);
        }
    }

    @Test
    public void version35ChangelogIncludesChangesDirectory() throws Exception {
        ClassLoaderResourceAccessor resourceAccessor = new ClassLoaderResourceAccessor();
        ChangeLogParser parser = ChangeLogParserFactory.getInstance().getParser(VERSION_3_5_CHANGELOG,
                resourceAccessor);
        assertNotNull("Parser should exist for 3.5.x.x changelog", parser);

        DatabaseChangeLog changeLog = parser.parse(VERSION_3_5_CHANGELOG, new ChangeLogParameters(), resourceAccessor);
        assertNotNull("Parsed 3.5.x.x changelog should not be null", changeLog);

        List<ChangeSet> changeSets = changeLog.getChangeSets();
        assertFalse("3.5.x.x changelog should contain changesets", changeSets.isEmpty());
    }

    @Test
    public void includeAllLoadsFilesAlphabetically() throws Exception {
        File tempDir = Files.createTempDirectory("lb-include-all").toFile();
        tempDir.deleteOnExit();

        File masterFile = new File(tempDir, "master.xml");
        File changesDir = new File(tempDir, "changes");
        changesDir.mkdirs();

        File fileB = new File(changesDir, "20261003T1200-OGC-100-second.xml");
        File fileA = new File(changesDir, "20261003T1100-OGC-100-first.xml");

        String csTemplate = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<databaseChangeLog\n"
                + "    xmlns=\"http://www.liquibase.org/xml/ns/dbchangelog\"\n"
                + "    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                + "    xsi:schemaLocation=\"http://www.liquibase.org/xml/ns/dbchangelog\n"
                + "    http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-3.8.xsd\"\n"
                + "    logicalFilePath=\"%s\">\n" + "  <changeSet id=\"%s\" author=\"tester\">\n"
                + "    <comment>Test change</comment>\n" + "  </changeSet>\n" + "</databaseChangeLog>";

        Files.writeString(fileB.toPath(),
                String.format(csTemplate, "20261003T1200-OGC-100-second", "20261003T1200-OGC-100-second"));
        Files.writeString(fileA.toPath(),
                String.format(csTemplate, "20261003T1100-OGC-100-first", "20261003T1100-OGC-100-first"));

        String masterContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" + "<databaseChangeLog\n"
                + "    xmlns=\"http://www.liquibase.org/xml/ns/dbchangelog\"\n"
                + "    xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\n"
                + "    xsi:schemaLocation=\"http://www.liquibase.org/xml/ns/dbchangelog\n"
                + "    http://www.liquibase.org/xml/ns/dbchangelog/dbchangelog-3.8.xsd\">\n"
                + "  <includeAll path=\"changes/\" relativeToChangelogFile=\"true\" errorIfMissingOrEmpty=\"false\"/>\n"
                + "</databaseChangeLog>";
        Files.writeString(masterFile.toPath(), masterContent);

        liquibase.resource.CompositeResourceAccessor accessor = new liquibase.resource.CompositeResourceAccessor(
                new ClassLoaderResourceAccessor(), new FileSystemResourceAccessor(tempDir));
        ChangeLogParser parser = ChangeLogParserFactory.getInstance().getParser("master.xml", accessor);
        DatabaseChangeLog changeLog = parser.parse("master.xml", new ChangeLogParameters(), accessor);

        List<ChangeSet> changeSets = changeLog.getChangeSets();
        assertEquals(2, changeSets.size());
        assertEquals("20261003T1100-OGC-100-first", changeSets.get(0).getId());
        assertEquals("20261003T1200-OGC-100-second", changeSets.get(1).getId());
    }
}
