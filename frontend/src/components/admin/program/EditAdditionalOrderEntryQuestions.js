import React, { useState, useEffect } from "react";
import {
  Form,
  FormLabel,
  Heading,
  TextArea,
  TextInput,
  Select,
  SelectItem,
  Button,
  Loading,
  Grid,
  Column,
  Section,
  Toggle,
} from "@carbon/react";
import { TrashCan, Save, AddAlt, QX } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import Questionnaire from "../../common/Questionnaire";

function EditAdditionalOrderEntryQuestions({
  additionalOrderEntryQuestions,
  handleFieldChange,
}) {
  const intl = useIntl();

  const [showJson, setShowJson] = useState(true);
  const [
    additionalOrderEntryQuestionsAreJson,
    setAdditionalOrderEntryQuestionsAreJson,
  ] = useState(false);

  const [
    additionalOrderEntryQuestionsJsonFormat,
    setAdditionalOrderEntryQuestionsJsonFormat,
  ] = useState(null);

  function isJson(item) {
    let value = typeof item !== "string" ? JSON.stringify(item) : item;
    try {
      value = JSON.parse(value);
    } catch (e) {
      return false;
    }

    return typeof value === "object" && value !== null;
  }

  useEffect(() => {
    setAdditionalOrderEntryQuestionsAreJson(
      isJson(additionalOrderEntryQuestions),
    );

    setAdditionalOrderEntryQuestionsJsonFormat(
      isJson(additionalOrderEntryQuestions)
        ? JSON.parse(additionalOrderEntryQuestions)
        : null,
    );
  }, [additionalOrderEntryQuestions]);

  const QuestionBuilder = ({ item }) => {
    const [itemText, setitemText] = useState(item?.text || "");
    const [itemType, setItemType] = useState(
      item?.type === "choice" && item?.repeats === true
        ? "choice-repeat"
        : item?.type || "",
    );
    const [options, setOptions] = useState(item?.answerOption || null);
    const [addingOption, setAddingOption] = useState(false);
    const [newOption, setNewOption] = useState("");

    const handleQuestionSave = (e) => {
      e.preventDefault();

      const updatedOptions = options ? [...options] : [];
      if (newOption && newOption.trim() !== "") {
        if (updatedOptions.length === 0) {
          updatedOptions.push({ valueString: newOption });
        } else if (!("valueCoding" in updatedOptions[0])) {
          updatedOptions.push({ valueString: newOption });
        }
        setOptions(updatedOptions);
        setNewOption("");
      }

      const newItem = {
        ...item,
        text: itemText,
        type: itemType === "choice-repeat" ? "choice" : itemType,
        answerOption: updatedOptions,
      };

      if (itemType === "choice") {
        newItem.repeats = false;
      } else if (itemType === "choice-repeat") {
        newItem.repeats = true;
      }

      if (itemType !== "choice" && itemType !== "choice-repeat") {
        delete newItem.answerOption;
      }

      const itemIndex = additionalOrderEntryQuestionsJsonFormat.item.findIndex(
        (i) => i.linkId === item.linkId,
      );

      if (itemIndex !== -1) {
        additionalOrderEntryQuestionsJsonFormat.item[itemIndex] = newItem; // Update the item in the JSON format
      }
      handleFieldChange({
        target: {
          name: "additionalOrderEntryQuestions",
          value: JSON.stringify(
            additionalOrderEntryQuestionsJsonFormat,
            null,
            4,
          ),
        },
      }); // Trigger field change
    };

    const handleQuestionDelete = (e) => {
      e.preventDefault();
      const itemIndex = additionalOrderEntryQuestionsJsonFormat.item.findIndex(
        (i) => i.linkId === item.linkId,
      );
      if (itemIndex !== -1) {
        additionalOrderEntryQuestionsJsonFormat.item.splice(itemIndex, 1); // Remove the item from the JSON format
      }
      handleFieldChange({
        target: {
          name: "additionalOrderEntryQuestions",
          value: JSON.stringify(
            additionalOrderEntryQuestionsJsonFormat,
            null,
            4,
          ),
        },
      }); // Trigger field change
    };

    const deleteOptionHandler = (index) => (e) => {
      e.preventDefault();
      const updatedOptions = options.filter((_, i) => i !== index);
      setOptions(updatedOptions);
    };

    const addOptionHandler = (e) => {
      e.preventDefault();
      setAddingOption(true);
      if (newOption && newOption.trim() !== "") {
        const updatedOptions = options ? [...options] : [];

        if (updatedOptions.length === 0) {
          updatedOptions.push({ valueString: newOption });
        } else if (!("valueCoding" in updatedOptions[0])) {
          updatedOptions.push({ valueString: newOption });
        }

        setOptions(updatedOptions);
        setNewOption("");
      }
    };

    return (
      <Grid
        style={{
          margin: "10px 0",
          backgroundColor: "#f1f1f1",
          padding: "10px",
          borderRadius: "5px",
        }}
      >
        <Column lg={4} md={2} sm={1}>
          <TextInput
            type="text"
            name="itemText"
            id="itemText"
            labelText="Question Text"
            placeholder="Question Text"
            value={itemText}
            onChange={(e) => setitemText(e.target.value)}
            invalid={itemText === ""}
            invalidText={intl.formatMessage({ id: "question.text.required" })}
          />
        </Column>
        <Column lg={4} md={2} sm={1}>
          <Select
            id="questionType"
            name="questionType"
            labelText="Question Type"
            value={
              itemType === "choice"
                ? item.repeats === true
                  ? "choice-repeat"
                  : "choice"
                : itemType
            }
            onChange={(e) => setItemType(e.target.value)}
          >
            <SelectItem value="boolean" text="Boolean" />
            <SelectItem value="choice" text="Choice" />
            <SelectItem value="choice-repeat" text="Checkbox" />
            <SelectItem value="integer" text="Integer" />
            <SelectItem value="decimal" text="Decimal" />
            <SelectItem value="date" text="Date" />
            <SelectItem value="time" text="Time" />
            <SelectItem value="string" text="String" />
            <SelectItem value="text" text="Text" />
            <SelectItem value="quantity" text="Quantity" />
          </Select>
        </Column>
        <Column lg={8} md={4} sm={2}>
          <br></br>
        </Column>
        <Column lg={4} md={2} sm={1}>
          {itemType === "boolean" && (
            <div>
              <FormLabel>Options</FormLabel>
              <div
                style={{
                  display: "flex",
                  flexDirection: "column",
                  gap: "10px",
                  alignItems: "flex-start",
                }}
              >
                <span>Yes</span>
                <span>No</span>
              </div>
            </div>
          )}
          {(itemType === "choice" || itemType === "choice-repeat") && (
            <div>
              <FormLabel>Options</FormLabel>
              <br></br>
              <div
                style={{
                  display: "flex",
                  flexDirection: "column",
                  gap: "10px",
                  alignItems: "flex-start",
                }}
              >
                {options?.map((option, index) => (
                  <>
                    {"valueString" in option && (
                      <span key={index}>
                        {option.valueString}
                        <TrashCan onClick={deleteOptionHandler(index)} />
                      </span>
                    )}
                    {"valueCoding" in option && (
                      <span key={index}>
                        {option.valueCoding.display}
                        <TrashCan onClick={deleteOptionHandler(index)} />
                      </span>
                    )}
                  </>
                ))}
                <span>
                  {addingOption && (
                    <TextInput
                      type="text"
                      name="newOption"
                      id="newOption"
                      labelText=""
                      placeholder="New Option"
                      value={newOption}
                      onChange={(e) => setNewOption(e.target.value)}
                    />
                  )}
                  <AddAlt onClick={addOptionHandler} />
                </span>
              </div>
            </div>
          )}
          {itemType === "integer" && (
            <div>
              <div
                style={{
                  display: "flex",
                  flexDirection: "column",
                  gap: "10px",
                  alignItems: "flex-start",
                }}
              >
                <span>number</span>
              </div>
            </div>
          )}
          {itemType === "decimal" && (
            <div>
              <div
                style={{
                  display: "flex",
                  flexDirection: "column",
                  gap: "10px",
                  alignItems: "flex-start",
                }}
              >
                <span>decimal number</span>
              </div>
            </div>
          )}
          {itemType === "date" && (
            <div>
              <div
                style={{
                  display: "flex",
                  flexDirection: "column",
                  gap: "10px",
                  alignItems: "flex-start",
                }}
              >
                <span>dd/mm/yyyy</span>
              </div>
            </div>
          )}
          {itemType === "time" && (
            <div>
              <div
                style={{
                  display: "flex",
                  flexDirection: "column",
                  gap: "10px",
                  alignItems: "flex-start",
                }}
              >
                <span>hh:mm</span>
              </div>
            </div>
          )}
          {itemType === "string" && (
            <div>
              <div
                style={{
                  display: "flex",
                  flexDirection: "column",
                  gap: "10px",
                  alignItems: "flex-start",
                }}
              >
                <span>string value</span>
              </div>
            </div>
          )}
          {itemType === "text" && (
            <div>
              <div
                style={{
                  display: "flex",
                  flexDirection: "column",
                  gap: "10px",
                  alignItems: "flex-start",
                }}
              >
                <span>text value</span>
              </div>
            </div>
          )}
          {itemType === "quantity" && (
            <div>
              <div
                style={{
                  display: "flex",
                  flexDirection: "column",
                  gap: "10px",
                  alignItems: "flex-start",
                }}
              >
                <span>quantity value</span>
              </div>
            </div>
          )}
        </Column>
        <Column lg={8} md={4} sm={2}>
          <br></br>
        </Column>
        <Column
          lg={8}
          md={4}
          sm={2}
          style={{ display: "flex", gap: "10px", alignItems: "center" }}
        >
          <Button onClick={handleQuestionSave}>
            <Save />
          </Button>
          <Button onClick={handleQuestionDelete}>
            <TrashCan />
          </Button>
        </Column>
        <Column lg={8} md={4} sm={2}>
          <br></br>
        </Column>
      </Grid>
    );
  };

  return (
    <>
      <Column lg={8} md={4} sm={2}>
        <Grid>
          <Column lg={4} md={2} sm={1}>
            <FormLabel>
              <FormattedMessage id="Questionnaire" />
            </FormLabel>
          </Column>
          <Column lg={4} md={2} sm={1}>
            <Toggle
              labelText="Edit Json"
              id="setShowJson"
              aria-label="toggle button"
              toggled={showJson}
              onToggle={() => {
                setShowJson(!showJson);
              }}
            />
          </Column>
        </Grid>
        {showJson ? (
          <TextArea
            name="additionalOrderEntryQuestions"
            id="additionalOrderEntryQuestions"
            labelText=""
            value={additionalOrderEntryQuestions || ""}
            onChange={handleFieldChange}
            invalid={
              !additionalOrderEntryQuestionsAreJson &&
              additionalOrderEntryQuestions !== ""
            }
            invalidText={intl.formatMessage({ id: "invalid.json" })}
          />
        ) : (
          <Grid>
            {/* UI buildler to edit additionalOrderEntryQuestions */}
            <Column lg={8} md={4} sm={2}>
              <TextInput
                type="text"
                name="questionnaireUUID"
                id="questionnaireUUID"
                labelText="Questionnaire id"
                value={additionalOrderEntryQuestionsJsonFormat?.id || ""}
                disabled={true}
              />
            </Column>
            <Column lg={8} md={4} sm={2}>
              {additionalOrderEntryQuestionsAreJson && (
                <FormLabel>
                  <FormattedMessage id="Questions" />
                </FormLabel>
              )}
            </Column>
            <Column
              lg={8}
              md={4}
              sm={2}
              style={{ overflowY: "scroll", maxHeight: "500px" }}
            >
              {additionalOrderEntryQuestionsJsonFormat?.item?.map(
                (item, index) => (
                  <QuestionBuilder key={index} item={item} />
                ),
              )}
            </Column>
            <Column lg={8} md={4} sm={2}>
              <br></br>
            </Column>
            <Column>
              <Button
                lg={4}
                md={4}
                sm={2}
                onClick={() => {
                  const newField = {
                    linkId: "",
                    text: "New Field",
                    type: "string",
                  };
                  newField.linkId = crypto.randomUUID();
                  setAdditionalOrderEntryQuestionsJsonFormat((prev) => ({
                    ...prev,
                    item: [...(prev.item || []), newField],
                  }));
                }}
              >
                <FormattedMessage id="Add New Question" />
              </Button>
            </Column>
          </Grid>
        )}
      </Column>
      <Column lg={8} md={4} sm={2}>
        {additionalOrderEntryQuestionsAreJson && (
          <div>
            <FormLabel>
              <FormattedMessage id="example" />
            </FormLabel>
            <div className="exampleDiv">
              <Questionnaire
                questionnaire={JSON.parse(additionalOrderEntryQuestions)}
              />
            </div>
          </div>
        )}
      </Column>
    </>
  );
}

export default EditAdditionalOrderEntryQuestions;
