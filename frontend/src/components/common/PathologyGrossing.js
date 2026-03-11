import React, { useRef , useEffect} from "react";
import {
    Grid,
    Column,
    TextArea,
    Select,
    SelectItem,
    Button,
    TextInput,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
// import Quill from "quill";
// import "quill/dist/quill.snow.css";

const PathologyGrossing = ({
    intl: intlProp,
    grossing,
    onGrossingChange,
  statusId = "status",
    statusName = "status",
    statusValue = "",
    onStatusChange = () => {},
    technicianId = "assignedTechnician",
    technicianName = "assignedTechnician",
    technicianValue = "",
    onTechnicianChange = () => {},
    specimenReceivedDate = "",
    onSpecimenReceivedDateChange = () => {},
    specimenCondition = "",
    onSpecimenConditionChange = () => {},
}) => {
    const intl = useIntl();
    const grossDescriptionRef = useRef(null);

    const statusOptions = [
        { id: "GROSSING", value: "Grossing" },
        { id: "PROCESSING", value: "Processing" },
        { id: "CUTTING", value: "Cutting" },
        { id: "SLICING", value: "Slicing" },
        { id: "STAINING", value: "Staining" },
        { id: "READY_PATHOLOGIST", value: "Ready for Pathologist" },
    ];

    const technicianOptions = [
        { id: "ELIS", value: "ELIS" },
        { id: "OPEN", value: "Open" },
        { id: "TECH_LAB", value: "Tech Lab" },
    ];

    const specimenConditionOptions = [
        { id: "ADEQUATE", value: "Adequate" },
        { id: "COMPROMISED", value: "Compromised" },
        { id: "INSUFFICIENT", value: "Insufficient" },
    ];

    const quickInsertOptions = [
        {
            id: "macro1",
            value:
                "Specimen received in formalin. Gross examination shows preserved architecture.",
        },
        {
            id: "macro2",
            value: "No obvious necrosis identified on gross examination.",
        },
        {
            id: "macro3",
            value: "Tissue fragments submitted for routine processing and staining.",
        },
    ];

    const applyFormat = (prefix, suffix = prefix) => {
        const textArea = grossDescriptionRef.current;
        const currentText = grossing || "";

        if (!textArea) {
            onGrossingChange(`${currentText}${prefix}${suffix}`);
            return;
        }

        const start = textArea.selectionStart;
        const end = textArea.selectionEnd;
        const selectedText = currentText.slice(start, end);
        const newText =
            currentText.slice(0, start) +
            `${prefix}${selectedText}${suffix}` +
            currentText.slice(end);

        onGrossingChange(newText);
    };

    const appendMacro = (macroText) => {
        const currentText = grossing || "";
        const separator = currentText.trim().length > 0 ? "\n" : "";
        onGrossingChange(`${currentText}${separator}${macroText}`);
    };

    const editorRef = useRef(null);

    return (
        <Grid fullWidth={true} className="gridBoundary">
            <Column lg={8} md={4} sm={4}>
                <Select
                    id={statusId}
                    name={statusName}
                    labelText={intl.formatMessage({ id: "label.button.select.status" })}
                    value={statusValue}
                    onChange={(event) => onStatusChange(event.target.value)}
                >
                    <SelectItem
                        value=""
                        text="Select or leave for queue"
                    />
                    {statusOptions.map((status) => (
                        <SelectItem key={status.id} text={status.value} value={status.id} />
                    ))}
                </Select>
            </Column>

            <Column lg={8} md={4} sm={4}>
                <Select
                    id={technicianId}
                    name={technicianName}
                    labelText={intl.formatMessage({ id: "label.button.select.technician" })}
                    value={technicianValue}
                    onChange={(event) => onTechnicianChange(event.target.value)}
                >
                    <SelectItem
                        value=""
                        text="Select or leave for queue"
                    />
                    {technicianOptions.map((user) => (
                        <SelectItem key={user.id} text={user.value} value={user.id} />
                    ))}
                </Select>
            </Column>

            <Column lg={8} md={4} sm={4}>
                <TextInput
                    id="specimenReceivedDate"
                    type="date"
                    labelText="Specimen Received Date"
                    value={specimenReceivedDate}
                    onChange={(event) => onSpecimenReceivedDateChange(event.target.value)}
                />
            </Column>

            <Column lg={8} md={4} sm={4}>
                <Select
                    id="specimenCondition"
                    name="specimenCondition"
                    labelText="Specimen Condition"
                    value={specimenCondition}
                    onChange={(event) => onSpecimenConditionChange(event.target.value)}
                >
                    <SelectItem value="" text="Select specimen condition" />
                    {specimenConditionOptions.map((condition) => (
                      <SelectItem
                        key={condition.id}
                        text={condition.value}
                        value={condition.id}
                      />
                    ))}
                </Select>
            </Column>

            <Column lg={16} md={8} sm={4}>
                      <div style={{ marginBottom: "0.5rem" }}>
    <Button size="sm" kind="ghost" onClick={() => applyFormat("bold")}>
      Bold
    </Button>
    <Button size="sm" kind="ghost" onClick={() => applyFormat("italic")}>
      Italic
    </Button>
    <Button size="sm" kind="ghost" onClick={() => applyFormat("underline")}>
      Underline
    </Button>
  </div>
            </Column>


  <div
    ref={editorRef}
    contentEditable
    suppressContentEditableWarning
    onInput={(e) => onGrossingChange(e.currentTarget.innerHTML)}
    style={{
      minHeight: "220px",
      border: "1px solid #8d8d8d",
      padding: "1rem",
      background: "white",
    }}
  />
        </Grid>
    );
};

export default PathologyGrossing;
