package org.openelisglobal.requester.controller.rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.TableIdService;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.person.valueholder.Person;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Search endpoint for the Environmental/Vector "Requestor" contact person,
 * independent of Provider (Clinical-only) and Organization. Mirrors
 * ProviderRestController's /provider/search shape so the frontend type-ahead
 * pattern is identical across all three requester domains.
 *
 * <p>
 * The response carries contact PII (name, phone, fax, email, department), so
 * access is restricted to the roles that run order entry. There is no
 * {@code system_module_url} row for {@code /rest/requestor/search} and
 * {@code ModuleAuthenticationInterceptor} fails open for unmapped {@code /rest}
 * paths, so the guard has to be declared here.
 */
@RestController
@RequestMapping("/rest")
@PreAuthorize("hasAnyRole('RECEPTION', 'RESULTS', 'VALIDATION', 'ADMIN')")
public class RequestorRestController {

    @Autowired
    private PersonService personService;

    @GetMapping(value = "/requestor/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> searchRequestors(@RequestParam(required = false) String search,
            @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "20") int pageSize) {

        try {
            long requestorContactTypeId = TableIdService.getInstance().REQUESTOR_CONTACT_REQUESTER_TYPE_ID;
            List<Person> persons;
            int totalCount;

            if (!GenericValidator.isBlankOrNull(search)) {
                int startRecNo = ((page - 1) * pageSize) + 1;
                persons = personService.getPagesOfSearchedRequestorContacts(startRecNo, search, requestorContactTypeId);
                totalCount = personService.getTotalSearchedRequestorContactCount(search, requestorContactTypeId);
            } else {
                persons = new ArrayList<>();
                totalCount = 0;
            }

            if (persons.size() > pageSize) {
                persons = persons.subList(0, pageSize);
            }

            List<Map<String, Object>> results = new ArrayList<>();
            for (Person person : persons) {
                Map<String, Object> requestorData = new HashMap<>();
                requestorData.put("id", person.getId());
                requestorData.put("personId", person.getId());
                requestorData.put("firstName", person.getFirstName());
                requestorData.put("lastName", person.getLastName());

                String fullName = "";
                if (person.getLastName() != null) {
                    fullName = person.getLastName();
                }
                if (person.getFirstName() != null) {
                    if (!fullName.isEmpty()) {
                        fullName += ", ";
                    }
                    fullName += person.getFirstName();
                }
                requestorData.put("name", fullName);
                requestorData.put("phone", person.getWorkPhone());
                requestorData.put("fax", person.getFax());
                requestorData.put("email", person.getEmail());
                requestorData.put("department", person.getDepartment());

                results.add(requestorData);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("requestors", results);
            response.put("totalCount", totalCount);
            response.put("page", page);
            response.put("pageSize", pageSize);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getName(), "searchRequestors",
                    "Error searching requestors: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}
