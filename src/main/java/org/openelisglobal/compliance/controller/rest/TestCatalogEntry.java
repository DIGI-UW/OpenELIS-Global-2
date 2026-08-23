package org.openelisglobal.compliance.controller.rest;

import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.Setter;

/**
 * Slim catalog row returned by {@code /rest/compliance/test-catalog}. Carries
 * the per-test metadata the compliance Link Test form needs (sample types,
 * result type, predefined select options) without leaking the underlying Test
 * entity's lazy associations.
 */
@Setter
@Getter
public class TestCatalogEntry {

    private String id;
    private String value;
    private String code;
    private String loinc;
    private List<String> sampleTypes = new ArrayList<>();
    private String resultType;
    private List<String> selectOptions = new ArrayList<>();

}
