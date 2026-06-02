package org.openelisglobal.vector.service;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.vector.valueholder.VectorPool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class VectorPoolLabelServiceImpl implements VectorPoolLabelService {

    @Autowired
    private VectorPoolService vectorPoolService;

    @Override
    public String intakeLotLabel(String accession, int oneBasedPosition) {
        return String.format("%s-P%02d", accession, oneBasedPosition);
    }

    @Override
    public String intakeLotBase(VectorPool intakePool, Sample sample) {
        List<VectorPool> intakePools = vectorPoolService.getBySampleId(sample.getId()).stream()
                .filter(p -> p.getParentPool() == null).sorted(Comparator.comparing(VectorPool::getId))
                .collect(Collectors.toList());
        int idx = 0;
        for (int i = 0; i < intakePools.size(); i++) {
            if (intakePool.getId().equals(intakePools.get(i).getId())) {
                idx = i;
                break;
            }
        }
        return intakeLotLabel(sample.getAccessionNumber(), idx + 1);
    }

    @Override
    public String subPoolLabel(String parentBase, int position) {
        String label = parentBase + "-S" + position;
        if (label.length() > MAX_LABEL_LENGTH) {
            throw new IllegalStateException("Pool label exceeds maximum length of " + MAX_LABEL_LENGTH + ": " + label);
        }
        return label;
    }
}
