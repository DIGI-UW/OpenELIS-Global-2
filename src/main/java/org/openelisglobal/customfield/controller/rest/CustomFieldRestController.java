package org.openelisglobal.customfield.controller.rest;

import java.util.List;

import org.openelisglobal.customfield.service.CustomFieldService;
import org.openelisglobal.customfield.valueholder.CustomField;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/rest/custom-fields")
public class CustomFieldRestController {

    @Autowired
    private CustomFieldService customFieldService;

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<CustomField>> getActiveCustomFields() {
        List<CustomField> fields = customFieldService.getActiveCustomFields();
        return ResponseEntity.ok(fields);
    }

    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CustomField> getCustomFieldById(@PathVariable String id) {
        CustomField field = customFieldService.get(id);
        return ResponseEntity.ok(field);
    }

    @GetMapping(value = "/all", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<CustomField>> getAllCustomFields() {
        List<CustomField> fields = customFieldService.getAll();
        return ResponseEntity.ok(fields);
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CustomField> createCustomField(@RequestBody CustomField customField) {
        customField.setIsActive(true);
        String id = customFieldService.insert(customField);
        CustomField saved = customFieldService.get(id);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CustomField> updateCustomField(@PathVariable String id,
            @RequestBody CustomField customField) {
        customField.setId(id);
        CustomField updated = customFieldService.save(customField);
        return ResponseEntity.ok(updated);
    }

    @PutMapping(value = "/{id}/deactivate", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CustomField> deactivateCustomField(@PathVariable String id) {
        CustomField field = customFieldService.get(id);
        field.setIsActive(false);
        CustomField updated = customFieldService.save(field);
        return ResponseEntity.ok(updated);
    }
}
