package org.openelisglobal.sample.util.CI.form;

import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrder;
import org.openelisglobal.patient.valueholder.ObservationData;
import org.openelisglobal.sample.form.ProjectData;

public interface IProjectForm {

    ProjectData getProjectData();

    void setProjectData(ProjectData projectData);

    ObservationData getObservations();

    void setObservations(ObservationData observationData);

    String getBirthDateForDisplay();

    void setBirthDateForDisplay(String birthDateForDisplay);

    String getSiteSubjectNumber();

    void setSiteSubjectNumber(String siteSubjectNumber);

    String getSubjectNumber();

    void setSubjectNumber(String subjectNumber);

    String getInterviewDate();

    String getReceivedDateForDisplay();

    String getInterviewTime();

    String getReceivedTimeForDisplay();

    Integer getCenterCode();

    String getPatientPK();

    String getSamplePK();

    String getLabNo();

    void setLabNo(String labNo);

    ElectronicOrder getElectronicOrder();

    void setElectronicOrder(ElectronicOrder electronicOrder);

    String getPatientFhirUuid();

    void setPatientFhirUuid(String patientFhirUuid);

    String getUpidCode();

    void setUpidCode(String upidCode);
}
