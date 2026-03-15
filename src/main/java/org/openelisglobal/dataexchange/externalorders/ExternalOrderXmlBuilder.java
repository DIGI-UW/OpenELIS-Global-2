package org.openelisglobal.dataexchange.externalorders;

import java.util.List;
import java.util.StringJoiner;
import org.openelisglobal.dataexchange.externalorders.dto.ExternalOrderRequest;

public class ExternalOrderXmlBuilder {

    /**
     * Builds sample XML with testSampleTypeMap populated for proper UI test
     * checkbox rendering.
     * 
     * @param samples                  List of sample objects (may be expanded from
     *                                 original request if tests span multiple
     *                                 sample types)
     * @param sampleTestIds            Test IDs for each sample
     * @param samplePanelIds           Panel IDs for each sample
     * @param sampleTestSampleTypeMaps testSampleTypeMap strings for each sample
     *                                 (format: "testId:sampleTypeId,...")
     * @return XML string for samples
     */
    public String buildSamplesXml(List<ExternalOrderRequest.ExternalOrderSample> samples,
            List<List<String>> sampleTestIds, List<List<String>> samplePanelIds,
            List<String> sampleTestSampleTypeMaps) {

        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
        sb.append("<samples>");

        for (int i = 0; i < samples.size(); i++) {
            ExternalOrderRequest.ExternalOrderSample sample = samples.get(i);
            List<String> testIds = sampleTestIds.get(i);
            List<String> panelIds = samplePanelIds.get(i);
            String testSampleTypeMap = (sampleTestSampleTypeMaps != null && i < sampleTestSampleTypeMaps.size())
                    ? sampleTestSampleTypeMaps.get(i)
                    : "";

            sb.append("<sample");
            appendAttr(sb, "sampleID", sample.getSampleTypeId());
            appendAttr(sb, "date", nullToEmpty(sample.getCollectionDate()));
            appendAttr(sb, "time", nullToEmpty(sample.getCollectionTime()));
            appendAttr(sb, "collector", nullToEmpty(sample.getCollector()));
            appendAttr(sb, "quantity", nullToEmpty(sample.getQuantity()));
            appendAttr(sb, "uom", nullToEmpty(sample.getUom()));
            appendAttr(sb, "tests", joinComma(testIds));
            appendAttr(sb, "testSectionMap", "");
            appendAttr(sb, "testSampleTypeMap", nullToEmpty(testSampleTypeMap));
            appendAttr(sb, "panels", joinComma(panelIds));
            appendAttr(sb, "rejected", "false");
            appendAttr(sb, "rejectReasonId", "");
            appendAttr(sb, "initialConditionIds", "");
            appendAttr(sb, "storageLocationId", "");
            appendAttr(sb, "storageLocationType", "");
            appendAttr(sb, "storagePositionCoordinate", "");
            sb.append("/>");
        }

        sb.append("</samples>");
        return sb.toString();
    }

    /**
     * Legacy method for backward compatibility - builds XML without
     * testSampleTypeMap.
     * 
     * @deprecated Use {@link #buildSamplesXml(List, List, List, List)} instead.
     */
    @Deprecated
    public String buildSamplesXml(List<ExternalOrderRequest.ExternalOrderSample> samples,
            List<List<String>> sampleTestIds, List<List<String>> samplePanelIds) {
        return buildSamplesXml(samples, sampleTestIds, samplePanelIds, null);
    }

    private static void appendAttr(StringBuilder sb, String name, String value) {
        sb.append(" ").append(name).append("='").append(escapeAttr(value)).append("'");
    }

    private static String joinComma(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        StringJoiner joiner = new StringJoiner(",");
        for (String v : values) {
            if (v != null && !v.trim().isEmpty()) {
                joiner.add(v.trim());
            }
        }
        return joiner.toString();
    }

    private static String nullToEmpty(String s) {
        return s == null ? "" : s;
    }

    private static String escapeAttr(String s) {
        if (s == null) {
            return "";
        }
        return s.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;").replace(">", "&gt;").replace("'",
                "&apos;");
    }
}
