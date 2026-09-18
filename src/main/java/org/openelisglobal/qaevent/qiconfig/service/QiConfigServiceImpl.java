package org.openelisglobal.qaevent.qiconfig.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.qaevent.qiconfig.dao.QiConfigDAO;
import org.openelisglobal.qaevent.qiconfig.dto.QiConfigView;
import org.openelisglobal.qaevent.qiconfig.dto.ResolvedConfig;
import org.openelisglobal.qaevent.qiconfig.valueholder.QiConfig;
import org.openelisglobal.qaevent.qiconfig.valueholder.QiIndicator;
import org.openelisglobal.test.service.TestSectionService;
import org.openelisglobal.test.valueholder.TestSection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class QiConfigServiceImpl extends AuditableBaseObjectServiceImpl<QiConfig, Long> implements QiConfigService {

    private static final BigDecimal HUNDRED = new BigDecimal("100");

    @Autowired
    protected QiConfigDAO baseObjectDAO;

    @Autowired
    private TestSectionService testSectionService;

    public QiConfigServiceImpl() {
        super(QiConfig.class);
        this.auditTrailLog = true;
    }

    @Override
    protected QiConfigDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<QiConfigView> getAllConfigs() {
        // Seed a view per known indicator so the admin page always shows all four,
        // even before any row is persisted.
        Map<String, QiConfigView> byIndicator = new LinkedHashMap<>();
        for (QiIndicator ind : QiIndicator.values()) {
            QiConfigView v = new QiConfigView();
            v.setIndicatorKey(ind.name());
            v.setDirection(ind.getDirection().name());
            v.setEnabled(Boolean.TRUE);
            byIndicator.put(ind.name(), v);
        }
        for (QiConfig row : baseObjectDAO.getAllOrderedByIndicator()) {
            QiConfigView v = byIndicator.get(row.getIndicatorKey());
            if (v == null) {
                continue; // legacy/unknown key — not shown
            }
            if (row.getTestCategoryId() == null) {
                v.setEnabled(row.getEnabled());
                v.setTarget(row.getTargetThreshold());
                v.setAction(row.getActionThreshold());
            } else {
                QiConfigView.Override o = new QiConfigView.Override();
                o.setId(String.valueOf(row.getId()));
                o.setTestCategoryId(row.getTestCategoryId());
                o.setTestSectionName(sectionName(row.getTestCategoryId()));
                o.setTarget(row.getTargetThreshold());
                o.setAction(row.getActionThreshold());
                v.getOverrides().add(o);
            }
        }
        return new ArrayList<>(byIndicator.values());
    }

    // Resolved inside the read tx so no lazy state escapes to the controller.
    private String sectionName(String sectionId) {
        TestSection ts = testSectionService.getTestSectionById(sectionId);
        return ts == null ? null : ts.getLocalizedName();
    }

    @Override
    @Transactional(readOnly = true)
    public ResolvedConfig resolve(String indicatorKey, String testSectionId) {
        QiIndicator ind = requireIndicator(indicatorKey);
        QiConfig def = baseObjectDAO.getDefault(ind.name());
        if (def == null) {
            throw new IllegalArgumentException("No default config for indicator: " + ind.name());
        }
        String direction = ind.getDirection().name();
        // Disabled default short-circuits: never consult an override (711 contract).
        if (!Boolean.TRUE.equals(def.getEnabled())) {
            return new ResolvedConfig(ind.name(), false, def.getTargetThreshold(), def.getActionThreshold(), direction);
        }
        if (testSectionId != null && !testSectionId.isBlank()) {
            QiConfig override = baseObjectDAO.getOverride(ind.name(), testSectionId);
            if (override != null) {
                return new ResolvedConfig(ind.name(), true, override.getTargetThreshold(),
                        override.getActionThreshold(), direction);
            }
        }
        return new ResolvedConfig(ind.name(), true, def.getTargetThreshold(), def.getActionThreshold(), direction);
    }

    @Override
    @Transactional
    public void saveIndicator(String indicatorKey, QiConfigView view, String sysUserId) {
        QiIndicator ind = requireIndicator(indicatorKey);
        validate(ind, view);

        // Default row (test_category_id IS NULL).
        applyAndSave(baseObjectDAO.getDefault(ind.name()), ind.name(), null,
                view.getEnabled() == null ? Boolean.TRUE : view.getEnabled(), view.getTarget(), view.getAction(),
                sysUserId);

        // Override rows: upsert those present, then delete those no longer present.
        List<QiConfigView.Override> incoming = view.getOverrides() == null ? List.of() : view.getOverrides();
        Set<String> keep = new HashSet<>();
        for (QiConfigView.Override o : incoming) {
            keep.add(o.getTestCategoryId());
            // Overrides are always "on"; enable/disable lives on the default row.
            applyAndSave(baseObjectDAO.getOverride(ind.name(), o.getTestCategoryId()), ind.name(),
                    o.getTestCategoryId(), Boolean.TRUE, o.getTarget(), o.getAction(), sysUserId);
        }
        for (QiConfig row : baseObjectDAO.getAllOrderedByIndicator()) {
            if (ind.name().equals(row.getIndicatorKey()) && row.getTestCategoryId() != null
                    && !keep.contains(row.getTestCategoryId())) {
                row.setSysUserId(sysUserId);
                delete(row);
            }
        }
    }

    /**
     * Persist one row through the audited service methods. Builds a FRESH detached
     * instance for the new values (never mutating the loaded {@code existing}), so
     * the base class re-reads the true pre-image and the history diff is real. On
     * update the loaded {@code @Version} ({@code last_updated}) is copied onto the
     * fresh instance so the optimistic-lock check on merge matches the DB — this
     * requires the row to carry a non-null version (the seed sets it; see qa/007
     * changeSet 003).
     */
    private void applyAndSave(QiConfig existing, String indicatorKey, String testCategoryId, Boolean enabled,
            BigDecimal target, BigDecimal action, String sysUserId) {
        QiConfig row = new QiConfig();
        if (existing != null) {
            row.setId(existing.getId());
            row.setLastupdated(existing.getLastupdated());
        }
        row.setIndicatorKey(indicatorKey);
        row.setTestCategoryId(testCategoryId);
        row.setEnabled(enabled);
        row.setTargetThreshold(target);
        row.setActionThreshold(action);
        row.setSysUserId(sysUserId);
        if (existing == null) {
            insert(row);
        } else {
            update(row);
        }
    }

    private QiIndicator requireIndicator(String indicatorKey) {
        QiIndicator ind = QiIndicator.fromKey(indicatorKey);
        if (ind == null) {
            throw new IllegalArgumentException("Unknown quality indicator: " + indicatorKey);
        }
        return ind;
    }

    private void validate(QiIndicator ind, QiConfigView view) {
        validateThresholds(ind, view.getTarget(), view.getAction(), ind.isThresholdsRequired());
        List<QiConfigView.Override> overrides = view.getOverrides() == null ? List.of() : view.getOverrides();
        Set<String> seen = new HashSet<>();
        for (QiConfigView.Override o : overrides) {
            if (o.getTestCategoryId() == null || o.getTestCategoryId().isBlank()) {
                throw new IllegalArgumentException("Override is missing a test category");
            }
            if (!seen.add(o.getTestCategoryId())) {
                throw new IllegalArgumentException("Duplicate override for test category " + o.getTestCategoryId());
            }
            // Overrides always carry both thresholds (no partial-inherit mode).
            validateThresholds(ind, o.getTarget(), o.getAction(), true);
        }
    }

    private void validateThresholds(QiIndicator ind, BigDecimal target, BigDecimal action, boolean required) {
        boolean bothNull = target == null && action == null;
        boolean anyNull = target == null || action == null;
        if (required && anyNull) {
            throw new IllegalArgumentException(ind.name() + " requires both target and action thresholds");
        }
        if (bothNull) {
            return; // permitted only when !required (e.g. NCE default)
        }
        if (anyNull) {
            throw new IllegalArgumentException("Provide both thresholds or neither");
        }
        requireRange(target);
        requireRange(action);
        if (ind.getDirection() == QiIndicator.Direction.HIGHER_BETTER && target.compareTo(action) <= 0) {
            throw new IllegalArgumentException(ind.name() + ": target must be greater than action (higher is better)");
        }
        if (ind.getDirection() == QiIndicator.Direction.LOWER_BETTER && target.compareTo(action) >= 0) {
            throw new IllegalArgumentException(ind.name() + ": target must be less than action (lower is better)");
        }
    }

    private void requireRange(BigDecimal v) {
        if (v.compareTo(BigDecimal.ZERO) < 0 || v.compareTo(HUNDRED) > 0) {
            throw new IllegalArgumentException("Threshold must be between 0 and 100");
        }
    }
}
