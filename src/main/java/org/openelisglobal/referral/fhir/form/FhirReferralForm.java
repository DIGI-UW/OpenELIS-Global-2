package org.openelisglobal.referral.fhir.form;

import java.util.List;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrder;

public class FhirReferralForm extends FhirReferralSearchForm {

    private static final long serialVersionUID = 3245627796529364543L;

    private List<ElectronicOrder> electronicOrders;

    public FhirReferralForm(FhirReferralSearchForm searchForm) {
        externalAccessionNumber = (searchForm.getExternalAccessionNumber());
        patientFirstName = (searchForm.getPatientFirstName());
        patientLastName = (searchForm.getPatientLastName());
        gender = (searchForm.getGender());
        patientID = (searchForm.getPatientID());
        dateOfBirth = (searchForm.getDateOfBirth());
        page = searchForm.getPage();
    }

    public void setElectronicOrders(List<ElectronicOrder> electronicOrders) {
        this.electronicOrders = electronicOrders;
    }

    public List<ElectronicOrder> getElectronicOrders() {
        return electronicOrders;
    }
}
