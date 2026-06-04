package org.openelisglobal.labelpreset.service;

import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.labelpreset.dao.LabelPresetDAO;
import org.openelisglobal.labelpreset.dao.TestLabelPresetLinkDAO;
import org.openelisglobal.labelpreset.valueholder.LabelPreset;
import org.openelisglobal.labelpreset.valueholder.TestLabelPresetLink;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for TestLabelPresetLink (OGC-285 M4). Adds
 * defense-in-depth validation that only per-sample presets may be linked to
 * tests (per data-model.md §3.1).
 */
@Service
public class TestLabelPresetLinkServiceImpl extends AuditableBaseObjectServiceImpl<TestLabelPresetLink, Integer>
        implements TestLabelPresetLinkService {

    @Autowired
    protected TestLabelPresetLinkDAO baseObjectDAO;

    @Autowired
    private LabelPresetDAO labelPresetDAO;

    public TestLabelPresetLinkServiceImpl() {
        super(TestLabelPresetLink.class);
    }

    @Override
    protected TestLabelPresetLinkDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<TestLabelPresetLink> listByTestId(String testId) {
        return baseObjectDAO.listByTestId(testId);
    }

    /**
     * Defense-in-depth: assert that the given preset has
     * {@code prints_per_sample = true}. If the preset is order-only
     * ({@code prints_per_sample = false}) or does not exist, throw
     * {@link IllegalStateException}.
     */
    @Override
    @Transactional(readOnly = true)
    public void assertPerSamplePreset(Integer presetId) {
        Optional<LabelPreset> opt = labelPresetDAO.get(presetId);
        LabelPreset preset = opt
                .orElseThrow(() -> new IllegalStateException("Label preset with id " + presetId + " does not exist"));
        if (!Boolean.TRUE.equals(preset.getPrintsPerSample())) {
            throw new IllegalStateException("Label preset '" + preset.getName() + "' (id=" + presetId
                    + ") is order-only " + "(prints_per_sample=false) and cannot be linked to a test's Labels tab");
        }
    }
}
