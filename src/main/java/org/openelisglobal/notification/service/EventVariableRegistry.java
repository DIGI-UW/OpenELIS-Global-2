package org.openelisglobal.notification.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.openelisglobal.notification.valueholder.NotificationPayloadTemplate.NotificationPayloadType;

/**
 * Single source of truth for the variables each notification event exposes to
 * its templates. Used by the dispatcher (context-builder side) and by the admin
 * template editor (so the right-column merge-field tiles, sample-value preview,
 * and rendered preview all stay in lockstep with what the runtime actually
 * emits).
 *
 * <p>
 * Each variable has three pieces:
 * <ul>
 * <li><b>token</b> — the canonical {@code [camelCase]} placeholder used in
 * stored templates and substituted at fire time.
 * <li><b>displayLabel</b> — the snake_case human-friendly label shown in the
 * admin UI's merge-field panel (matches the mockup).
 * <li><b>description</b> — short prose explaining the variable's meaning.
 * </ul>
 * Sample values are the canonical preview values used by the rendered-preview
 * panel; they're representative of real production values so admins can see
 * roughly what a notification will look like.
 */
public final class EventVariableRegistry {

    public static final class Variable {
        private final String token;
        private final String displayLabel;
        private final String description;

        public Variable(String token, String displayLabel, String description) {
            this.token = token;
            this.displayLabel = displayLabel;
            this.description = description;
        }

        public String getToken() {
            return token;
        }

        public String getDisplayLabel() {
            return displayLabel;
        }

        public String getDescription() {
            return description;
        }
    }

    private EventVariableRegistry() {
    }

    public static List<Variable> variablesFor(NotificationPayloadType type) {
        return switch (type) {
        case REFERRAL_OUT -> referralOutVariables();
        case SUBCONTRACT_DISPATCHED -> subcontractDispatchedVariables();
        default -> List.of();
        };
    }

    public static Map<String, String> sampleValuesFor(NotificationPayloadType type) {
        return switch (type) {
        case REFERRAL_OUT -> referralOutSampleValues();
        case SUBCONTRACT_DISPATCHED -> subcontractDispatchedSampleValues();
        default -> Map.of();
        };
    }

    private static List<Variable> referralOutVariables() {
        List<Variable> vars = new ArrayList<>();
        vars.add(new Variable("orderId", "order_id", "Order accession number"));
        vars.add(new Variable("sampleAccessionNumber", "sample_id", "Sample / specimen ID (lab number)"));
        vars.add(new Variable("referralId", "referral_id", "Internal referral identifier"));
        vars.add(new Variable("labName", "referred_lab", "Name of the external laboratory receiving the referral"));
        vars.add(
                new Variable("referredTests", "referred_tests", "Comma-separated list of test names in this referral"));
        vars.add(new Variable("testName", "test_name", "Single referred test name (first one if multiple)"));
        vars.add(new Variable("referralDate", "referral_date", "Date the referral was created (lab locale format)"));
        vars.add(new Variable("expectedReturn", "expected_return", "Expected result return date (blank if not set)"));
        vars.add(new Variable("sendingLabName", "lab_name", "Sending laboratory's name"));
        vars.add(new Variable("sendingLabPhone", "lab_phone", "Sending laboratory's contact phone"));
        vars.add(new Variable("patientFirstName", "patient_first_name", "Patient first name"));
        vars.add(new Variable("patientLastName", "patient_last_name", "Patient last name"));
        return vars;
    }

    private static Map<String, String> referralOutSampleValues() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("orderId", "ENV-2026-00841");
        values.put("sampleAccessionNumber", "ENV-2026-00841");
        values.put("referralId", "42");
        values.put("labName", "Pusat Lab Lingkungan Hidup");
        values.put("referredTests", "Mercury (Hg), Total Coliform");
        values.put("testName", "Mercury (Hg)");
        values.put("referralDate", "20 Apr 2026");
        values.put("expectedReturn", "27 Apr 2026");
        values.put("sendingLabName", "BLHD Kota Bogor");
        values.put("sendingLabPhone", "+62-251-8321-000");
        values.put("patientFirstName", "Jane");
        values.put("patientLastName", "Doe");
        return values;
    }

    private static List<Variable> subcontractDispatchedVariables() {
        List<Variable> vars = new ArrayList<>(referralOutVariables());
        vars.add(new Variable("agreementReference", "agreement_reference",
                "Subcontract agreement reference / contract number"));
        vars.add(new Variable("handoffDatetime", "handoff_datetime", "When the sample was physically handed off"));
        vars.add(new Variable("cocContactName", "coc_contact_name",
                "Chain-of-custody contact name at the receiving lab"));
        return vars;
    }

    private static Map<String, String> subcontractDispatchedSampleValues() {
        Map<String, String> values = new LinkedHashMap<>(referralOutSampleValues());
        values.put("agreementReference", "AGR-2026-017");
        values.put("handoffDatetime", "25 Apr 2026 09:30");
        values.put("cocContactName", "Jane Doe");
        return values;
    }
}
