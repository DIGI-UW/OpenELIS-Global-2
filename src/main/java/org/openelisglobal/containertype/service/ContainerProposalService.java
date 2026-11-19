package org.openelisglobal.containertype.service;

import java.util.List;
import org.openelisglobal.containertype.form.ContainerProposalForm;
import org.openelisglobal.containertype.form.ContainerProposalView;

/**
 * Proposes the containers to draw for an order's samples (FR-B24, B25, G7, G8).
 */
public interface ContainerProposalService {

    /** One proposal per sample sent, in the order sent. */
    List<ContainerProposalView> propose(ContainerProposalForm form);

    /**
     * Whether a sample of the given type may be saved in the container: it exists,
     * yields the sample type, and is active unless the sample already had it.
     */
    boolean canHold(Long containerTypeId, String sampleTypeId, Long keptContainerTypeId);
}
