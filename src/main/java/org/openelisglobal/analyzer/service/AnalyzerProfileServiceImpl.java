package org.openelisglobal.analyzer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.openelisglobal.analyzer.dao.AnalyzerProfileApplicationDAO;
import org.openelisglobal.analyzer.dao.AnalyzerProfileDAO;
import org.openelisglobal.analyzer.valueholder.AnalyzerProfile;
import org.openelisglobal.analyzer.valueholder.AnalyzerProfileApplication;
import org.openelisglobal.common.dao.BaseDAO;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.service.BaseObjectServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AnalyzerProfileServiceImpl extends BaseObjectServiceImpl<AnalyzerProfile, String>
        implements AnalyzerProfileService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final AnalyzerProfileDAO analyzerProfileDAO;
    private final AnalyzerProfileApplicationDAO analyzerProfileApplicationDAO;

    @Autowired
    public AnalyzerProfileServiceImpl(AnalyzerProfileDAO analyzerProfileDAO,
            AnalyzerProfileApplicationDAO analyzerProfileApplicationDAO) {
        super(AnalyzerProfile.class);
        this.analyzerProfileDAO = analyzerProfileDAO;
        this.analyzerProfileApplicationDAO = analyzerProfileApplicationDAO;
    }

    @Override
    protected BaseDAO<AnalyzerProfile, String> getBaseObjectDAO() {
        return analyzerProfileDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnalyzerProfile> listBySource(String source) {
        return analyzerProfileDAO.findBySource(source);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AnalyzerProfile> listByMetaId(String profileMetaId) {
        return analyzerProfileDAO.findByMetaId(profileMetaId);
    }

    @Override
    @Transactional(readOnly = true)
    public AnalyzerProfile getLatestByMetaId(String profileMetaId) {
        return analyzerProfileDAO.findLatestByMetaId(profileMetaId);
    }

    @Override
    public String importProfile(Map<String, Object> profilePayload, String source, String sysUserId) {
        @SuppressWarnings("unchecked")
        Map<String, Object> meta = (Map<String, Object>) profilePayload.get("profileMeta");
        if (meta == null) {
            throw new LIMSRuntimeException("profileMeta is required");
        }
        String profileMetaId = (String) meta.get("id");
        String version = (String) meta.get("version");
        String displayName = (String) meta.get("displayName");
        if (profileMetaId == null || profileMetaId.trim().isEmpty()) {
            throw new LIMSRuntimeException("profileMeta.id is required");
        }
        if (version == null || version.trim().isEmpty()) {
            throw new LIMSRuntimeException("profileMeta.version is required");
        }
        if (displayName == null || displayName.trim().isEmpty()) {
            displayName = profileMetaId + " " + version;
        }

        if (analyzerProfileDAO.existsByMetaIdAndVersion(profileMetaId, version)) {
            throw new LIMSRuntimeException("Duplicate profile: " + profileMetaId + " " + version);
        }

        try {
            String profileJson = OBJECT_MAPPER.writeValueAsString(profilePayload);
            String checksum = computeSha256(profileJson);

            AnalyzerProfile profile = new AnalyzerProfile();
            profile.setProfileMetaId(profileMetaId);
            profile.setProfileMetaVersion(version);
            profile.setDisplayName(displayName);
            profile.setSource(source);
            profile.setProfileJson(profileJson);
            profile.setChecksumSha256(checksum);
            profile.setIsMutable(true);
            profile.setIsLatest(false);
            profile.setCreatedBy(sysUserId);
            profile.setUpdatedBy(sysUserId);

            if (meta.get("compatMinVersion") != null) {
                profile.setCompatMinVersion((String) meta.get("compatMinVersion"));
            }
            if (meta.get("compatMaxVersion") != null) {
                profile.setCompatMaxVersion((String) meta.get("compatMaxVersion"));
            }

            return analyzerProfileDAO.insert(profile);
        } catch (LIMSRuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error importing profile", e);
        }
    }

    @Override
    public void applyProfileToAnalyzer(String analyzerId, String profileId, String sysUserId) {
        Optional<AnalyzerProfile> opt = analyzerProfileDAO.get(profileId);
        if (opt.isEmpty()) {
            throw new LIMSRuntimeException("Profile not found: " + profileId);
        }
        AnalyzerProfile profile = opt.get();

        AnalyzerProfileApplication app = new AnalyzerProfileApplication();
        app.setAnalyzerId(Integer.valueOf(analyzerId));
        app.setSourceProfileId(profile.getId());
        app.setSourceProfileMetaId(profile.getProfileMetaId());
        app.setSourceProfileVersion(profile.getProfileMetaVersion());
        app.setAppliedBy(sysUserId);

        analyzerProfileApplicationDAO.insert(app);
    }

    private static String computeSha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new LIMSRuntimeException("Error computing checksum", e);
        }
    }
}
