package org.openelisglobal.program.service;

import java.util.List;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.openelisglobal.program.valueholder.ProgramSample;
import org.openelisglobal.sample.service.SampleLabUnitScopeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/**
 * The lab-unit scope of the pathology, immunohistochemistry and cytology
 * dashboards and case views: a case is read by the users who may read its order
 * ({@link SampleLabUnitScopeService}), so a case outside the user's lab units
 * is neither listed, counted nor opened.
 */
@Service
public class ProgramSampleLabUnitScope {

    @Autowired
    private SampleLabUnitScopeService sampleLabUnitScopeService;

    /** True when the user reads only some lab units. */
    @Transactional(readOnly = true)
    public boolean isRestricted(String sysUserId) {
        return !sampleLabUnitScopeService.restrictedSectionIds(sysUserId).isEmpty();
    }

    /** The cases of the list whose order the user may read, in list order. */
    @Transactional(readOnly = true)
    public <T extends ProgramSample> List<T> visible(String sysUserId, List<T> cases) {
        if (!isRestricted(sysUserId)) {
            return cases;
        }
        return cases.stream().filter(c -> c != null && sampleLabUnitScopeService.isVisible(sysUserId, c.getSample()))
                .collect(Collectors.toList());
    }

    /**
     * How many cases from the source the user may read and that match. The source
     * is read inside this transaction, so the match may follow lazy relations.
     */
    @Transactional(readOnly = true)
    public <T extends ProgramSample> long count(String sysUserId, Supplier<List<T>> source, Predicate<T> match) {
        return visible(sysUserId, source.get()).stream().filter(match).count();
    }

    /** Refuses, as not found, a case whose order the user may not read. */
    @Transactional(readOnly = true)
    public void requireVisible(String sysUserId, ProgramSample programSample) {
        if (programSample == null || !sampleLabUnitScopeService.isVisible(sysUserId, programSample.getSample())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
    }
}
