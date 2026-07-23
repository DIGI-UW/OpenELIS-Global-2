package org.openelisglobal.provider.controller.rest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.apache.commons.validator.GenericValidator;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.provider.service.ProviderService;
import org.openelisglobal.provider.valueholder.Provider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/rest")
public class ProviderRestController {

    @Autowired
    private ProviderService providerService;
    @Autowired
    private PersonService personService;

    @GetMapping(value = "/Provider/raw/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Provider> getProvider(@PathVariable String id) {
        Provider provider = providerService.get(id);
        return ResponseEntity.ok(provider);
    }

    @GetMapping(value = "/Provider/Person/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<Person> getPerson(@PathVariable String id) {
        Person person = personService.get(id);
        return ResponseEntity.ok(person);
    }

    @PostMapping(value = "/Provider/FhirUuid", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public ResponseEntity<?> insertOrUpdateProviderByFhirUuid(@RequestParam(required = false) UUID fhirUuid,
            @RequestBody Provider provider) {
        try {
            if (fhirUuid == null) {
                fhirUuid = UUID.randomUUID();
            }
            Provider updatedProvider = providerService.insertOrUpdateProviderByFhirUuid(fhirUuid, provider);
            return ResponseEntity.ok(updatedProvider);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing request.");
        }
    }

    @GetMapping(value = "/provider/search", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, Object>> searchProviders(@RequestParam(required = false) String search,
            @RequestParam(required = false) String phone, @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {

        try {
            List<Provider> providers;
            int totalCount;
            int startRecNo = ((page - 1) * pageSize) + 1;

            if (!GenericValidator.isBlankOrNull(phone)) {
                providers = providerService.getPagesOfSearchedProvidersByPhone(startRecNo, phone);
                totalCount = providerService.getTotalSearchedProviderCountByPhone(phone);
            } else if (!GenericValidator.isBlankOrNull(search)) {
                providers = providerService.getPagesOfSearchedProviders(startRecNo, search);
                totalCount = providerService.getTotalSearchedProviderCount(search);
            } else {
                providers = providerService.getPageOfProviders(startRecNo);
                totalCount = (int) providerService.getCount();
            }

            // DAOs fetch pageSize+1 rows for "has next page" detection — trim to requested
            // size
            if (providers.size() > pageSize) {
                providers = providers.subList(0, pageSize);
            }

            List<Map<String, Object>> results = new ArrayList<>();
            for (Provider provider : providers) {
                Map<String, Object> providerData = new HashMap<>();
                providerData.put("id", provider.getId());

                if (provider.getPerson() != null) {
                    personService.getData(provider.getPerson());
                    providerData.put("personId", provider.getPerson().getId());
                    providerData.put("firstName", provider.getPerson().getFirstName());
                    providerData.put("lastName", provider.getPerson().getLastName());

                    String fullName = "";
                    if (provider.getPerson().getLastName() != null) {
                        fullName = provider.getPerson().getLastName();
                    }
                    if (provider.getPerson().getFirstName() != null) {
                        if (!fullName.isEmpty()) {
                            fullName += ", ";
                        }
                        fullName += provider.getPerson().getFirstName();
                    }
                    providerData.put("name", fullName);

                    String providerPhone = provider.getPerson().getPrimaryPhone();
                    if (GenericValidator.isBlankOrNull(providerPhone)) {
                        providerPhone = provider.getPerson().getWorkPhone();
                    }
                    providerData.put("phone", providerPhone);
                    providerData.put("fax", provider.getPerson().getFax());
                    providerData.put("email", provider.getPerson().getEmail());
                }

                providerData.put("externalId", provider.getExternalId());
                providerData.put("isActive", "Y".equals(provider.getActive()));

                results.add(providerData);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("providers", results);
            response.put("totalCount", totalCount);
            response.put("page", page);
            response.put("pageSize", pageSize);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            LogEvent.logError(this.getClass().getName(), "searchProviders",
                    "Error searching providers: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
        }
    }
}
