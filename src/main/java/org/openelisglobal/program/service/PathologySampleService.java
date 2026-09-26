package org.openelisglobal.program.service;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import org.openelisglobal.common.service.BaseObjectService;
import org.openelisglobal.program.controller.pathology.PathologySampleForm;
import org.openelisglobal.program.valueholder.pathology.PathologyBlock;
import org.openelisglobal.program.valueholder.pathology.PathologySample;
import org.openelisglobal.program.valueholder.pathology.PathologySample.PathologyStatus;
import org.openelisglobal.program.valueholder.pathology.PathologySlide;
import org.openelisglobal.systemuser.valueholder.SystemUser;

public interface PathologySampleService extends BaseObjectService<PathologySample, Integer> {

    /** Audit verb recorded when a block is retired. */
    String PATHOLOGY_BLOCK_DEACTIVATED = "PATHOLOGY_BLOCK_DEACTIVATED";

    /** Audit verb recorded when a slide is retired. */
    String PATHOLOGY_SLIDE_DEACTIVATED = "PATHOLOGY_SLIDE_DEACTIVATED";

    List<PathologySample> getWithStatus(List<PathologyStatus> statuses);

    List<PathologySample> searchWithStatusAndTerm(List<PathologyStatus> statuses, String searchTerm);

    void assignTechnician(Integer pathologySampleId, SystemUser systemUser, String curUserId);

    void assignPathologist(Integer pathologySampleId, SystemUser systemUser, String curUserId);

    Long getCountWithStatus(List<PathologyStatus> statuses);

    Long getCountWithOpenRequests();

    Long getCountWithStatusBetweenDates(List<PathologyStatus> statuses, Timestamp from, Timestamp to);

    void updateWithFormValues(Integer pathologySampleId, PathologySampleForm form);

    /**
     * Retires a block, keeping the row and the designation it carries. A block is
     * held for years after its case closes and a label printed from it must still
     * resolve, so a block is taken out of use rather than deleted.
     *
     * @return the block, or empty when no block carries that id
     */
    Optional<PathologyBlock> deactivateBlock(Integer blockId, String reason, String sysUserId);

    /**
     * Retires a slide, keeping the row and the designation it carries, on the same
     * reasoning as {@link #deactivateBlock}.
     *
     * @return the slide, or empty when no slide carries that id
     */
    Optional<PathologySlide> deactivateSlide(Integer slideId, String reason, String sysUserId);
}
