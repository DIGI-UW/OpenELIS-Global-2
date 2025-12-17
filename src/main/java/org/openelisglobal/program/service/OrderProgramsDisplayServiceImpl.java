package org.openelisglobal.program.service;

import jakarta.transaction.Transactional;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.Questionnaire;
import org.hl7.fhir.r4.model.QuestionnaireResponse;
import org.openelisglobal.common.services.SampleOrderService;
import org.openelisglobal.common.util.DateUtil;
import org.openelisglobal.dataexchange.fhir.FhirUtil;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.program.valueholder.OrderProgramDisplayItem;
import org.openelisglobal.program.valueholder.ProgramSample;
import org.openelisglobal.sample.bean.SampleOrderItem;
import org.openelisglobal.sample.service.SampleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class OrderProgramsDisplayServiceImpl implements OrderProgramsDisplayService {

    @Autowired
    private ProgramSampleService programSampleService;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private FhirUtil fhirUtil;

    @Autowired
    private OrganizationService organizationService;

    @Override
    @Transactional
    public OrderProgramDisplayItem getOrderProgramById(Integer programSampleId) {
        ProgramSample ps = programSampleService.get(programSampleId);
        return convert(ps);
    }

    private OrderProgramDisplayItem convert(ProgramSample ps) {

        OrderProgramDisplayItem display = new OrderProgramDisplayItem();
        display.setProgramSampleId(ps.getId());
        display.setProgramName(ps.getProgram().getProgramName());
        display.setProgramCode(ps.getProgram().getCode());

        display.setAccessionNumber(ps.getSample().getAccessionNumber());
        display.setReceivedDate(ps.getSample().getReceivedDate());

        Patient patient = sampleService.getPatient(ps.getSample());
        display.setFirstName(patient.getPerson().getFirstName());
        display.setLastName(patient.getPerson().getLastName());
        display.setGender(patient.getGender());
        display.setAge(DateUtil.getCurrentAgeForDate(patient.getBirthDate(), DateUtil.getNowAsTimestamp()));

        display.setPatientPK(patient.getId());

        SampleOrderService sampleOrderService = new SampleOrderService(ps.getSample());
        SampleOrderItem sampleItem = sampleOrderService.getSampleOrderItem();
        display.setReferringFacility(sampleItem.getReferringSiteName());

        if (StringUtils.isNotBlank(sampleItem.getReferringSiteDepartmentId())) {
            Organization org = organizationService.get(sampleItem.getReferringSiteDepartmentId());
            if (org != null) {
                display.setDepartment(org.getOrganizationName());
            }
        }

        display.setRequester(sampleItem.getProviderLastName() + " " + sampleItem.getProviderFirstName());

        if (ps.getProgram().getQuestionnaireUUID() != null) {
            display.setProgramQuestionnaire(fhirUtil.getLocalFhirClient().read().resource(Questionnaire.class)
                    .withId(ps.getProgram().getQuestionnaireUUID().toString()).execute());
        }

        if (ps.getQuestionnaireResponseUuid() != null) {

            QuestionnaireResponse qr = fhirUtil.getLocalFhirClient().read().resource(QuestionnaireResponse.class)
                    .withId(ps.getQuestionnaireResponseUuid().toString()).execute();

            display.setProgramQuestionnaireResponse(qr);

            display.setQuestionnaireStatus(qr.hasStatus() ? qr.getStatus().toCode().toUpperCase() : "UNKNOWN");
        } else {
            display.setQuestionnaireStatus("NOT_STARTED");
        }

        return display;
    }
}
