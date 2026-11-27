package org.openelisglobal.microbiology.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Synthetic thresholds exercise the transport contract, not a clinical catalog.
 */
public class ReferencePackageFixture {
    public final Map<String, String> files = new LinkedHashMap<>();
    public final String version;
    public final String organism;
    public final String antibiotic;
    public final String edition;

    public ReferencePackageFixture(String version, String suffix) throws Exception {
        this.version = version;
        organism = "TEST_ORG_" + suffix;
        antibiotic = "TEST_AB_" + suffix;
        edition = "2026-" + version + "-" + suffix;
        files.put("organisms.csv", "source,code,displayName,whonetCode,gramStain\nAMR," + organism
                + ",Synthetic organism " + suffix + ",,GRAM_NEGATIVE\n");
        files.put("antimicrobials.csv", "source,code,displayName,whonetCode,antibioticClass\nAMR," + antibiotic
                + ",Synthetic agent " + suffix + "," + antibiotic + ",Test class\n");
        files.put("breakpoints.csv",
                "publisher,version,organism_or_group,antibiotic_whonet_code,method,specimen_type_id,breakpoint_type,susceptible_value,intermediate_lower_value,intermediate_upper_value,resistant_value,units,resistant_inclusive,organism_source,organism_code,antibiotic_source,antibiotic_code,notes\n"
                        + "EUCAST," + edition + ",Synthetic organism " + suffix + "," + antibiotic
                        + ",MIC,,MIC,1,,,4,mg/L,false,AMR," + organism + ",AMR," + antibiotic
                        + ",Synthetic evidence\n");
        StringBuilder vectors = new StringBuilder(
                "publisher,version,organism_code,antibiotic_code,method,value,expected\n");
        for (String observation : List.of("0.5,S", "1,S", "1.0001,I", "4,I", "4.0001,R")) {
            vectors.append("EUCAST,").append(edition).append(',').append(organism).append(',').append(antibiotic)
                    .append(",MIC,").append(observation).append('\n');
        }
        files.put("interpretation-vectors.csv", vectors.toString());
        files.put("coverage.csv", "organism_code,guideline,antibiotic_code,method,status,reason,source_rows\n"
                + organism + ",EUCAST 2026," + antibiotic + ",MIC,SUPPORTED,,1\n");
        files.put("source-rows.csv", "source_row,guideline,organism_code,antibiotic_code,method,status\n1,EUCAST 2026,"
                + organism + "," + antibiotic + ",MIC,SEE_COVERAGE\n");
        files.put("scope.json", new ObjectMapper().writeValueAsString(Map.of("guidelines", List.of("EUCAST 2026"),
                "organisms", List.of(organism), "methods", List.of("MIC"))));
        files.put("NOTICE.txt", "Synthetic contract fixture; not for clinical use.\n");
    }

}
