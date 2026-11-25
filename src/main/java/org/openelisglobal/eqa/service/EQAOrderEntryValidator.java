package org.openelisglobal.eqa.service;

import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.openelisglobal.eqa.valueholder.EQALabProgramEnrollment;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.valueholder.Sample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * What a new EQA order must name before it is saved (OGC-1483): one of the
 * lab's active enrollments, and a cycle, when it names one, that is an open
 * cycle of that enrollment's scheme. The cycle itself stays optional, since
 * some labs track cycles in other software. An order already recorded as EQA
 * keeps what it was saved with.
 */
@Service
public class EQAOrderEntryValidator {

    public static final String ENROLLMENT_FIELD = "sampleOrderItems.eqaProgramId";
    public static final String CYCLE_FIELD = "sampleOrderItems.eqaCycleId";

    @Autowired
    private EQALabProgramEnrollmentService enrollmentService;

    @Autowired
    private EQACycleService cycleService;

    @Autowired
    private SampleEQAService sampleEQAService;

    @Autowired
    private SampleService sampleService;

    /** A refused field and the message key that says why. */
    public record Refusal(String field, String code) {
    }

    @Transactional(readOnly = true)
    public Optional<Refusal> check(String labNumber, String enrollmentId, String cycleId) {
        Sample existing = StringUtils.isBlank(labNumber) ? null : sampleService.getSampleByAccessionNumber(labNumber);
        if (existing != null && existing.getId() != null
                && sampleEQAService.findBySampleId(Long.valueOf(existing.getId())).isPresent()) {
            return Optional.empty();
        }
        String enrollment = StringUtils.trimToNull(enrollmentId);
        if (enrollment == null) {
            return Optional.of(new Refusal(ENROLLMENT_FIELD, "order.eqa.schemeRequired"));
        }
        EQALabProgramEnrollment active = enrollmentService.findActiveEnrollments().stream()
                .filter(candidate -> enrollment.equals(String.valueOf(candidate.getId()))).findFirst().orElse(null);
        if (active == null) {
            return Optional.of(new Refusal(ENROLLMENT_FIELD, "order.eqa.schemeUnknown"));
        }
        String cycle = StringUtils.trimToNull(cycleId);
        Long cycleNumber = cycle == null ? null : parseId(cycle);
        if (cycle != null
                && (cycleNumber == null || !cycleService.isOpenCycleOfScheme(cycleNumber, active.getProgramName()))) {
            return Optional.of(new Refusal(CYCLE_FIELD, "order.eqa.cycleNotInScheme"));
        }
        return Optional.empty();
    }

    private static Long parseId(String value) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
