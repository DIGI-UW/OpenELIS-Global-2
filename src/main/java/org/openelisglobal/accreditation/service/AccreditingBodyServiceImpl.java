package org.openelisglobal.accreditation.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import org.openelisglobal.accreditation.dao.AccreditingBodyDAO;
import org.openelisglobal.accreditation.dao.TestAccreditationDAO;
import org.openelisglobal.accreditation.dto.AccreditationSummary;
import org.openelisglobal.accreditation.dto.AccreditingBodyView;
import org.openelisglobal.accreditation.valueholder.AccreditationStatus;
import org.openelisglobal.accreditation.valueholder.AccreditingBody;
import org.openelisglobal.accreditation.valueholder.LogoVisibilityMode;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.image.service.ImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * OGC-686 — accrediting bodies: CRUD, validation, and the derived portfolio
 * view.
 *
 * <p>
 * Every write goes through the audited base-class methods, so
 * {@code accrediting_body} must have its {@code reference_tables} row
 * (registered in {@code liquibase/qa/013}) or {@code saveHistory} throws and
 * rolls the write back.
 *
 * <p>
 * Reads assemble their DTOs inside the transaction — enrolled counts come from
 * one grouped query rather than N per-body lookups.
 */
@Service
public class AccreditingBodyServiceImpl extends AuditableBaseObjectServiceImpl<AccreditingBody, Long>
        implements AccreditingBodyService {

    /** FRS §5: 2–16 chars, uppercase alphanumerics plus hyphen. */
    private static final Pattern CODE_PATTERN = Pattern.compile("^[A-Z0-9-]{2,16}$");

    private static final int NAME_MAX = 120;

    @Autowired
    protected AccreditingBodyDAO baseObjectDAO;

    @Autowired
    private TestAccreditationDAO testAccreditationDAO;

    @Autowired
    private ImageService imageService;

    public AccreditingBodyServiceImpl() {
        super(AccreditingBody.class);
        this.auditTrailLog = true;
    }

    @Override
    protected AccreditingBodyDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccreditingBodyView> getBodyViews() {
        Map<Long, Long> counts = enrolledCounts();
        LocalDate today = LocalDate.now();
        List<AccreditingBodyView> views = new ArrayList<>();
        for (AccreditingBody body : baseObjectDAO.getAllOrdered()) {
            long enrolled = counts.getOrDefault(body.getId(), 0L);
            views.add(toView(body, enrolled, today));
        }
        return views;
    }

    @Override
    @Transactional(readOnly = true)
    public AccreditationSummary getSummary() {
        LocalDate today = LocalDate.now();
        List<AccreditingBody> bodies = baseObjectDAO.getAllOrdered();

        AccreditationSummary summary = new AccreditationSummary();
        summary.totalBodies = bodies.size();

        List<String> inForceNames = new ArrayList<>();
        int active = 0;
        int expiring = 0;
        int expired = 0;
        AccreditationStatus worst = null;

        for (AccreditingBody body : bodies) {
            AccreditationStatus status = AccreditationStatus.of(body.getActive(), body.getExpiresOn(), today);
            if (status == AccreditationStatus.INACTIVE) {
                continue; // inactive bodies are not part of the portfolio picture
            }
            // The three counts are mutually exclusive: an expired body is counted as
            // expired and NOT as active, so each field means what its name says.
            if (status == AccreditationStatus.EXPIRED) {
                expired++;
            } else {
                if (status == AccreditationStatus.EXPIRING) {
                    expiring++;
                } else {
                    active++;
                }
                // Only in-date bodies can be claimed, so only they are named.
                inForceNames.add(body.getName());
            }
            // Ordinals ascend EXPIRED < EXPIRING < ACTIVE, so the most alarming
            // status seen is the smallest ordinal.
            if (worst == null || status.ordinal() < worst.ordinal()) {
                worst = status;
            }
        }

        summary.activeBodies = active;
        summary.expiringBodies = expiring;
        summary.expiredBodies = expired;
        summary.inForceBodyNames = inForceNames;
        summary.worstStatus = worst == null ? null : worst.name();
        return summary;
    }

    @Override
    @Transactional
    public AccreditingBody createBody(AccreditingBody body, String sysUserId) {
        String code = normalizeCode(body.getCode());
        validateCode(code);
        if (baseObjectDAO.getByCode(code) != null) {
            throw new IllegalArgumentException("An accrediting body with code " + code + " already exists");
        }
        AccreditingBody row = new AccreditingBody();
        row.setCode(code);
        applyEditableFields(row, body);
        row.setSysUserId(sysUserId);
        insert(row);
        return row;
    }

    @Override
    @Transactional
    public AccreditingBody updateBody(Long id, AccreditingBody incoming, String sysUserId) {
        AccreditingBody existing = require(id);
        // FR-11: code is read-only after create. Reject rather than silently drop it,
        // so a client sending a changed code learns it did nothing.
        if (incoming.getCode() != null) {
            String submitted = normalizeCode(incoming.getCode());
            if (!submitted.equals(existing.getCode())) {
                throw new IllegalArgumentException("Accrediting body code cannot be changed after creation");
            }
        }
        AccreditingBody row = detachedCopy(existing);
        applyEditableFields(row, incoming);
        row.setSysUserId(sysUserId);
        update(row);
        return row;
    }

    @Override
    @Transactional
    public void deleteBody(Long id, String sysUserId) {
        AccreditingBody existing = require(id);
        long enrolled = testAccreditationDAO.countByBody(id);
        if (enrolled > 0) {
            // FR-6: the DB FK would refuse this anyway; failing here turns a 500-ish
            // constraint violation into an explainable message with the count in it.
            throw new IllegalArgumentException(
                    "Cannot delete — " + enrolled + " test accreditations reference this body. Remove them first.");
        }
        String orphanedLogo = existing.getLogoImageId();
        existing.setSysUserId(sysUserId);
        delete(existing);
        deleteLogoImage(orphanedLogo, sysUserId);
    }

    @Override
    @Transactional
    public AccreditingBody setLogo(Long id, String logoImageId, String sysUserId) {
        AccreditingBody row = detachedCopy(require(id));
        String previousLogo = row.getLogoImageId();
        row.setLogoImageId(logoImageId);
        row.setSysUserId(sysUserId);
        update(row);
        // Replacing or clearing the logo leaves the old image unreferenced. Each
        // body owns its logo 1:1 (uploadLogo creates a fresh image per body), so
        // once the body no longer points at it, nothing does — delete it to avoid
        // the storage leak.
        if (previousLogo != null && !previousLogo.equals(logoImageId)) {
            deleteLogoImage(previousLogo, sysUserId);
        }
        return row;
    }

    /**
     * Removes an orphaned logo image. The {@code image} table is not
     * history-tracked, so this is a plain delete; the FK from
     * {@code accrediting_body} has already been cleared or dropped by the caller.
     */
    private void deleteLogoImage(String logoImageId, String sysUserId) {
        if (logoImageId != null) {
            imageService.delete(logoImageId, sysUserId);
        }
    }

    /**
     * A fresh detached instance carrying every persisted field of {@code existing}.
     * Callers then overwrite only what they mean to change.
     *
     * <p>
     * Never mutate the loaded entity: the audit base class re-reads the pre-image
     * to build its history diff, so an in-place edit would diff against itself. The
     * loaded {@code @Version} rides along so the optimistic-lock check on merge
     * matches the row in the DB.
     */
    private AccreditingBody detachedCopy(AccreditingBody existing) {
        AccreditingBody row = new AccreditingBody();
        row.setId(existing.getId());
        row.setLastupdated(existing.getLastupdated());
        row.setCode(existing.getCode());
        row.setName(existing.getName());
        row.setLogoImageId(existing.getLogoImageId());
        row.setExpiresOn(existing.getExpiresOn());
        row.setLogoVisibilityMode(existing.getLogoVisibilityMode());
        row.setThresholdPct(existing.getThresholdPct());
        row.setDisplayOrder(existing.getDisplayOrder());
        row.setActive(existing.getActive());
        return row;
    }

    private AccreditingBody require(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Accrediting body id is required");
        }
        Optional<AccreditingBody> found = baseObjectDAO.get(id);
        return found.orElseThrow(() -> new IllegalArgumentException("No accrediting body with id " + id));
    }

    /** Copies the mutable fields, validating each. Never touches id or code. */
    private void applyEditableFields(AccreditingBody target, AccreditingBody source) {
        String name = source.getName() == null ? null : source.getName().trim();
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("Accrediting body name is required");
        }
        if (name.length() > NAME_MAX) {
            throw new IllegalArgumentException("Accrediting body name must be at most " + NAME_MAX + " characters");
        }
        if (source.getExpiresOn() == null) {
            throw new IllegalArgumentException("Expiry date is required");
        }

        LogoVisibilityMode mode = source.getLogoVisibilityMode() == null ? LogoVisibilityMode.ANY_ACCREDITED_TEST
                : source.getLogoVisibilityMode();
        // Threshold is stored even under ANY_ACCREDITED_TEST (so toggling the mode
        // does not lose it), but it is only *required* to be meaningful under
        // PERCENTAGE.
        Short threshold = source.getThresholdPct() == null ? (short) 80 : source.getThresholdPct();
        if (threshold < 0 || threshold > 100) {
            throw new IllegalArgumentException("Threshold must be between 0 and 100");
        }

        target.setName(name);
        target.setExpiresOn(source.getExpiresOn());
        target.setLogoVisibilityMode(mode);
        target.setThresholdPct(threshold);
        target.setDisplayOrder(source.getDisplayOrder() == null ? (short) 0 : source.getDisplayOrder());
        target.setActive(source.getActive() == null ? Boolean.TRUE : source.getActive());
    }

    private String normalizeCode(String code) {
        return code == null ? "" : code.trim().toUpperCase();
    }

    private void validateCode(String code) {
        if (!CODE_PATTERN.matcher(code).matches()) {
            throw new IllegalArgumentException(
                    "Code must be 2–16 characters: uppercase letters, digits or hyphens only");
        }
    }

    private Map<Long, Long> enrolledCounts() {
        Map<Long, Long> counts = new HashMap<>();
        for (Object[] pair : baseObjectDAO.countEnrolledTestsByBody()) {
            counts.put((Long) pair[0], (Long) pair[1]);
        }
        return counts;
    }

    private AccreditingBodyView toView(AccreditingBody body, long enrolled, LocalDate asOf) {
        AccreditingBodyView view = new AccreditingBodyView();
        view.id = body.getId();
        view.code = body.getCode();
        view.name = body.getName();
        view.logoImageId = body.getLogoImageId();
        view.expiresOn = body.getExpiresOn();
        view.logoVisibilityMode = body.getLogoVisibilityMode() == null ? null : body.getLogoVisibilityMode().name();
        view.thresholdPct = body.getThresholdPct();
        view.displayOrder = body.getDisplayOrder();
        view.active = body.getActive();
        view.enrolledTestCount = enrolled;
        view.status = AccreditationStatus.of(body.getActive(), body.getExpiresOn(), asOf).name();
        return view;
    }
}
