package org.openelisglobal.analyzer.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Entity preview for mapping preview result
 * 
 */
public class EntityPreview {
    private List<Map<String, Object>> tests;
    private List<Map<String, Object>> results;
    private Map<String, Object> sample;

    public EntityPreview() {
        this.tests = new ArrayList<>();
        this.results = new ArrayList<>();
        this.sample = new HashMap<>();
    }

    public List<Map<String, Object>> getTests() {
        return tests;
    }

    public void setTests(List<Map<String, Object>> tests) {
        this.tests = tests;
    }

    public List<Map<String, Object>> getResults() {
        return results;
    }

    public void setResults(List<Map<String, Object>> results) {
        this.results = results;
    }

    public Map<String, Object> getSample() {
        return sample;
    }

    public void setSample(Map<String, Object> sample) {
        this.sample = sample;
    }
}
