package org.openelisglobal.dataexchange.externalorders.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.openelisglobal.dataexchange.externalorders.ExternalOrderXmlBuilder;
import org.openelisglobal.dataexchange.externalorders.dto.ExternalOrderRequest;
import org.openelisglobal.panel.service.PanelService;
import org.openelisglobal.panel.valueholder.Panel;
import org.openelisglobal.panelitem.service.PanelItemService;
import org.openelisglobal.panelitem.valueholder.PanelItem;
import org.openelisglobal.patient.action.IPatientUpdate.PatientUpdateStatus;
import org.openelisglobal.patient.action.bean.PatientManagementInfo;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.organization.service.OrganizationService;
import org.openelisglobal.organization.valueholder.Organization;
import org.openelisglobal.person.service.PersonService;
import org.openelisglobal.person.valueholder.Person;
import org.openelisglobal.typeofsample.service.TypeOfSampleTestService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSampleTest;
import org.openelisglobal.sample.bean.SampleOrderItem;
import org.openelisglobal.sample.form.SamplePatientEntryForm;
import org.openelisglobal.sample.util.AccessionNumberUtil;
import org.openelisglobal.sample.valueholder.OrderPriority;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ExternalOrderFormMapperServiceImpl implements ExternalOrderFormMapperService {

    @Autowired
    private PatientService patientService;

    @Autowired
    private TestService testService;

    @Autowired
    private PanelService panelService;

    @Autowired
    private PanelItemService panelItemService;

    @Autowired
    private OrganizationService organizationService;

    @Autowired
    private PersonService personService;

    @Autowired
    private TypeOfSampleTestService typeOfSampleTestService;

    @Override
    public SamplePatientEntryForm buildForm(ExternalOrderRequest externalOrderRequest) {
        SamplePatientEntryForm form = new SamplePatientEntryForm();
        form.setSampleOrderItems(new SampleOrderItem());

        Patient patient = patientService.getPatientForGuid(externalOrderRequest.getPatientGuid());
        if (patient == null) {
            throw new IllegalArgumentException("Unknown patientGuid");
        }

        PatientManagementInfo patientInfo = new PatientManagementInfo();
        patientInfo.setPatientPK(patient.getId());
        patientInfo.setGuid(externalOrderRequest.getPatientGuid());
        if (patient.getPerson() != null) {
            patientInfo.setFirstName(patient.getPerson().getFirstName());
            patientInfo.setLastName(patient.getPerson().getLastName());
            if (patient.getPerson().getPrimaryPhone() != null && !patient.getPerson().getPrimaryPhone().trim().isEmpty()) {
                patientInfo.setPrimaryPhone(patient.getPerson().getPrimaryPhone());
            } else {
                patientInfo.setPrimaryPhone(patient.getPerson().getWorkPhone());
            }
            patientInfo.setEmail(patient.getPerson().getEmail());
            patientInfo.setStreetAddress(patient.getPerson().getStreetAddress());
            patientInfo.setCity(patient.getPerson().getCity());
        }
        patientInfo.setGender(patient.getGender());
        patientInfo.setBirthDateForDisplay(patient.getBirthDateForDisplay());
        patientInfo.setNationalId(patient.getNationalId());
        patientInfo.setPatientUpdateStatus(PatientUpdateStatus.NO_ACTION);
        form.setPatientProperties(patientInfo);
        form.setPatientUpdateStatus(PatientUpdateStatus.NO_ACTION);

        form.getSampleOrderItems().setExternalOrderNumber(externalOrderRequest.getExternalOrderNumber());
        form.getSampleOrderItems().setReferringSiteId(externalOrderRequest.getReferringSiteId());
        form.getSampleOrderItems().setReferringSiteName(externalOrderRequest.getReferringSiteName());
        form.getSampleOrderItems().setReferringSiteDepartmentId(externalOrderRequest.getReferringSiteDepartmentId());
        form.getSampleOrderItems().setProviderPersonId(externalOrderRequest.getProviderPersonId());
        form.getSampleOrderItems().setProviderFirstName(externalOrderRequest.getProviderFirstName());
        form.getSampleOrderItems().setProviderLastName(externalOrderRequest.getProviderLastName());
        form.getSampleOrderItems().setProviderWorkPhone(externalOrderRequest.getProviderWorkPhone());
        form.getSampleOrderItems().setProviderFax(externalOrderRequest.getProviderFax());
        form.getSampleOrderItems().setProviderEmail(externalOrderRequest.getProviderEmail());

        resolveReferringSite(form.getSampleOrderItems());
        resolveRequester(form.getSampleOrderItems());

        if (externalOrderRequest.getPriority() != null) {
            form.getSampleOrderItems().setPriority(OrderPriority.valueOf(externalOrderRequest.getPriority()));
        } else {
            form.getSampleOrderItems().setPriority(OrderPriority.ROUTINE);
        }

        if (externalOrderRequest.getProgramId() != null) {
            form.getSampleOrderItems().setProgramId(externalOrderRequest.getProgramId());
        }

        if (externalOrderRequest.getReceivedDate() != null) {
            form.getSampleOrderItems().setReceivedDateForDisplay(toUiDate(externalOrderRequest.getReceivedDate()));
        }
        if (externalOrderRequest.getReceivedTime() != null) {
            form.getSampleOrderItems().setReceivedTime(externalOrderRequest.getReceivedTime());
        }
        if (externalOrderRequest.getRequestDate() != null) {
            form.getSampleOrderItems().setRequestDate(toUiDate(externalOrderRequest.getRequestDate()));
        }

        String fallbackUiDate = pickFallbackCollectionDate(externalOrderRequest);
        if (form.getSampleOrderItems().getReceivedDateForDisplay() == null
                || form.getSampleOrderItems().getReceivedDateForDisplay().trim().isEmpty()) {
            form.getSampleOrderItems().setReceivedDateForDisplay(fallbackUiDate);
        }
        if (form.getSampleOrderItems().getReceivedTime() == null
                || form.getSampleOrderItems().getReceivedTime().trim().isEmpty()) {
            form.getSampleOrderItems().setReceivedTime("00:00");
        }
        if (form.getSampleOrderItems().getRequestDate() == null
                || form.getSampleOrderItems().getRequestDate().trim().isEmpty()) {
            form.getSampleOrderItems().setRequestDate(fallbackUiDate);
        }

        if (form.getSampleOrderItems().getLabNo() == null || form.getSampleOrderItems().getLabNo().trim().isEmpty()) {
            form.getSampleOrderItems().setLabNo(generateAccessionNumber());
        }

        ExternalOrderXmlBuilder xmlBuilder = new ExternalOrderXmlBuilder();
        List<ExternalOrderRequest.ExternalOrderSample> samples = externalOrderRequest.getSamples();

        String fallbackCollectionDate = fallbackUiDate;
        for (ExternalOrderRequest.ExternalOrderSample sample : samples) {
            if (sample.getCollectionDate() == null || sample.getCollectionDate().trim().isEmpty()) {
                sample.setCollectionDate(fallbackCollectionDate);
            }
            if (sample.getCollectionTime() == null || sample.getCollectionTime().trim().isEmpty()) {
                sample.setCollectionTime("00:00");
            }
        }

        List<List<String>> sampleTestIds = new ArrayList<>();
        List<List<String>> samplePanelIds = new ArrayList<>();

        for (ExternalOrderRequest.ExternalOrderSample sample : samples) {
            Set<String> testIds = new LinkedHashSet<>();
            Set<String> panelIds = new LinkedHashSet<>();

            if (sample.getTests() != null) {
                for (ExternalOrderRequest.ExternalOrderTestRef testRef : sample.getTests()) {
                    String id = resolveTestId(testRef);
                    if (id == null) {
                        throw new IllegalArgumentException("Unknown test reference");
                    }
                    testIds.add(id);
                }
            }

            if (sample.getPanels() != null) {
                for (ExternalOrderRequest.ExternalOrderPanelRef panelRef : sample.getPanels()) {
                    Panel panel = resolvePanel(panelRef);
                    if (panel == null) {
                        throw new IllegalArgumentException("Unknown panel reference");
                    }
                    panelIds.add(panel.getId());

                    List<PanelItem> panelItems = panelItemService.getPanelItemsForPanel(panel.getId());
                    if (panelItems != null) {
                        for (PanelItem pi : panelItems) {
                            if (pi.getTest() != null && pi.getTest().getId() != null) {
                                testIds.add(pi.getTest().getId());
                            }
                        }
                    }
                }
            }

            sampleTestIds.add(new ArrayList<>(testIds));
            samplePanelIds.add(new ArrayList<>(panelIds));
        }

        for (int i = 0; i < samples.size(); i++) {
            ExternalOrderRequest.ExternalOrderSample sample = samples.get(i);
            if (sample.getSampleTypeId() == null || sample.getSampleTypeId().trim().isEmpty()) {
                String resolvedSampleTypeId = resolveSampleTypeId(sampleTestIds.get(i));
                if (resolvedSampleTypeId == null) {
                    throw new IllegalArgumentException("Unable to resolve sample type");
                }
                sample.setSampleTypeId(resolvedSampleTypeId);
            }
        }

        form.setSampleXML(xmlBuilder.buildSamplesXml(samples, sampleTestIds, samplePanelIds));
        return form;
    }

    private String resolveSampleTypeId(List<String> testIds) {
        if (testIds == null || testIds.isEmpty()) {
            return null;
        }

        for (String testId : testIds) {
            if (testId == null || testId.trim().isEmpty()) {
                continue;
            }
            List<TypeOfSampleTest> mappings = typeOfSampleTestService.getTypeOfSampleTestsForTest(testId.trim());
            if (mappings != null && !mappings.isEmpty()) {
                TypeOfSampleTest chosen = mappings.get(0);
                if (chosen != null && chosen.getTypeOfSampleId() != null
                        && !chosen.getTypeOfSampleId().trim().isEmpty()) {
                    return chosen.getTypeOfSampleId().trim();
                }
            }
        }

        return null;
    }

    private void resolveReferringSite(SampleOrderItem sampleOrderItems) {
        if (sampleOrderItems == null) {
            return;
        }

        // If only ID is provided, populate display name so the UI doesn't show raw numeric IDs.
        if (sampleOrderItems.getReferringSiteId() != null && !sampleOrderItems.getReferringSiteId().trim().isEmpty()
                && (sampleOrderItems.getReferringSiteName() == null
                        || sampleOrderItems.getReferringSiteName().trim().isEmpty())) {
            Organization org = organizationService.getOrganizationById(sampleOrderItems.getReferringSiteId().trim());
            if (org != null && org.getOrganizationName() != null) {
                sampleOrderItems.setReferringSiteName(org.getOrganizationName());
            }
        }

        // If only free-text name is provided, try to resolve an existing organization ID (best-effort).
        if ((sampleOrderItems.getReferringSiteId() == null || sampleOrderItems.getReferringSiteId().trim().isEmpty())
                && sampleOrderItems.getReferringSiteName() != null
                && !sampleOrderItems.getReferringSiteName().trim().isEmpty()) {
            Organization probe = new Organization();
            probe.setOrganizationName(sampleOrderItems.getReferringSiteName().trim());
            Organization existing = organizationService.getActiveOrganizationByName(probe, true);
            if (existing != null && existing.getId() != null) {
                sampleOrderItems.setReferringSiteId(existing.getId());
            }
        }
    }

    private void resolveRequester(SampleOrderItem sampleOrderItems) {
        if (sampleOrderItems == null) {
            return;
        }

        // If we have providerPersonId but missing display names, fill them in.
        if (sampleOrderItems.getProviderPersonId() != null && !sampleOrderItems.getProviderPersonId().trim().isEmpty()
                && ((sampleOrderItems.getProviderFirstName() == null
                        || sampleOrderItems.getProviderFirstName().trim().isEmpty())
                        || (sampleOrderItems.getProviderLastName() == null
                                || sampleOrderItems.getProviderLastName().trim().isEmpty()))) {
            Person person = personService.getPersonById(sampleOrderItems.getProviderPersonId().trim());
            if (person != null) {
                if (sampleOrderItems.getProviderFirstName() == null
                        || sampleOrderItems.getProviderFirstName().trim().isEmpty()) {
                    sampleOrderItems.setProviderFirstName(person.getFirstName());
                }
                if (sampleOrderItems.getProviderLastName() == null
                        || sampleOrderItems.getProviderLastName().trim().isEmpty()) {
                    sampleOrderItems.setProviderLastName(person.getLastName());
                }
            } else {
                sampleOrderItems.setProviderPersonId("");
                if (sampleOrderItems.getProviderFirstName() == null
                        || sampleOrderItems.getProviderFirstName().trim().isEmpty()) {
                    sampleOrderItems.setProviderFirstName("Unknown");
                }
                if (sampleOrderItems.getProviderLastName() == null
                        || sampleOrderItems.getProviderLastName().trim().isEmpty()) {
                    sampleOrderItems.setProviderLastName("Unknown");
                }
            }
        }

        if (sampleOrderItems.getProviderFirstName() == null || sampleOrderItems.getProviderFirstName().trim().isEmpty()) {
            sampleOrderItems.setProviderFirstName("Unknown");
        }
        if (sampleOrderItems.getProviderLastName() == null || sampleOrderItems.getProviderLastName().trim().isEmpty()) {
            sampleOrderItems.setProviderLastName("Unknown");
        }
    }

    private String resolveTestId(ExternalOrderRequest.ExternalOrderTestRef testRef) {
        if (testRef == null) {
            return null;
        }
        if (testRef.getTestGuid() != null && !testRef.getTestGuid().trim().isEmpty()) {
            Test test = testService.getTestByGUID(testRef.getTestGuid());
            return test != null ? test.getId() : null;
        }
        if (testRef.getLoinc() != null && !testRef.getLoinc().trim().isEmpty()) {
            List<Test> tests = testService.getActiveTestsByLoinc(testRef.getLoinc().trim());
            if (tests != null && !tests.isEmpty()) {
                return tests.get(0).getId();
            }
        }
        return null;
    }

    private Panel resolvePanel(ExternalOrderRequest.ExternalOrderPanelRef panelRef) {
        if (panelRef == null) {
            return null;
        }
        if (panelRef.getPanelGuid() != null && !panelRef.getPanelGuid().trim().isEmpty()) {
            return panelService.getPanelByGUID(panelRef.getPanelGuid());
        }
        if (panelRef.getLoinc() != null && !panelRef.getLoinc().trim().isEmpty()) {
            return panelService.getPanelByLoincCode(panelRef.getLoinc().trim());
        }
        return null;
    }

    private String generateAccessionNumber() {
        int attempts = 0;
        while (attempts < 100) {
            String candidate = AccessionNumberUtil.getMainAccessionNumberGenerator().getNextAvailableAccessionNumber(null,
                    true);
            if (candidate != null && !AccessionNumberUtil.isUsed(candidate)) {
                return candidate;
            }
            attempts++;
        }
        throw new IllegalStateException("Unable to generate accession number");
    }

    private String toUiDate(String date) {
        if (date == null) {
            return null;
        }
        String d = date.trim();
        if (d.matches("\\d{4}-\\d{2}-\\d{2}")) {
            String[] parts = d.split("-");
            return parts[1] + "/" + parts[2] + "/" + parts[0];
        }
        return d;
    }

    private String pickFallbackCollectionDate(ExternalOrderRequest request) {
        if (request.getReceivedDate() != null && !request.getReceivedDate().trim().isEmpty()) {
            return toUiDate(request.getReceivedDate());
        }
        if (request.getRequestDate() != null && !request.getRequestDate().trim().isEmpty()) {
            return toUiDate(request.getRequestDate());
        }
        return toUiDate(LocalDate.now().toString());
    }
}
