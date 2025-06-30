package org.openelisglobal.referral.fhir.form;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import org.openelisglobal.common.form.BaseForm;
import org.openelisglobal.common.util.IdValuePair;
import org.openelisglobal.common.util.validator.CustomDateValidator.DateRelation;
import org.openelisglobal.common.validator.ValidationHelper;
import org.openelisglobal.validation.annotations.ValidDate;
import org.openelisglobal.validation.annotations.ValidName;
import org.openelisglobal.validation.constraintvalidator.NameValidator.NameType;

public class FhirReferralSearchForm extends BaseForm {

    private static final long serialVersionUID = 3245627796529364543L;

    protected String externalAccessionNumber;

    protected String patientID;

    @ValidName(nameType = NameType.LAST_NAME)
    protected String patientLastName;

    @ValidName(nameType = NameType.FIRST_NAME)
    protected String patientFirstName;

    @ValidDate(relative = DateRelation.PAST)
    protected String dateOfBirth;

    @Pattern(regexp = ValidationHelper.GENDER_REGEX)
    protected String gender;

    protected List<IdValuePair> genders;

    @Min(1)
    protected int page = 1;

    public String getPatientID() {
        return patientID;
    }

    public void setPatientID(String patientID) {
        this.patientID = patientID;
    }

    public String getExternalAccessionNumber() {
        return externalAccessionNumber;
    }

    public void setExternalAccessionNumber(String externalAccessionNumber) {
        this.externalAccessionNumber = externalAccessionNumber;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public String getDateOfBirth() {
        return dateOfBirth;
    }

    public void setDateOfBirth(String dateOfBirth) {
        this.dateOfBirth = dateOfBirth;
    }

    public String getPatientFirstName() {
        return patientFirstName;
    }

    public void setPatientFirstName(String patientFirstName) {
        this.patientFirstName = patientFirstName;
    }

    public String getPatientLastName() {
        return patientLastName;
    }

    public void setPatientLastName(String patientLastName) {
        this.patientLastName = patientLastName;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    public List<IdValuePair> getGenders() {
        return genders;
    }

    public void setGenders(List<IdValuePair> genders) {
        this.genders = genders;
    }
}
