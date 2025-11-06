package org.openelisglobal.dataexchange.orderresult;

import ca.uhn.hl7v2.HL7Exception;
import ca.uhn.hl7v2.model.v251.message.OML_O21;
import ca.uhn.hl7v2.model.v251.message.ORU_R01;
import ca.uhn.hl7v2.model.v251.segment.NTE;
import ca.uhn.hl7v2.model.v251.segment.OBR;
import ca.uhn.hl7v2.model.v251.segment.OBX;
import ca.uhn.hl7v2.model.v251.segment.ORC;
import ca.uhn.hl7v2.model.v251.segment.PID;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import org.openelisglobal.dataexchange.order.valueholder.ElectronicOrder;
import org.openelisglobal.dataexchange.resultreporting.beans.ResultReportXmit;
import org.openelisglobal.dataexchange.resultreporting.beans.ResultXmit;
import org.openelisglobal.dataexchange.resultreporting.beans.TestResultsXmit;
import org.openelisglobal.dataexchange.service.order.ElectronicOrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;

@Service
@Scope("prototype")
public class OrderResponseWorker {

    public enum Event {
        ORDER_RECEIVED_NO_SPEC, SPEC_RECEIVED, PRELIMINARY_RESULT, RESULT, FINAL_RESULT, CORRECTION, TESTING_NOT_DONE
    }

    @Autowired
    private ElectronicOrderService eOrderService;

    private Event event;
    private ElectronicOrder eOrder;
    private OML_O21 originalMessage = new OML_O21();

    private static int sequenceNum = 0;

    private ORU_R01 hl7Message;

    public ORU_R01 getHl7Message() {
        return hl7Message;
    }

    public void setHl7Message(ORU_R01 hl7Message) {
        this.hl7Message = hl7Message;
    }

    public void createReport(ResultReportXmit resultReport) throws HL7Exception, IOException {
        hl7Message = new ORU_R01();

        createMSHSegment();
        for (TestResultsXmit testResults : resultReport.getTestResults()) {
            event = testResults.getResultsEvent();
            List<ElectronicOrder> eOrders = eOrderService
                    .getElectronicOrdersByExternalId(testResults.getReferringOrderNumber());
            eOrder = eOrders.size() > 0 ? eOrders.get(eOrders.size() - 1) : null;
            if (eOrder != null)
                originalMessage.parse(eOrder.getData());
            createPIDSegment(testResults);
            createORCSegment(testResults);
            createOBRSegment(testResults);
            for (ResultXmit testResult : testResults.getResults()) {
                if (event.equals(Event.FINAL_RESULT) || event.equals(Event.PRELIMINARY_RESULT)
                        || event.equals(Event.RESULT) || event.equals(Event.CORRECTION)) {
                    createOBXSegment(testResults, testResult);
                    createNTESegment(testResults, testResult);
                }
            }
        }

        // LogEvent.logInfo(this.getClass().getSimpleName(), "method unkown",
        // hl7Message.encode());
    }

    private void createMSHSegment() throws HL7Exception, IOException {
        hl7Message.initQuickstart("ORU", "R01", "P");
        hl7Message.getMSH().getMsh3_SendingApplication().getHd2_UniversalID().setValue("OpenELIS");
        hl7Message.getMSH().getMsh5_ReceivingApplication().getHd2_UniversalID().setValue("iSantePlus");
        hl7Message.getMSH().getMsh10_MessageControlID().setValue(generateMessageId());
    }

    private void createPIDSegment(TestResultsXmit testResult) throws HL7Exception {
        PID originalPidSegment = originalMessage.getPATIENT().getPID();
        PID pidSegment = hl7Message.getPATIENT_RESULT().getPATIENT().getPID();
        pidSegment.parse(originalPidSegment.encode());
    }

    private void createORCSegment(TestResultsXmit testResult) throws HL7Exception {
        ORC originalOrcSegment = originalMessage.getORDER().getORC();
        ORC orcSegment = hl7Message.getPATIENT_RESULT().getORDER_OBSERVATION().getORC();
        orcSegment.parse(originalOrcSegment.encode());
        orcSegment.getOrc1_OrderControl().setValue("RE");
        orcSegment.getOrc3_FillerOrderNumber().getEi1_EntityIdentifier().setValue(testResult.getAccessionNumber());
    }

    private void createOBRSegment(TestResultsXmit testResult) throws HL7Exception {
        String obr25 = "";
        switch (event) {
        case ORDER_RECEIVED_NO_SPEC:
            obr25 = "O";
            break;
        case SPEC_RECEIVED:
            obr25 = "I";
            break;
        case PRELIMINARY_RESULT:
            obr25 = "P";
            break;
        case RESULT:
            obr25 = "A";
            break;
        case FINAL_RESULT:
            obr25 = "F";
            break;
        case CORRECTION:
            obr25 = "C";
            break;
        case TESTING_NOT_DONE:
            obr25 = "X";
        }
        OBR originalObrSegment = originalMessage.getORDER().getOBSERVATION_REQUEST().getOBR();
        OBR obrSegment = hl7Message.getPATIENT_RESULT().getORDER_OBSERVATION().getOBR();
        obrSegment.parse(originalObrSegment.encode());
        obrSegment.getObr25_ResultStatus().setValue(obr25);
    }

    private void createOBXSegment(TestResultsXmit testResults, ResultXmit testResult) throws HL7Exception {
        int obxNum = hl7Message.getPATIENT_RESULT().getORDER_OBSERVATION().getOBSERVATIONReps();
        OBX obxSegment = hl7Message.getPATIENT_RESULT().getORDER_OBSERVATION().getOBSERVATION(obxNum).getOBX();
        obxSegment.getObx1_SetIDOBX().setValue(Integer.toString(obxNum + 1));
        obxSegment.getObx2_ValueType().setValue(testResult.getTypeResult());
        obxSegment.getObx3_ObservationIdentifier().getCe1_Identifier().setValue(testResult.getLoinc());
        obxSegment.getObx3_ObservationIdentifier().getCe2_Text().setValue(testResults.getTest().getText());
        obxSegment.getObx3_ObservationIdentifier().getCe3_NameOfCodingSystem().setValue("LN");
        String typeResult = testResult.getTypeResult();
        if ("CE".equals(typeResult)) {
            String ce = testResult.getResult().getCode() + "^" + testResult.getResult().getText() + "";
            obxSegment.getObx5_ObservationValue(0).getData().parse(ce);
        } else if ("NM".equals(typeResult)) {
            obxSegment.getObx5_ObservationValue(0).getData().parse(testResult.getResult().getText());
            obxSegment.getObx6_Units().getCe1_Identifier().setValue(testResults.getValidRange().getUnits());
        } else {
            obxSegment.getObx5_ObservationValue(0).getData().parse(testResult.getResult().getText());
        }
    }

    private void createNTESegment(TestResultsXmit testResults, ResultXmit testResult) throws HL7Exception {
        NTE nteSegment = hl7Message.getPATIENT_RESULT().getORDER_OBSERVATION().getOBSERVATION().getNTE();
        nteSegment.getComment(0).setValue(testResults.getTestNotes());
    }

    private String generateMessageId() {
        String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
        if (sequenceNum != 0) {
            sequenceNum = ((sequenceNum - 1) % 99999) + 1;
        }
        return timestamp + String.format("%05d", sequenceNum);
    }
}
