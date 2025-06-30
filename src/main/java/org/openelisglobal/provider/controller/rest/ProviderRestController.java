package org.openelisglobal.provider.controller.rest;

import java.util.UUID;
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
}
