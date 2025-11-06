package org.openelisglobal.program.valueholder.cytology;

import java.util.List;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.program.valueholder.cytology.CytologyDiagnosis.CytologyDiagnosisResultType;
import org.openelisglobal.program.valueholder.cytology.CytologyDiagnosis.DiagnosisCategory;
import org.openelisglobal.program.valueholder.cytology.CytologySpecimenAdequacy.SpecimenAdequancySatisfaction;

public class CytologyCaseViewDisplayItem extends CytologyDisplayItem {

    private String age;

    private String sex;

    private String referringFacility;

    private String department;

    private String requester;

    private Questionnaire programQuestionnaire;

    private QuestionnaireResponse programQuestionnaireResponse;

    private String assignedTechnicianId;

    private String assignedPathologistId;

    private List<CytologySlide> slides;

    private SpecimenAdequacy specimenAdequacy;

    private Diagnosis diagnosis;

    private List<CytologyReport> reports;

    public String getAge() {
        return age;
    }

    public List<CytologyReport> getReports() {
        return reports;
    }

    public void setReports(List<CytologyReport> reports) {
        this.reports = reports;
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

    public String getAssignedTechnicianId() {
        return assignedTechnicianId;
    }

    public void setAssignedTechnicianId(String assignedTechnicianId) {
        this.assignedTechnicianId = assignedTechnicianId;
    }

    public String getAssignedPathologistId() {
        return assignedPathologistId;
    }

    public void setAssignedPathologistId(String assignedPathologistId) {
        this.assignedPathologistId = assignedPathologistId;
    }

    public List<CytologySlide> getSlides() {
        return slides;
    }

    public void setSlides(List<CytologySlide> slides) {
        this.slides = slides;
    }

    public SpecimenAdequacy getSpecimenAdequacy() {
        return specimenAdequacy;
    }

    public void setSpecimenAdequacy(SpecimenAdequacy specimenAdequacy) {
        this.specimenAdequacy = specimenAdequacy;
    }

    public Diagnosis getDiagnosis() {
        return diagnosis;
    }

    public void setDiagnosis(Diagnosis diagnosis) {
        this.diagnosis = diagnosis;
    }

    public static class SpecimenAdequacy {

        private List<IdValuePair> values;

        private SpecimenAdequancySatisfaction satisfaction;

        public List<IdValuePair> getValues() {
            return values;
        }

        public void setValues(List<IdValuePair> values) {
            this.values = values;
        }

        public SpecimenAdequancySatisfaction getSatisfaction() {
            return satisfaction;
        }

        public void setSatisfaction(SpecimenAdequancySatisfaction satisfaction) {
            this.satisfaction = satisfaction;
        }
    }

    public static class Diagnosis {

        private Boolean negativeDiagnosis = true;

        private List<DiagnosisResultsMap> diagnosisResultsMaps;

        public Boolean getNegativeDiagnosis() {
            return negativeDiagnosis;
        }

        public void setNegativeDiagnosis(Boolean negativeDiagnosis) {
            this.negativeDiagnosis = negativeDiagnosis;
        }

        public List<DiagnosisResultsMap> getDiagnosisResultsMaps() {
            return diagnosisResultsMaps;
        }

        public void setDiagnosisResultsMaps(List<DiagnosisResultsMap> diagnosisResultsMaps) {
            this.diagnosisResultsMaps = diagnosisResultsMaps;
        }

        public static class DiagnosisResultsMap {

            private List<IdValuePair> results;

            private DiagnosisCategory category;

            private CytologyDiagnosisResultType resultType;

            public CytologyDiagnosisResultType getResultType() {
                return resultType;
            }

            public void setResultType(CytologyDiagnosisResultType resultType) {
                this.resultType = resultType;
            }

            public DiagnosisCategory getCategory() {
                return category;
            }

            public void setCategory(DiagnosisCategory category) {
                this.category = category;
            }

            public List<IdValuePair> getResults() {
                return results;
            }

            public void setResults(List<IdValuePair> results) {
                this.results = results;
            }
        }
    }
}
