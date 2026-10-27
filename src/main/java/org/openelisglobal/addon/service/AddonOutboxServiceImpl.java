package org.openelisglobal.addon.service;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.openelisglobal.addon.dao.AddonDAO;
import org.openelisglobal.addon.dao.AddonOutboxDAO;
import org.openelisglobal.addon.dao.AddonOutboxDAO.Claim;
import org.openelisglobal.addon.valueholder.Addon;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AddonOutboxServiceImpl implements AddonOutboxService {

    static final int LEASE_SECONDS = 60;
    static final int FAN_OUT_BATCH = 500;

    @Autowired
    private AddonOutboxDAO outboxDAO;
    @Autowired
    private AddonDAO addonDAO;
    @Autowired
    private AddonSettings settings;

    @Override
    public List<Claim> claim(String worker, int limit) {
        outboxDAO.fanOut(FAN_OUT_BATCH);
        return outboxDAO.claimDue(worker, limit, LEASE_SECONDS);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Target> target(String addonId) {
        return addonDAO.get(addonId).filter(Addon::isEnabled).filter(a -> a.getEndpoint() != null)
                .map(a -> new Target(a.getEndpoint(), a.getEventSecret()));
    }

    @Override
    public void delivered(Claim claim, String worker, int statusCode) {
        outboxDAO.recordAttempt(claim.eventId(), claim.addonId(), worker, DELIVERED, statusCode, null, null);
    }

    @Override
    public void failed(Claim claim, String worker, Integer statusCode, String error) {
        int attempts = claim.attempts() + 1;
        String status = attempts >= MAX_ATTEMPTS ? FAILED : PENDING;
        outboxDAO.recordAttempt(claim.eventId(), claim.addonId(), worker, status, statusCode, error,
                Instant.now().plus(AddonOutboxService.backoff(attempts)));
    }

    @Override
    public void dropped(Claim claim, String worker) {
        outboxDAO.recordAttempt(claim.eventId(), claim.addonId(), worker, DROPPED, null, "add-on disabled", null);
    }

    @Override
    public int purge() {
        return outboxDAO.purgeOlderThan(settings.eventRetentionDays());
    }
}
