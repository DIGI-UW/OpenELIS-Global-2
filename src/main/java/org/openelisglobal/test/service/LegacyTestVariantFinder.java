package org.openelisglobal.test.service;

import java.util.ArrayList;
import java.util.List;
import org.openelisglobal.common.util.TestDescriptionNormalizer;
import org.openelisglobal.test.valueholder.Test;
import org.openelisglobal.typeofsample.service.TypeOfSampleService;
import org.openelisglobal.typeofsample.valueholder.TypeOfSample;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Finds the records an earlier catalog loader wrote as one test per specimen,
 * {@code Name(SampleType)}, for a plain test name. The name is compared the way
 * the database normalises descriptions (no spaces, punctuation, accents or
 * case), so {@code HIVVIRALLOAD(Serum)} is a variant of {@code HIV Viral Load}.
 * Only a parenthesised part that names a known sample type counts: a test
 * called {@code Glucose (fasting)} is a different test, not a variant of
 * Glucose.
 */
@Service
public class LegacyTestVariantFinder {

    public record LegacyTestVariant(Test test, TypeOfSample specimen, String specimenLabel) {
    }

    @Autowired
    private TestService testService;

    @Autowired
    private TypeOfSampleService typeOfSampleService;

    public List<LegacyTestVariant> find(String plainName) {
        String normalizedName = TestDescriptionNormalizer.normalizeText(plainName);
        List<LegacyTestVariant> variants = new ArrayList<>();
        if (normalizedName.isEmpty()) {
            return variants;
        }
        for (Test candidate : testService.getTestsByNormalizedDescriptionPrefix(plainName)) {
            String description = candidate.getDescription();
            if (description == null || !description.endsWith(")")) {
                continue;
            }
            int open = description.lastIndexOf('(');
            if (open <= 0 || !normalizedName
                    .equals(TestDescriptionNormalizer.normalizeText(description.substring(0, open)))) {
                continue;
            }
            String label = description.substring(open + 1, description.length() - 1).trim();
            TypeOfSample specimen = findSampleType(label);
            if (specimen != null) {
                variants.add(new LegacyTestVariant(candidate, specimen, label));
            }
        }
        return variants;
    }

    private TypeOfSample findSampleType(String name) {
        if (name.isEmpty()) {
            return null;
        }
        for (TypeOfSample sampleType : typeOfSampleService.getAllTypeOfSamples()) {
            if (name.equalsIgnoreCase(sampleType.getLocalizedName())
                    || name.equalsIgnoreCase(sampleType.getDescription())
                    || name.equalsIgnoreCase(sampleType.getLocalAbbreviation())) {
                return sampleType;
            }
        }
        return null;
    }
}
