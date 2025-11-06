package org.openelisglobal.program.valueholder.immunohistochemistry;

import java.util.List;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.program.valueholder.pathology.PathologyBlock;
import org.openelisglobal.program.valueholder.pathology.PathologySlide;

public class ImmunohistochemistryCaseViewDisplayItem extends ImmunohistochemistryDisplayItem {

    private String age;
    private String sex;
    private String referringFacility;
    private String department;
    private String requester;
    private Questionnaire programQuestionnaire;
    private QuestionnaireResponse programQuestionnaireResponse;
    private String assignedTechnicianId;
    private String assignedPathologistId;
    private List<ImmunohistochemistrySampleReport> reports;
    private Boolean reffered = false;
    // refered from pathology.These are Read Only
    private List<PathologyBlock> blocks;
    private List<PathologySlide> slides;
    private List<IdValuePair> techniques;
    private List<IdValuePair> requests;
    private List<IdValuePair> conclusions;
    private String conclusionText;
    private String grossExam;
    private String microscopyExam;

    public String getAge() {
        return age;
    }

    public void setAge(String age) {
        this.age = age;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    public String getReferringFacility() {
        return referringFacility;
    }

    public void setReferringFacility(String referringFacility) {
        this.referringFacility = referringFacility;
    }

    public Questionnaire getProgramQuestionnaire() {
        return programQuestionnaire;
    }

    public void setProgramQuestionnaire(Questionnaire programQuestionnaire) {
        this.programQuestionnaire = programQuestionnaire;
    }

    public QuestionnaireResponse getProgramQuestionnaireResponse() {
        return programQuestionnaireResponse;
    }

    public void setProgramQuestionnaireResponse(QuestionnaireResponse programQuestionnaireResponse) {
        this.programQuestionnaireResponse = programQuestionnaireResponse;
    }

    public String getAssignedPathologistId() {
        return assignedPathologistId;
    }

    public void setAssignedPathologistId(String assignedPathologistId) {
        this.assignedPathologistId = assignedPathologistId;
    }

    public String getAssignedTechnicianId() {
        return assignedTechnicianId;
    }

    public void setAssignedTechnicianId(String assignedTechnicianId) {
        this.assignedTechnicianId = assignedTechnicianId;
    }

    public List<ImmunohistochemistrySampleReport> getReports() {
        return reports;
    }

    public void setReports(List<ImmunohistochemistrySampleReport> reports) {
        this.reports = reports;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getRequester() {
        return requester;
    }

    public void setRequester(String requester) {
        this.requester = requester;
    }

    public List<PathologyBlock> getBlocks() {
        return blocks;
    }

    public void setBlocks(List<PathologyBlock> blocks) {
        this.blocks = blocks;
    }

    public List<PathologySlide> getSlides() {
        return slides;
    }

    public void setSlides(List<PathologySlide> slides) {
        this.slides = slides;
    }

    public List<IdValuePair> getTechniques() {
        return techniques;
    }

    public void setTechniques(List<IdValuePair> techniques) {
        this.techniques = techniques;
    }

    public List<IdValuePair> getRequests() {
        return requests;
    }

    public void setRequests(List<IdValuePair> requests) {
        this.requests = requests;
    }

    public List<IdValuePair> getConclusions() {
        return conclusions;
    }

    public void setConclusions(List<IdValuePair> conclusions) {
        this.conclusions = conclusions;
    }

    public String getConclusionText() {
        return conclusionText;
    }

    public void setConclusionText(String conclusionText) {
        this.conclusionText = conclusionText;
    }

    public String getGrossExam() {
        return grossExam;
    }

    public void setGrossExam(String grossExam) {
        this.grossExam = grossExam;
    }

    public String getMicroscopyExam() {
        return microscopyExam;
    }

    public void setMicroscopyExam(String microscopyExam) {
        this.microscopyExam = microscopyExam;
    }

    public Boolean getReffered() {
        return reffered;
    }

    public void setReffered(Boolean reffered) {
        this.reffered = reffered;
    }
}
