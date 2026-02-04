package org.openelisglobal.notebook.service;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.hibernate.Hibernate;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.common.services.IStatusService;
import org.openelisglobal.common.services.StatusService.SampleStatus;
import org.openelisglobal.notebook.form.TraditionalMedicineManifestImportForm;
import org.openelisglobal.notebook.valueholder.NoteBook;
import org.openelisglobal.notebook.valueholder.NoteBookPage;
import org.openelisglobal.notebook.valueholder.NotebookEntry;
import org.openelisglobal.notebook.valueholder.NotebookPageSample;
import org.openelisglobal.sample.dao.SampleDAO;
import org.openelisglobal.sample.exception.DuplicateAccessionNumberException;
import org.openelisglobal.sample.service.SampleService;
import org.openelisglobal.sample.util.AccessionNumberHandler;
import org.openelisglobal.sample.valueholder.Sample;
import org.openelisglobal.sampleitem.service.SampleItemService;
import org.openelisglobal.sampleitem.valueholder.SampleItem;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Traditional Medicine manifest import: parses traditional medicine-specific
 * CSV, creates sample + sample items, links to notebook entry, and stores
 * metadata on page 1 NotebookPageSample data.
 */
@Service
public class TraditionalMedicineManifestImportServiceImpl implements TraditionalMedicineManifestImportService {

    private static final String DEFAULT_SAMPLE_TYPE = "Plant Material";

    // SRS-defined sample categories for Traditional Medicine Laboratory
    private static final List<SampleCategory> VALID_SAMPLE_CATEGORIES = List.of(
            new SampleCategory("whole_plant", "Whole Plant"), new SampleCategory("plant_part", "Plant Part"),
            new SampleCategory("plant_extract", "Plant Extract"),
            new SampleCategory("fractionated_extract", "Fractionated Extract"),
            new SampleCategory("purified_compound", "Purified Compound"),
            new SampleCategory("formulated_product", "Formulated Product"),
            new SampleCategory("reference_standard", "Reference Standard"));

    @Autowired
    private TypeOfSampleService typeOfSampleService;

    @Autowired
    private SampleService sampleService;

    @Autowired
    private SampleDAO sampleDAO;

    @Autowired
    private SampleItemService sampleItemService;

    @Autowired
    private NotebookEntryService notebookEntryService;

    @Autowired
    private NotebookSampleEntryService notebookSampleEntryService;

    @Autowired
    private NotebookPageSampleService notebookPageSampleService;

    @Autowired
    private IStatusService statusService;

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    public ParsedManifest parseManifestCsv(InputStream csvInput, TraditionalMedicineManifestImportForm columnMapping) {
        List<TraditionalMedicineManifestRow> rows = new ArrayList<>();
        List<ParseError> errors = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(csvInput, StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null || headerLine.isBlank()) {
                return new ParsedManifest(rows, errors);
            }

            String[] headers = parseCSVLine(headerLine);
            Map<String, Integer> columnIndex = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                columnIndex.put(headers[i].trim().toLowerCase(), i);
            }

            // Map columns
            Integer sampleIdIdx = getColumnIndex(columnIndex, columnMapping.getSampleIdColumn());
            Integer sampleCategoryIdx = getColumnIndex(columnIndex, columnMapping.getSampleCategoryColumn());
            Integer sourceTypeIdx = getColumnIndex(columnIndex, columnMapping.getSourceTypeColumn());
            Integer originLocationIdx = getColumnIndex(columnIndex, columnMapping.getOriginLocationColumn());
            Integer collectionSiteIdx = getColumnIndex(columnIndex, columnMapping.getCollectionSiteColumn());
            Integer collectionDateIdx = getColumnIndex(columnIndex, columnMapping.getCollectionDateColumn());
            Integer collectedByIdx = getColumnIndex(columnIndex, columnMapping.getCollectedByColumn());
            Integer localNameIdx = getColumnIndex(columnIndex, columnMapping.getLocalNameColumn());
            Integer scientificNameIdx = getColumnIndex(columnIndex, columnMapping.getScientificNameColumn());
            Integer speciesIdx = getColumnIndex(columnIndex, columnMapping.getSpeciesColumn());
            Integer plantPartIdx = getColumnIndex(columnIndex, columnMapping.getPlantPartColumn());
            Integer sampleConditionIdx = getColumnIndex(columnIndex, columnMapping.getSampleConditionColumn());
            Integer intendedUseIdx = getColumnIndex(columnIndex, columnMapping.getIntendedUseColumn());
            Integer numOfSamplesIdx = getColumnIndex(columnIndex, columnMapping.getNumOfSamplesColumn());
            Integer notesIdx = getColumnIndex(columnIndex, columnMapping.getNotesColumn());

