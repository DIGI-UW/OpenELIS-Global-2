package org.openelisglobal.biorepository.service;

import java.io.BufferedReader;
import java.io.StringReader;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.openelisglobal.biorepository.dao.RetentionPolicyDAO;
import org.openelisglobal.biorepository.valueholder.RetentionPolicy;
import org.openelisglobal.biorepository.valueholder.RetentionPolicy.PeriodUnit;
import org.openelisglobal.common.service.AuditableBaseObjectServiceImpl;
import org.openelisglobal.project.service.ProjectService;
import org.openelisglobal.project.valueholder.Project;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service implementation for RetentionPolicy management.
 */
@Service
public class RetentionPolicyServiceImpl extends AuditableBaseObjectServiceImpl<RetentionPolicy, Integer>
        implements RetentionPolicyService {

    @Autowired
    protected RetentionPolicyDAO baseObjectDAO;

    @Autowired
    private ProjectService projectService;

    @Autowired
    private TypeOfSampleService typeOfSampleService;

    RetentionPolicyServiceImpl() {
        super(RetentionPolicy.class);
    }

    @Override
    protected RetentionPolicyDAO getBaseObjectDAO() {
        return baseObjectDAO;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RetentionPolicy> getAllActive() {
        return baseObjectDAO.findAllActive();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RetentionPolicy> findApplicablePolicy(Integer projectId, Integer sampleTypeId) {
        return baseObjectDAO.findApplicablePolicy(projectId, sampleTypeId);
    }

    @Override
    @Transactional(readOnly = true)
    public LocalDate calculateExpiryDate(Integer projectId, Integer sampleTypeId, LocalDate fromDate) {
        if (fromDate == null) {
            return null;
        }

        Optional<RetentionPolicy> policy = findApplicablePolicy(projectId, sampleTypeId);
        return policy.map(p -> p.calculateExpiryDate(fromDate)).orElse(null);
    }

    @Override
    @Transactional
    public RetentionPolicy createFromCsv(String policyName, String projectName, String sampleTypeName, String periodStr,
            String sysUserId) {

        RetentionPolicy policy = new RetentionPolicy();
        policy.setPolicyName(policyName.trim());
        policy.setSysUserId(sysUserId);
        policy.setIsActive(true);

        // Parse period string
        Object[] periodParts = RetentionPolicy.parsePeriodString(periodStr);
        policy.setPeriodValue((Integer) periodParts[0]);
        policy.setPeriodUnit((PeriodUnit) periodParts[1]);

        // Resolve project if provided
        if (projectName != null && !projectName.trim().isEmpty()) {
            Project project = findProjectByName(projectName.trim());
            if (project != null) {
                policy.setProjectId(Integer.parseInt(project.getId()));
                policy.setProjectName(project.getProjectName());
            } else {
                // Store the name even if we can't resolve the ID
                policy.setProjectName(projectName.trim());
            }
        }

        // Resolve sample type if provided
        if (sampleTypeName != null && !sampleTypeName.trim().isEmpty()) {
            TypeOfSample sampleType = findSampleTypeByName(sampleTypeName.trim());
            if (sampleType != null) {
                policy.setSampleTypeId(Integer.parseInt(sampleType.getId()));
                policy.setSampleTypeName(sampleType.getLocalizedName());
            } else {
                // Store the name even if we can't resolve the ID
                policy.setSampleTypeName(sampleTypeName.trim());
            }
        }

        // Validate that at least one of project or sample type is provided
        if (policy.getProjectId() == null && policy.getProjectName() == null && policy.getSampleTypeId() == null
                && policy.getSampleTypeName() == null) {
            throw new IllegalArgumentException("Policy must specify either a project or sample type: " + policyName);
        }

        return save(policy);
    }

    @Override
    @Transactional
    public List<RetentionPolicy> importFromCsv(String csvContent, String sysUserId) {
        List<RetentionPolicy> imported = new ArrayList<>();
        List<String> errors = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new StringReader(csvContent))) {
            String line;
            int lineNumber = 0;
            String[] headers = null;

            while ((line = reader.readLine()) != null) {
                lineNumber++;

                // Skip empty lines
                if (line.trim().isEmpty()) {
                    continue;
                }

                // Parse header row
                if (headers == null) {
                    headers = parseCsvLine(line);
                    validateHeaders(headers);
                    continue;
                }

                // Parse data row
                try {
                    String[] values = parseCsvLine(line);
                    if (values.length < 4) {
                        errors.add("Line " + lineNumber + ": Not enough columns");
                        continue;
                    }

                    String policyName = getValue(values, 0);
                    String projectName = getValue(values, 1);
                    String sampleTypeName = getValue(values, 2);
                    String periodStr = getValue(values, 3);

                    if (policyName == null || policyName.isEmpty()) {
                        errors.add("Line " + lineNumber + ": Policy name is required");
                        continue;
                    }

                    if (periodStr == null || periodStr.isEmpty()) {
                        errors.add("Line " + lineNumber + ": Period is required");
                        continue;
                    }

                    RetentionPolicy policy = createFromCsv(policyName, projectName, sampleTypeName, periodStr,
                            sysUserId);
                    imported.add(policy);

                } catch (Exception e) {
                    errors.add("Line " + lineNumber + ": " + e.getMessage());
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse CSV: " + e.getMessage(), e);
        }

        if (!errors.isEmpty()) {
            throw new RuntimeException("CSV import completed with errors:\n" + String.join("\n", errors));
        }

        return imported;
    }

    @Override
    @Transactional
    public void deactivate(Integer policyId) {
        RetentionPolicy policy = get(policyId);
        if (policy != null) {
            policy.setIsActive(false);
            update(policy);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsForProject(Integer projectId) {
        return baseObjectDAO.existsByProjectId(projectId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsForSampleType(Integer sampleTypeId) {
        return baseObjectDAO.existsBySampleTypeIdAndNoProject(sampleTypeId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RetentionPolicy> findApplicablePolicyByProjectName(String projectName, Integer sampleTypeId) {
        return baseObjectDAO.findApplicablePolicyByNames(projectName, sampleTypeId);
    }

    @Override
    @Transactional(readOnly = true)
    public LocalDate calculateExpiryDateByProjectName(String projectName, Integer sampleTypeId, LocalDate fromDate) {
        if (fromDate == null) {
            return null;
        }

        Optional<RetentionPolicy> policy = findApplicablePolicyByProjectName(projectName, sampleTypeId);
        return policy.map(p -> p.calculateExpiryDate(fromDate)).orElse(null);
    }

    // ========================================
    // Helper methods
    // ========================================

    private Project findProjectByName(String name) {
        try {
            List<Project> projects = projectService.getAllProjects();
            for (Project p : projects) {
                if (p.getProjectName() != null && p.getProjectName().equalsIgnoreCase(name)) {
                    return p;
                }
            }
        } catch (Exception e) {
            // Log and continue
        }
        return null;
    }

    private TypeOfSample findSampleTypeByName(String name) {
        try {
            List<TypeOfSample> types = typeOfSampleService.getAllTypeOfSamples();
            for (TypeOfSample t : types) {
                String localName = t.getLocalizedName();
                String description = t.getDescription();
                if ((localName != null && localName.equalsIgnoreCase(name))
                        || (description != null && description.equalsIgnoreCase(name))) {
                    return t;
                }
            }
        } catch (Exception e) {
            // Log and continue
        }
        return null;
    }

    private String[] parseCsvLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (char c : line.toCharArray()) {
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                values.add(current.toString().trim());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        values.add(current.toString().trim());

        return values.toArray(new String[0]);
    }

    private void validateHeaders(String[] headers) {
        if (headers.length < 4) {
            throw new IllegalArgumentException(
                    "CSV must have at least 4 columns: Policy Name, Project, Sample Type, Period");
        }
        // Headers are case-insensitive, just check we have enough columns
    }

    private String getValue(String[] values, int index) {
        if (index >= values.length) {
            return null;
        }
        String value = values[index];
        if (value == null || value.trim().isEmpty() || value.equals("-")) {
            return null;
        }
        return value.trim();
    }
}
