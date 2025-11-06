package org.openelisglobal.common.rest.provider.bean.patientHistory;

import java.util.List;

public class TestDisplay {

    // observations
    List<ResultDisplay> obs;
    String datatype;
    Double lowAbsolute;
    String display;
    String conceptUuid;
    String units;
    Double hiNormal;
    Double lowNormal;
    Double highCritical;
    Double lowCritical;

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

    public Double getLowAbsolute() {
        return lowAbsolute;
    }

    public void setLowAbsolute(Double lowAbsolute) {
        this.lowAbsolute = lowAbsolute;
    }

    public String getDisplay() {
        return display;
    }

    public void setDisplay(String display) {
        this.display = display;
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

    public Double getHighCritical() {
        return highCritical;
    }

    public void setHighCritical(Double highCritical) {
        this.highCritical = highCritical;
    }

    public Double getLowCritical() {
        return lowCritical;
    }

    public void setLowCritical(Double lowCritical) {
        this.lowCritical = lowCritical;
    }
}
