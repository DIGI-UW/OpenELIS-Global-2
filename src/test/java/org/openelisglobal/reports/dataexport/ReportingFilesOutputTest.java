package org.openelisglobal.reports.dataexport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeFalse;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.UUID;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.openelisglobal.reports.dataexport.service.ReportingException;
import org.openelisglobal.reports.dataexport.service.ReportingFiles;
import org.openelisglobal.reports.dataexport.service.ReportingSettings;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * OGC-1266: an export directory the application could not write (a volume
 * created root-owned) failed every Custom Data Export as a generic "could not
 * be generated", with nothing in the log.
 */
public class ReportingFilesOutputTest {

    private Path root;
    private ReportingFiles files;

    @Before
    public void setUp() throws Exception {
        assumeFalse("root can write to any directory", "root".equals(System.getProperty("user.name")));
        root = Files.createTempDirectory("reporting-output");
        ReportingSettings settings = new ReportingSettings();
        ReflectionTestUtils.setField(settings, "directory", root.resolve("reporting").toString());
        files = new ReportingFiles();
        ReflectionTestUtils.setField(files, "settings", settings);
    }

    @After
    public void tearDown() throws Exception {
        if (root != null) {
            Files.setPosixFilePermissions(root, PosixFilePermissions.fromString("rwx------"));
            try (var paths = Files.walk(root)) {
                paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> path.toFile().delete());
            }
        }
    }

    @Test
    public void aWritableDirectoryStagesTheJob() throws Exception {
        assertTrue(files.outputWritable());
        assertTrue(Files.exists(files.stage(UUID.randomUUID().toString(), UUID.randomUUID().toString())));
    }

    @Test
    public void anUnwritableDirectoryFailsTheJobWithItsOwnReason() throws Exception {
        Files.setPosixFilePermissions(root, PosixFilePermissions.fromString("r-x------"));

        assertFalse(files.outputWritable());
        try {
            files.stage(UUID.randomUUID().toString(), UUID.randomUUID().toString());
            fail("staging into an unwritable directory must fail");
        } catch (ReportingException error) {
            assertEquals(ReportingFiles.OUTPUT_UNAVAILABLE, error.getMessage());
        }
    }
}
