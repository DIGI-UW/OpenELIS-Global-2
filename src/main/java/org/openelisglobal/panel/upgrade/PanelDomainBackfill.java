package org.openelisglobal.panel.upgrade;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;
import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;
import org.openelisglobal.common.domain.Domain;
import org.openelisglobal.common.log.LogEvent;

/**
 * OGC-1599: changeset 074 gave every existing panel the CLINICAL default and
 * nothing set it again until OGC-1209, so a database upgraded from before that
 * fix files its vector and environmental panels under Clinical. This runs once
 * from the changelog and moves a panel still on CLINICAL to the one domain all
 * its member sample types belong to, when its member tests are in that domain
 * too: the panel save guard refuses a domain the member tests do not share, so
 * any other result would leave the panel uneditable.
 * <p>
 * A panel whose sample types span several domains, that has no sample types, or
 * whose tests are in another domain than its sample types keeps its domain and
 * is named in the log with the reason. A domain other than CLINICAL was set on
 * purpose and is never changed, so running this again changes nothing.
 */
public class PanelDomainBackfill implements CustomTaskChange {

    private static final String PANELS = "SELECT id, name FROM clinlims.panel WHERE domain = 'CLINICAL' ORDER BY id";

    private static final String MEMBERS = "SELECT pi.panel_id, t.domain AS test_domain, ts.id AS type_id,"
            + " ts.domain AS type_domain FROM clinlims.panel_item pi"
            + " JOIN clinlims.panel p ON p.id = pi.panel_id AND p.domain = 'CLINICAL'"
            + " JOIN clinlims.test t ON t.id = pi.test_id"
            + " LEFT JOIN clinlims.sampletype_test st ON st.test_id = t.id"
            + " LEFT JOIN clinlims.type_of_sample ts ON ts.id = st.sample_type_id";

    private static final String MOVE = "UPDATE clinlims.panel SET domain = ?, lastupdated = now()"
            + " WHERE id = ? AND domain = 'CLINICAL'";

    /** A panel left on its domain, and why. */
    public record Kept(String panel, String reason) {
    }

    /** Panels moved, by name with their new domain, and panels kept. */
    public record Outcome(Map<String, String> moved, List<Kept> kept) {
    }

    private Outcome outcome;

    @Override
    public void execute(Database database) throws CustomChangeException {
        try {
            outcome = apply(((JdbcConnection) database.getConnection()).getUnderlyingConnection());
        } catch (SQLException e) {
            throw new CustomChangeException("Could not re-derive panel domains", e);
        }
        outcome.moved().forEach((panel, domain) -> LogEvent.logInfo(getClass().getSimpleName(), "execute",
                "Panel '" + panel + "' is now filed under " + domain + ", the domain of its member sample types."));
        outcome.kept().forEach(kept -> LogEvent.logWarn(getClass().getSimpleName(), "execute", "Panel '" + kept.panel()
                + "' keeps CLINICAL: " + kept.reason() + ". Set its domain in the Panel editor."));
    }

    /** Re-derives the domains on this connection and says what it did. */
    public static Outcome apply(Connection connection) throws SQLException {
        Map<Long, String> panels = new LinkedHashMap<>();
        try (PreparedStatement statement = connection.prepareStatement(PANELS);
                ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                panels.put(rows.getLong("id"), rows.getString("name"));
            }
        }
        Map<Long, Set<Domain>> testDomains = new TreeMap<>();
        Map<Long, Set<Domain>> typeDomains = new TreeMap<>();
        try (PreparedStatement statement = connection.prepareStatement(MEMBERS);
                ResultSet rows = statement.executeQuery()) {
            while (rows.next()) {
                long panel = rows.getLong("panel_id");
                testDomains.computeIfAbsent(panel, id -> EnumSet.noneOf(Domain.class))
                        .add(Domain.valueOf(Domain.normalize(rows.getString("test_domain"))));
                rows.getLong("type_id");
                if (rows.wasNull()) {
                    continue;
                }
                Domain type = Domain.fromRaw(rows.getString("type_domain"));
                if (type != null) {
                    typeDomains.computeIfAbsent(panel, id -> EnumSet.noneOf(Domain.class)).add(type);
                }
            }
        }

        Map<String, String> moved = new LinkedHashMap<>();
        List<Kept> kept = new ArrayList<>();
        try (PreparedStatement move = connection.prepareStatement(MOVE)) {
            for (Map.Entry<Long, String> panel : panels.entrySet()) {
                Set<Domain> types = typeDomains.getOrDefault(panel.getKey(), Set.of());
                Set<Domain> tests = testDomains.getOrDefault(panel.getKey(), Set.of());
                if (tests.isEmpty()) {
                    kept.add(new Kept(panel.getValue(), "it has no tests, so no sample types"));
                } else if (types.isEmpty()) {
                    kept.add(new Kept(panel.getValue(), "its member tests have no sample types with a domain"));
                } else if (types.size() > 1) {
                    kept.add(new Kept(panel.getValue(), "its sample types are in several domains " + names(types)));
                } else {
                    Domain domain = types.iterator().next();
                    if (!tests.equals(Set.of(domain))) {
                        kept.add(new Kept(panel.getValue(),
                                "its sample types are " + domain + " but its member tests are " + names(tests)));
                        continue;
                    }
                    if (domain == Domain.CLINICAL) {
                        continue;
                    }
                    move.setString(1, domain.name());
                    move.setLong(2, panel.getKey());
                    if (move.executeUpdate() == 1) {
                        moved.put(panel.getValue(), domain.name());
                    }
                }
            }
        }
        return new Outcome(moved, kept);
    }

    private static String names(Set<Domain> domains) {
        return domains.stream().map(Domain::name).collect(Collectors.joining(", ", "(", ")"));
    }

    @Override
    public String getConfirmationMessage() {
        return outcome == null ? "Panel domains not re-derived"
                : "Panels filed under the domain of their sample types: " + outcome.moved().size()
                        + "; panels kept on CLINICAL: " + outcome.kept().size();
    }

    @Override
    public void setUp() {
    }

    @Override
    public void setFileOpener(ResourceAccessor resourceAccessor) {
    }

    @Override
    public ValidationErrors validate(Database database) {
        return new ValidationErrors();
    }
}
