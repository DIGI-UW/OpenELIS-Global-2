package org.openelisglobal.common.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import org.junit.Test;

/**
 * OGC-1420 (1b): files saved by Excel import as typed. "CSV UTF-8" starts with
 * a byte order mark, which must not become part of the first column name; plain
 * "CSV" is Windows-1252, whose accented letters must not turn into U+FFFD.
 */
public class CsvParsingUtilReaderTest {

    private static final String TEXT = "type,name\nHealth Centre,Clinique Sainte-Thérèse\n";

    private static String read(byte[] bytes) throws Exception {
        StringBuilder text = new StringBuilder();
        try (BufferedReader reader = CsvParsingUtil.openCsvReader(new ByteArrayInputStream(bytes))) {
            String line;
            while ((line = reader.readLine()) != null) {
                text.append(line).append('\n');
            }
        }
        return text.toString();
    }

    @Test
    public void aUtf8FileWithAByteOrderMarkReadsWithoutTheMark() throws Exception {
        byte[] body = TEXT.getBytes(StandardCharsets.UTF_8);
        byte[] withBom = new byte[body.length + 3];
        withBom[0] = (byte) 0xEF;
        withBom[1] = (byte) 0xBB;
        withBom[2] = (byte) 0xBF;
        System.arraycopy(body, 0, withBom, 3, body.length);

        assertEquals(TEXT, read(withBom));
    }

    @Test
    public void aWindows1252FileKeepsItsAccents() throws Exception {
        assertEquals(TEXT, read(TEXT.getBytes(Charset.forName("windows-1252"))));
    }

    @Test
    public void aUtf8FileWithoutAMarkReadsAsUtf8() throws Exception {
        assertEquals(TEXT, read(TEXT.getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    public void anEmptyFileHasNoLines() throws Exception {
        try (BufferedReader reader = CsvParsingUtil.openCsvReader(new ByteArrayInputStream(new byte[0]))) {
            assertNull(reader.readLine());
        }
    }
}
