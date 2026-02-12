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
 *
 * <p>Contributor(s): CIRG, University of Washington, Seattle WA.
 */
package org.openelisglobal.analyzer.service;

import ca.uhn.hl7v2.DefaultHapiContext;
import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.HapiContext;
import ca.uhn.hl7v2.model.Message;
import ca.uhn.hl7v2.model.v251.datatype.CE;
import ca.uhn.hl7v2.model.v251.message.ORM_O01;
import ca.uhn.hl7v2.model.v251.message.ORU_R01;
import ca.uhn.hl7v2.model.v251.segment.OBR;
import ca.uhn.hl7v2.model.v251.segment.ORC;
import ca.uhn.hl7v2.parser.PipeParser;
import ca.uhn.hl7v2.util.Terser;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

/**
 * HL7 v2.x message parsing and generation implementation.
 *
 * <p>
 */
@Service
public class HL7MessageServiceImpl implements HL7MessageService {

    private static final Pattern SEGMENT_DELIMITER = Pattern.compile("\\r\\n|\\r|\\n");
    private static final String FIELD_SEP = "|";

    private final HapiContext hapiContext;
    private final PipeParser parser;

    public HL7MessageServiceImpl() {
        this.hapiContext = new DefaultHapiContext();
        this.parser = hapiContext.getPipeParser();
    }

    @Override
    public OruR01ParseResult parseOruR01(String rawMessage) {
        if (StringUtils.isBlank(rawMessage)) {
            throw new HL7ParseException("Raw ORU^R01 message is null or empty");
        }
        try {
            Message msg = parser.parse(normalizeSegmentTerminators(rawMessage));
            if (!(msg instanceof ORU_R01)) {
                throw new HL7ParseException("Message is not ORU^R01: " + msg.getClass().getSimpleName());
            }
            ORU_R01 oru = (ORU_R01) msg;
            return extractOruResult(oru);
        } catch (HL7Exception e) {
            throw new HL7ParseException("Failed to parse ORU^R01: " + e.getMessage(), e);
        }
    }

