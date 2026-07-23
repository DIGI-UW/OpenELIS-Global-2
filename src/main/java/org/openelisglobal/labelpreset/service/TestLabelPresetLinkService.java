package org.openelisglobal.labelpreset.service;

import java.util.List;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.labelpreset.valueholder.TestLabelPresetLink;

/**
 * Service for TestLabelPresetLink (OGC-285 M4). Extends the standard base
 * service and adds validation logic for the per-sample-only enforcement rule.
 */
public interface TestLabelPresetLinkService extends BaseObjectService<TestLabelPresetLink, Integer> {

    /**
     * All preset links for a test.
     *
     * @param testId the legacy Test PK (String)
     */
    List<TestLabelPresetLink> listByTestId(String testId);

    /**
     * Defense-in-depth: assert that the given preset has
     * {@code prints_per_sample = true}. Throws {@link IllegalStateException} if the
     * preset exists but is order-only (i.e. {@code prints_per_sample = false}).
     * This enforces the rule that only per-sample presets may be linked to tests.
     *
     * @param presetId PK of the LabelPreset to check
     * @throws IllegalStateException if the preset is order-only or not found
     */
    void assertPerSamplePreset(Integer presetId);
}
