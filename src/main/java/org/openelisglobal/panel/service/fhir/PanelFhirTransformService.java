package org.openelisglobal.panel.service.fhir;

import org.openelisglobal.panel.valueholder.Panel;

/**
 * Service for transforming OpenELIS Panels to FHIR List resources
 * and syncing them to OpenMRS FHIR server.
 * 
 * Panels are synced as FHIR List resources which become LabSet concepts in
 * OpenMRS:
 * - List.id = panel.guid (preserved as Concept.uuid)
 * - List.title = panel.description (concept name)
 * - List.entry[].item.reference = "ObservationDefinition/{test.guid}"
 * (setMembers)
 */
public interface PanelFhirTransformService {

    /**
     * Transform OpenELIS Panel to FHIR List and sync to configured OpenMRS servers.
     * 
     * @param panel The panel to sync
     */
    void syncPanelToFhir(Panel panel);
}
