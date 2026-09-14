package org.openelisglobal.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.junit.Assert;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

/**
 * A WAR deploys twice when a Host's appBase scan ({@code autoDeploy} and
 * {@code deployOnStartup}, true while absent) covers a WAR an explicit
 * {@code <Context>} declares under another name, or when two of its contexts
 * resolve to one {@code docBase}: the second brings its own Spring root
 * context, so each {@code @Scheduled} method fires twice; two Hosts over one
 * WAR go unflagged.
 *
 * <p>
 * Tomcat 10.1's Host documentation says to "turn off automatic application
 * deployment or specify deployIgnore carefully" where server.xml defines
 * contexts, or "the web applications will each be deployed twice".
 */
public class ServerXmlSingleDeploymentTest {

    @Test
    public void hostsDeclaringAnInAppBaseContextDoNotAlsoScanAppBase() throws Exception {
        List<String> violations = new ArrayList<>();
        for (Path config : serverXmlConfigs()) {
            for (Element host : elementsNamed(parse(config), "Host")) {
                String appBase = appBaseOf(host);
                long inAppBase = childElements(host, "Context").stream()
                        .filter(context -> scanWouldDuplicate(context, appBase)).count();
                if (inAppBase == 0) {
                    continue;
                }
                for (String attribute : List.of("autoDeploy", "deployOnStartup")) {
                    // An absent deployOnStartup defaults to true and scans appBase anyway.
                    if (!(host.hasAttribute(attribute) && !Boolean.parseBoolean(host.getAttribute(attribute)))) {
                        violations.add(describe(config, host) + " declares " + inAppBase
                                + " in-appBase <Context> element(s) but " + attribute + " is "
                                + (host.hasAttribute(attribute) ? "\"" + host.getAttribute(attribute) + "\"" : "absent")
                                + " rather than \"false\", so that WAR deploys twice");
                    }
                }
            }
        }
        Assert.assertEquals(String.join("\n", violations), List.of(), violations);
    }

    @Test
    public void noTwoContextsOfOneHostShareADocBaseOrAPath() throws Exception {
        List<String> violations = new ArrayList<>();
        for (Path config : serverXmlConfigs()) {
            for (Element host : elementsNamed(parse(config), "Host")) {
                String appBase = appBaseOf(host);
                violations.addAll(repeatedAttribute(config, host, "docBase", context -> docBaseKey(context, appBase),
                        "so that WAR deploys twice"));
                violations.addAll(
                        repeatedAttribute(config, host, "path", context -> baseName(context.getAttribute("path")),
                                "so two web applications claim one context path"));
            }
        }
        Assert.assertEquals(String.join("\n", violations), List.of(), violations);
    }

    private static List<String> repeatedAttribute(Path config, Element host, String attribute,
            Function<Element, String> key, String consequence) {
        List<String> violations = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (Element context : childElements(host, "Context")) {
            String resolved = key.apply(context);
            if (!seen.add(resolved)) {
                violations.add(describe(config, host) + " declares more than one <Context> whose " + attribute
                        + " resolves to \"" + resolved + "\", " + consequence);
            }
        }
        return violations;
    }

    /**
     * {@code webapps}, the documented default, for both an absent {@code appBase}
     * and an empty one, which {@code getAttribute} reports alike.
     */
    private static String appBaseOf(Element host) {
        String appBase = host.getAttribute("appBase");
        return appBase.isEmpty() ? "webapps" : appBase;
    }

    /**
     * Spellings of one deployable collapse to one key; {@code .war} drops because
     * Tomcat appends it when the directory a {@code docBase} names is absent.
     */
    private static String docBaseKey(Element context, String appBase) {
        List<String> doc = resolvedDocBase(context, appBase);
        List<String> tail = new ArrayList<>(
                doc.subList(Math.max(endOfAppBase(doc, resolve(segments(appBase))), 0), doc.size()));
        if (!tail.isEmpty() && tail.get(tail.size() - 1).endsWith(".war")) {
            String name = tail.get(tail.size() - 1);
            tail.set(tail.size() - 1, name.substring(0, name.length() - ".war".length()));
        }
        return String.join("/", tail);
    }

    /**
     * What {@code ContextConfig#fixDocBase} resolves: a relative {@code docBase}
     * from inside {@code appBase}, and an absent one from {@code path}.
     */
    private static List<String> resolvedDocBase(Element context, String appBase) {
        String docBase = context.getAttribute("docBase");
        if (docBase.isEmpty()) {
            docBase = baseName(context.getAttribute("path"));
        }
        List<String> joined = new ArrayList<>();
        if (!docBase.startsWith("/")) {
            joined.addAll(segments(appBase));
        }
        joined.addAll(segments(docBase));
        return resolve(joined);
    }

