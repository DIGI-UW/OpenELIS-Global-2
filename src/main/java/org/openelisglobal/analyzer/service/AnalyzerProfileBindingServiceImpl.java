package org.openelisglobal.analyzer.service;

import org.openelisglobal.analyzer.dao.AnalyzerProfileBindingDAO;
import org.openelisglobal.analyzer.valueholder.Analyzer;
import org.openelisglobal.analyzer.valueholder.AnalyzerProfileBinding;
import org.openelisglobal.analyzer.valueholder.CommunicationMode;
import org.openelisglobal.analyzer.valueholder.ProtocolVersion;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.service.BaseObjectServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AnalyzerProfileBindingServiceImpl extends BaseObjectServiceImpl<AnalyzerProfileBinding, String>
        implements AnalyzerProfileBindingService {

    private final AnalyzerProfileBindingDAO bindingDAO;
    private final BridgeProfileCatalogService catalogService;
    private final AnalyzerSiteBindingService siteBindingService;

    @Autowired
    public AnalyzerProfileBindingServiceImpl(AnalyzerProfileBindingDAO bindingDAO,
            BridgeProfileCatalogService catalogService, AnalyzerSiteBindingService siteBindingService) {
        super(AnalyzerProfileBinding.class);
        this.bindingDAO = bindingDAO;
        this.catalogService = catalogService;
        this.siteBindingService = siteBindingService;
    }

    @Override
    protected BaseDAO<AnalyzerProfileBinding, String> getBaseObjectDAO() {
        return bindingDAO;
    }

    @Override
    public AnalyzerProfileBinding resolveActiveRevision(String profileId, int profileRevision, String sysUserId) {
        return resolveActiveProfile(profileId, profileRevision, sysUserId).binding();
    }

    private ResolvedProfile resolveActiveProfile(String profileId, int profileRevision, String sysUserId) {
        String normalizedProfileId = normalizeProfileId(profileId);
        if (profileRevision < 1) {
            throw new AnalyzerProfileBindingException("Profile revision must be at least 1");
        }

        BridgeAnalyzerProfile profile = findProfile(normalizedProfileId, profileRevision);
        if (!"ACTIVE".equals(profile.status())) {
            throw new AnalyzerProfileBindingException(
                    profileLabel(normalizedProfileId, profileRevision) + " is not active");
        }

        String fingerprint = profile.revisionFingerprint();
        AnalyzerProfileBinding binding = bindingDAO.findByProfileIdAndRevision(normalizedProfileId, profileRevision)
                .map(existing -> {
                    if (!fingerprint.equals(existing.getProfileFingerprint())) {
                        throw new AnalyzerProfileBindingException(
                                profileLabel(normalizedProfileId, profileRevision) + " changed fingerprint");
                    }
                    return existing;
                }).orElseGet(() -> createBinding(normalizedProfileId, profileRevision, fingerprint, sysUserId));
        return new ResolvedProfile(binding, profile);
    }

    @Override
    public AnalyzerProfileBinding assignProfile(Analyzer analyzer, String profileId, int profileRevision,
            String sysUserId) {
        if (analyzer == null) {
            throw new AnalyzerProfileBindingException("Analyzer is required");
        }
        String normalizedProfileId = normalizeProfileId(profileId);
        AnalyzerProfileBinding existing = analyzer.getPinnedProfileBinding();
        if (existing != null && normalizedProfileId.equals(existing.getProfileId())
                && profileRevision == existing.getProfileRevision()) {
            return existing;
        }

        ResolvedProfile resolved = resolveActiveProfile(normalizedProfileId, profileRevision, sysUserId);
        applyInitialDefaults(analyzer, resolved.profile());
        AnalyzerSiteBindingSnapshot siteBinding = siteBindingService.resolveInitialRevision(resolved.binding(),
                resolved.profile().document(), sysUserId);
        analyzer.setSiteBindingRevision(siteBinding.revision());
        analyzer.setProfileBinding(null);
        return resolved.binding();
    }

    @Override
    @Transactional(readOnly = true)
    public long getAnalyzerUsageCount(String bindingId) {
        return bindingDAO.countAnalyzersByBindingId(bindingId);
    }

    private BridgeAnalyzerProfile findProfile(String profileId, int profileRevision) {
        return catalogService.getCatalog().profiles().stream()
                .map(revision -> BridgeAnalyzerProfile.from(revision.profile()))
                .filter(profile -> profileId.equals(profile.profileId()) && profileRevision == profile.revision())
                .findFirst().orElseThrow(() -> new AnalyzerProfileBindingException(
                        profileLabel(profileId, profileRevision) + " was not found"));
    }

    private static void applyInitialDefaults(Analyzer analyzer, BridgeAnalyzerProfile profile) {
        if (analyzer.getType() == null || analyzer.getType().isBlank()) {
            analyzer.setType(profile.protocol());
        }
        if (analyzer.getProtocolVersion() == null && !"FILE".equals(profile.protocol())) {
            ProtocolVersion protocolVersion = profile.resolvedProtocolVersion();
            if (protocolVersion == null) {
                throw new AnalyzerProfileBindingException(
                        "Bridge profile " + profile.profileId() + " has an unsupported protocol version");
            }
            analyzer.setProtocolVersion(protocolVersion);
        }
        if (analyzer.getCommunicationMode() == null && profile.communicationMode() != null) {
            CommunicationMode communicationMode = profile.resolvedCommunicationMode();
            if (communicationMode == null) {
                throw new AnalyzerProfileBindingException(
                        "Bridge profile " + profile.profileId() + " has an unsupported communication mode");
            }
            analyzer.setCommunicationMode(communicationMode);
        }

        BridgeAnalyzerProfile.InstanceDefaults defaults = profile.instanceDefaults();
        if (analyzer.getPort() == null) {
            analyzer.setPort(defaults.port());
        }
        if (analyzer.getFileFormat() == null || analyzer.getFileFormat().isBlank()) {
            analyzer.setFileFormat(defaults.fileFormat());
        }
        if (analyzer.getFilePattern() == null || analyzer.getFilePattern().isBlank()) {
            analyzer.setFilePattern(defaults.filePattern());
        }
        if (analyzer.getHasHeader() == null) {
            analyzer.setHasHeader(defaults.hasHeader());
        }
        if (analyzer.getDelimiter() == null || analyzer.getDelimiter().isBlank()) {
            analyzer.setDelimiter(defaults.delimiter());
        }
        if (analyzer.getSkipRows() == null) {
            analyzer.setSkipRows(defaults.skipRows());
        }
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

    private record ResolvedProfile(AnalyzerProfileBinding binding, BridgeAnalyzerProfile profile) {
    }
}
