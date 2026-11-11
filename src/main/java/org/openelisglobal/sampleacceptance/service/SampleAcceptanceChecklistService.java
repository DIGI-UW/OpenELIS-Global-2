package org.openelisglobal.sampleacceptance.service;

import java.util.List;
import org.openelisglobal.dictionary.valueholder.Dictionary;

/**
 * Configuration service for the S-09 (OGC-580) Sample Acceptance Checklist —
 * the pre-analytical specimen-acceptance gate (ISO 15189 §5.4 / 17025 §7.4).
 *
 * <p>
 * This is deliberately distinct from the OGC-356 "QA Checklist"
 * ({@code SampleQaChecklistService}, category {@code QAChecklistItem}), which
 * verifies that an order was set up correctly. Item definitions are reused from
 * the {@code Dictionary} infrastructure (categories
 * {@code SampleAcceptanceItem*}) rather than a bespoke table, and enforcement
 * modes live in {@code site_information}; this service owns no storage of its
 * own — it resolves the effective per-domain list / enforcement mode (read
 * side) and brokers admin edits onto that shared infrastructure (write side).
 */
public interface SampleAcceptanceChecklistService {

    /** Dictionary category holding the lab-wide (all-domains) fallback list. */
    String LAB_WIDE_CATEGORY = "SampleAcceptanceItem";

    /**
     * Prefix for per-domain categories — the domain is appended uppercase, e.g.
     * {@code SampleAcceptanceItem_CLINICAL}.
     */
    String DOMAIN_CATEGORY_PREFIX = "SampleAcceptanceItem_";

    /** Default enforcement mode when none is configured. */
    String DEFAULT_ENFORCEMENT = "OPTIONAL";

    /** The navigation target for the lab-wide list. */
    String ALL_DOMAINS = "ALL";

    /** Active lab-wide checklist items, ordered by {@code sortOrder}. */
    List<Dictionary> listLabWide();

    /**
     * The resolved checklist for an order's domain: the domain's own active items
     * when it has at least one, otherwise the lab-wide list. The two lists are
     * never merged. A {@code null}/blank domain resolves to the lab-wide list.
     *
     * @param domain CLINICAL, ENVIRONMENTAL, or VECTOR (case-insensitive)
     */
    List<Dictionary> listForDomain(String domain);

    /**
     * Enforcement mode (MANDATORY / OPTIONAL / OFF) for the given domain, read from
     * the {@code sampleAcceptanceChecklist.enforcement.*} configuration settings.
     * Returns {@link #DEFAULT_ENFORCEMENT} for an unknown/blank domain or unset
     * value.
     *
     * @param domain CLINICAL, ENVIRONMENTAL, or VECTOR (case-insensitive)
     */
    String getEnforcement(String domain);

    // ---- admin (write) side -------------------------------------------------

    /**
     * Everything the admin screen needs for one navigation target: the target's own
     * items (active <em>and</em> inactive, ordered by {@code sortOrder}), the
     * lab-wide items, whether the domain overrides the lab-wide list, and the
     * current enforcement mode (null for {@code ALL}).
     *
     * @param domain {@link #ALL_DOMAINS}, CLINICAL, ENVIRONMENTAL, or VECTOR
     *               (case-insensitive)
     */
    AdminChecklistView getAdminView(String domain);

    /**
     * Create a new checklist item under the given navigation target. Generates a
     * stable {@code dictEntry} key, stores {@code label} as the localized name, and
     * appends it to the end of the target's list.
     *
     * @return the created item
     */
    Dictionary createItem(String domain, String label, String sysUserId);

    /** Rename and/or activate-deactivate an existing item. */
    Dictionary updateItem(String id, String label, boolean active, String sysUserId);

    /**
     * Persist the {@code sortOrder} of the target's items to match the given id
     * order (1..N).
     */
    void reorder(String domain, List<String> orderedIds, String sysUserId);

    /**
     * Set the per-domain enforcement mode.
     *
     * @param domain CLINICAL, ENVIRONMENTAL, or VECTOR (case-insensitive)
     * @param mode   MANDATORY, OPTIONAL, or OFF
     */
    void setEnforcement(String domain, String mode, String sysUserId);
}
