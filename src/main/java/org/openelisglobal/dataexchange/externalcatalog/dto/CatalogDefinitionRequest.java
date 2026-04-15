package org.openelisglobal.dataexchange.externalcatalog.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import lombok.Data;

@Data
public class CatalogDefinitionRequest {
    // Differentiation
    private boolean panel; // true if this is a panel, false if it's a test

    // Common fields
    @JsonProperty("testUuid")
    private String testUuid; // Unified identifier for both Test and Panel
    @JsonProperty("loincCode")
    private String loincCode;
    @JsonProperty("nameEnglish")
    private String nameEnglish;
    @JsonProperty("nameFrench")
    private String nameFrench;
    @JsonProperty("reportNameEnglish")
    private String reportNameEnglish;
    @JsonProperty("reportNameFrench")
    private String reportNameFrench;
    private BigDecimal price;
    private boolean active = true;

    // Test specific identification (Ignored if panel=true)
    @JsonProperty("testSectionId")
    private String testSectionId;
    @JsonProperty("testSectionName")
    private String testSectionName;
    @JsonProperty("uomId")
    private String uomId;
    @JsonProperty("uomName")
    private String uomName;
    @JsonProperty("resultTypeId")
    private String resultTypeId;
    @JsonProperty("resultTypeName")
    private String resultTypeName;

    private boolean orderable = true;
    private boolean notifyResults = false;
    private boolean inLabOnly = false;
    private boolean antimicrobialResistance = false;

    // Test specific associations
    @JsonProperty("sampleTypeIds")
    private List<String> sampleTypeIds = new ArrayList<>();
    @JsonProperty("sampleTypeNames")
    private List<String> sampleTypeNames = new ArrayList<>();
    @JsonProperty("panelIds")
    private List<String> panelIds = new ArrayList<>();
    @JsonProperty("panelNames")
    private List<String> panelNames = new ArrayList<>();

    @JsonProperty("numericConfig")
    private NumericConfig numericConfig;
    @JsonProperty("dictionaryConfig")
    private DictionaryConfig dictionaryConfig;

    // Panel specific associations (Ignored if panel=false)
    @JsonProperty("sampleTypeId")
    private String sampleTypeId;
    @JsonProperty("sampleTypeName")
    private String sampleTypeName;
    @JsonProperty("memberTestUuids")
    private List<String> memberTestUuids = new ArrayList<>();
    @JsonProperty("memberTestLoincCodes")
    private List<String> memberTestLoincCodes = new ArrayList<>();
    @JsonProperty("memberTestNames")
    private List<String> memberTestNames = new ArrayList<>();

    @Data
    public static class NumericConfig {
        @JsonProperty("lowValid")
        private String lowValid;
        @JsonProperty("highValid")
        private String highValid;
        @JsonProperty("lowReportingRange")
        private String lowReportingRange;
        @JsonProperty("highReportingRange")
        private String highReportingRange;
        @JsonProperty("lowCritical")
        private String lowCritical;
        @JsonProperty("highCritical")
        private String highCritical;
        @JsonProperty("significantDigits")
        private String significantDigits;
        private List<ResultLimitDTO> limits = new ArrayList<>();
    }

    @Data
    public static class ResultLimitDTO {
        private String gender; // M, F, or null
        @JsonProperty("lowAge")
        private String lowAge;
        @JsonProperty("highAge")
        private String highAge;
        @JsonProperty("lowNormal")
        private String lowNormal;
        @JsonProperty("highNormal")
        private String highNormal;
    }

    @Data
    public static class DictionaryConfig {
        @JsonProperty("dictionaryReferenceId")
        private String dictionaryReferenceId;
        @JsonProperty("dictionaryReferenceName")
        private String dictionaryReferenceName;
        private List<DictionaryEntryDTO> entries = new ArrayList<>();
    }

    @Data
    public static class DictionaryEntryDTO {
        @JsonProperty("dictionaryId")
        private String dictionaryId;
        @JsonProperty("dictionaryUuid")
        private String dictionaryUuid;
        @JsonProperty("dictionaryLoincCode")
        private String dictionaryLoincCode;
        @JsonProperty("dictionaryName")
        private String dictionaryName;
        @JsonProperty("isDefault")
        private boolean isDefault;
        @JsonProperty("isQuantifiable")
        private boolean isQuantifiable;
    }
}
