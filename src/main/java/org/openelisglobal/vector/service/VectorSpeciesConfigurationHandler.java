package org.openelisglobal.vector.service;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.common.log.LogEvent;
import org.openelisglobal.configuration.service.DomainConfigurationHandler;
import org.openelisglobal.dictionarycategory.service.DictionaryCategoryService;
import org.openelisglobal.dictionarycategory.valueholder.DictionaryCategory;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.openelisglobal.vector.valueholder.VectorSpecies;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads vector species from vector-species.csv at startup.
 *
 * CSV format (first line is header, leading # lines are skipped):
 * genus,species,subspecies,sampleTypeAbbrev,isActive,pathogenCategory,lifecycleCategory
 *
 * pathogenCategory — name of a dictionary_category (from
 * vector-dictionaries.csv) stored as an FK on
 * vector_species.pathogen_category_id lifecycleCategory — same, stored as an FK
 * on vector_species.lifecycle_category_id
 *
 * Runs at load order 400: after dictionaries (300) and sample-types (100).
 * Existing records matched by (genus, species, subspecies, sampleTypeId) are
 * updated; new ones are inserted.
 */
@Component
public class VectorSpeciesConfigurationHandler implements DomainConfigurationHandler {

    @Autowired
    private VectorSpeciesService vectorSpeciesService;

    @Autowired
    private TypeOfSampleService typeOfSampleService;

    @Autowired
    private DictionaryCategoryService dictionaryCategoryService;

    @Override
    public String getDomainName() {
        return "vector-species";
    }

    @Override
    public String getFileExtension() {
        return "csv";
    }

    @Override
    public int getLoadOrder() {
        return 400;
    }

    @Override
    @Transactional
    public void processConfiguration(InputStream inputStream, String fileName) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));

        // Skip leading comment/blank lines to find the actual header
        String headerLine = null;
        String readLine;
        while ((readLine = reader.readLine()) != null) {
            if (!readLine.trim().isEmpty() && !readLine.trim().startsWith("#")) {
                headerLine = readLine;
                break;
            }
        }
        if (headerLine == null) {
            throw new IllegalArgumentException("Vector species file " + fileName + " is empty or has no header");
        }

        String[] headers = parseCsvLine(headerLine);
        int genusIdx = findColumn(headers, "genus");
        int speciesIdx = findColumn(headers, "species");
        int subspeciesIdx = findColumn(headers, "subspecies");
        int sampleTypeIdx = findColumn(headers, "sampleTypeAbbrev");
        int isActiveIdx = findColumn(headers, "isActive");
        int pathogenCatIdx = findColumn(headers, "pathogenCategory");
        int lifecycleCatIdx = findColumn(headers, "lifecycleCategory");

        if (genusIdx < 0 || speciesIdx < 0 || sampleTypeIdx < 0) {
            throw new IllegalArgumentException(
                    "Vector species CSV " + fileName + " must have genus, species, sampleTypeAbbrev columns");
        }

        int loaded = 0;
        String line;
        int lineNum = 1;
        while ((line = reader.readLine()) != null) {
            lineNum++;
            if (line.trim().isEmpty() || line.trim().startsWith("#")) {
                continue;
            }
            try {
                if (processRow(parseCsvLine(line), genusIdx, speciesIdx, subspeciesIdx, sampleTypeIdx, isActiveIdx,
                        pathogenCatIdx, lifecycleCatIdx)) {
                    loaded++;
                }
            } catch (Exception e) {
                LogEvent.logError(this.getClass().getSimpleName(), "processConfiguration",
                        "Error on line " + lineNum + " of " + fileName + ": " + e.getMessage());
            }
        }

        LogEvent.logInfo(this.getClass().getSimpleName(), "processConfiguration",
                "Loaded " + loaded + " vector species from " + fileName);
    }

    private boolean processRow(String[] values, int genusIdx, int speciesIdx, int subspeciesIdx, int sampleTypeIdx,
            int isActiveIdx, int pathogenCatIdx, int lifecycleCatIdx) {

        String genus = get(values, genusIdx);
        String species = get(values, speciesIdx);
        String subspecies = get(values, subspeciesIdx);
        String sampleTypeAbbrev = get(values, sampleTypeIdx);

        if (genus.isEmpty() || species.isEmpty() || sampleTypeAbbrev.isEmpty()) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processRow",
                    "Skipping row with missing genus, species, or sampleTypeAbbrev");
            return false;
        }

        TypeOfSample sampleType = typeOfSampleService.getTypeOfSampleByLocalAbbrevAndDomain(sampleTypeAbbrev, "V");
        if (sampleType == null) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "processRow",
                    "Unknown sampleTypeAbbrev '" + sampleTypeAbbrev + "' — skipping " + genus + " " + species);
            return false;
        }

        boolean isActive = !"N".equalsIgnoreCase(get(values, isActiveIdx));
        DictionaryCategory pathogenCategory = resolveCategory(get(values, pathogenCatIdx));
        DictionaryCategory lifecycleCategory = resolveCategory(get(values, lifecycleCatIdx));

        VectorSpecies existing = findExisting(genus, species, subspecies, sampleType.getId());

        if (existing != null) {
            existing.setActive(isActive);
            if (pathogenCategory != null) {
                existing.setPathogenCategoryId(Long.valueOf(pathogenCategory.getId()));
            }
            if (lifecycleCategory != null) {
                existing.setLifecycleCategoryId(Long.valueOf(lifecycleCategory.getId()));
            }
            existing.setSysUserId("1");
            vectorSpeciesService.update(existing);
            LogEvent.logInfo(this.getClass().getSimpleName(), "processRow",
                    "Updated vector species: " + genus + " " + species);
        } else {
            VectorSpecies newSpecies = new VectorSpecies();
            newSpecies.setGenus(genus);
            newSpecies.setSpecies(species);
            newSpecies.setSubspecies(subspecies.isEmpty() ? null : subspecies);
            newSpecies.setActive(isActive);
            if (pathogenCategory != null) {
                newSpecies.setPathogenCategoryId(Long.valueOf(pathogenCategory.getId()));
            }
            if (lifecycleCategory != null) {
                newSpecies.setLifecycleCategoryId(Long.valueOf(lifecycleCategory.getId()));
            }
            vectorSpeciesService.create(newSpecies, sampleType.getId(), "1");
            LogEvent.logInfo(this.getClass().getSimpleName(), "processRow",
                    "Created vector species: " + genus + " " + species);
        }
        return true;
    }

    private DictionaryCategory resolveCategory(String categoryName) {
        if (categoryName == null || categoryName.isEmpty()) {
            return null;
        }
        DictionaryCategory category = dictionaryCategoryService.getDictionaryCategoryByName(categoryName);
        if (category == null) {
            LogEvent.logWarn(this.getClass().getSimpleName(), "resolveCategory", "Dictionary category '" + categoryName
                    + "' not found — check vector-dictionaries.csv loaded first");
        }
        return category;
    }

    private VectorSpecies findExisting(String genus, String species, String subspecies, String sampleTypeId) {
        List<VectorSpecies> bySampleType = vectorSpeciesService.getBySampleTypeId(sampleTypeId);
        for (VectorSpecies s : bySampleType) {
            if (genus.equalsIgnoreCase(s.getGenus()) && species.equalsIgnoreCase(s.getSpecies())
                    && nullSafeEquals(subspecies.isEmpty() ? null : subspecies, s.getSubspecies())) {
                return s;
            }
        }
        return null;
    }

    private boolean nullSafeEquals(String a, String b) {
        if (a == null && b == null)
            return true;
        if (a == null || b == null)
            return false;
        return a.equalsIgnoreCase(b);
    }

    private String get(String[] values, int idx) {
        if (idx < 0 || idx >= values.length)
            return "";
        return values[idx] != null ? values[idx] : "";
    }

    private int findColumn(String[] headers, String name) {
        for (int i = 0; i < headers.length; i++) {
            if (name.equalsIgnoreCase(headers[i].trim()))
                return i;
        }
        return -1;
    }

    private String[] parseCsvLine(String line) {
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
}
