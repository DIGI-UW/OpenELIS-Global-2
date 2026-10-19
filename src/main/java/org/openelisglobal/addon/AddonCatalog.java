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

    public static final String EVENT_ORDER_CREATED = "order.created";
    public static final String EVENT_RESULT_VALIDATED = "result.validated";
    public static final String EVENT_REPORT_PRINTED = "report.printed";

    public static final String SLOT_RESULT_VALIDATION_SIDE_PANEL = "result-validation-side-panel-slot";
    public static final String SLOT_RESULT_LOOKUP_SIDE_PANEL = "result-lookup-side-panel-slot";
    public static final String SLOT_APP_MENU = "app-menu-slot";

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
            new Scope(PATIENT_READ, "Sex and date of birth of the patient of those orders (no name, no identifiers)"));

    public static final List<EventType> EVENTS = List.of(
            new EventType(EVENT_ORDER_CREATED, ORDERS_READ, "A new order (sample) is saved"),
            new EventType(EVENT_RESULT_VALIDATED, RESULTS_READ, "A test result is validated (Finalized)"),
            new EventType(EVENT_REPORT_PRINTED, ORDERS_READ, "A patient report is printed or reprinted"));

    public static final List<Slot> SLOTS = List.of(
            new Slot(SLOT_RESULT_VALIDATION_SIDE_PANEL, Set.of("VALIDATION", "ADMIN"),
                    "Side panel of the result validation page"),
            new Slot(SLOT_RESULT_LOOKUP_SIDE_PANEL, Set.of("RESULTS", "VALIDATION", "REPORTS", "ADMIN"),
                    "Side panel of the patient results page"),
            new Slot(SLOT_APP_MENU, Set.of(), "Entry in the main menu, opening a full page"));

    /** At most this many lab numbers travel in a UI context or a token. */
    public static final int MAX_CONTEXT_ACCESSIONS = 50;

    private static final String ACCESSION_PATTERN = "[A-Za-z0-9._-]{1,64}";

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
        if (granted.contains(ORDERS_READ) && context.get("accessionNumbers") instanceof Collection<?> raw) {
            List<String> accessions = new ArrayList<>();
            for (Object a : raw) {
                String s = String.valueOf(a);
                if (isAccessionNumber(s) && !accessions.contains(s) && accessions.size() < MAX_CONTEXT_ACCESSIONS) {
                    accessions.add(s);
                }
            }
            out.put("accessionNumbers", accessions);
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
