package org.openelisglobal.provider.controller.rest;

import jakarta.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.common.rest.BaseRestController;
import org.openelisglobal.dictionary.service.DictionaryService;
import org.openelisglobal.dictionary.valueholder.Dictionary;
import org.openelisglobal.dictionarycategory.service.DictionaryCategoryService;
import org.openelisglobal.dictionarycategory.valueholder.DictionaryCategory;
import org.openelisglobal.provider.service.ProviderTitleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * The Provider Titles admin page (OGC-1223). Titles are the
 * {@code providerTitle} dictionary category, so this is a thin door onto that
 * one category rather than a second store.
 * <p>
 * There is no delete: a title that falls out of use is deactivated, and the
 * providers already carrying it keep it.
 */
@RestController
@RequestMapping("/rest/admin/provider-titles")
@PreAuthorize("hasRole('ADMIN')")
public class ProviderTitleAdminRestController extends BaseRestController {

    /** One title as the page lists it. */
    public static class ProviderTitleForm {
        public String id;
        public String title;
        public String abbreviation;
        public Integer sortOrder;
        public boolean active;
        public int inUse;
    }

    @Autowired
    private ProviderTitleService providerTitleService;

    @Autowired
    private DictionaryService dictionaryService;

    @Autowired
    private DictionaryCategoryService dictionaryCategoryService;

    @GetMapping
    public ResponseEntity<List<ProviderTitleForm>> list() {
        List<ProviderTitleForm> titles = new ArrayList<>();
        for (Dictionary entry : providerTitleService.getAllTitles()) {
            titles.add(toForm(entry));
        }
        return ResponseEntity.ok(titles);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody ProviderTitleForm body, HttpServletRequest request) {
        String rejection = validate(body, null);
        if (rejection != null) {
            return ResponseEntity.unprocessableEntity().body(rejection);
        }
        DictionaryCategory category = dictionaryCategoryService
                .getDictionaryCategoryByName(ProviderTitleService.CATEGORY_NAME);
        if (category == null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("the providerTitle dictionary category is missing");
        }
        Dictionary entry = new Dictionary();
        entry.setDictionaryCategory(category);
        apply(body, entry, request);
        dictionaryService.insert(entry);
        return ResponseEntity.ok(toForm(entry));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable String id, @RequestBody ProviderTitleForm body,
            HttpServletRequest request) {
        Dictionary entry = dictionaryService.get(id);
        if (entry == null) {
            return ResponseEntity.notFound().build();
        }
        String rejection = validate(body, id);
        if (rejection != null) {
            return ResponseEntity.unprocessableEntity().body(rejection);
        }
        apply(body, entry, request);
        dictionaryService.update(entry);
        return ResponseEntity.ok(toForm(entry));
    }

    @PatchMapping("/{id}/active")
    public ResponseEntity<?> setActive(@PathVariable String id, @RequestParam boolean active,
            HttpServletRequest request) {
        Dictionary entry = dictionaryService.get(id);
        if (entry == null) {
            return ResponseEntity.notFound().build();
        }
        entry.setIsActive(active ? "Y" : "N");
        entry.setSysUserId(getSysUserId(request));
        dictionaryService.update(entry);
        return ResponseEntity.ok(toForm(entry));
    }

    private void apply(ProviderTitleForm body, Dictionary entry, HttpServletRequest request) {
        entry.setDictEntry(body.title.trim());
        entry.setLocalAbbreviation(body.abbreviation.trim());
        entry.setSortOrder(body.sortOrder);
        entry.setIsActive(body.active ? "Y" : "N");
        entry.setSysUserId(getSysUserId(request));
    }

    /**
     * Title and abbreviation are each required and each unique among the active
     * titles, compared without case so "dr" cannot join "Dr".
     */
    private String validate(ProviderTitleForm body, String editingId) {
        if (body == null || body.title == null || body.title.isBlank()) {
            return "a title is required";
        }
        if (body.abbreviation == null || body.abbreviation.isBlank()) {
            return "an abbreviation is required";
        }
        if (body.title.trim().length() > 50) {
            return "a title is at most 50 characters";
        }
        if (body.abbreviation.trim().length() > 10) {
            return "an abbreviation is at most 10 characters";
        }
        for (Dictionary existing : providerTitleService.getAllTitles()) {
            if (existing.getId().equals(editingId) || !"Y".equals(existing.getIsActive())) {
                continue;
            }
            if (body.title.trim().equalsIgnoreCase(existing.getDictEntry())) {
                return "\"" + body.title.trim() + "\" is already in the list";
            }
            if (body.abbreviation.trim().equalsIgnoreCase(existing.getLocalAbbreviation())) {
                return "\"" + body.abbreviation.trim() + "\" is already in the list";
            }
        }
        return null;
    }

    private ProviderTitleForm toForm(Dictionary entry) {
        ProviderTitleForm form = new ProviderTitleForm();
        form.id = entry.getId();
        form.title = entry.getDictEntry();
        form.abbreviation = entry.getLocalAbbreviation();
        form.sortOrder = entry.getSortOrder();
        form.active = "Y".equals(entry.getIsActive());
        form.inUse = providerTitleService.countProvidersUsing(entry.getLocalAbbreviation());
        return form;
    }
}
