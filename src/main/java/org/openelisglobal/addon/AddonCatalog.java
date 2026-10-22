package org.openelisglobal.addon;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Everything an add-on can ask for: the permissions (scopes), the events it can
 * subscribe to, and the UI slots it can contribute to. This class is the only
 * list; manifest validation, token checks and the administration page (through
 * {@code GET /rest/addons/catalog}) all read it.
 *
 * <p>
 * Slot names follow the OpenMRS 3 extension system: kebab-case ending in
 * {@code -slot}.
 */
public final class AddonCatalog {

    public static final String ORDERS_READ = "orders.read";
    public static final String RESULTS_READ = "results.read";
    public static final String PATIENT_READ = "patient.read";
    public static final String AI_CONTEXT = "ai.context";

    public static final String EVENT_ORDER_CREATED = "order.created";
    public static final String EVENT_RESULT_VALIDATED = "result.validated";
    public static final String EVENT_REPORT_PRINTED = "report.printed";

    public static final String SLOT_RESULT_VALIDATION_SIDE_PANEL = "result-validation-side-panel-slot";
    public static final String SLOT_RESULT_LOOKUP_SIDE_PANEL = "result-lookup-side-panel-slot";
    public static final String SLOT_APP_MENU = "app-menu-slot";
    public static final String SLOT_TOP_NAV_ACTIONS = "top-nav-actions-slot";

    /** A permission an add-on can be granted. */
    public record Scope(String key, String description) {
    }

    /** An event type and the scope an add-on needs to receive it. */
    public record EventType(String type, String scope, String description) {
    }

    /**
     * A UI slot. {@code roles}: a user needs one of them for the slot's add-on
     * frames to get a token (empty = any signed-in user).
     */
    public record Slot(String name, Set<String> roles, String description) {
    }

    public static final List<Scope> SCOPES = List.of(
            new Scope(ORDERS_READ, "Lab number, received date and test count of the orders in its context"),
            new Scope(RESULTS_READ, "Test results of those orders"),
            new Scope(PATIENT_READ, "Sex and date of birth of the patient of those orders (no name, no identifiers)"),
            new Scope(AI_CONTEXT,
                    "De-identified results of the orders in its context, for decision support: sex, age in years,"
                            + " results with reference ranges, earlier results of the same tests (as days before);"
                            + " no name, identifier, lab number or date"));

    public static final List<EventType> EVENTS = List.of(
            new EventType(EVENT_ORDER_CREATED, ORDERS_READ, "A new order (sample) is saved"),
            new EventType(EVENT_RESULT_VALIDATED, RESULTS_READ, "A test result is validated (Finalized)"),
            new EventType(EVENT_REPORT_PRINTED, ORDERS_READ, "A patient report is printed or reprinted"));

    public static final List<Slot> SLOTS = List.of(
            new Slot(SLOT_RESULT_VALIDATION_SIDE_PANEL, Set.of("VALIDATION", "ADMIN"),
                    "Side panel of the result validation page"),
            new Slot(SLOT_RESULT_LOOKUP_SIDE_PANEL, Set.of("RECEPTION", "RESULTS", "VALIDATION", "REPORTS", "ADMIN"),
                    "Side panel of the patient results page"),
            new Slot(SLOT_APP_MENU, Set.of(), "Entry in the main menu, opening a full page"),
            new Slot(SLOT_TOP_NAV_ACTIONS, Set.of(), "Button in the top navigation bar, opening a side panel"));

    /** At most this many lab numbers travel in a UI context or a token. */
    public static final int MAX_CONTEXT_ACCESSIONS = 50;

    /** At most this many hosts in a manifest's {@code network.egress}. */
    public static final int MAX_EGRESS_HOSTS = 10;

    private static final String ACCESSION_PATTERN = "[A-Za-z0-9._-]{1,64}";
    private static final String HOST_LABEL = "[a-z0-9](?:[a-z0-9-]{0,61}[a-z0-9])?";
    private static final String EGRESS_PATTERN = HOST_LABEL + "(?:\\." + HOST_LABEL + ")+(?::[0-9]{1,5})?";

    private AddonCatalog() {
    }

    public static boolean isScope(String key) {
        return SCOPES.stream().anyMatch(s -> s.key().equals(key));
    }

    /** The scope needed for an event type, or null if the type is unknown. */
    public static String scopeForEvent(String type) {
        return EVENTS.stream().filter(e -> e.type().equals(type)).map(EventType::scope).findFirst().orElse(null);
    }

    public static Slot slot(String name) {
        return SLOTS.stream().filter(s -> s.name().equals(name)).findFirst().orElse(null);
    }

    public static boolean isAccessionNumber(String value) {
        return value != null && value.matches(ACCESSION_PATTERN);
    }

    /**
     * A host an add-on may declare in {@code network.egress}: a lowercase DNS name
     * with at least two labels, optionally {@code :port}. Not an IP address and no
     * wildcard, so the consent screen always shows a name the administrator can
     * judge.
     */
    public static boolean isEgressHost(String value) {
        if (value == null || value.length() > 260 || !value.matches(EGRESS_PATTERN)) {
            return false;
        }
        String host = value.contains(":") ? value.substring(0, value.indexOf(':')) : value;
        if (host.matches("[0-9.]+")) {
            return false;
        }
        if (value.contains(":")) {
            int port = Integer.parseInt(value.substring(value.indexOf(':') + 1));
            return port >= 1 && port <= 65535;
        }
        return true;
    }

    /**
     * The orders a UI token is about: the lab numbers of the page's context, kept
     * when the add-on may read orders ({@code orders.read}) or their de-identified
     * context ({@code ai.context}); at most {@link #MAX_CONTEXT_ACCESSIONS}.
     */
    public static List<String> contextOrders(Map<String, ?> context, Collection<String> granted) {
        List<String> accessions = new ArrayList<>();
        if (context == null || !(granted.contains(ORDERS_READ) || granted.contains(AI_CONTEXT))) {
            return accessions;
        }
        if (context.get("accessionNumbers") instanceof Collection<?> raw) {
            for (Object a : raw) {
                String s = String.valueOf(a);
                if (isAccessionNumber(s) && !accessions.contains(s) && accessions.size() < MAX_CONTEXT_ACCESSIONS) {
                    accessions.add(s);
                }
            }
        }
        return accessions;
    }

    /**
     * The part of a page's context that is passed on to an add-on: the page name
     * always, the lab numbers only with {@code orders.read}, at most
     * {@link #MAX_CONTEXT_ACCESSIONS}. Every other key (patient name, display
     * labels) is dropped here, on the server, whatever the browser sent.
     */
    public static Map<String, Object> filterContext(Map<String, ?> context, Collection<String> granted) {
        Map<String, Object> out = new LinkedHashMap<>();
        if (context == null) {
            return out;
        }
        if (context.get("page") instanceof String page && page.matches("[a-z0-9-]{1,40}")) {
            out.put("page", page);
        }
        if (granted.contains(ORDERS_READ) && context.get("accessionNumbers") instanceof Collection<?>) {
            out.put("accessionNumbers", contextOrders(context, granted));
        }
        return out;
    }

    /** The catalog as served to the administration page. */
    public static Map<String, Object> describe() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("scopes", SCOPES);
        out.put("events", EVENTS);
        out.put("slots", SLOTS.stream().map(s -> Map.of("name", s.name(), "description", s.description())).toList());
        return out;
    }
}