            String line;
            int rowNumber = 1; // header line
            while ((line = reader.readLine()) != null) {
                rowNumber++;
                if (line.isBlank()) {
                    continue;
                }

                String[] values = parseCSVLine(line);

                String sampleId = getValueAtIndex(values, sampleIdIdx);
                String sampleCategory = getValueAtIndex(values, sampleCategoryIdx);
                String sourceType = getValueAtIndex(values, sourceTypeIdx);
                String originLocation = getValueAtIndex(values, originLocationIdx);
                String collectionSite = getValueAtIndex(values, collectionSiteIdx);
                String collectionDate = getValueAtIndex(values, collectionDateIdx);
                String collectedBy = getValueAtIndex(values, collectedByIdx);
                String localName = getValueAtIndex(values, localNameIdx);
                String scientificName = getValueAtIndex(values, scientificNameIdx);
                String species = getValueAtIndex(values, speciesIdx);
                String plantPart = getValueAtIndex(values, plantPartIdx);
                String sampleCondition = getValueAtIndex(values, sampleConditionIdx);
                String intendedUse = getValueAtIndex(values, intendedUseIdx);
                String numOfSamplesStr = getValueAtIndex(values, numOfSamplesIdx);
                String notes = getValueAtIndex(values, notesIdx);

                // Validate required fields
                if (sampleId == null || sampleId.isBlank()) {
                    errors.add(new ParseError(rowNumber, "sample_id", "Sample ID is required"));
                    continue;
                }

                if (sourceType == null || sourceType.isBlank()) {
                    errors.add(new ParseError(rowNumber, "source_type", "Source type is required"));
                    continue;
                }

                int numOfSamples;
                try {
                    numOfSamples = numOfSamplesStr != null && !numOfSamplesStr.isBlank()
                            ? Integer.parseInt(numOfSamplesStr.trim())
                            : 1;
                } catch (NumberFormatException e) {
                    errors.add(
                            new ParseError(rowNumber, "num_of_samples", "Invalid number format: " + numOfSamplesStr));
                    continue;
                }

                rows.add(new TraditionalMedicineManifestRow(rowNumber, sampleId.trim(), sampleCategory, sourceType,
                        originLocation, collectionSite, collectionDate, collectedBy, localName, scientificName, species,
                        plantPart, sampleCondition, intendedUse, numOfSamples, notes));
            }

        } catch (IOException e) {
            errors.add(new ParseError(0, "file", "Error reading CSV file: " + e.getMessage()));
        }

        return new ParsedManifest(rows, errors);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ParseError> validateSampleData(ParsedManifest manifest) {
        List<ParseError> errors = new ArrayList<>();

        // Get valid category descriptions
        List<String> validCategoryDescriptions = VALID_SAMPLE_CATEGORIES.stream().map(SampleCategory::description)
                .toList();

        for (TraditionalMedicineManifestRow row : manifest.rows()) {
            // Validate sample categories (if provided)
            if (row.sampleCategory() != null && !row.sampleCategory().isBlank()
                    && !validCategoryDescriptions.contains(row.sampleCategory())) {
                String validValues = String.join(", ", validCategoryDescriptions);
                errors.add(new ParseError(row.rowNumber(), "sample_category",
                        "Invalid sample category: " + row.sampleCategory() + ". Must be one of: " + validValues));
            }

            // Validate source types
            List<String> validSourceTypes = List.of("Plant material", "Other traditional medicine source",
                    "Plant Extract");
            if (row.sourceType() != null
                    && !validSourceTypes.stream().anyMatch(t -> t.equalsIgnoreCase(row.sourceType()))) {
                errors.add(new ParseError(row.rowNumber(), "source_type", "Invalid source type: " + row.sourceType()
                        + ". Must be one of: Plant material, Other traditional medicine source, Plant Extract"));
            }

            // Validate plant parts if provided
            List<String> validPlantParts = List.of("Root", "Leaf", "Stem", "Bark", "Flower", "Seed", "Whole plant",
                    "Rhizome", "Multiple", "Whole organism", "Bee product");
            if (row.plantPart() != null && !row.plantPart().isBlank()
                    && !validPlantParts.stream().anyMatch(p -> p.equalsIgnoreCase(row.plantPart()))) {
                errors.add(new ParseError(row.rowNumber(), "plant_part", "Invalid plant part: " + row.plantPart()
                        + ". Must be one of: Root, Leaf, Stem, Bark, Flower, Seed, Whole plant, Rhizome, Multiple, Whole organism, Bee product"));
            }

            // Validate sample conditions if provided
            List<String> validConditions = List.of("Fresh", "Dried", "Preserved", "Processed", "Crystalline");
            if (row.sampleCondition() != null && !row.sampleCondition().isBlank()
                    && !validConditions.stream().anyMatch(c -> c.equalsIgnoreCase(row.sampleCondition()))) {
                errors.add(new ParseError(row.rowNumber(), "sample_condition", "Invalid sample condition: "
                        + row.sampleCondition() + ". Must be one of: Fresh, Dried, Preserved, Processed, Crystalline"));
            }
        }

        return errors;
    }

    @Override
    @Transactional
    public TraditionalMedicineManifestImportResult createSamplesForEntry(Integer entryId, ParsedManifest manifest,
            String sysUserId) {
        List<ParseError> errors = new ArrayList<>();
        List<String> createdAccessionNumbers = new ArrayList<>();

        Optional<NotebookEntry> optEntry = notebookEntryService.getMatch("id", entryId);
        if (optEntry.isEmpty()) {
            errors.add(new ParseError(0, "entry", "Notebook entry not found: " + entryId));
            return new TraditionalMedicineManifestImportResult(0, 0, errors, createdAccessionNumbers);
        }

        NotebookEntry entry = optEntry.get();
        NoteBook notebook = entry.getNotebook();
        Integer firstPageId = null;
        if (notebook != null) {
            Hibernate.initialize(notebook.getPages());
            List<NoteBookPage> pages = new ArrayList<>(notebook.getPages());
            pages.sort((a, b) -> {
                Integer o1 = a.getOrder() != null ? a.getOrder() : Integer.MAX_VALUE;
                Integer o2 = b.getOrder() != null ? b.getOrder() : Integer.MAX_VALUE;
                return o1.compareTo(o2);
            });
            if (!pages.isEmpty()) {
                firstPageId = pages.get(0).getId();
            }
        }

        String sampleEnteredStatusId = statusService.getStatusID(SampleStatus.Entered);
        if (sampleEnteredStatusId == null || "-1".equals(sampleEnteredStatusId)) {
            sampleEnteredStatusId = "20";
        }

        // Get or create default sample type for traditional medicine
        TypeOfSample sampleType = getOrCreateDefaultSampleType();

        int totalRequested = 0;
        int totalCreated = 0;

        for (TraditionalMedicineManifestRow row : manifest.rows()) {
            if (row.numOfSamples() <= 0) {
                continue;
            }

            totalRequested += row.numOfSamples();

            Sample parentSample = new Sample();
            parentSample.setSysUserId(sysUserId);
            parentSample.setEnteredDate(new java.sql.Date(System.currentTimeMillis()));
            parentSample.setReceivedTimestamp(new Timestamp(System.currentTimeMillis()));
            String sampleIdDb;
            try {
                AccessionNumberHandler handler = new AccessionNumberHandler(sampleService, sampleDAO, entityManager,
                        this.getClass());
                sampleIdDb = handler.generateAndInsertWithUniqueAccessionNumber(parentSample);
                parentSample.setId(sampleIdDb);
            } catch (DuplicateAccessionNumberException e) {
                errors.add(new ParseError(row.rowNumber(), "sample",
                        "Failed to generate unique accession number: " + e.getMessage()));
                LogEvent.logError("Duplicate accession number error for row " + row.rowNumber(), e);
                continue;
            }

            for (int seq = 1; seq <= row.numOfSamples(); seq++) {
                SampleItem item = new SampleItem();
                item.setSample(parentSample);
                item.setTypeOfSample(sampleType);
                item.setExternalId(generateExternalId(row.sampleId(), seq));
                item.setSortOrder(Integer.toString(seq));
                item.setStatusId(sampleEnteredStatusId);
                item.setSysUserId(sysUserId);

                String itemId = sampleItemService.insert(item);
                item.setId(itemId);
                totalCreated++;
                createdAccessionNumbers.add(parentSample.getAccessionNumber());

                notebookEntryService.addSample(entryId, item, sysUserId);

                if (firstPageId != null) {
                    NotebookPageSample nps = notebookPageSampleService.getByPageIdAndSampleItemId(firstPageId,
                            Integer.parseInt(itemId));
                    if (nps != null) {
                        Map<String, Object> data = nps.getData() != null ? new HashMap<>(nps.getData())
                                : new HashMap<>();
                        // Store all traditional medicine metadata
                        data.put("sampleCategory",
                                row.sampleCategory() != null ? row.sampleCategory() : "Traditional medicine");
                        data.put("sourceType", row.sourceType());
                        data.put("originLocation", row.originLocation());
                        data.put("collectionSite", row.collectionSite());
                        data.put("collectionDate", row.collectionDate());
                        data.put("collectedBy", row.collectedBy());
                        data.put("localName", row.localName());
                        data.put("scientificName", row.scientificName());
                        data.put("species", row.species());
                        data.put("plantPart", row.plantPart());
                        data.put("sampleCondition", row.sampleCondition());
                        data.put("intendedUse", row.intendedUse());
                        data.put("notes", row.notes());
                        nps.setData(data);
                        notebookPageSampleService.update(nps);
                    }
                }
            }
        }

        return new TraditionalMedicineManifestImportResult(totalRequested, totalCreated, errors,
                createdAccessionNumbers.stream().distinct().toList());
    }

    private TypeOfSample getOrCreateDefaultSampleType() {
        TypeOfSample searchType = new TypeOfSample();
        searchType.setDescription(DEFAULT_SAMPLE_TYPE);
        TypeOfSample found = typeOfSampleService.getTypeOfSampleByDescriptionAndDomain(searchType, true);
        if (found != null) {
            return found;
        }
        // Return the first available sample type as fallback
        List<TypeOfSample> allTypes = typeOfSampleService.getAllTypeOfSamples();
        return allTypes.isEmpty() ? null : allTypes.get(0);
    }

    private String generateExternalId(String sampleId, int seq) {
        return String.format("%s-%03d", sampleId, seq);
    }

    private String[] parseCSVLine(String line) {
        List<String> values = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
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

    private Integer getColumnIndex(Map<String, Integer> columnIndex, String columnName) {
        if (columnName == null || columnName.isBlank()) {
            return null;
        }
        return columnIndex.get(columnName.trim().toLowerCase());
    }

    private String getValueAtIndex(String[] values, Integer index) {
        if (index == null || index < 0 || index >= values.length) {
            return null;
        }
        String value = values[index];
        return value != null && !value.isBlank() ? value.trim() : null;
    }

    @Override
    public List<SampleCategory> getValidSampleCategories() {
        return new ArrayList<>(VALID_SAMPLE_CATEGORIES);
    }
}
