import React, { useEffect, useRef, useState } from "react";
import {
  Select,
  SelectItem,
  Column,
  Grid,
  InlineNotification,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import "../../index.css";
import "../Style.css";
import { getFromOpenElisServer } from "../utils/Utils";
import Questionnaire from "../common/Questionnaire";

/**
 * The default program is matched on its code, not its display name: a site that
 * renames or translates "Routine Testing" keeps its default either way.
 */
const ROUTINE_PROGRAM_CODE = "ROUTINE";

/**
 * Maps the order form's one-letter domain code onto the catalog domain the
 * program picker filters by (OGC-781 FR-6). The clinical patient flow leaves
 * the code unset.
 */
export const programDomainForOrder = (orderFormValues) => {
  const raw = orderFormValues?.sampleOrderItems?.domain;
  if (raw === "E") return "ENVIRONMENTAL";
  if (raw === "V") return "VECTOR";
  return "CLINICAL";
};

export const ProgramSelect = ({
  programChange = () => {
    console.debug("default programChange function does nothing");
  },
  orderFormValues,
  editable,
  domain,
}) => {
  const componentMounted = useRef(false);

  const intl = useIntl();

  const [programs, setPrograms] = useState([]);
  const [programsLoaded, setProgramsLoaded] = useState(false);
  const appendedProgramIdRef = useRef(null);
  const currentProgramId = orderFormValues?.sampleOrderItems?.programId;

  const fetchPrograms = (programsList) => {
    if (componentMounted.current) {
      // Anything but a list leaves the select empty rather than letting a
      // later find() throw and take the whole order page down with it.
      setPrograms(Array.isArray(programsList) ? programsList : []);
      setProgramsLoaded(true);
    }
  };

  // An existing order may name a program the picker no longer offers (it was
  // deactivated or belongs to another domain); keep showing it.
  useEffect(() => {
    if (!programsLoaded || !currentProgramId) {
      return;
    }
    const known = programs.some(
      (program) => String(program.id) === String(currentProgramId),
    );
    if (known || appendedProgramIdRef.current === String(currentProgramId)) {
      return;
    }
    appendedProgramIdRef.current = String(currentProgramId);
    getFromOpenElisServer(`/rest/program/${currentProgramId}`, (response) => {
      if (!componentMounted.current || !response?.program?.programName) {
        return;
      }
      setPrograms((previous) =>
        previous.some(
          (program) => String(program.id) === String(currentProgramId),
        )
          ? previous
          : [
              ...previous,
              {
                id: String(response.program.id || currentProgramId),
                value: response.program.programName,
                code: response.program.code,
              },
            ],
      );
    });
  }, [programsLoaded, programs, currentProgramId]);

  useEffect(() => {
    if (!orderFormValues?.sampleOrderItems?.programId) {
      programChange({
        target: {
          value: programs.find((program) => {
            return program.code?.toUpperCase() === ROUTINE_PROGRAM_CODE;
          })?.id,
        },
      });
    }
  }, [programs]);

  useEffect(() => {
    componentMounted.current = true;
    const url = domain
      ? `/rest/user-programs?domain=${encodeURIComponent(domain)}`
      : "/rest/user-programs";
    getFromOpenElisServer(url, fetchPrograms);
    return () => {
      componentMounted.current = false;
    };
  }, [domain]);

  return (
    <>
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          {programsLoaded && programs.length === 0 && (
            <InlineNotification
              kind="info"
              lowContrast
              hideCloseButton
              title={
                domain
                  ? intl.formatMessage(
                      { id: "orderEntry.programPicker.empty.domain" },
                      {
                        domain: intl.formatMessage({
                          id: `label.domain.${domain}`,
                        }),
                      },
                    )
                  : intl.formatMessage({ id: "orderEntry.programPicker.empty" })
              }
            />
          )}
          {programs.length > 0 && (
            <Select
              id="additionalQuestionsSelect"
              labelText={intl.formatMessage({ id: "label.program" })}
              onChange={programChange}
              value={orderFormValues?.sampleOrderItems?.programId}
              disabled={editable ? editable : false}
            >
              <SelectItem value="" text="" />
              {programs.map((program) => {
                return (
                  <SelectItem
                    key={program.id}
                    value={program.id}
                    text={program.value}
                  />
                );
              })}
            </Select>
          )}
        </Column>
      </Grid>
    </>
  );
};

const OrderEntryAdditionalQuestions = ({
  orderFormValues,
  setOrderFormValues = () => {
    console.debug("default setOrderFormValues change function does nothing");
  },
}) => {
  const [questionnaire, setQuestionnaire] = useState(
    orderFormValues?.sampleOrderItems?.questionnaire,
  );
  const [questionnaireResponse, setQuestionnaireResponse] = useState(
    orderFormValues?.sampleOrderItems?.additionalQuestions,
  );

  const handleProgramSelection = (event) => {
    const newProgramId = event?.target?.value || "";

    if (!newProgramId) {
      setQuestionnaire(null);
      setQuestionnaireResponse(null);
      setOrderFormValues((prev) => ({
        ...prev,
        sampleOrderItems: {
          ...prev.sampleOrderItems,
          programId: "",
          questionnaire: null,
          additionalQuestions: null,
        },
      }));
    } else {
      setOrderFormValues((prev) => ({
        ...prev,
        sampleOrderItems: {
          ...prev.sampleOrderItems,
          programId: newProgramId,
        },
      }));
      getFromOpenElisServer(
        "/rest/program/" + newProgramId + "/questionnaire",
        (res) => setAdditionalQuestions(res, event),
      );
    }
  };

  function convertQuestionnaireToResponse(questionnaire) {
    var items = [];
    if (questionnaire && "item" in questionnaire) {
      for (let i = 0; i < questionnaire.item.length; i++) {
        let currentItem = questionnaire.item[i];
        items.push({
          linkId: currentItem.linkId,
          definition: currentItem.definition,
          text: currentItem.text,
          answer: [],
        });
      }

      var convertedQuestionnaireResponse = {
        resourceType: "QuestionnaireResponse",
        id: "",
        questionnaire: "Questionnaire/" + questionnaire.id,
        status: "in-progress",
        item: items,
      };
      return convertedQuestionnaireResponse;
    }
    return null;
  }

  function setAdditionalQuestions(res, event) {
    console.debug(res);
    if (res && "item" in res) {
      setQuestionnaire(res);
      var convertedQuestionnaireResponse = convertQuestionnaireToResponse(res);
      setQuestionnaireResponse(convertedQuestionnaireResponse);

      setOrderFormValues((prev) => ({
        ...prev,
        sampleOrderItems: {
          ...prev.sampleOrderItems,
          questionnaire: res,
          programId: event
            ? event.target.value
            : prev.sampleOrderItems.programId,
          additionalQuestions: convertedQuestionnaireResponse,
        },
      }));
    } else {
      setQuestionnaire(null);
      setQuestionnaireResponse(null);
      setOrderFormValues((prev) => ({
        ...prev,
        sampleOrderItems: {
          ...prev.sampleOrderItems,
          questionnaire: null,
          additionalQuestions: null,
        },
      }));
    }
  }

  const getAnswer = (linkId) => {
    var responseItem = questionnaireResponse?.item?.find(
      (item) => item.linkId === linkId,
    );
    var questionnaireItem = questionnaire?.item?.find(
      (item) => item.linkId === linkId,
    );
    switch (questionnaireItem.type) {
      case "boolean":
        return responseItem?.answer
          ? responseItem?.answer[0]?.valueBoolean
          : "";
      case "decimal":
        return responseItem?.answer
          ? responseItem?.answer[0]?.valueDecimal
          : "";
      case "integer":
        return responseItem?.answer
          ? responseItem?.answer[0]?.valueInteger
          : "";
      case "date":
        return responseItem?.answer ? responseItem?.answer[0]?.valueDate : "";
      case "time":
        return responseItem?.answer ? responseItem?.answer[0]?.valueTime : "";
      case "string":
      case "text":
        return responseItem?.answer ? responseItem?.answer[0]?.valueString : "";
      case "quantity":
        return responseItem?.answer
          ? responseItem?.answer[0]?.valueQuantity
          : "";
      case "choice":
        if (responseItem?.answer) {
          return responseItem?.answer[0]?.valueCoding
            ? responseItem?.answer[0]?.valueCoding.code
            : responseItem?.answer[0]?.valueString;
        }
    }
  };

  const answerChange = (e) => {
    const { id, value } = e.target;

    var updatedQuestionnaireResponse = { ...questionnaireResponse };
    var responseItem = updatedQuestionnaireResponse.item.find(
      (item) => item.linkId === id,
    );
    var questionnaireItem = questionnaire.item.find(
      (item) => item.linkId === id,
    );
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
        case "choice":
          //make single select and multiselect have the same shape to reuse code
          var items = value;
          if (!Array.isArray(items)) {
            items = [{ value: value }];
          }
          for (var i = 0; i < items.length; i++) {
            var curValue = items[i].value;
            var option = questionnaireItem?.answerOption?.find(
              (option) => option?.valueCoding?.code === curValue,
            );
            if (option) {
              responseItem.answer.push({ valueCoding: option.valueCoding });
            } else {
              option = questionnaireItem?.answerOption?.find(
                (option) => option.valueString === curValue,
              );
              if (option) {
                responseItem.answer.push({ valueString: option.valueString });
              } else {
                console.error(
                  "couldn't find a matching questionnaire answer for '" +
                    curValue +
                    "'",
                );
              }
            }
          }
          break;
      }
    }
    setQuestionnaireResponse(updatedQuestionnaireResponse);
    setOrderFormValues({
      ...orderFormValues,
      sampleOrderItems: {
        ...orderFormValues.sampleOrderItems,
        additionalQuestions: updatedQuestionnaireResponse,
      },
    });
  };

  return (
    <>
      <Grid fullWidth={true}>
        <Column lg={16} md={8} sm={4}>
          <div className="orderLegendBody">
            <h3>
              <FormattedMessage id="select.program" />
            </h3>
            <ProgramSelect
              programChange={handleProgramSelection}
              orderFormValues={orderFormValues}
              domain={programDomainForOrder(orderFormValues)}
            />
            <Questionnaire
              questionnaire={questionnaire}
              onAnswerChange={answerChange}
              getAnswer={getAnswer}
            />
            {questionnaireResponse && (
              <input
                type="hidden"
                name="additionalQuestions"
                value={questionnaireResponse}
              />
            )}
          </div>
        </Column>
      </Grid>
    </>
  );
};

export default OrderEntryAdditionalQuestions;
