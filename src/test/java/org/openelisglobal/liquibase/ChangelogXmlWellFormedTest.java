package org.openelisglobal.liquibase;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.Test;
import org.xml.sax.SAXParseException;

/**
 * Every Liquibase changelog must be well-formed XML.
 *
 * <p>
 * This looks trivial until it happens: a bare {@code &} in a changelog — even
 * inside an SQL comment, where it reads as ordinary prose — makes the SAX
 * parser reject the file. Liquibase runs during Spring context initialization,
 * so the failure does not surface as a migration warning. It fails the first
 * DAO bean and cascades until the whole context dies, while Tomcat still logs a
 * successful "Server startup in [...] milliseconds" and the application answers
 * every request with a redirect to HTML. Nothing else catches it: not the
 * build, not spotless, not any test that does not boot the context.
 *
 * <p>
 * Parsing costs milliseconds and needs no database, so it runs with the unit
 * tests rather than alongside the migration integration test.
 */
public class ChangelogXmlWellFormedTest {

    private static final Path CHANGELOG_ROOT = Paths.get("src/main/resources/liquibase");

    @Test
    public void everyChangelogIsWellFormedXml() throws Exception {
        List<Path> changelogs = listChangelogs();
        assertTrue("expected to find changelogs under " + CHANGELOG_ROOT, changelogs.size() > 0);

        StringBuilder failures = new StringBuilder();
        for (Path changelog : changelogs) {
            try {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                // The changelogs reference a remote DTD/XSD; resolving it would make
                // this test depend on the network, and it is not what we are checking.
                factory.setNamespaceAware(true);
                factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
                factory.newDocumentBuilder().parse(changelog.toFile());
            } catch (SAXParseException e) {
                failures.append(String.format("%n  %s (line %d, column %d): %s", changelog, e.getLineNumber(),
                        e.getColumnNumber(), e.getMessage()));
            }
        }

        if (failures.length() > 0) {
            fail("Malformed changelog XML — the application will not start with these:" + failures);
        }
    }

    private List<Path> listChangelogs() throws Exception {
        if (!Files.isDirectory(CHANGELOG_ROOT)) {
            return List.of();
        }
        try (Stream<Path> paths = Files.walk(CHANGELOG_ROOT)) {
            return paths.filter(Files::isRegularFile).filter(p -> p.getFileName().toString().endsWith(".xml")).sorted()
                    .collect(Collectors.toList());
        }
    }

    /**
     * Inversion check: the parser this test relies on must actually reject a bare
     * ampersand in element text. Without this, a misconfigured factory would make
     * the test above pass on a file that takes the application down.
     *
     * <p>
     * Writing this probe caught a wrong assumption: a bare {@code &} inside an XML
     * <em>comment</em> parses without complaint. The defect was in element text — a
     * SQL comment inside a {@code <sql>} block — so the probe has to use that shape
     * or it proves nothing.
     */
    @Test
    public void bareAmpersandIsRejected() throws Exception {
        File temp = File.createTempFile("changelog-bare-ampersand", ".xml");
        temp.deleteOnExit();
        // Note this is element TEXT, not an XML comment: the ampersand that took
        // the application down sat in a SQL comment inside a <sql> element, which
        // the parser reads as character data. A bare '&' inside an XML comment
        // parses fine, so probing with one would prove nothing.
        Files.writeString(temp.toPath(), "<?xml version=\"1.0\"?><root><sql>-- Label & Store</sql></root>");

        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            factory.newDocumentBuilder().parse(temp);
            fail("a bare '&' should not parse — this test cannot catch the real defect");
        } catch (SAXParseException expected) {
            assertTrue("expected an entity-reference complaint, got: " + expected.getMessage(),
                    expected.getMessage().contains("entity"));
        }
    }
}
