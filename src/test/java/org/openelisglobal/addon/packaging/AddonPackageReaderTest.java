package org.openelisglobal.addon.packaging;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.Test;
import org.openelisglobal.addon.AddonTestPackages;

public class AddonPackageReaderTest {

    private static String code(byte[] zip) {
        return assertThrows(AddonPackageException.class, () -> AddonPackageReader.read(zip)).getCode();
    }

    private static byte[] rawZip(String... namesAndContents) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            for (int i = 0; i < namesAndContents.length; i += 2) {
                zip.putNextEntry(new ZipEntry(namesAndContents[i]));
                zip.write(namesAndContents[i + 1].getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        }
        return bytes.toByteArray();
    }

    @Test
    public void readsEveryFile() {
        Map<String, byte[]> files = new LinkedHashMap<>();
        files.put("addon.json", "{}".getBytes(StandardCharsets.UTF_8));
        files.put("ui/a/b.js", "x".getBytes(StandardCharsets.UTF_8));
        Map<String, byte[]> read = AddonPackageReader.read(AddonTestPackages.zip(files));
        assertEquals(2, read.size());
        assertArrayEquals("x".getBytes(StandardCharsets.UTF_8), read.get("ui/a/b.js"));
    }

    @Test
    public void refusesPathsThatLeaveThePackage() throws IOException {
        assertEquals("package.badPath", code(rawZip("../evil.sh", "x")));
        assertEquals("package.badPath", code(rawZip("ui/../../evil.sh", "x")));
        assertEquals("package.badPath", code(rawZip("/etc/passwd", "x")));
        assertEquals("package.badPath", code(rawZip("ui\\evil.js", "x")));
    }

    @Test
    public void refusesAFileThatInflatesBeyondTheLimit() throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            zip.putNextEntry(new ZipEntry("ui/zeros.txt"));
            byte[] chunk = new byte[1024 * 1024];
            for (int i = 0; i < 6; i++) {
                zip.write(chunk);
            }
            zip.closeEntry();
        }
        // a few kilobytes on the wire, 6 MB inflated
        assertEquals("package.tooLarge", code(bytes.toByteArray()));
    }

    @Test
    public void refusesWhatIsNotAZip() {
        assertEquals("package.notZip", code("not a zip".getBytes(StandardCharsets.UTF_8)));
        assertEquals("package.empty", code(new byte[0]));
    }
}
