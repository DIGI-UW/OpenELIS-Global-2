package org.openelisglobal.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.Test;

/**
 * A bean whose class implements a {@code @PreAuthorize}-gated interface is
 * wrapped by method security in a <b>JDK proxy that implements the interface
 * only</b>. Injecting it by its concrete class therefore fails at boot:
 *
 * <pre>
 * BeanNotOfRequiredTypeException: Bean named 'configurationInitializationService'
 *   is expected to be of type 'ConfigurationInitializationService'
 *   but was actually of type 'jdk.proxy4.$Proxy1208'
 * </pre>
 *
 * <p>
 * That took the webapp container down in E2E on 2026-09-21 — every shard died
 * at "Start containers" and no spec ran — after develop's
 * {@code CatalogImportServiceImpl} autowired
 * {@code ConfigurationInitializationService} by class. Develop has no gates, so
 * no proxy, so it boots there; the failure exists only once the two are
 * combined, and the integration test context excludes both beans, so no Spring
 * test can see it. This source scan can, before the merge.
 *
 * <p>
 * Rule: inject a gated bean by its interface. The scan finds every class that
 * method security will JDK-proxy (implements a gated interface, or is itself
 * gated and implements any interface) and fails on any {@code @Autowired}/
 * {@code @Inject}/{@code @Resource} field, or {@code @Autowired}-constructor /
 * {@code @Bean}-method parameter, typed as that class.
 */
public class ProxiedBeanInjectionTest {

    private static final Path MAIN = Paths.get("src/main/java");
    private static final Pattern INTERFACE = Pattern.compile("public\\s+interface\\s+(\\w+)\\b");
    private static final Pattern CLASS_IMPLEMENTS = Pattern
            .compile("public\\s+(?:abstract\\s+)?class\\s+(\\w+)\\b[^{]*?implements\\s+([^{]+)\\{", Pattern.DOTALL);

    /**
     * Pure over a name→source map so the detection itself can be inverted below.
     */
    static List<String> violations(Map<String, String> sources) {
        Set<String> gatedInterfaces = new HashSet<>();
        for (Map.Entry<String, String> e : sources.entrySet()) {
            Matcher m = INTERFACE.matcher(e.getValue());
            if (e.getValue().contains("@PreAuthorize") && m.find() && m.group(1).equals(e.getKey())) {
                gatedInterfaces.add(e.getKey());
            }
        }
        Map<String, String> proxied = new HashMap<>(); // class → the interface that causes the proxy
        for (Map.Entry<String, String> e : sources.entrySet()) {
            Matcher m = CLASS_IMPLEMENTS.matcher(e.getValue());
            if (!m.find() || !m.group(1).equals(e.getKey())) {
                continue;
            }
            for (String raw : m.group(2).split(",")) {
                String iface = raw.trim().split("<")[0];
                if (gatedInterfaces.contains(iface) || e.getValue().contains("@PreAuthorize")) {
                    proxied.put(e.getKey(), iface);
                    break;
                }
            }
        }
        List<String> out = new ArrayList<>();
        if (proxied.isEmpty()) {
            return out;
        }
        String alt = String.join("|", proxied.keySet());
        Pattern field = Pattern.compile("@(?:Autowired|Inject|Resource)\\b[^;{}]*?\\b(" + alt + ")\\s+(\\w+)\\s*;",
                Pattern.DOTALL);
        Pattern param = Pattern.compile("\\b(" + alt + ")\\s+(\\w+)\\s*(?=[,)])");
        for (Map.Entry<String, String> e : sources.entrySet()) {
            String src = e.getValue();
            Matcher f = field.matcher(src);
            while (f.find()) {
                out.add(e.getKey() + " injects field '" + f.group(2) + "' as concrete " + f.group(1) + "; inject "
                        + proxied.get(f.group(1)) + " instead");
            }
            Matcher pm = param.matcher(src);
            while (pm.find()) {
                String head = src.substring(Math.max(0, pm.start() - 500), pm.start());
                String near = src.substring(Math.max(0, pm.start() - 120), pm.start());
                if (head.matches("(?s).*@(Autowired|Bean)\\b.*")
                        && !near.matches("(?s).*\\b(implements|extends)\\b.*")) {
                    out.add(e.getKey() + " takes parameter '" + pm.group(2) + "' as concrete " + pm.group(1)
                            + "; inject " + proxied.get(pm.group(1)) + " instead");
                }
            }
        }
        return out;
    }

    @Test
    public void detection_findsConcreteInjection_andAcceptsInterfaceInjection() {
        Map<String, String> tree = new HashMap<>();
        tree.put("GatedApi", "public interface GatedApi { @PreAuthorize(\"x\") void go(); }");
        tree.put("GatedImpl", "public class GatedImpl implements GatedApi { public void go() {} }");
        tree.put("BadField", "class BadField { @Autowired\n private GatedImpl svc; }");
        tree.put("BadCtor", "class BadCtor { @Autowired\n BadCtor(GatedImpl svc) {} }");
        tree.put("GoodField", "class GoodField { @Autowired\n private GatedApi svc; }");
        tree.put("Unrelated", "class Unrelated { @Autowired\n private PlainThing t; }");
        List<String> v = violations(tree);
        assertEquals(v.toString(), 2, v.size());
        assertTrue(v.get(0) + v.get(1), v.stream().anyMatch(s -> s.startsWith("BadField ")));
        assertTrue(v.get(0) + v.get(1), v.stream().anyMatch(s -> s.startsWith("BadCtor ")));
    }

    @Test
    public void noBeanIsInjectedByAClassThatMethodSecurityWillProxyAway() throws IOException {
        Map<String, String> sources = new HashMap<>();
        try (Stream<Path> files = Files.walk(MAIN)) {
            for (Path p : files.filter(f -> f.toString().endsWith(".java")).toList()) {
                String name = p.getFileName().toString();
                sources.put(name.substring(0, name.length() - ".java".length()), Files.readString(p));
            }
        }
        assertTrue("Indexed too few sources — scan would silently pass", sources.size() > 500);
        List<String> v = violations(sources);
        assertTrue("Gated beans are JDK-proxied and must be injected by interface, or the webapp fails at boot:\n"
                + String.join("\n", v), v.isEmpty());
    }
}
