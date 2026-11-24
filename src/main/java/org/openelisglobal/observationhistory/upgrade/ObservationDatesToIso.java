package org.openelisglobal.observationhistory.upgrade;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;
import liquibase.change.custom.CustomTaskChange;
import liquibase.database.Database;
import liquibase.database.jvm.JdbcConnection;
import liquibase.exception.CustomChangeException;
import liquibase.exception.ValidationErrors;
import liquibase.resource.ResourceAccessor;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.util.IsoDateNormalizer;

/**
 * OGC-1669: an order's request date and next visit date were kept as text in
 * the site's date format of the day they were saved, and read in the format of
 * the day they are read, so after a site changed its format older orders could
 * not be saved and reports swapped day and month. They are now kept as ISO
 * dates. This runs once from the changelog and rewrites each value that reads
 * strictly in the site's current date format as an ISO date. A value already
 * ISO is left alone, and one that reads in neither is left and named in the
 * log.
 */
public class ObservationDatesToIso implements CustomTaskChange {

    private static final String LOCALE = "SELECT value FROM clinlims.site_information"
            + " WHERE name = 'default date locale'";

    private static final String VALUES = "SELECT oh.id, oh.value FROM clinlims.observation_history oh"
            + " JOIN clinlims.observation_history_type oht ON oht.id = oh.observation_history_type_id"
            + " WHERE oht.type_name IN ('requestDate', 'nextVisitDate') AND oh.value IS NOT NULL"
            + " AND oh.value <> '' ORDER BY oh.id";

    private static final String UPDATE = "UPDATE clinlims.observation_history SET value = ? WHERE id = ?";

    /**
     * Values rewritten as ISO dates, values already ISO, and values left unread.
     */
    public record Outcome(int converted, int alreadyIso, List<String> unreadable) {
    }

    private Outcome outcome;

    @Override
    public void execute(Database database) throws CustomChangeException {
        try {
            outcome = apply(((JdbcConnection) database.getConnection()).getUnderlyingConnection());
        } catch (SQLException e) {
            throw new CustomChangeException("Could not store order dates as ISO dates", e);
        }
        if (!outcome.unreadable().isEmpty()) {
            LogEvent.logWarn(getClass().getSimpleName(), "execute",
                    "Order dates left as they were, not readable in the site's date format: "
                            + String.join(", ", outcome.unreadable()));
        }
    }

    /** Rewrites the dates on this connection and says what it did. */
    public static Outcome apply(Connection connection) throws SQLException {
        return apply(connection, siteLocale(connection));
    }

    /** The same, reading the stored dates in this locale's date format. */
    public static Outcome apply(Connection connection, Locale locale) throws SQLException {
        String pattern = displayPattern(locale);
        String other = otherPattern(pattern);
        int converted = 0;
        int alreadyIso = 0;
        List<String> unreadable = new ArrayList<>();
        try (PreparedStatement select = connection.prepareStatement(VALUES);
                PreparedStatement update = connection.prepareStatement(UPDATE);
                ResultSet rows = select.executeQuery()) {
            while (rows.next()) {
                String value = rows.getString("value").trim();
                if (value.matches("\\d{4}-\\d{2}-\\d{2}")) {
                    alreadyIso++;
                    continue;
                }
                LocalDate date = IsoDateNormalizer.toLocalDate(value, pattern);
                if (date == null && other != null) {
                    date = IsoDateNormalizer.toLocalDate(value, other);
                }
                if (date == null) {
                    unreadable.add("observation " + rows.getLong("id") + " '" + value + "'");
                    continue;
                }
                update.setString(1, date.toString());
                update.setLong(2, rows.getLong("id"));
                update.executeUpdate();
                converted++;
            }
        }
        return new Outcome(converted, alreadyIso, unreadable);
    }

    /**
     * The other of the two day and month orders the shipped formats use. A site
     * that changed its format before this upgrade holds older dates in the other
     * order; a value that reads only that way is unambiguous, and one that reads
     * both ways is taken in the site's own order.
     */
    static String otherPattern(String pattern) {
        if ("MM/dd/yyyy".equals(pattern)) {
            return "dd/MM/yyyy";
        }
        if ("dd/MM/yyyy".equals(pattern)) {
            return "MM/dd/yyyy";
        }
        return null;
    }

    private static Locale siteLocale(Connection connection) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(LOCALE);
                ResultSet rows = statement.executeQuery()) {
            String tag = rows.next() ? rows.getString("value") : null;
            return Locale.forLanguageTag(tag == null || tag.isBlank() ? "fr-FR" : tag.trim());
        }
    }

    /**
     * The site's date pattern as the interface reads it, from its language's
     * message file, falling back to English as the message source does.
     */
    static String displayPattern(Locale locale) {
        String pattern = patternIn("languages/message_" + locale.getLanguage() + ".properties");
        return pattern != null ? pattern : patternIn("languages/message_en.properties");
    }

    private static String patternIn(String resource) {
        try (InputStream in = ObservationDatesToIso.class.getClassLoader().getResourceAsStream(resource)) {
            if (in == null) {
                return null;
            }
            Properties messages = new Properties();
            messages.load(in);
            String pattern = messages.getProperty("date.format.formatKey");
            return pattern == null || pattern.isBlank() ? null : pattern.trim();
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public String getConfirmationMessage() {
        return outcome == null ? "Order dates not rewritten"
                : "Order dates stored as ISO dates: " + outcome.converted() + "; already ISO: " + outcome.alreadyIso()
                        + "; left unread: " + outcome.unreadable().size();
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
