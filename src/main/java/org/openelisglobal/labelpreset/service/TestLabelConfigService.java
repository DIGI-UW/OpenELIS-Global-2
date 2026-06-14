package org.openelisglobal.labelpreset.service;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.labelpreset.form.TestLabelConfigForm;
import org.openelisglobal.labelpreset.valueholder.TestLabelConfig;
import org.openelisglobal.labelpreset.valueholder.TestLabelPresetLink;

/**
 * Service interface for the test-level label configuration (OGC-285 M4).
 * Provides read + full-replace semantics for the Labels tab data model.
 */
public interface TestLabelConfigService {

    /**
     * Load the {@link TestLabelConfig} row for the given test (may be absent if the
     * test has never had a Labels tab saved).
     *
     * @param testId the legacy Test PK (String)
     */
    Optional<TestLabelConfig> getByTestId(String testId);

    /**
     * Load all {@link TestLabelPresetLink} rows for the given test.
     *
     * @param testId the legacy Test PK (String)
     */
    List<TestLabelPresetLink> getLinksByTestId(String testId);

    /**
     * Full-replace the Labels tab configuration for a test. Semantics:
     * <ol>
     * <li>Validate that every preset in the form has
     * {@code prints_per_sample = true} (calls
     * {@link TestLabelPresetLinkService#assertPerSamplePreset}).</li>
     * <li>Validate there are no duplicate {@code presetId} entries in the form
     * links.</li>
     * <li>Upsert the {@link TestLabelConfig} row (allow_order_entry_override).</li>
     * <li>Delete all existing {@link TestLabelPresetLink} rows for the test.</li>
     * <li>Insert the new link rows from the form.</li>
     * </ol>
     *
     * @param testId    the legacy Test PK (String)
     * @param form      validated form payload
     * @param sysUserId the system user id for audit
     * @throws IllegalStateException if any linked preset is order-only or if there
     *                               are duplicate presetIds in the form
     */
    void replace(String testId, TestLabelConfigForm form, String sysUserId);
}
