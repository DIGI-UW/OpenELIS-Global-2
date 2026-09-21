package org.openelisglobal.security;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.openelisglobal.common.constants.Privileges;
import org.openelisglobal.security.login.CustomUserDetailsService;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Authorities a real user of a seeded base role holds, derived from the
 * Liquibase role-privilege seed rather than hand-listed in each test.
 *
 * <p>
 * Under privilege-based RBAC a {@code ROLE_*} authority alone satisfies no
 * service gate; the gates check {@code PRIV_*}. A slice test that builds its
 * user with {@code user("x").roles("RESULTS")} therefore describes a user who
 * can reach nothing, and its 200-expectations fail at the first gated service —
 * not because the gate is wrong but because the fixture predates it. Building
 * the user from {@link #role(String)} instead gives it exactly what the seed
 * grants that role, so the test asserts the real policy: if it still gets a
 * 403, the seed does not grant what the screen needs, and that is a finding,
 * not a fixture bug.
 *
 * <p>
 * Names are mapped with the same functions production uses
 * ({@link CustomUserDetailsService#toRoleAuthority} / {@code toPrivAuthority}),
 * so {@code role("RESULTS")} and {@code role("Results")} are the same user.
 * Both seed row shapes are read: {@code r.name = 'X' AND p.name = 'y'} and
 * {@code p.name IN ('y', 'z')}. Global Admin is not a seeded mapping —
 * production grants it everything via the {@code "*"} sentinel — so
 * {@link #admin()} carries every privilege constant, exactly as the integration
 * super-user does.
 */
public final class SeededRoleAuthorities {

    static final Path SEED_DIR = Paths.get("src/main/resources/liquibase/3.5.x.x");

    private static final Pattern ROLE_TERM = Pattern.compile("r\\.name\\s*(?:=\\s*'([^']+)'|IN\\s*\\(([^)]*)\\))");
    private static final Pattern PRIV_TERM = Pattern.compile("p\\.name\\s*(?:=\\s*'([^']+)'|IN\\s*\\(([^)]*)\\))");
    private static final Pattern QUOTED = Pattern.compile("'([^']+)'");

    private static final Map<String, Set<String>> GRANTS = load();

    private SeededRoleAuthorities() {
    }

    /** Seed role name → privilege names granted to it. */
    public static Map<String, Set<String>> grants() {
        return Collections.unmodifiableMap(GRANTS);
    }

    /**
     * Authorities for a seeded base role, by seed name ({@code "Results"}) or
     * Spring role ({@code "RESULTS"}). {@code "ADMIN"} and {@code "GLOBAL_ADMIN"}
     * resolve to {@link #admin()}. Unknown names throw, listing what is known, so a
     * typo cannot silently produce a privilege-less user.
     */
    public static List<GrantedAuthority> role(String name) {
        String wanted = CustomUserDetailsService.toRoleAuthority(name);
        if (wanted.equals("ROLE_ADMIN") || wanted.equals("ROLE_GLOBAL_ADMIN")) {
            return admin();
        }
        for (Map.Entry<String, Set<String>> e : GRANTS.entrySet()) {
            if (CustomUserDetailsService.toRoleAuthority(e.getKey()).equals(wanted)) {
                List<GrantedAuthority> out = new ArrayList<>();
                out.add(new SimpleGrantedAuthority(wanted));
                for (String priv : e.getValue()) {
                    out.add(new SimpleGrantedAuthority(CustomUserDetailsService.toPrivAuthority(priv)));
                }
                return out;
            }
        }
        throw new IllegalArgumentException("No seeded role maps to " + wanted + "; known: " + GRANTS.keySet());
    }

    /**
     * Global Admin: {@code ROLE_ADMIN}, {@code ROLE_GLOBAL_ADMIN} and every
     * privilege.
     */
    public static List<GrantedAuthority> admin() {
        List<GrantedAuthority> out = new ArrayList<>();
        out.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        out.add(new SimpleGrantedAuthority("ROLE_GLOBAL_ADMIN"));
        for (String a : allPrivilegeAuthorityNames()) {
            out.add(new SimpleGrantedAuthority(a));
        }
        return out;
    }

    /**
     * Every {@code PRIV_*} authority, by reflection over {@link Privileges} so a
     * new constant can never silently be missing. Skips the Global Admin sentinel
     * ({@code "*"}), which is never a stored privilege name.
     */
    public static List<String> allPrivilegeAuthorityNames() {
        List<String> out = new ArrayList<>();
        for (Field field : Privileges.class.getDeclaredFields()) {
            if (!Modifier.isStatic(field.getModifiers()) || field.getType() != String.class) {
                continue;
            }
            try {
                String value = (String) field.get(null);
                if (value == null || !value.contains(":")) {
                    continue;
                }
                out.add(CustomUserDetailsService.toPrivAuthority(value));
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Could not read privilege constant " + field.getName(), e);
            }
        }
        return out;
    }

    private static Map<String, Set<String>> load() {
        Map<String, Set<String>> grants = new LinkedHashMap<>();
        List<Path> seeds;
        try (Stream<Path> files = Files.list(SEED_DIR)) {
            seeds = files.filter(f -> f.getFileName().toString().startsWith("012-004")).sorted().toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        if (seeds.isEmpty()) {
            throw new IllegalStateException("No 012-004* seed files under " + SEED_DIR);
        }
        for (Path seed : seeds) {
            String xml;
            try {
                xml = Files.readString(seed);
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            // One grant statement per SQL statement; the NOT EXISTS guards use bare
            // "name", not "p.name", so they do not register as grants.
            for (String stmt : xml.split(";")) {
                Set<String> roles = names(ROLE_TERM, stmt);
                Set<String> privs = names(PRIV_TERM, stmt);
                for (String r : roles) {
                    grants.computeIfAbsent(r, k -> new LinkedHashSet<>()).addAll(privs);
                }
            }
        }
        return grants;
    }

    private static Set<String> names(Pattern term, String stmt) {
        Set<String> out = new LinkedHashSet<>();
        Matcher m = term.matcher(stmt);
        while (m.find()) {
            if (m.group(1) != null) {
                out.add(m.group(1));
            } else {
                Matcher q = QUOTED.matcher(m.group(2));
                while (q.find()) {
                    out.add(q.group(1));
                }
            }
        }
        return out;
    }
}