    @Override
    public String generateOrmO01(OrmO01Request request) {
        if (request == null) {
            throw new HL7GenerationException("OrmO01Request is null");
        }
        try {
            ORM_O01 orm = new ORM_O01();
            orm.initQuickstart("ORM", "O01", "P");
            Terser t = new Terser(orm);

            t.set("/.MSH-3-1", "OpenELIS");
            t.set("/.MSH-4-1", "LAB");
            t.set("/.MSH-5-1",
                    request.getReceivingApplication() != null ? request.getReceivingApplication() : "ANALYZER");
            t.set("/.MSH-6-1", request.getReceivingFacility() != null ? request.getReceivingFacility() : "LAB");
            t.set("/.MSH-7", formatTs(System.currentTimeMillis()));
            t.set("/.MSH-9-1", "ORM");
            t.set("/.MSH-9-2", "O01");
            t.set("/.MSH-10", "ORM" + System.currentTimeMillis());
            t.set("/.MSH-12", "2.5.1");

            if (request.getPatientId() != null) {
                t.set("/.PATIENT/PID-3-1", request.getPatientId());
            }
            if (request.getPatientLastName() != null || request.getPatientFirstName() != null) {
                t.set("/.PATIENT/PID-5-1", request.getPatientLastName() != null ? request.getPatientLastName() : "");
                t.set("/.PATIENT/PID-5-2", request.getPatientFirstName() != null ? request.getPatientFirstName() : "");
            }
            if (request.getPatientDob() != null) {
                t.set("/.PATIENT/PID-7", request.getPatientDob());
            }
            if (request.getPatientGender() != null) {
                t.set("/.PATIENT/PID-8", request.getPatientGender());
            }

            String placer = request.getPlacerOrderNumber() != null ? request.getPlacerOrderNumber() : "";
            String filler = request.getFillerOrderNumber() != null ? request.getFillerOrderNumber() : "";
            ORC orc = orm.getORDER().getORC();
            orc.getOrc1_OrderControl().setValue("NW");
            orc.getOrc2_PlacerOrderNumber().getEi1_EntityIdentifier().setValue(placer);
            orc.getOrc3_FillerOrderNumber().getEi1_EntityIdentifier().setValue(filler);

            OBR obr = orm.getORDER().getORDER_DETAIL().getOBR();
            obr.getObr1_SetIDOBR().setValue("1");
            obr.getObr2_PlacerOrderNumber().getEi1_EntityIdentifier().setValue(placer);
            obr.getObr3_FillerOrderNumber().getEi1_EntityIdentifier().setValue(filler);
            List<OrmOrderItem> orders = request.getOrders();
            if (orders != null && !orders.isEmpty()) {
                OrmOrderItem first = orders.get(0);
                String code = first.getTestCode() != null ? first.getTestCode() : "";
                String name = first.getTestName() != null ? first.getTestName() : "";
                obr.getObr4_UniversalServiceIdentifier().getCe1_Identifier().setValue(code);
                obr.getObr4_UniversalServiceIdentifier().getCe2_Text().setValue(name);
            } else {
                obr.getObr4_UniversalServiceIdentifier().getCe1_Identifier().setValue("1");
            }
            obr.getObr25_ResultStatus().setValue("R");

            return parser.encode(orm);
        } catch (HL7Exception e) {
            throw new HL7GenerationException("Failed to generate ORM^O01: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new HL7GenerationException("Failed to encode ORM^O01: " + e.getMessage(), e);
        }
    }

    @Override
    public MshInfo extractMshInfo(String rawMessage) {
        if (StringUtils.isBlank(rawMessage)) {
            return new MshInfoImpl("", "");
        }
        List<String> lines = toSegmentLines(rawMessage);
        for (String line : lines) {
            if (line.startsWith("MSH" + FIELD_SEP)) {
                String[] f = line.split("\\" + FIELD_SEP, -1);
                String msh3 = f.length > 2 ? StringUtils.defaultString(f[2]).trim() : "";
                String msh4 = f.length > 3 ? StringUtils.defaultString(f[3]).trim() : "";
                return new MshInfoImpl(msh3, msh4);
            }
        }
        return new MshInfoImpl("", "");
    }

    @Override
    public List<String> toSegmentLines(String rawMessage) {
        if (StringUtils.isBlank(rawMessage)) {
            return new ArrayList<>();
        }
        String normalized = normalizeSegmentTerminators(rawMessage);
        List<String> out = new ArrayList<>();
        for (String s : SEGMENT_DELIMITER.split(normalized)) {
            String t = s.trim();
            if (!t.isEmpty()) {
                out.add(t);
            }
        }
        return out;
    }

    private static String normalizeSegmentTerminators(String raw) {
        return raw.replace("\r\n", "\r").replace("\n", "\r").replace("\r\r", "\r");
    }

    private static String formatTs(long ms) {
        return new java.text.SimpleDateFormat("yyyyMMddHHmmss.SSS").format(new java.util.Date(ms));
    }

    private OruR01ParseResult extractOruResult(ORU_R01 oru) throws HL7Exception {
        String patientId = "";
        String placer = "";
        String filler = "";
        String serviceId = "";
        List<HL7MessageService.ObxResult> results = new ArrayList<>();

        if (oru.getPATIENT_RESULTReps() > 0) {
            var pr = oru.getPATIENT_RESULT(0);
            var pid = pr.getPATIENT().getPID();
            patientId = getCXId(pid.getPid3_PatientIdentifierList(0));

            if (pr.getORDER_OBSERVATIONReps() > 0) {
                var oo = pr.getORDER_OBSERVATION(0);
                var orc = oo.getORC();
                placer = orc.getOrc2_PlacerOrderNumber().getEi1_EntityIdentifier().getValue();
                filler = orc.getOrc3_FillerOrderNumber().getEi1_EntityIdentifier().getValue();
                var obr = oo.getOBR();
                serviceId = obr.getObr4_UniversalServiceIdentifier().getCe1_Identifier().getValue();
                if (StringUtils.isBlank(serviceId)) {
                    serviceId = obr.getObr4_UniversalServiceIdentifier().getCe2_Text().getValue();
                }

                int n = oo.getOBSERVATIONReps();
                for (int i = 0; i < n; i++) {
                    var obx = oo.getOBSERVATION(i).getOBX();
                    String vt = obx.getObx2_ValueType().getValue();
                    CE ce = obx.getObx3_ObservationIdentifier();
                    String code = ce.getCe1_Identifier().getValue();
                    String name = ce.getCe2_Text().getValue();
                    if (StringUtils.isBlank(code)) {
                        String enc = ce.encode();
                        String[] comp = enc.split("\\^", -1);
                        if (comp.length >= 4 && StringUtils.isNotBlank(comp[3])) {
                            code = comp[3].trim();
                        }
                        if (comp.length >= 5 && StringUtils.isBlank(name) && StringUtils.isNotBlank(comp[4])) {
                            name = comp[4].trim();
                        }
                    }
                    String val = "";
                    if (obx.getObx5_ObservationValueReps() > 0) {
                        val = obx.getObx5_ObservationValue(0).getData().encode();
                    }
                    String units = "";
                    try {
                        if (obx.getObx6_Units() != null) {
                            units = obx.getObx6_Units().getCe1_Identifier().getValue();
                        }
                    } catch (Exception ignored) {
                        // optional
                    }
                    results.add(new ObxResultImpl(code, name, val, units, vt));
                }
            }
        }

        return new OruR01ParseResultImpl(patientId, placer, filler, serviceId, results);
    }

    private static String getCXId(ca.uhn.hl7v2.model.v251.datatype.CX cx) {
        try {
            return cx.getCx1_IDNumber().getValue();
        } catch (Exception e) {
            return "";
        }
    }

    // --- DTO implementations ---

    private static final class OruR01ParseResultImpl implements OruR01ParseResult {
        private final String patientId;
        private final String placerOrderNumber;
        private final String fillerOrderNumber;
        private final String serviceId;
        private final List<ObxResult> results;

        OruR01ParseResultImpl(String patientId, String placerOrderNumber, String fillerOrderNumber, String serviceId,
                List<ObxResult> results) {
            this.patientId = patientId;
            this.placerOrderNumber = placerOrderNumber;
            this.fillerOrderNumber = fillerOrderNumber;
            this.serviceId = serviceId;
            this.results = results != null ? new ArrayList<>(results) : new ArrayList<>();
        }

        @Override
        public String getPatientId() {
            return patientId;
        }

        @Override
        public String getPlacerOrderNumber() {
            return placerOrderNumber;
        }

        @Override
        public String getFillerOrderNumber() {
            return fillerOrderNumber;
        }

        @Override
        public String getServiceId() {
            return serviceId;
        }

        @Override
        public List<ObxResult> getResults() {
            return new ArrayList<>(results);
        }
    }

    private static final class ObxResultImpl implements ObxResult {
        private final String testCode;
        private final String testName;
        private final String value;
        private final String units;
        private final String valueType;

        ObxResultImpl(String testCode, String testName, String value, String units, String valueType) {
            this.testCode = testCode;
            this.testName = testName;
            this.value = value;
            this.units = units;
            this.valueType = valueType;
        }

        @Override
        public String getTestCode() {
            return testCode;
        }

        @Override
        public String getTestName() {
            return testName;
        }

        @Override
        public String getValue() {
            return value;
        }

        @Override
        public String getUnits() {
            return units;
        }

        @Override
        public String getValueType() {
            return valueType;
        }
    }

    private static final class MshInfoImpl implements MshInfo {
        private final String sendingApplication;
        private final String sendingFacility;

        MshInfoImpl(String sendingApplication, String sendingFacility) {
            this.sendingApplication = sendingApplication;
            this.sendingFacility = sendingFacility;
        }

        @Override
        public String getSendingApplication() {
            return sendingApplication;
        }

        @Override
        public String getSendingFacility() {
            return sendingFacility;
        }
    }
}
