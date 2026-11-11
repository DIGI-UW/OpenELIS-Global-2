package org.openelisglobal.notification.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openelisglobal.notification.valueholder.NotificationPayloadTemplate.NotificationPayloadType;

/**
 * Single source of truth for the merge fields each message type may use. The
 * dispatcher substitutes them at fire time, the admin template editor offers
 * only these, and a template is refused on save when it names any other field
 * (FR-A14).
 *
 * <p>
 * Privacy rule (FR-D1): no slot offers the patient's name or national id, so no
 * email subject, email body, SMS or WhatsApp text can carry them.
 *
 * <p>
 * Each variable has three pieces:
 * <ul>
 * <li><b>token</b> the canonical {@code [camelCase]} placeholder used in stored
 * templates and substituted at fire time.
 * <li><b>displayLabel</b> the snake_case label shown in the admin UI's
 * merge-field panel.
 * <li><b>description</b> short prose explaining the variable's meaning.
 * </ul>
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

    private static final Pattern TOKEN = Pattern.compile("\\[([A-Za-z0-9_]+)\\]");

    /** Fields that identify a patient; never allowed in any message text. */
    public static final Set<String> IDENTIFYING_TOKENS = Set.of("patientFirstName", "patientLastName",
            "patientLastNameInitial", "patientName", "patientFullName", "nationalId", "patientNationalId",
            "subjectNumber");

    private EventVariableRegistry() {
    }

    public static List<Variable> variablesFor(NotificationPayloadType type) {
        return switch (type) {
        case REFERRAL_OUT -> referralOutVariables();
        case SUBCONTRACT_DISPATCHED -> subcontractDispatchedVariables();
        case REFERRAL_REJECTED_NEEDS_RECOLLECTION -> referralRejectedVariables();
        case REFERRAL_NUDGE -> referralNudgeVariables();
        case SAMPLE_RESAMPLED -> sampleResampledVariables();
        case TEST_RESULT -> testResultVariables();
        };
    }

    public static Map<String, String> sampleValuesFor(NotificationPayloadType type) {
        return switch (type) {
        case REFERRAL_OUT -> referralOutSampleValues();
        case SUBCONTRACT_DISPATCHED -> subcontractDispatchedSampleValues();
        case REFERRAL_REJECTED_NEEDS_RECOLLECTION -> referralRejectedSampleValues();
        case REFERRAL_NUDGE -> referralNudgeSampleValues();
        case SAMPLE_RESAMPLED -> sampleResampledSampleValues();
        case TEST_RESULT -> testResultSampleValues();
        };
    }

    /** The {@code [tokens]} a template text names, in order of appearance. */
    public static List<String> tokensIn(String text) {
        List<String> tokens = new ArrayList<>();
        if (text == null) {
            return tokens;
        }
        Matcher matcher = TOKEN.matcher(text);
        while (matcher.find()) {
            if (!tokens.contains(matcher.group(1))) {
                tokens.add(matcher.group(1));
            }
        }
        return tokens;
    }

    /**
     * The tokens in {@code text} that the slot does not allow: identifying fields
     * first, then anything the type does not offer.
     */
    public static List<String> disallowedTokens(NotificationPayloadType type, String text) {
        List<String> disallowed = new ArrayList<>();
        Set<String> allowed = new java.util.HashSet<>();
        for (Variable variable : variablesFor(type)) {
            allowed.add(variable.getToken());
        }
        for (String token : tokensIn(text)) {
            if (IDENTIFYING_TOKENS.contains(token) || !allowed.contains(token)) {
                disallowed.add(token);
            }
        }
        return disallowed;
    }

    private static List<Variable> referralNudgeVariables() {
        List<Variable> vars = new ArrayList<>();
        vars.add(new Variable("sampleAccessionNumber", "sample_id", "Sample / specimen ID (lab number)"));
        vars.add(new Variable("referralId", "referral_id", "Internal referral identifier"));
        vars.add(new Variable("labName", "referred_lab", "Name of the external laboratory receiving the referral"));
        vars.add(new Variable("testName", "test_name", "Referred test name"));
        vars.add(new Variable("daysOutstanding", "days_outstanding", "Days the referral has been outstanding"));
        vars.add(new Variable("freeFormMessage", "free_form_message", "Optional free-text note from the lab manager"));
        return vars;
    }

    private static Map<String, String> referralNudgeSampleValues() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("sampleAccessionNumber", "ENV-2026-00841");
        values.put("referralId", "42");
        values.put("labName", "Pusat Lab Lingkungan Hidup");
        values.put("testName", "Mercury (Hg)");
        values.put("daysOutstanding", "12");
        values.put("freeFormMessage", "Kindly prioritise: follow-up pending.");
        return values;
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
        return values;
    }

    private static List<Variable> subcontractDispatchedVariables() {
        List<Variable> vars = new ArrayList<>(referralOutVariables());
        vars.add(new Variable("agreementReference", "agreement_reference",
                "Subcontract agreement reference / contract number"));
        vars.add(new Variable("handoffDatetime", "handoff_datetime", "When the sample was physically handed off"));
        vars.add(new Variable("expectedReturnDate", "expected_return_date", "Expected result return date"));
        vars.add(new Variable("cocContactName", "coc_contact_name",
                "Chain-of-custody contact name at the receiving lab"));
        return vars;
    }

    private static Map<String, String> subcontractDispatchedSampleValues() {
        Map<String, String> values = new LinkedHashMap<>(referralOutSampleValues());
        values.put("agreementReference", "AGR-2026-017");
        values.put("handoffDatetime", "25 Apr 2026 09:30");
        values.put("expectedReturnDate", "27 Apr 2026");
        values.put("cocContactName", "Jane Doe");
        return values;
    }

    private static List<Variable> referralRejectedVariables() {
        List<Variable> vars = new ArrayList<>();
        vars.add(new Variable("sampleAccessionNumber", "sample_id", "Sample / specimen ID (lab number)"));
        vars.add(new Variable("referralId", "referral_id", "Internal referral identifier"));
        vars.add(new Variable("testName", "test_name", "Rejected test name"));
        vars.add(new Variable("labName", "referred_lab", "Reference laboratory that returned the result"));
        vars.add(new Variable("rejectReason", "reject_reason", "Reason the returned result was rejected"));
        vars.add(new Variable("deepLink", "deep_link", "Link to the order in OpenELIS for re-collection"));
        return vars;
    }

    private static Map<String, String> referralRejectedSampleValues() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("sampleAccessionNumber", "ENV-2026-00841");
        values.put("referralId", "42");
        values.put("testName", "Mercury (Hg)");
        values.put("labName", "Pusat Lab Lingkungan Hidup");
        values.put("rejectReason", "Hemolyzed sample: re-collection required");
        values.put("deepLink", "https://openelis.example.org/result?accessionNumber=ENV-2026-00841");
        return values;
    }

    private static List<Variable> sampleResampledVariables() {
        List<Variable> vars = new ArrayList<>();
        vars.add(new Variable("labNumber", "lab_number", "Lab number of the order that needs re-collection"));
        vars.add(new Variable("reason", "reason", "Why the sample must be collected again"));
        vars.add(new Variable("newLabNumber", "new_lab_number", "Lab number of the replacement order"));
        return vars;
    }

    private static Map<String, String> sampleResampledSampleValues() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("labNumber", "CPHL-26-004211");
        values.put("reason", "Sample hemolyzed");
        values.put("newLabNumber", "CPHL-26-004230");
        return values;
    }

    private static List<Variable> testResultVariables() {
        List<Variable> vars = new ArrayList<>();
        vars.add(new Variable("testName", "test_name", "Name of the test whose result was released"));
        vars.add(new Variable("testResult", "test_result", "The released result, as displayed"));
        vars.add(new Variable("accessPassword", "access_password", "One-time password for the results view"));
        vars.add(new Variable("accessAddress", "access_address", "Address of the results view"));
        return vars;
    }

    private static Map<String, String> testResultSampleValues() {
        Map<String, String> values = new LinkedHashMap<>();
        values.put("testName", "Malaria RDT");
        values.put("testResult", "Negative");
        values.put("accessPassword", "K7M-PQ4");
        values.put("accessAddress", "https://openelis.example.org/results");
        return values;
    }
}
