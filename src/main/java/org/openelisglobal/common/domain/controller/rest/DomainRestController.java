package org.openelisglobal.common.domain.controller.rest;

import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.common.domain.Domain;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * The single endpoint that serves the catalog domain list to the UI, so tests,
 * sample types, and results all render the same options without hard-coding
 * them. Backed by {@link Domain}, the one source of truth.
 */
@RestController
@RequestMapping("/rest")
public class DomainRestController {

    public static class DomainDto {
        public String id;
        public String labelKey;

        public DomainDto(Domain domain) {
            this.id = domain.name();
            this.labelKey = domain.getLabelKey();
        }
    }

    @GetMapping(value = "/domains", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<DomainDto> listDomains() {
        List<DomainDto> domains = new ArrayList<>();
        for (Domain domain : Domain.values()) {
            domains.add(new DomainDto(domain));
        }
        return domains;
    }
}
