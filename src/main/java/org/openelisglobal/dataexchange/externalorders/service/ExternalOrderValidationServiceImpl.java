package org.openelisglobal.dataexchange.externalorders.service;

import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.dataexchange.externalorders.dto.ExternalOrderRequest;
import org.openelisglobal.dataexchange.externalorders.dto.ValidationReport;
import org.openelisglobal.dataexchange.externalorders.dto.ValidationResult;
import org.openelisglobal.panel.service.PanelService;
import org.openelisglobal.panel.valueholder.Panel;
import org.openelisglobal.patient.service.PatientService;
import org.openelisglobal.patient.valueholder.Patient;
import org.openelisglobal.test.service.TestService;
import org.openelisglobal.test.valueholder.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of external order validation service. Validates patient,
 * tests, and panels before storage.
 */
@Service
public class ExternalOrderValidationServiceImpl implements ExternalOrderValidationService {

    @Autowired
    private PatientService patientService;

    @Autowired
    private TestService testService;

    @Autowired
    private PanelService panelService;

    @Override
    @Transactional(readOnly = true)
    public ValidationReport validateOrder(ExternalOrderRequest request) {
        ValidationReport report = new ValidationReport();
        report.setPatientGuid(request.getPatientGuid());

        // Validate patient
        Patient patient = patientService.getPatientForGuid(request.getPatientGuid());
        report.setPatientValid(patient != null);
        if (patient == null) {
            report.setPatientRejectionReason("GUID_NOT_FOUND");
        }

        // Count and validate tests/panels
        int totalTests = 0;
        int totalPanels = 0;
        boolean hasRemovedTests = false;
        boolean hasRemovedPanels = false;

        if (request.getSamples() != null) {
            for (ExternalOrderRequest.ExternalOrderSample sample : request.getSamples()) {
                // Validate tests
                if (sample.getTests() != null) {
                    for (ExternalOrderRequest.ExternalOrderTestRef testRef : sample.getTests()) {
                        totalTests++;
                        ValidationResult vr = validateTest(testRef);
                        if (vr.isValid()) {
                            report.getValidTests().add(vr);
                        } else {
                            report.getInvalidTests().add(vr);
                        }
                    }
                }

                // Check for removed tests
                if (sample.getRemovedTests() != null && !sample.getRemovedTests().isEmpty()) {
                    hasRemovedTests = true;
                }

                // Validate panels
                if (sample.getPanels() != null) {
                    for (ExternalOrderRequest.ExternalOrderPanelRef panelRef : sample.getPanels()) {
                        totalPanels++;
                        ValidationResult vr = validatePanel(panelRef);
                        if (vr.isValid()) {
                            report.getValidPanels().add(vr);
                        } else {
                            report.getInvalidPanels().add(vr);
                        }
                    }
                }

                // Check for removed panels
                if (sample.getRemovedPanels() != null && !sample.getRemovedPanels().isEmpty()) {
                    hasRemovedPanels = true;
                }
            }
        }

        report.setTotalTestsReceived(totalTests);
        report.setTotalPanelsReceived(totalPanels);
        report.setOverallValid(report.isFullyValid());
        report.setHasRemovedTests(hasRemovedTests);
        report.setHasRemovedPanels(hasRemovedPanels);

        return report;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean validatePatient(String patientGuid) {
        if (patientGuid == null || patientGuid.trim().isEmpty()) {
            return false;
        }
        Patient patient = patientService.getPatientForGuid(patientGuid);
        return patient != null;
    }

    @Override
    public ExternalOrderRequest filterValidItems(ExternalOrderRequest original, ValidationReport report) {
        if (!report.canStore()) {
            return null;
        }

        ExternalOrderRequest filtered = new ExternalOrderRequest();
        filtered.setExternalOrderNumber(original.getExternalOrderNumber());
        filtered.setPatientGuid(original.getPatientGuid());
        filtered.setPriority(original.getPriority());
        filtered.setReferringSiteId(original.getReferringSiteId());
        filtered.setReferringSiteName(original.getReferringSiteName());
        filtered.setReferringSiteDepartmentId(original.getReferringSiteDepartmentId());
        filtered.setProviderPersonId(original.getProviderPersonId());
        filtered.setProviderFirstName(original.getProviderFirstName());
        filtered.setProviderLastName(original.getProviderLastName());
        filtered.setProviderWorkPhone(original.getProviderWorkPhone());
        filtered.setProviderFax(original.getProviderFax());
        filtered.setProviderEmail(original.getProviderEmail());
        filtered.setReceivedDate(original.getReceivedDate());
        filtered.setReceivedTime(original.getReceivedTime());
        filtered.setRequestDate(original.getRequestDate());
        filtered.setProgramId(original.getProgramId());

        // Build sets of valid test and panel IDs for quick lookup
        List<String> validTestIds = new ArrayList<>();
        for (ValidationResult vr : report.getValidTests()) {
            if (vr.getResolvedId() != null) {
                validTestIds.add(vr.getResolvedId());
            }
        }

        List<String> validPanelIds = new ArrayList<>();
        for (ValidationResult vr : report.getValidPanels()) {
            if (vr.getResolvedId() != null) {
                validPanelIds.add(vr.getResolvedId());
            }
        }

        // Filter samples to only include valid tests/panels
        List<ExternalOrderRequest.ExternalOrderSample> filteredSamples = new ArrayList<>();
        if (original.getSamples() != null) {
            for (ExternalOrderRequest.ExternalOrderSample sample : original.getSamples()) {
                ExternalOrderRequest.ExternalOrderSample filteredSample = new ExternalOrderRequest.ExternalOrderSample();
                filteredSample.setSampleTypeId(sample.getSampleTypeId());
                filteredSample.setCollectionDate(sample.getCollectionDate());
                filteredSample.setCollectionTime(sample.getCollectionTime());
                filteredSample.setCollector(sample.getCollector());
                filteredSample.setQuantity(sample.getQuantity());
                filteredSample.setUom(sample.getUom());

                // Filter tests
                List<ExternalOrderRequest.ExternalOrderTestRef> filteredTests = new ArrayList<>();
                if (sample.getTests() != null) {
                    for (ExternalOrderRequest.ExternalOrderTestRef testRef : sample.getTests()) {
                        String resolvedId = resolveTestId(testRef);
                        if (resolvedId != null && validTestIds.contains(resolvedId)) {
                            filteredTests.add(testRef);
                        }
                    }
                }
                filteredSample.setTests(filteredTests.isEmpty() ? null : filteredTests);

                // Filter panels
                List<ExternalOrderRequest.ExternalOrderPanelRef> filteredPanels = new ArrayList<>();
                if (sample.getPanels() != null) {
                    for (ExternalOrderRequest.ExternalOrderPanelRef panelRef : sample.getPanels()) {
                        String resolvedId = resolvePanelId(panelRef);
                        if (resolvedId != null && validPanelIds.contains(resolvedId)) {
                            filteredPanels.add(panelRef);
                        }
                    }
                }
                filteredSample.setPanels(filteredPanels.isEmpty() ? null : filteredPanels);

                // Copy removedTests and removedPanels as-is (no validation needed for removals)
                filteredSample.setRemovedTests(sample.getRemovedTests());
                filteredSample.setRemovedPanels(sample.getRemovedPanels());

                // Add sample if it has tests, panels, removedTests, or removedPanels
                if ((filteredSample.getTests() != null && !filteredSample.getTests().isEmpty())
                        || (filteredSample.getPanels() != null && !filteredSample.getPanels().isEmpty())
                        || (filteredSample.getRemovedTests() != null && !filteredSample.getRemovedTests().isEmpty())
                        || (filteredSample.getRemovedPanels() != null
                                && !filteredSample.getRemovedPanels().isEmpty())) {
                    filteredSamples.add(filteredSample);
                }
            }
        }

        filtered.setSamples(filteredSamples.isEmpty() ? null : filteredSamples);
        return filtered;
    }

    private ValidationResult validateTest(ExternalOrderRequest.ExternalOrderTestRef testRef) {
        if (testRef == null) {
            return ValidationResult.invalidForGuid(null);
        }

        // Validate by GUID
        if (testRef.getTestGuid() != null && !testRef.getTestGuid().trim().isEmpty()) {
            Test test = testService.getTestByGUID(testRef.getTestGuid().trim());
            if (test != null) {
                return ValidationResult.validForGuid(testRef.getTestGuid().trim(), test.getId(), test.getName());
            }
            return ValidationResult.invalidForGuid(testRef.getTestGuid().trim());
        }

        // Validate by LOINC
        if (testRef.getLoinc() != null && !testRef.getLoinc().trim().isEmpty()) {
            List<Test> tests = testService.getActiveTestsByLoinc(testRef.getLoinc().trim());
            if (tests != null && !tests.isEmpty()) {
                Test test = tests.get(0);
                return ValidationResult.validForLoinc(testRef.getLoinc().trim(), test.getId(), test.getName());
            }
            return ValidationResult.invalidForLoinc(testRef.getLoinc().trim());
        }

        return ValidationResult.invalidForGuid(null);
    }

    private ValidationResult validatePanel(ExternalOrderRequest.ExternalOrderPanelRef panelRef) {
        if (panelRef == null) {
            return ValidationResult.invalidForGuid(null);
        }

        // Validate by GUID
        if (panelRef.getPanelGuid() != null && !panelRef.getPanelGuid().trim().isEmpty()) {
            Panel panel = panelService.getPanelByGUID(panelRef.getPanelGuid().trim());
            if (panel != null) {
                return ValidationResult.validForGuid(panelRef.getPanelGuid().trim(), panel.getId(),
                        panel.getPanelName());
            }
            return ValidationResult.invalidForGuid(panelRef.getPanelGuid().trim());
        }

        // Validate by LOINC
        if (panelRef.getLoinc() != null && !panelRef.getLoinc().trim().isEmpty()) {
            Panel panel = panelService.getPanelByLoincCode(panelRef.getLoinc().trim());
            if (panel != null) {
                return ValidationResult.validForLoinc(panelRef.getLoinc().trim(), panel.getId(), panel.getPanelName());
            }
            return ValidationResult.invalidForLoinc(panelRef.getLoinc().trim());
        }

        return ValidationResult.invalidForGuid(null);
    }

    private String resolveTestId(ExternalOrderRequest.ExternalOrderTestRef testRef) {
        if (testRef == null) {
            return null;
        }
        if (testRef.getTestGuid() != null && !testRef.getTestGuid().trim().isEmpty()) {
            Test test = testService.getTestByGUID(testRef.getTestGuid().trim());
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

    private String resolvePanelId(ExternalOrderRequest.ExternalOrderPanelRef panelRef) {
        if (panelRef == null) {
            return null;
        }
        if (panelRef.getPanelGuid() != null && !panelRef.getPanelGuid().trim().isEmpty()) {
            Panel panel = panelService.getPanelByGUID(panelRef.getPanelGuid().trim());
            return panel != null ? panel.getId() : null;
        }
        if (panelRef.getLoinc() != null && !panelRef.getLoinc().trim().isEmpty()) {
            Panel panel = panelService.getPanelByLoincCode(panelRef.getLoinc().trim());
            return panel != null ? panel.getId() : null;
        }
        return null;
    }
}