    /** {@code ContextName#getBaseName}: {@code /a/b/} names the WAR {@code a#b}. */
    private static String baseName(String path) {
        List<String> segments = segments(path);
        return segments.isEmpty() ? "ROOT" : String.join("#", segments);
    }

    private static String describe(Path config, Element host) {
        return config + ": <Host name=\"" + host.getAttribute("name") + "\" appBase=\"" + appBaseOf(host) + "\">";
    }

    /**
     * Whether the appBase scan deploys this docBase a second time; it does not when
     * the declared name matches it, per {@code HostConfig.deploymentExists}.
     */
    private static boolean scanWouldDuplicate(Element context, String appBase) {
        return resolvesInside(context, appBase)
                && !docBaseKey(context, appBase).equals(baseName(context.getAttribute("path")));
    }

    /**
     * Whether the appBase scan reaches this {@code <Context>} too; a placeholder
     * {@code appBase} and one of no segments are read as containing the docBase.
     */
    private static boolean resolvesInside(Element context, String appBase) {
        if (appBase.contains("${")) {
            return true;
        }
        List<String> base = resolve(segments(appBase));
        return base.isEmpty() || endOfAppBase(resolvedDocBase(context, appBase), base) >= 0;
    }

    /**
     * How far into {@code doc} the deepest {@code base} match reaches, or -1;
     * deepest, so {@code webapps/../webapps/App} keys as {@code App} does.
     */
    private static int endOfAppBase(List<String> doc, List<String> base) {
        int end = -1;
        for (int i = 0; !base.isEmpty() && i + base.size() < doc.size(); i++) {
            if (doc.subList(i, i + base.size()).equals(base)) {
                end = i + base.size();
            }
        }
        return end;
    }

    /**
     * A {@code ..} drops the segment it follows; a leftover one had none to drop.
     */
    private static List<String> resolve(List<String> segments) {
        List<String> resolved = new ArrayList<>();
        for (String segment : segments) {
            String last = resolved.isEmpty() ? null : resolved.get(resolved.size() - 1);
            if ("..".equals(segment) && last != null && !"..".equals(last)) {
                resolved.remove(resolved.size() - 1);
            } else {
                resolved.add(segment);
            }
        }
        return resolved;
    }

    private static List<String> segments(String path) {
        List<String> segments = new ArrayList<>();
        for (String segment : Arrays.asList(path.split("/"))) {
            if (!segment.isEmpty() && !".".equals(segment)) {
                segments.add(segment);
            }
        }
        return segments;
    }

    private static List<Path> serverXmlConfigs() throws IOException, InterruptedException {
        List<Path> configs = trackedFilesMatching("server.xml");
        Assert.assertFalse("found no tracked Tomcat server.xml to check; the search itself is broken",
                configs.isEmpty());
        return configs;
    }

    /**
     * Git's index rather than the filesystem: an untracked stray server.xml in
     * somebody's checkout is not this repo's configuration.
     */
    private static List<Path> trackedFilesMatching(String nameSuffix) throws IOException, InterruptedException {
        Process git = new ProcessBuilder("git", "ls-files", "-z").redirectErrorStream(true).start();
        String listing = new String(git.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        Assert.assertEquals("git ls-files failed: " + listing, 0, git.waitFor());
        List<Path> matches = new ArrayList<>();
        for (String entry : listing.split("\0")) {
            if (!entry.isEmpty() && Path.of(entry).getFileName().toString().endsWith(nameSuffix)) {
                matches.add(Path.of(entry));
            }
        }
        matches.sort(null);
        return matches;
    }

    private static Document parse(Path path) throws ParserConfigurationException, SAXException, IOException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);
        return factory.newDocumentBuilder().parse(path.toFile());
    }

    private static List<Element> elementsNamed(Document document, String tagName) {
        List<Element> matches = new ArrayList<>();
        NodeList found = document.getElementsByTagName(tagName);
        for (int i = 0; i < found.getLength(); i++) {
            matches.add((Element) found.item(i));
        }
        return matches;
    }

    private static List<Element> childElements(Element parent, String tagName) {
        List<Element> matches = new ArrayList<>();
        NodeList children = parent.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.ELEMENT_NODE && tagName.equals(child.getNodeName())) {
                matches.add((Element) child);
            }
        }
        return matches;
    }
}
