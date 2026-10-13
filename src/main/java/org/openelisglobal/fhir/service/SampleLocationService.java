package org.openelisglobal.fhir.service;

import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.vector.valueholder.VectorSamplingSite;

/**
 * Where an environmental or vector sample was collected, and the organization
 * whose site Location stands for that place in FHIR.
 */
public interface SampleLocationService {

    /**
     * Where the sample was collected: the sample item's own collection site, else
     * the order's environmental sampling site, else its vector collection site.
     * Null when none was recorded.
     */
    VectorSamplingSite getSamplingSite(Sample sample, SampleItem sampleItem);

    /**
     * The organization that stands for a sampling site, when it has a FHIR id; its
     * site Location is the subject of a patientless ServiceRequest.
     */
    Organization getSamplingSiteOrganization(VectorSamplingSite site);
}
