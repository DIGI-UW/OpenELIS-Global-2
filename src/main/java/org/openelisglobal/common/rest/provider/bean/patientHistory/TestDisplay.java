package org.openelisglobal.common.rest.provider.bean.patientHistory;

import java.util.List;

/**
 * One series of the Patient History tree: the results of a single test, sample
 * type and result component. {@code display} is the tree label — the test name,
 * qualified with the component when the test has more than one — while
 * {@code testName}, {@code sampleType} and {@code component} keep the three
 * apart so the timeline can identify what a row actually measures.
 */
public class TestDisplay {

    // observations
    List<ResultDisplay> obs;
    String datatype;
    String display;
    String testName;
    String sampleType;
    String sampleTypeId;
    String component;
    String componentId;
    String conceptUuid;
    String units;
    String range;
    Double hiNormal;
    Double lowNormal;
    Double hiCritical;
    Double lowCritical;
    Double hiAbsolute;
    Double lowAbsolute;

    public List<ResultDisplay> getObs() {
        return obs;
    }

    public void setObs(List<ResultDisplay> obs) {
        this.obs = obs;
    }

    public String getDatatype() {
        return datatype;
    }

    public void setDatatype(String datatype) {
        this.datatype = datatype;
    }

    public String getDisplay() {
        return display;
    }

    public void setDisplay(String display) {
        this.display = display;
    }

    public String getTestName() {
        return testName;
    }

    public void setTestName(String testName) {
        this.testName = testName;
    }

    public String getSampleType() {
        return sampleType;
    }

    public void setSampleType(String sampleType) {
        this.sampleType = sampleType;
    }

    public String getSampleTypeId() {
        return sampleTypeId;
    }

    public void setSampleTypeId(String sampleTypeId) {
        this.sampleTypeId = sampleTypeId;
    }

    public String getComponent() {
        return component;
    }

    public void setComponent(String component) {
        this.component = component;
    }

    public String getComponentId() {
        return componentId;
    }

    public void setComponentId(String componentId) {
        this.componentId = componentId;
    }

    public String getConceptUuid() {
        return conceptUuid;
    }

    public void setConceptUuid(String conceptUuid) {
        this.conceptUuid = conceptUuid;
    }

    public String getUnits() {
        return units;
    }

    public void setUnits(String units) {
        this.units = units;
    }

    public String getRange() {
        return range;
    }

    public void setRange(String range) {
        this.range = range;
    }

    public Double getHiNormal() {
        return hiNormal;
    }

    public void setHiNormal(Double hiNormal) {
        this.hiNormal = hiNormal;
    }

    public Double getLowNormal() {
        return lowNormal;
    }

    public void setLowNormal(Double lowNormal) {
        this.lowNormal = lowNormal;
    }

    public Double getHiCritical() {
        return hiCritical;
    }

    public void setHiCritical(Double hiCritical) {
        this.hiCritical = hiCritical;
    }

    public Double getLowCritical() {
        return lowCritical;
    }

    public void setLowCritical(Double lowCritical) {
        this.lowCritical = lowCritical;
    }

    public Double getHiAbsolute() {
        return hiAbsolute;
    }

    public void setHiAbsolute(Double hiAbsolute) {
        this.hiAbsolute = hiAbsolute;
    }

    public Double getLowAbsolute() {
        return lowAbsolute;
    }

    public void setLowAbsolute(Double lowAbsolute) {
        this.lowAbsolute = lowAbsolute;
    }
}
