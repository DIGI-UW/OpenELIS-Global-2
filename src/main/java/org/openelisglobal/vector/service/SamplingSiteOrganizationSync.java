package org.openelisglobal.vector.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.organization.service.OrganizationIdentifierService;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.service.OrganizationTypeService;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.organization.valueholder.OrganizationIdentifier;
import org.openelisglobal.organization.valueholder.OrganizationType;
import org.openelisglobal.vector.valueholder.VectorSamplingSite;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * OGC-1363 (FR-A3, Dependency 3): a sampling site saved through the vector
 * endpoints (the old admin page, the order-entry site picker, the seed helpers)
 * keeps the organization it is linked to in step, creating it on the first
 * save, so the Locations menu reads the same code, name, status, contact, GPS,
 * description and location whichever side wrote them last.
 */
@Component
public class SamplingSiteOrganizationSync {

    static final String SITE_TYPE = "sampling site";

    @Autowired
    private OrganizationService organizationService;
    @Autowired
    private OrganizationTypeService organizationTypeService;
    @Autowired
    private OrganizationIdentifierService identifierService;

    @Transactional
    public void syncFromSite(VectorSamplingSite site, String sysUserId) {
        if (site == null || GenericValidator.isBlankOrNull(site.getName())) {
            return;
        }
        Organization organization = site.getOrganizationId() == null ? null
                : organizationService.get(String.valueOf(site.getOrganizationId()));
        boolean isNew = organization == null;
        if (isNew) {
            organization = new Organization();
            organization.setMlsSentinelLabFlag("N");
            organization.setFhirUuid(UUID.randomUUID());
            organization.setSource(GenericValidator.isBlankOrNull(site.getSource()) ? "LOCAL" : site.getSource());
        }
        organization.setOrganizationName(trimTo(site.getName(), 200));
        organization.setCode(trimTo(site.getCode(), 20));
        organization.setShortName(trimTo(site.getCode(), 15));
        organization.setIsActive(Boolean.FALSE.equals(site.getActive()) ? "N" : "Y");
        organization.setContactName(trimTo(site.getContactName(), 100));
        organization.setPhone(trimTo(site.getContactPhone(), 20));
        organization.setGpsLatitude(decimal(site.getGpsLatitude()));
        organization.setGpsLongitude(decimal(site.getGpsLongitude()));
        organization.setDescription(trimTo(site.getDescription(), 1000));
        Organization parent = GenericValidator.isBlankOrNull(site.getLocationOrgId())
                || !site.getLocationOrgId().matches("\\d+") ? null : organizationService.get(site.getLocationOrgId());
        organization.setOrganization(parent);
        organization.setSysUserId(sysUserId);
        if (isNew) {
            organization.setId(organizationService.insertUnchecked(organization));
            OrganizationType type = organizationTypeService.getOrganizationTypeByName(SITE_TYPE);
            if (type != null) {
                organizationService.linkOrganizationAndType(organization, type.getId());
            }
            site.setOrganizationId(Integer.valueOf(organization.getId()));
        } else {
            organizationService.updateUnchecked(organization);
        }
        Integer numericId = Integer.valueOf(organization.getId());
        List<OrganizationIdentifier> identifiers = identifierService.getForOrganization(numericId);
        boolean codeStored = identifiers.stream().anyMatch(identifier -> identifier.isReporting()
                && identifier.getValue().equalsIgnoreCase(site.getCode() == null ? "" : site.getCode().trim()));
        if (!codeStored && !GenericValidator.isBlankOrNull(site.getCode())) {
            List<OrganizationIdentifier> wanted = new java.util.ArrayList<>();
            wanted.add(new OrganizationIdentifier(numericId, OrganizationIdentifier.CODE_LABEL, site.getCode().trim(),
                    true));
            for (OrganizationIdentifier identifier : identifiers) {
                if (!identifier.isReporting()
                        && !OrganizationIdentifier.CODE_LABEL.equalsIgnoreCase(identifier.getLabel())) {
                    wanted.add(
                            new OrganizationIdentifier(numericId, identifier.getLabel(), identifier.getValue(), false));
                }
            }
            identifierService.replaceForOrganization(numericId, wanted, sysUserId);
        }
    }

    private static String trimTo(String text, int length) {
        if (GenericValidator.isBlankOrNull(text)) {
            return null;
        }
        String trimmed = text.trim();
        return trimmed.length() > length ? trimmed.substring(0, length) : trimmed;
    }

    private static BigDecimal decimal(String text) {
        if (GenericValidator.isBlankOrNull(text)) {
            return null;
        }
        try {
            return new BigDecimal(text.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
