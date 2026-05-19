package org.openelisglobal.accreditation.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import org.openelisglobal.accreditation.dao.AccreditingBodyDAO;
import org.openelisglobal.accreditation.valueholder.AccreditingBody;
import org.openelisglobal.accreditation.valueholder.AccreditingBody.LogoVisibilityMode;
import org.openelisglobal.common.exception.LIMSDuplicateRecordException;
import org.openelisglobal.common.exception.LIMSRuntimeException;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@DependsOn({ "springContext" })
public class AccreditingBodyServiceImpl extends AuditableBaseObjectServiceImpl<AccreditingBody, Long>
        implements AccreditingBodyService {

    @Value("${org.openelisglobal.accreditation.logo.dir:/var/lib/openelis-global/accreditation/logos/}")
    private String accreditationLogoDir;

    @Autowired
    private AccreditingBodyDAO baseObjectDAO;

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
    public AccreditingBody getByCode(String code) {
        return baseObjectDAO.findByCode(code);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccreditingBody> getAllActive() {
        return baseObjectDAO.findAllActive();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccreditingBody> getAllOrderedByDisplayOrder() {
        return baseObjectDAO.findAllOrderedByDisplayOrder();
    }

    @Override
    @Transactional(readOnly = true)
    public long countTestAccreditations(Long id) {
        return baseObjectDAO.countTestAccreditationsByBodyId(id);
    }

    @Override
    @Transactional
    public Long insert(AccreditingBody accreditingBody) {
        if (accreditingBody.getLogoVisibilityMode() == null) {
            accreditingBody.setLogoVisibilityMode(LogoVisibilityMode.ANY_ACCREDITED_TEST);
        }
        validateThresholdForPercentageMode(accreditingBody);
        if (accreditingBody.getThresholdPct() == null) {
            accreditingBody.setThresholdPct((short) 80);
        }
        if (accreditingBody.getDisplayOrder() == null) {
            accreditingBody.setDisplayOrder((short) 0);
        }
        if (accreditingBody.getActive() == null) {
            accreditingBody.setActive(true);
        }
        if (baseObjectDAO.findByCode(accreditingBody.getCode()) != null) {
            throw new LIMSDuplicateRecordException("Duplicate record exists for code: " + accreditingBody.getCode());
        }
        return super.insert(accreditingBody);
    }

    @Override
    @Transactional
    public AccreditingBody update(AccreditingBody accreditingBody) {
        Optional<AccreditingBody> existingOpt = baseObjectDAO.get(accreditingBody.getId());

        if (!existingOpt.isPresent()) {
            throw new LIMSRuntimeException("AccreditingBody not found with id: " + accreditingBody.getId());
        }

        AccreditingBody existing = existingOpt.get();

        if (!existing.getCode().equals(accreditingBody.getCode())) {
            throw new LIMSRuntimeException("Code is immutable after creation. Cannot change '" + existing.getCode()
                    + "' to '" + accreditingBody.getCode() + "'");
        }

        if (accreditingBody.getThresholdPct() == null) {
            accreditingBody.setThresholdPct(existing.getThresholdPct());
        }

        validateThresholdForPercentageMode(accreditingBody);

        return super.update(accreditingBody);
    }

    @Override
    @Transactional
    public void delete(AccreditingBody accreditingBody) {
        long refCount = baseObjectDAO.countTestAccreditationsByBodyId(accreditingBody.getId());
        if (refCount > 0) {
            throw new LIMSRuntimeException("Cannot delete accrediting body '" + accreditingBody.getCode()
                    + "': referenced by " + refCount + " test accreditation(s)");
        }
        super.delete(accreditingBody);
    }

    @Override
    @Transactional
    public AccreditingBody updateLogoPath(Long id, String logoPath, String sysUserId) {
        Optional<AccreditingBody> existingOpt = baseObjectDAO.get(id);

        if (!existingOpt.isPresent()) {
            throw new LIMSRuntimeException("AccreditingBody not found with id: " + id);
        }

        AccreditingBody existing = existingOpt.get();
        existing.setLogoPath(logoPath);
        existing.setSysUserId(sysUserId);
        return super.update(existing);
    }

    @Override
    @Transactional
    public AccreditingBody uploadLogo(Long id, MultipartFile file, String sysUserId) {
        Optional<AccreditingBody> existingOpt = baseObjectDAO.get(id);
        if (!existingOpt.isPresent()) {
            throw new LIMSRuntimeException("AccreditingBody not found with id: " + id);
        }
        AccreditingBody existing = existingOpt.get();

        try {
            // Ensure directory exists
            Path logoDir = Paths.get(accreditationLogoDir);
            if (!Files.exists(logoDir)) {
                Files.createDirectories(logoDir);
            }

            // Delete old logo if exists
            if (existing.getLogoPath() != null) {
                try {
                    Files.deleteIfExists(Paths.get(existing.getLogoPath()));
                } catch (IOException e) {
                    LogEvent.logError("Error deleting old logo file: " + existing.getLogoPath(), e);
                }
            }

            // Generate filename: {id}_{originalFilename}
            String filename = id + "_" + file.getOriginalFilename();
            Path filePath = logoDir.resolve(filename);

            // Save file
            file.transferTo(filePath.toFile());

            // Update entity
            return updateLogoPath(id, filePath.toAbsolutePath().toString(), sysUserId);
        } catch (IOException e) {
            LogEvent.logError("Error saving logo file", e);
            throw new LIMSRuntimeException("Failed to save logo file", e);
        }
    }

    @Override
    @Transactional
    public AccreditingBody removeLogo(Long id, String sysUserId) {
        Optional<AccreditingBody> existingOpt = baseObjectDAO.get(id);
        if (!existingOpt.isPresent()) {
            throw new LIMSRuntimeException("AccreditingBody not found with id: " + id);
        }
        AccreditingBody existing = existingOpt.get();

        if (existing.getLogoPath() != null) {
            try {
                Files.deleteIfExists(Paths.get(existing.getLogoPath()));
            } catch (IOException e) {
                LogEvent.logError("Error deleting logo file: " + existing.getLogoPath(), e);
                throw new LIMSRuntimeException("Failed to delete logo file", e);
            }
        }

        return updateLogoPath(id, null, sysUserId);
    }

    private void validateThresholdForPercentageMode(AccreditingBody body) {
        if (LogoVisibilityMode.PERCENTAGE.equals(body.getLogoVisibilityMode()) && body.getThresholdPct() == null) {
            throw new LIMSRuntimeException("threshold_pct is required when logo_visibility_mode is PERCENTAGE");
        }
    }
}
