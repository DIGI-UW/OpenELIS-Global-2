package org.openelisglobal.testsupport;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** Page-level assertions and optional retained output for visual inspection. */
public final class PdfRegression {
    private PdfRegression() {
    }

    public static void everyPage(byte[] pdf, String name, String... fields) throws IOException {
        int pages = PdfText.pageCount(pdf);
        assertTrue("scenario must exercise continuation pages", pages > 1);
        for (int page = 1; page <= pages; page++) {
            String text = PdfText.ofPage(pdf, page);
            for (String field : fields) {
                assertTrue(name + " page " + page + " lacks " + field + ": " + text, text.contains(field));
            }
        }
        save(pdf, name);
    }

    public static List<String> pages(byte[] pdf) throws IOException {
        List<String> pages = new ArrayList<>();
        int count = PdfText.pageCount(pdf);
        for (int page = 1; page <= count; page++) {
            pages.add(PdfText.ofPage(pdf, page));
        }
        return pages;
    }

    public static void samePage(List<String> pages, String marker, String identity) {
        Pattern mark = Pattern.compile("(?<!\\S)" + Pattern.quote(marker) + "(?=\\s|$)");
        Pattern owner = Pattern.compile("(?<!\\S)" + Pattern.quote(identity) + "(?=\\s|$)");
        int occurrences = 0;
        for (int page = 1; page <= pages.size(); page++) {
            String text = pages.get(page - 1);
            if (mark.matcher(text).find()) {
                occurrences++;
                org.junit.Assert.assertTrue(
                        "page " + page + " contains " + marker + " without " + identity + ": " + text,
                        owner.matcher(text).find());
            }
        }
        org.junit.Assert.assertEquals("marker must be present on exactly one page: " + marker, 1, occurrences);
    }

    public static void save(byte[] pdf, String name) throws IOException {
        String destination = System.getProperty("reporting.pdf.output");
        if (destination != null) {
            Path dir = Path.of(destination);
            Files.createDirectories(dir);
            Files.write(dir.resolve(name + ".pdf"), pdf);
        }
    }
}
