import React, { useCallback, useEffect, useRef, useState } from "react";
import { useIntl, FormattedMessage } from "react-intl";
import {
  Grid,
  Column,
  Tile,
  ComboBox,
  TextInput,
  Select,
  SelectItem,
  DatePicker,
  DatePickerInput,
  InlineNotification,
  Modal,
} from "@carbon/react";
import { getFromOpenElisServer } from "../../../utils/Utils";
import Questionnaire from "../../../common/Questionnaire";
import VectorFieldSurveyPanel from "./VectorFieldSurveyPanel";
import MicrobiologyOrderEntrySection from "../../../microbiology/MicrobiologyOrderEntrySection";

/**
 * ProgramSection - Program selection with dynamic additional fields
 *
 * Implements:
 * - ORD-10: Program field typeahead ComboBox
 * - Program-specific additional fields (VL, EID, TB, etc.)
 */

const ProgramSection = ({
  orderData,
  setOrderData,
  samples = [],
  isReadOnly,
}) => {
  const intl = useIntl();
  const componentMounted = useRef(true);
  const questionnaireProgramIdRef = useRef(null);

  const [programs, setPrograms] = useState([]);
  const [programsLoaded, setProgramsLoaded] = useState(false);
  const [questionnaire, setQuestionnaire] = useState(
    orderData?.sampleOrderItems?.questionnaire || null,
  );
  const [pendingProgram, setPendingProgram] = useState(undefined);

  const hasCultureWorkflow = samples.some((sample) =>
    (sample.tests || []).some((test) => test.cultureWorkflowType),
  );
  const microbiologyProgram = programs.find(
    (program) => program.code?.toUpperCase() === "MICROBIOLOGY",
  );
  const currentProgramId = orderData?.sampleOrderItems?.programId;
  const effectiveProgramId =
    hasCultureWorkflow && microbiologyProgram
      ? microbiologyProgram.id
      : currentProgramId;
  const selectedProgram =
    programs.find(
      (program) => String(program.id) === String(effectiveProgramId || ""),
    ) || null;
  const microbiologyProgramSelected =
    selectedProgram?.code?.toUpperCase() === "MICROBIOLOGY";
  const displayedQuestionnaire = microbiologyProgramSelected
    ? null
    : questionnaire;
  const questionnaireResponse =
    orderData?.sampleOrderItems?.additionalQuestions || null;
  const hasMicrobiologyDetail = Object.values(
    orderData?.microbiologyOrderDetail || {},
  ).some((value) => value !== "" && value !== null && value !== false);

  // Convert questionnaire to response format
  const convertQuestionnaireToResponse = (questionnaireData) => {
    if (!questionnaireData || !questionnaireData.item) {
      return null;
    }

    const items = questionnaireData.item.map((currentItem) => ({
      linkId: currentItem.linkId,
      definition: currentItem.definition,
      text: currentItem.text,
      answer: [],
    }));

    return {
      resourceType: "QuestionnaireResponse",
      id: "",
      questionnaire: "Questionnaire/" + questionnaireData.id,
      status: "in-progress",
      item: items,
    };
  };

  // Fetch programs on mount
  useEffect(() => {
    componentMounted.current = true;
    getFromOpenElisServer("/rest/user-programs", (response) => {
      if (componentMounted.current && response) {
        setPrograms(response);
        setProgramsLoaded(true);
      }
    });
    return () => {
      componentMounted.current = false;
    };
  }, []);

  useEffect(() => {
    if (!hasCultureWorkflow || !microbiologyProgram) {
      return;
    }

    if (
      String(orderData?.sampleOrderItems?.programId || "") !==
        String(microbiologyProgram.id) ||
      String(orderData?.sampleOrderItems?.microbiologyProgramId || "") !==
        String(microbiologyProgram.id)
    ) {
      setOrderData((previous) => ({
        ...previous,
        sampleOrderItems: {
          ...previous.sampleOrderItems,
          microbiologyPreviousProgramId:
            previous.sampleOrderItems?.microbiologyPreviousProgramId ??
            previous.sampleOrderItems?.programId ??
            "",
          programId: microbiologyProgram.id,
          microbiologyProgramId: microbiologyProgram.id,
        },
      }));
    }
  }, [
    hasCultureWorkflow,
    microbiologyProgram,
    orderData?.sampleOrderItems?.programId,
    setOrderData,
  ]);

  // Fetch program-specific questionnaire. Saved responses remain canonical in
  // orderData; this component only owns the fetched questionnaire structure.
  const fetchProgramQuestionnaire = useCallback(
    (programId, preserveResponses = false) => {
      if (!programId) {
        return;
      }
      getFromOpenElisServer(
        `/rest/program/${programId}/questionnaire`,
        (response) => {
          if (componentMounted.current && response?.item) {
            setQuestionnaire(response);

            const convertedResponse = preserveResponses
              ? undefined
              : convertQuestionnaireToResponse(response);
            setOrderData((prev) => ({
              ...prev,
              sampleOrderItems: {
                ...prev.sampleOrderItems,
                questionnaire: response,
                ...(preserveResponses
                  ? {}
                  : { additionalQuestions: convertedResponse }),
              },
            }));
          } else if (componentMounted.current) {
            setQuestionnaire(null);
            if (!preserveResponses) {
              setOrderData((prev) => ({
                ...prev,
                sampleOrderItems: {
                  ...prev.sampleOrderItems,
                  questionnaire: null,
                  additionalQuestions: null,
                },
              }));
            }
          }
        },
      );
    },
    [setOrderData],
  );

  useEffect(() => {
    if (
      !programsLoaded ||
      !selectedProgram ||
      selectedProgram.code?.toUpperCase() === "MICROBIOLOGY"
    ) {
      questionnaireProgramIdRef.current = null;
      return;
    }
    if (
      String(questionnaireProgramIdRef.current) === String(selectedProgram.id)
    ) {
      return;
    }

    questionnaireProgramIdRef.current = selectedProgram.id;
    fetchProgramQuestionnaire(
      selectedProgram.id,
      Boolean(orderData?.sampleOrderItems?.additionalQuestions),
    );
  }, [
    fetchProgramQuestionnaire,
    orderData?.sampleOrderItems?.additionalQuestions,
    programsLoaded,
    selectedProgram,
  ]);

  const applyProgramChange = (selectedItem, discardMicrobiologyDetail) => {
    if (selectedItem) {
      setOrderData((prev) => ({
        ...prev,
        sampleOrderItems: {
          ...prev.sampleOrderItems,
          programId: selectedItem.id,
          microbiologyProgramId:
            selectedItem.code?.toUpperCase() === "MICROBIOLOGY"
              ? selectedItem.id
              : undefined,
        },
        ...(discardMicrobiologyDetail
          ? {
              microbiologyOrderDetail: {
                cultureMethodId: "",
                patientOrigin: "",
                numberOfSets: "",
                clinicalHistory: "",
                antibioticExposure: false,
                criticalNotificationPreference: null,
              },
            }
          : {}),
      }));
      if (selectedItem.code?.toUpperCase() === "MICROBIOLOGY") {
        setQuestionnaire(null);
      }
    } else {
      setOrderData((prev) => ({
        ...prev,
        sampleOrderItems: {
          ...prev.sampleOrderItems,
          programId: "",
          questionnaire: null,
          additionalQuestions: null,
          microbiologyProgramId: undefined,
        },
        ...(discardMicrobiologyDetail
          ? {
              microbiologyOrderDetail: {
                cultureMethodId: "",
                patientOrigin: "",
                numberOfSets: "",
                clinicalHistory: "",
                antibioticExposure: false,
                criticalNotificationPreference: null,
              },
            }
          : {}),
      }));
      setQuestionnaire(null);
    }
  };

  // Handle program selection. A typed culture test owns the derived Program,
  // while the manual fallback can be changed after confirming data loss.
  const handleProgramChange = ({ selectedItem }) => {
    const leavesManualMicrobiology =
      microbiologyProgramSelected &&
      !hasCultureWorkflow &&
      selectedItem?.code?.toUpperCase() !== "MICROBIOLOGY";
    if (leavesManualMicrobiology && hasMicrobiologyDetail) {
      setPendingProgram(selectedItem ?? null);
      return;
    }
    applyProgramChange(selectedItem, false);
  };

  const confirmProgramChange = () => {
    applyProgramChange(pendingProgram ?? null, true);
    setPendingProgram(undefined);
  };

  // Get answer for a questionnaire item
  const getAnswer = (linkId) => {
    if (!questionnaireResponse?.item || !questionnaire?.item) {
      return "";
    }

    const responseItem = questionnaireResponse.item.find(
      (item) => item.linkId === linkId,
    );
    const questionnaireItem = questionnaire.item.find(
      (item) => item.linkId === linkId,
    );

    if (!responseItem || !questionnaireItem || !responseItem.answer?.length) {
      return "";
    }

    switch (questionnaireItem.type) {
      case "boolean":
        return responseItem.answer[0]?.valueBoolean ?? "";
      case "decimal":
        return responseItem.answer[0]?.valueDecimal ?? "";
      case "integer":
        return responseItem.answer[0]?.valueInteger ?? "";
      case "date":
        return responseItem.answer[0]?.valueDate ?? "";
      case "time":
        return responseItem.answer[0]?.valueTime ?? "";
      case "string":
      case "text":
        return responseItem.answer[0]?.valueString ?? "";
      case "quantity":
        return responseItem.answer[0]?.valueQuantity ?? "";
      case "choice":
        return responseItem.answer[0]?.valueCoding
          ? responseItem.answer[0].valueCoding.code
          : (responseItem.answer[0]?.valueString ?? "");
      default:
        return "";
    }
  };

  // Handle questionnaire answer change
  const handleAnswerChange = (e) => {
    const { id, value } = e.target;

    if (!questionnaireResponse || !questionnaire) {
      return;
    }

    const updatedQuestionnaireResponse = { ...questionnaireResponse };
    const responseItem = updatedQuestionnaireResponse.item.find(
      (item) => item.linkId === id,
    );
    const questionnaireItem = questionnaire.item.find(
      (item) => item.linkId === id,
    );

    if (!responseItem || !questionnaireItem) {
      return;
    }

    responseItem.answer = [];

    if (value !== "") {
      switch (questionnaireItem.type) {
        case "boolean":
          responseItem.answer.push({ valueBoolean: value });
          break;
        case "decimal":
          responseItem.answer.push({ valueDecimal: value });
          break;
        case "integer":
          responseItem.answer.push({ valueInteger: value });
          break;
        case "date":
          responseItem.answer.push({ valueDate: value });
          break;
        case "time":
          responseItem.answer.push({ valueTime: value });
          break;
        case "string":
        case "text":
          responseItem.answer.push({ valueString: value });
          break;
        case "quantity":
          responseItem.answer.push({ valueQuantity: value });
          break;
        case "choice": {
          // Handle single select and multiselect
          let items = value;
          if (!Array.isArray(items)) {
            items = [{ value: value }];
          }
          for (const item of items) {
            const curValue = item.value || item;
            const option = questionnaireItem.answerOption?.find(
              (opt) => opt?.valueCoding?.code === curValue,
            );
            if (option) {
              responseItem.answer.push({ valueCoding: option.valueCoding });
            } else {
              const stringOption = questionnaireItem.answerOption?.find(
                (opt) => opt.valueString === curValue,
              );
              if (stringOption) {
                responseItem.answer.push({
                  valueString: stringOption.valueString,
                });
              }
            }
          }
          break;
        }
        default:
          break;
      }
    }

    setOrderData((prev) => ({
      ...prev,
      sampleOrderItems: {
        ...prev.sampleOrderItems,
        additionalQuestions: updatedQuestionnaireResponse,
      },
    }));
  };

  // Simple handler for VL-specific hardcoded fields (flat object structure)
  const [vlFields, setVlFields] = useState(
    orderData?.sampleOrderItems?.vlProgramFields || {},
  );

  const handleVLFieldChange = (field, value) => {
    const updatedVlFields = { ...vlFields, [field]: value };
    setVlFields(updatedVlFields);
    setOrderData((prev) => ({
      ...prev,
      sampleOrderItems: {
        ...prev.sampleOrderItems,
        vlProgramFields: updatedVlFields,
      },
    }));
  };

  // VL Program specific fields (example)
  const renderVLProgramFields = () => (
    <div className="program-fields">
      <Grid>
        <Column lg={5} md={4} sm={4}>
          <Select
            id="arvRegimen"
            labelText={intl.formatMessage({
              id: "vl.arvRegimen",
              defaultMessage: "ARV Regimen",
            })}
            value={vlFields.arvRegimen || ""}
            onChange={(e) => handleVLFieldChange("arvRegimen", e.target.value)}
            disabled={isReadOnly}
          >
            <SelectItem
              value=""
              text={intl.formatMessage({
                id: "select.regimen",
                defaultMessage: "Select Regimen...",
              })}
            />
            <SelectItem value="TDF/3TC/DTG" text="TDF/3TC/DTG" />
            <SelectItem value="TDF/3TC/EFV" text="TDF/3TC/EFV" />
            <SelectItem value="AZT/3TC/NVP" text="AZT/3TC/NVP" />
            <SelectItem value="ABC/3TC/DTG" text="ABC/3TC/DTG" />
          </Select>
        </Column>
        <Column lg={5} md={4} sm={4}>
          <TextInput
            id="durationOnARV"
            labelText={intl.formatMessage({
              id: "vl.durationOnARV",
              defaultMessage: "Duration on ARV (months)",
            })}
            placeholder={intl.formatMessage({
              id: "vl.durationOnARV.placeholder",
              defaultMessage: "Enter months on treatment",
            })}
            value={vlFields.durationOnARV || ""}
            onChange={(e) =>
              handleVLFieldChange("durationOnARV", e.target.value)
            }
            disabled={isReadOnly}
          />
        </Column>
        <Column lg={6} md={4} sm={4}>
          <Select
            id="vlIndication"
            labelText={intl.formatMessage({
              id: "vl.indication",
              defaultMessage: "Indication for VL Test",
            })}
            value={vlFields.vlIndication || ""}
            onChange={(e) =>
              handleVLFieldChange("vlIndication", e.target.value)
            }
            disabled={isReadOnly}
          >
            <SelectItem
              value=""
              text={intl.formatMessage({
                id: "select",
                defaultMessage: "Select...",
              })}
            />
            <SelectItem value="routine" text="Routine Monitoring" />
            <SelectItem value="targeted" text="Targeted (Clinical Suspicion)" />
            <SelectItem value="confirmatory" text="Confirmatory" />
          </Select>
        </Column>

        <Column lg={8} md={4} sm={4}>
          <Select
            id="pregnancyStatus"
            labelText={intl.formatMessage({
              id: "vl.pregnancyStatus",
              defaultMessage: "Pregnancy / Breastfeeding Status",
            })}
            value={vlFields.pregnancyStatus || ""}
            onChange={(e) =>
              handleVLFieldChange("pregnancyStatus", e.target.value)
            }
            disabled={isReadOnly}
          >
            <SelectItem
              value=""
              text={intl.formatMessage({
                id: "select",
                defaultMessage: "Select...",
              })}
            />
            <SelectItem value="not_applicable" text="Not Applicable" />
            <SelectItem value="pregnant" text="Pregnant" />
            <SelectItem value="breastfeeding" text="Breastfeeding" />
          </Select>
        </Column>
        <Column lg={8} md={4} sm={4}>
          <DatePicker
            datePickerType="single"
            dateFormat="d/m/Y"
            onChange={(dates) => {
              if (dates && dates[0]) {
                const date = dates[0];
                const formatted = `${date.getDate().toString().padStart(2, "0")}/${(date.getMonth() + 1).toString().padStart(2, "0")}/${date.getFullYear()}`;
                handleVLFieldChange("lastVLDate", formatted);
              }
            }}
          >
            <DatePickerInput
              id="lastVLDate"
              labelText={intl.formatMessage({
                id: "vl.lastVLDate",
                defaultMessage: "Date of Last VL Result",
              })}
              placeholder="dd/mm/yyyy"
              disabled={isReadOnly}
            />
          </DatePicker>
        </Column>

        <Column lg={8} md={4} sm={4}>
          <TextInput
            id="lastVLResult"
            labelText={intl.formatMessage({
              id: "vl.lastVLResult",
              defaultMessage: "Last VL Result (copies/mL)",
            })}
            placeholder={intl.formatMessage({
              id: "vl.lastVLResult.placeholder",
              defaultMessage: "e.g., 150",
            })}
            value={vlFields.lastVLResult || ""}
            onChange={(e) =>
              handleVLFieldChange("lastVLResult", e.target.value)
            }
            disabled={isReadOnly}
          />
        </Column>
      </Grid>
    </div>
  );

  // Check if VL program is selected
  const isVLProgram =
    selectedProgram?.value?.toLowerCase().includes("vl") ||
    selectedProgram?.value?.toLowerCase().includes("viral load");

  // Check if the Vector Field Survey program is selected (custom larval/pupal panel)
  const isVectorFieldSurvey = selectedProgram?.value
    ?.toLowerCase()
    .includes("vector field survey");

  return (
    <Tile className="order-section program-section">
      <Modal
        open={pendingProgram !== undefined}
        modalHeading={intl.formatMessage({
          id: "microbiology.orderEntry.programDiscardHeading",
        })}
        primaryButtonText={intl.formatMessage({
          id: "microbiology.orderEntry.discardConfirm",
        })}
        secondaryButtonText={intl.formatMessage({ id: "button.cancel" })}
        danger
        onRequestSubmit={confirmProgramChange}
        onRequestClose={() => setPendingProgram(undefined)}
      >
        <p>
          {intl.formatMessage({
            id: "microbiology.orderEntry.programDiscardMessage",
          })}
        </p>
      </Modal>
      <h4 className="section-title">
        <FormattedMessage id="label.program" defaultMessage="Program" />
      </h4>

      <Grid>
        <Column lg={8} md={6} sm={4}>
          <ComboBox
            key={selectedProgram?.id || "empty"}
            id="program"
            titleText={intl.formatMessage({
              id: "label.program",
              defaultMessage: "Program",
            })}
            items={programs}
            itemToString={(item) => (item ? item.value : "")}
            selectedItem={selectedProgram}
            onChange={handleProgramChange}
            placeholder={intl.formatMessage({
              id: "program.placeholder",
              defaultMessage: "Type to filter or select from the list",
            })}
            disabled={isReadOnly || hasCultureWorkflow}
          />
          <p className="helper-text">
            {hasCultureWorkflow ? (
              <FormattedMessage id="microbiology.orderEntry.programDerived" />
            ) : (
              <FormattedMessage
                id="program.helper"
                defaultMessage="Type to filter or select from the list. Selecting a program displays its specific Additional Order Information fields below."
              />
            )}
          </p>
        </Column>
      </Grid>

      {hasCultureWorkflow && programsLoaded && !microbiologyProgram && (
        <InlineNotification
          kind="error"
          lowContrast
          hideCloseButton
          title={intl.formatMessage({
            id: "microbiology.orderEntry.programMissingTitle",
          })}
          subtitle={intl.formatMessage({
            id: "microbiology.orderEntry.programMissingMessage",
          })}
        />
      )}

      {(hasCultureWorkflow || microbiologyProgramSelected) && (
        <MicrobiologyOrderEntrySection
          samples={samples}
          orderFormValues={orderData}
          setOrderFormValues={setOrderData}
          enabled={hasCultureWorkflow || microbiologyProgramSelected}
          isReadOnly={isReadOnly}
        />
      )}

      {/* Additional Order Information - Program Specific */}
      {selectedProgram && (
        <div className="additional-order-info">
          <h5 className="subsection-title">
            <FormattedMessage
              id="order.additionalInfo"
              defaultMessage="Additional Order Information"
            />
            {" — "}
            {selectedProgram.value}
          </h5>
          <p className="helper-text">
            <FormattedMessage
              id="order.additionalInfo.helper"
              defaultMessage="These fields are specific to the selected program and provide additional context needed for this workflow."
            />
          </p>

          {/* Render program-specific fields or fall back to the generic Questionnaire */}
          {isVLProgram ? (
            renderVLProgramFields()
          ) : isVectorFieldSurvey && displayedQuestionnaire ? (
            <VectorFieldSurveyPanel
              questionnaire={displayedQuestionnaire}
              getAnswer={getAnswer}
              onAnswerChange={handleAnswerChange}
              isReadOnly={isReadOnly}
            />
          ) : displayedQuestionnaire ? (
            <Questionnaire
              questionnaire={displayedQuestionnaire}
              onAnswerChange={handleAnswerChange}
              getAnswer={getAnswer}
            />
          ) : null}
        </div>
      )}
    </Tile>
  );
};

export default ProgramSection;
