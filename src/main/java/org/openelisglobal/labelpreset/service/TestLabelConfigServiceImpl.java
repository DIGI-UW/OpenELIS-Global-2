package org.openelisglobal.labelpreset.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.openelisglobal.labelpreset.dao.LabelPresetDAO;
import org.openelisglobal.labelpreset.dao.TestLabelConfigDAO;
import org.openelisglobal.labelpreset.dao.TestLabelPresetLinkDAO;
import org.openelisglobal.labelpreset.form.TestLabelConfigForm;
import org.openelisglobal.labelpreset.valueholder.LabelPreset;
import org.openelisglobal.labelpreset.valueholder.TestLabelConfig;
import org.openelisglobal.labelpreset.valueholder.TestLabelPresetLink;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for the test-level label configuration (OGC-285 M4,
 * T108). Encapsulates the full-replace semantics: validate presets, upsert
 * TestLabelConfig, delete+insert TestLabelPresetLink rows.
 */
@Service
public class TestLabelConfigServiceImpl implements TestLabelConfigService {

    @Autowired
    private LabelPresetDAO labelPresetDAO;

    @Autowired
    private TestLabelConfigDAO testLabelConfigDAO;

    @Autowired
    private TestLabelPresetLinkDAO testLabelPresetLinkDAO;

    @Autowired
    private TestLabelPresetLinkService testLabelPresetLinkService;

    @Autowired
    private TestService testService;

    @Override
    @Transactional(readOnly = true)
    public Optional<TestLabelConfig> getByTestId(String testId) {
        return testLabelConfigDAO.getByTestId(testId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestLabelPresetLink> getLinksByTestId(String testId) {
        List<TestLabelPresetLink> links = testLabelPresetLinkDAO.listByTestId(testId);
        // Initialize the lazy preset association within this read tx so callers (e.g.
        // the controller response builder) can read preset id/name after the tx
        // closes without a LazyInitializationException.
        links.forEach(link -> org.hibernate.Hibernate.initialize(link.getPreset()));
        return links;
    }

    /**
     * Full-replace semantics per tasks.md T108:
     * <ol>
     * <li>Validate no duplicate presetId entries in the form.</li>
     * <li>Validate each preset has prints_per_sample=true via
     * assertPerSamplePreset.</li>
     * <li>Upsert the TestLabelConfig row.</li>
     * <li>Delete existing links.</li>
     * <li>Insert new links.</li>
     * </ol>
     */
    @Override
    @Transactional
    public void replace(String testId, TestLabelConfigForm form, String sysUserId) {
        // 1. Validate no duplicate presetId values.
        List<TestLabelConfigForm.LinkEntry> entries = form.getLinks();
        Set<Integer> seen = new HashSet<>();
        for (TestLabelConfigForm.LinkEntry entry : entries) {
            if (!seen.add(entry.getPresetId())) {
                throw new IllegalStateException(
                        "Duplicate presetId " + entry.getPresetId() + " in TestLabelConfig replace request");
            }
        }

        // 2. Validate each preset is per-sample (defense-in-depth per T110).
        for (TestLabelConfigForm.LinkEntry entry : entries) {
            testLabelPresetLinkService.assertPerSamplePreset(entry.getPresetId());
        }

        // 3. Upsert TestLabelConfig.
        Optional<TestLabelConfig> existing = testLabelConfigDAO.getByTestId(testId);
        TestLabelConfig config;
        boolean isNew;
        if (existing.isPresent()) {
            config = existing.get();
            isNew = false;
        } else {
            config = new TestLabelConfig();
            Test test = testService.getTestById(testId);
            if (test == null) {
                throw new IllegalStateException("Test with id " + testId + " not found");
            }
            config.setTest(test);
            isNew = true;
        }
        config.setAllowOrderEntryOverride(form.getAllowOrderEntryOverride());
        config.setSysUserId(sysUserId);
        if (isNew) {
            testLabelConfigDAO.insert(config);
        } else {
            testLabelConfigDAO.update(config);
        }

        // 4. Delete existing links.
        List<TestLabelPresetLink> oldLinks = testLabelPresetLinkDAO.listByTestId(testId);
        for (TestLabelPresetLink link : oldLinks) {
            testLabelPresetLinkDAO.delete(link);
        }

        // 5. Insert new links.
        Test test = testService.getTestById(testId);
        List<TestLabelPresetLink> newLinks = new ArrayList<>();
        for (TestLabelConfigForm.LinkEntry entry : entries) {
            TestLabelPresetLink link = new TestLabelPresetLink();
            link.setTest(test);
            // Attach the MANAGED preset (not a transient new LabelPreset(id)) so the
            // link insert does not fail with TransientPropertyValueException.
            LabelPreset preset = labelPresetDAO.get(entry.getPresetId())
                    .orElseThrow(() -> new IllegalStateException("Preset not found: " + entry.getPresetId()));
            link.setPreset(preset);
            link.setDefaultQty(entry.getDefaultQty());
            link.setMaxQty(entry.getMaxQty());
            link.setAllowOverride(entry.getAllowOverride());
            link.setSysUserId(sysUserId);
            newLinks.add(link);
        }
        for (TestLabelPresetLink link : newLinks) {
            testLabelPresetLinkDAO.insert(link);
        }
    }
}
