/**
 * The contents of this file are subject to the Mozilla Public License Version 1.1 (the "License");
 * you may not use this file except in compliance with the License. You may obtain a copy of the
 * License at http://www.mozilla.org/MPL/
 *
 * <p>Software distributed under the License is distributed on an "AS IS" basis, WITHOUT WARRANTY OF
 * ANY KIND, either express or implied. See the License for the specific language governing rights
 * and limitations under the License.
 *
 * <p>The Original Code is OpenELIS code.
 *
 * <p>Copyright (C) The Minnesota Department of Health. All Rights Reserved.
 */
package org.openelisglobal.result.valueholder;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.util.Objects;
import java.util.UUID;
import org.openelisglobal.analysis.valueholder.Analysis;
import org.openelisglobal.analyte.valueholder.Analyte;
import org.openelisglobal.common.util.StringUtil;
import org.openelisglobal.common.valueholder.EnumValueItemImpl;
import org.openelisglobal.common.valueholder.ValueHolder;
import org.openelisglobal.common.valueholder.ValueHolderInterface;
import org.openelisglobal.dataexchange.orderresult.OrderResponseWorker.Event;
import org.openelisglobal.testresult.valueholder.TestResult;
import org.springframework.beans.factory.annotation.Value;

public class Result extends EnumValueItemImpl {

    private static final long serialVersionUID = 1L;

    private String id;
    private UUID fhirUuid;
    private ValueHolderInterface analysis;
    private ValueHolderInterface analyte;
    private ValueHolderInterface testResult;
    private String sortOrder;
    private String isReportable;
    private String resultType;
    private String value;
    private Double minNormal;
    private Double maxNormal;
    private int significantDigits;
    private ValueHolder parentResult;
    private int grouping;

    @Value("${viralload.limit.low:49}")
    private Integer virralloadLowLimit;

    private Event resultEvent;

    public Result() {
        super();
        analysis = new ValueHolder();
        analyte = new ValueHolder();
        testResult = new ValueHolder();
        parentResult = new ValueHolder();
    }

    public Analysis getAnalysis() {
        return (Analysis) this.analysis.getValue();
    }

    public void setAnalysis(Analysis analysis) {
        this.analysis.setValue(analysis);
    }

    public Analyte getAnalyte() {
        return (Analyte) this.analyte.getValue();
    }

    public void setAnalyte(Analyte analyte) {
        this.analyte.setValue(analyte);
    }

    public String getIsReportable() {
        return isReportable;
    }

    public void setIsReportable(String isReportable) {
        this.isReportable = isReportable;
    }

    public String getResultType() {
        return resultType;
    }

    public void setResultType(String resultType) {
        this.resultType = resultType;
    }

    @Override
    public String getSortOrder() {
        return sortOrder;
    }

    @Override
    public void setSortOrder(String sortOrder) {
        this.sortOrder = sortOrder;
    }

    public TestResult getTestResult() {
        return (TestResult) this.testResult.getValue();
    }

    public void setTestResult(TestResult testResult) {
        this.testResult.setValue(testResult);
    }

    public String getValue() {
        return value;
    }

    public String getValue(Boolean getActualNumericValue) {
        if (getActualNumericValue) {
            if ((this.resultType.equals("N")) && this.value != null) {
                return StringUtil.getActualNumericValue(value);
            }
        }
        return value;
    }

    @JsonIgnore
    public long getVLValueAsNumber() {
        long finalResult = 0;
        String workingResult = value.split("\\(")[0].trim();
        if (workingResult.toLowerCase().contains("log7") || workingResult.contains(">")) {
            finalResult = 10000000;
        } else if (workingResult.toUpperCase().contains("LL") || workingResult.contains("<")) {
            finalResult = virralloadLowLimit;
        } else {
            try {
                finalResult = Long.parseLong(workingResult.replaceAll("[^0-9]", ""));
            } catch (Exception e) {
                finalResult = -1;
            }
        }

        return finalResult;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return this.id;
    }

    public Double getMinNormal() {
        return minNormal;
    }

    public void setMinNormal(Double minNormal) {
        this.minNormal = minNormal;
    }

    public Double getMaxNormal() {
        return maxNormal;
    }

    public void setMaxNormal(Double maxNormal) {
        this.maxNormal = maxNormal;
    }

    public int getSignificantDigits() {
        return significantDigits;
    }

    public void setSignificantDigits(int significantDigits) {
        this.significantDigits = significantDigits;
    }

    public Result getParentResult() {
        return (Result) parentResult.getValue();
    }

    public void setParentResult(Result parentResult) {
        this.parentResult.setValue(parentResult);
    }

    public int getGrouping() {
        return grouping;
    }

    public void setGrouping(int grouping) {
        this.grouping = grouping;
    }

    public Event getResultEvent() {
        return resultEvent;
    }

    public void setResultEvent(Event resultEvent) {
        this.resultEvent = resultEvent;
    }

    public UUID getFhirUuid() {
        return fhirUuid;
    }

    public void setFhirUuid(UUID fhirUuid) {
        this.fhirUuid = fhirUuid;
    }

    public String getFhirUuidAsString() {
        return fhirUuid == null ? "" : fhirUuid.toString();
    }

    public Integer getVirralloadLowLimit() {
        return virralloadLowLimit;
    }

    public void setVirralloadLowLimit(Integer virralloadLowLimit) {
        this.virralloadLowLimit = virralloadLowLimit;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        Result that = (Result) o;
        return Objects.equals(this.id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
