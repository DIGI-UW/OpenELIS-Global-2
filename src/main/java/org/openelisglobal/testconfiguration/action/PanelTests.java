package org.openelisglobal.testconfiguration.action;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.spring.util.SpringContext;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.typeofsample.service.TypeOfSamplePanelService;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.openelisglobal.typeofsample.valueholder.TypeOfSamplePanel;

public class PanelTests {
    private IdValuePair panelIdValuePair;
    private IdValuePair sampleTypeIdValuePair;
    private List<IdValuePair> tests = new ArrayList<>();
    private List<IdValuePair> availableTests = new ArrayList<>();

    TypeOfSampleService typeOfSampleService = SpringContext.getBean(TypeOfSampleService.class);
    TypeOfSamplePanelService typeOfSamplePanelService = SpringContext.getBean(TypeOfSamplePanelService.class);

    public PanelTests(IdValuePair panelPair) {
        panelIdValuePair = panelPair;
        List<TypeOfSample> typeOfSamples = new ArrayList<>();
        List<TypeOfSamplePanel> typeOfSamplePanels = typeOfSamplePanelService
                .getTypeOfSamplePanelsForPanel(panelPair.getId());

        for (TypeOfSamplePanel typeOfSamplePanel : typeOfSamplePanels) {
            typeOfSamples.add(typeOfSampleService.getTypeOfSampleById(typeOfSamplePanel.getTypeOfSampleId()));
        }
        if (typeOfSamples != null && typeOfSamples.size() > 0) {
            TypeOfSample typeOfSample = typeOfSamples.get(0);
            sampleTypeIdValuePair = new IdValuePair(typeOfSample.getId(), typeOfSample.getLocalizedName());
        }
    }

    public PanelTests() {
    }

    public IdValuePair getPanelIdValuePair() {
        return panelIdValuePair;
    }

    public void setPanelIdValuePair(IdValuePair panelIdValuePair) {
        this.panelIdValuePair = panelIdValuePair;
    }

    public IdValuePair getSampleTypeIdValuePair() {
        return sampleTypeIdValuePair;
    }

    public void setSampleTypeIdValuePair(IdValuePair sampleTypeIdValuePair) {
        this.sampleTypeIdValuePair = sampleTypeIdValuePair;
    }

    public List<IdValuePair> getTests() {
        return tests;
    }

    public void setTests(List<IdValuePair> tests, Set<String> testIdSet) {
        this.tests = tests;
        List<Test> allTests = typeOfSampleService.getAllTestsBySampleTypeId(sampleTypeIdValuePair.getId());

        for (Test test : allTests) {
            if (!testIdSet.contains(test.getId())) {
                availableTests.add(new IdValuePair(test.getId(), test.getLocalizedName()));
            }
        }
    }

    public List<IdValuePair> getAvailableTests() {
        return availableTests;
    }

    public void setAvailableTests(List<IdValuePair> availableTests) {
        this.availableTests = availableTests;
    }
}
