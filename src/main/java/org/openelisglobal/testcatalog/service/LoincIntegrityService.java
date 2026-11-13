package org.openelisglobal.testcatalog.service;

import java.util.ArrayList;
import java.util.List;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.service.TestServiceImpl;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.testterminology.service.TestTerminologyMappingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * LOINC integrity guardrails (FR-15 to FR-18): the warnings the Terminology
 * section shows, and that activation re-surfaces (OGC-1119). Warnings only,
 * never a hard block.
 *
 * <p>
 * Grounded in the resolver: an active, orderable test with no LOINC anywhere is
 * never matched by {@code getActiveTestsByLoinc}, so analyzer and electronic
 * results cannot reach it; two active tests sharing a LOINC are resolved to the
 * first match, so results for that code may land on the wrong test.
 */
@Service
public class LoincIntegrityService {

    public static class TestRef {
        public String testId;
        public String name;
    }

    public static class LoincIntegrity {
        public String loinc;
        public boolean active;
        public boolean noLoinc;
        public List<TestRef> duplicates = new ArrayList<>();
    }

    private final TestService testService;

    private final TestTerminologyMappingService terminologyService;

    public LoincIntegrityService(TestService testService, TestTerminologyMappingService terminologyService) {
        this.testService = testService;
        this.terminologyService = terminologyService;
    }

    /**
     * A mapping on a component or a single specimen is still a LOINC the resolver
     * can match, so only a test with no LOINC at all is flagged; the duplicate
     * check runs across the whole active catalog because the resolver is not
     * scoped.
     */
    @Transactional(readOnly = true)
    public LoincIntegrity check(Test test) {
        LoincIntegrity integrity = new LoincIntegrity();
        integrity.loinc = test.getLoinc();
        integrity.active = test.isActive();
        integrity.noLoinc = test.isActive() && Boolean.TRUE.equals(test.getOrderable())
                && GenericValidator.isBlankOrNull(test.getLoinc())
                && !terminologyService.hasActiveMappingForSource(test.getId(), "LOINC");
        if (!GenericValidator.isBlankOrNull(test.getLoinc())) {
            for (Test other : testService.getActiveTestsByLoinc(test.getLoinc())) {
                if (other.getId() != null && !other.getId().equals(test.getId())) {
                    TestRef ref = new TestRef();
                    ref.testId = other.getId();
                    ref.name = TestServiceImpl.getLocalizedTestNameWithType(other);
                    integrity.duplicates.add(ref);
                }
            }
        }
        return integrity;
    }
}
