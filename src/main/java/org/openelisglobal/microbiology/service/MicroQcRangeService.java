package org.openelisglobal.microbiology.service;

import java.math.BigDecimal;
import java.util.Optional;
import org.openelisglobal.microbiology.dao.MicroQcRangeDAO;
import org.openelisglobal.microbiology.valueholder.MicroAstMethod;
import org.openelisglobal.microbiology.valueholder.MicroQcRange;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MicroQcRangeService {
    private final MicroQcRangeDAO ranges;

    public MicroQcRangeService(MicroQcRangeDAO ranges) {
        this.ranges = ranges;
    }

    public Optional<MicroQcRange> find(String qcStrain, String antibioticId, MicroAstMethod method, String diskContent,
            String standardId) {
        return ranges.find(trimToNull(qcStrain), antibioticId, method.name(), trimToNull(diskContent), standardId);
    }

    @Transactional
    public boolean upsert(String qcStrain, String antibioticId, MicroAstMethod method, String diskContent,
            String standardId, BigDecimal low, BigDecimal high) {
        String strain = trimToNull(qcStrain);
        String antibiotic = trimToNull(antibioticId);
        String standard = trimToNull(standardId);
        String disk = trimToNull(diskContent);
        if (strain == null || antibiotic == null || standard == null || method == null) {
            throw new IllegalArgumentException("QC range needs a strain, antibiotic, method and standard");
        }
        if (low == null || high == null) {
            throw new IllegalArgumentException("QC range needs a low and a high value");
        }
        if (low.compareTo(high) > 0) {
            throw new IllegalArgumentException("QC range low value " + low + " is above its high value " + high);
        }
        if (method == MicroAstMethod.MIC && disk != null) {
            throw new IllegalArgumentException("A MIC QC range cannot carry a disk content");
        }
        Optional<MicroQcRange> existing = ranges.find(strain, antibiotic, method.name(), disk, standard);
        if (existing.isPresent()) {
            MicroQcRange range = existing.get();
            if (range.getLow().compareTo(low) == 0 && range.getHigh().compareTo(high) == 0) {
                return false;
            }
            range.setLow(low);
            range.setHigh(high);
            ranges.update(range);
            return true;
        }
        MicroQcRange range = new MicroQcRange();
        range.setQcStrain(strain);
        range.setAntibioticId(antibiotic);
        range.setMethod(method.name());
        range.setDiskContent(disk);
        range.setStandardId(standard);
        range.setLow(low);
        range.setHigh(high);
        ranges.insert(range);
        return true;
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
