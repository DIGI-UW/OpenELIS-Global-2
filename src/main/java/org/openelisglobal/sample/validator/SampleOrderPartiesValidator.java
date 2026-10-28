package org.openelisglobal.sample.validator;

import java.util.List;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.validator.ValidationHelper;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.provider.service.ProviderService;
import org.openelisglobal.sample.bean.SampleOrderItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.validation.Errors;
import org.springframework.validation.FieldError;

/**
 * Checks the referring site, site department and requester an order names by
 * id, for every form that saves an order. An id that names nothing is refused
 * with a field error naming it, instead of failing in the save with a missing
 * row or a NullPointerException that reached the caller as a bare HTTP 500, or
 * being ignored while the save answered 200 (a stale remembered site or
 * requester, one removed while the form was open, or an API client).
 */
@Component
public class SampleOrderPartiesValidator {

    private static final List<String> PARTY_FIELDS = List.of("sampleOrderItems.referringSiteId",
            "sampleOrderItems.referringSiteDepartmentId", "sampleOrderItems.providerPersonId");

    @Autowired
    private OrganizationService organizationService;
    @Autowired
    private PersonService personService;
    @Autowired
    private ProviderService providerService;

    public void validate(SampleOrderItem sampleOrder, Errors errors) {
        if (sampleOrder == null) {
            return;
        }
        validateOrganizationId(sampleOrder.getReferringSiteId(), "sampleOrderItems.referringSiteId", "referring site",
                errors);
        validateOrganizationId(sampleOrder.getReferringSiteDepartmentId(), "sampleOrderItems.referringSiteDepartmentId",
                "referring site department", errors);
        validateProviderPersonId(sampleOrder.getProviderPersonId(), errors);
    }

    /** The errors this validator raised, among all the errors of a form. */
    public static List<FieldError> partyErrors(Errors errors) {
        return errors.getFieldErrors().stream().filter(error -> PARTY_FIELDS.contains(error.getField())).toList();
    }

    private void validateOrganizationId(String organizationId, String field, String displayName, Errors errors) {
        if (GenericValidator.isBlankOrNull(organizationId)) {
            return;
        }
        ValidationHelper.validateIdField(organizationId, field, displayName, errors, false);
        if (errors.hasFieldErrors(field)) {
            return;
        }
        if (organizationService.getOrganizationById(organizationId) == null) {
            errors.rejectValue(field, "error.organization.notFound", new Object[] { displayName, organizationId },
                    "The " + displayName + " with id " + organizationId + " does not exist");
        }
    }

    private void validateProviderPersonId(String personId, Errors errors) {
        String field = "sampleOrderItems.providerPersonId";
        if (GenericValidator.isBlankOrNull(personId)) {
            return;
        }
        ValidationHelper.validateIdField(personId, field, "requester", errors, false);
        if (errors.hasFieldErrors(field)) {
            return;
        }
        Person person = personService.getPersonById(personId);
        if (person == null || providerService.getProviderByPerson(person) == null) {
            errors.rejectValue(field, "error.provider.notFound", new Object[] { personId },
                    "The requester with id " + personId + " does not exist");
        }
    }
}
