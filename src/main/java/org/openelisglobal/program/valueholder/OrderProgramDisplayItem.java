package org.openelisglobal.program.valueholder;

import java.util.Date;
import lombok.Getter;
import lombok.Setter;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.QuestionnaireResponse;

@Setter
@Getter
public class OrderProgramDisplayItem {

    private Integer programSampleId;
    private String programName;
    private String programCode;

    private String accessionNumber;
    private Date receivedDate;

    private String firstName;
    private String lastName;
    private String age;
    private String gender;
    private String patientPK;
    private String referringFacility;
    private String department;
    private String requester;

    private Questionnaire programQuestionnaire;
    private QuestionnaireResponse programQuestionnaireResponse;
    private String questionnaireStatus;

}
