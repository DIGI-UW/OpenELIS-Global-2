package org.openelisglobal.analyzer.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.openelisglobal.analyzer.dao.AnalyzerProfileBindingDAO;
import org.openelisglobal.analyzer.valueholder.AnalyzerProfileBinding;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.service.BaseObjectServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AnalyzerProfileBindingServiceImpl extends BaseObjectServiceImpl<AnalyzerProfileBinding, String>
        implements AnalyzerProfileBindingService {

    private static final String FINGERPRINT_PATTERN = "sha256:[0-9a-f]{64}";

    private final AnalyzerProfileBindingDAO bindingDAO;
    private final BridgeProfileCatalogService catalogService;

    @Autowired
    public AnalyzerProfileBindingServiceImpl(AnalyzerProfileBindingDAO bindingDAO,
            BridgeProfileCatalogService catalogService) {
        super(AnalyzerProfileBinding.class);
        this.bindingDAO = bindingDAO;
        this.catalogService = catalogService;
    }

    @Override
    protected BaseDAO<AnalyzerProfileBinding, String> getBaseObjectDAO() {
        return bindingDAO;
    }

    @Override
    public AnalyzerProfileBinding resolveActiveRevision(String profileId, int profileRevision, String sysUserId) {
        String normalizedProfileId = normalizeProfileId(profileId);
        if (profileRevision < 1) {
            throw new AnalyzerProfileBindingException("Profile revision must be at least 1");
        }

        JsonNode profile = findProfile(normalizedProfileId, profileRevision);
        if (!"ACTIVE".equals(profile.path("status").asText())) {
            throw new AnalyzerProfileBindingException(
                    profileLabel(normalizedProfileId, profileRevision) + " is not active");
        }

        String fingerprint = profile.path("revisionFingerprint").asText();
        if (!fingerprint.matches(FINGERPRINT_PATTERN)) {
            throw new AnalyzerProfileBindingException(
                    profileLabel(normalizedProfileId, profileRevision) + " has an invalid fingerprint");
        }

        return bindingDAO.findByProfileIdAndRevision(normalizedProfileId, profileRevision).map(existing -> {
            if (!fingerprint.equals(existing.getProfileFingerprint())) {
                throw new AnalyzerProfileBindingException(
                        profileLabel(normalizedProfileId, profileRevision) + " changed fingerprint");
            }
            return existing;
        }).orElseGet(() -> createBinding(normalizedProfileId, profileRevision, fingerprint, sysUserId));
    }

    @Override
    @Transactional(readOnly = true)
    public long getAnalyzerUsageCount(String bindingId) {
        return bindingDAO.countAnalyzersByBindingId(bindingId);
    }

    private JsonNode findProfile(String profileId, int profileRevision) {
        return catalogService.getCatalog().profiles().stream().map(BridgeProfileCatalog.ProfileRevision::profile)
                .filter(profile -> profileId.equals(profile.path("profileId").asText())
                        && profileRevision == profile.path("revision").asInt(-1))
                .findFirst().orElseThrow(() -> new AnalyzerProfileBindingException(
                        profileLabel(profileId, profileRevision) + " was not found"));
    }

    private AnalyzerProfileBinding createBinding(String profileId, int profileRevision, String fingerprint,
            String sysUserId) {
        AnalyzerProfileBinding binding = new AnalyzerProfileBinding();
        binding.setProfileId(profileId);
        binding.setProfileRevision(profileRevision);
        binding.setProfileFingerprint(fingerprint);
        binding.setSysUserId(sysUserId);
        bindingDAO.insert(binding);
        return binding;
    }

    private static String normalizeProfileId(String profileId) {
        if (profileId == null || profileId.trim().isEmpty()) {
            throw new AnalyzerProfileBindingException("Profile ID is required");
        }
        return profileId.trim();
    }

    private static String profileLabel(String profileId, int profileRevision) {
        return "Bridge profile " + profileId + " revision " + profileRevision;
    }
}
