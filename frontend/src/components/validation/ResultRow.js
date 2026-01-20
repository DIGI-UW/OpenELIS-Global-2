import React, { useState } from "react";
import {
  Button,
  TextArea,
  TextInput,
  Select,
  SelectItem,
  Tabs,
  TabList,
  Tab,
  TabPanels,
  TabPanel,
  Tag,
  InlineNotification,
} from "@carbon/react";
import { Locked, Unlocked, Renew, CheckmarkOutline } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import ResultDetailsPanel from "./ResultDetailsPanel";

const ResultRow = ({ result }) => {
  const intl = useIntl();
  const [isUnlocked, setIsUnlocked] = useState(false);
  const [editedResult, setEditedResult] = useState(result.result);
  const [editedNote, setEditedNote] = useState(result.note || "");
  const [selectedInterpretation, setSelectedInterpretation] = useState("");

  const patientInfo = result.patientInfoObject || {};
  const enteredBy = result.enteredByObject || {};

  return (
    <div
      className="result-row-expanded"
      style={{ padding: "1rem", backgroundColor: "#f4f4f4" }}
    >
      <div
        className="patient-banner"
        style={{
          marginBottom: "1rem",
          padding: "1rem",
          backgroundColor: "#e0e0e0",
          borderRadius: "4px",
        }}
      >
        <div
          style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
          }}
        >
          <div>
            <strong>
              <FormattedMessage id="validation.row.patient.name" />:
            </strong>{" "}
            {patientInfo.name || result.patientName}
          </div>
          <div>
            <strong>
              <FormattedMessage id="validation.row.patient.id" />:
            </strong>{" "}
            {patientInfo.id}
          </div>
          <div>
            <strong>
              <FormattedMessage id="validation.row.patient.dob" />:
            </strong>{" "}
            {patientInfo.dob}
          </div>
          <div>
            <strong>
              <FormattedMessage id="validation.row.patient.sex" />:
            </strong>{" "}
            {patientInfo.sex}
          </div>
          <div>
            <strong>
              <FormattedMessage id="validation.row.patient.age" />:
            </strong>{" "}
            {patientInfo.age}
          </div>
        </div>
      </div>

      <div
        className="entry-banner"
        style={{
          marginBottom: "1rem",
          padding: "1rem",
          backgroundColor: "#ffffff",
          border: "1px solid #e0e0e0",
          borderRadius: "4px",
        }}
      >
        <div
          style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
          }}
        >
          <div>
            <strong>
              <FormattedMessage id="validation.row.entry.by" />:
            </strong>{" "}
            {enteredBy.name}
          </div>
          <div>
            <strong>
              <FormattedMessage id="validation.row.entry.date" />:
            </strong>{" "}
            {enteredBy.date}
          </div>
          <div>
            <strong>
              <FormattedMessage id="validation.row.method" />:
            </strong>{" "}
            <Tag type={result.isManual ? "blue" : "cyan"}>
              {result.method || (result.isManual ? "Manual" : "Analyzer")}
            </Tag>
          </div>
          <Button
            kind={isUnlocked ? "danger--tertiary" : "tertiary"}
            size="sm"
            renderIcon={isUnlocked ? Unlocked : Locked}
            onClick={() => setIsUnlocked(!isUnlocked)}
          >
            {isUnlocked ? (
              <FormattedMessage id="validation.row.locked" />
            ) : (
              <FormattedMessage id="validation.row.unlock" />
            )}
          </Button>
        </div>
      </div>

      <div
        className="result-value-section"
        style={{
          marginBottom: "1rem",
          padding: "1rem",
          backgroundColor: "#ffffff",
          border: "1px solid #e0e0e0",
          borderRadius: "4px",
        }}
      >
        <div
          style={{
            display: "grid",
            gridTemplateColumns: "1fr 1fr 1fr",
            gap: "1rem",
          }}
        >
          <TextInput
            id={`result-value-${result.analysisId}`}
            labelText={intl.formatMessage({
              id: "validation.row.result.value",
            })}
            value={editedResult}
            onChange={(e) => setEditedResult(e.target.value)}
            disabled={!isUnlocked}
          />
          <TextInput
            id={`normal-range-${result.analysisId}`}
            labelText={intl.formatMessage({ id: "validation.row.normalrange" })}
            value={result.normalRange}
            disabled
          />
          <Select
            id={`interpretation-${result.analysisId}`}
            labelText={intl.formatMessage({
              id: "validation.row.interpretation",
            })}
            value={selectedInterpretation}
            onChange={(e) => setSelectedInterpretation(e.target.value)}
            disabled={!isUnlocked}
          >
            <SelectItem
              value=""
              text={intl.formatMessage({
                id: "validation.row.interpretation.none",
              })}
            />
            <SelectItem
              value="normal"
              text={intl.formatMessage({
                id: "validation.row.interpretation.normal",
              })}
            />
            <SelectItem
              value="abnormal"
              text={intl.formatMessage({
                id: "validation.row.interpretation.abnormal",
              })}
            />
            <SelectItem
              value="critical"
              text={intl.formatMessage({
                id: "validation.row.interpretation.critical",
              })}
            />
          </Select>
        </div>

        {result.flags && result.flags.length > 0 && (
          <div style={{ marginTop: "1rem" }}>
            <strong>
              <FormattedMessage id="validation.row.flags" />:
            </strong>{" "}
            {result.flags.map((flag, idx) => (
              <Tag
                key={idx}
                type={
                  flag === "critical"
                    ? "red"
                    : flag === "above-normal"
                      ? "red"
                      : flag === "below-normal"
                        ? "blue"
                        : "purple"
                }
              >
                {flag}
              </Tag>
            ))}
          </div>
        )}
      </div>

      <div className="notes-section" style={{ marginBottom: "1rem" }}>
        <TextArea
          id={`notes-${result.analysisId}`}
          labelText={intl.formatMessage({ id: "validation.row.notes" })}
          placeholder={intl.formatMessage({
            id: "validation.row.notes.placeholder",
          })}
          value={editedNote}
          onChange={(e) => setEditedNote(e.target.value)}
          rows={3}
          disabled={!isUnlocked}
        />

        {result.pastNotes && result.pastNotes.length > 0 && (
          <div style={{ marginTop: "0.5rem" }}>
            <strong>
              <FormattedMessage id="validation.row.notes.previous" />:
            </strong>
            {result.pastNotes.map((note, idx) => (
              <InlineNotification
                key={idx}
                kind="info"
                subtitle={note.value}
                lowContrast
                hideCloseButton
              />
            ))}
          </div>
        )}
      </div>

      <Tabs>
        <TabList aria-label="Result details tabs">
          <Tab>
            <FormattedMessage id="validation.details.method.tab" />
          </Tab>
          <Tab>
            <FormattedMessage id="validation.details.orderinfo.tab" />
          </Tab>
          <Tab>
            <FormattedMessage id="validation.details.attachments.tab" />
          </Tab>
          <Tab>
            <FormattedMessage id="validation.details.history.tab" />
          </Tab>
          <Tab>
            <FormattedMessage id="validation.details.qc.tab" />
          </Tab>
        </TabList>
        <TabPanels>
          <TabPanel>
            <ResultDetailsPanel analysisId={result.analysisId} tab="method" />
          </TabPanel>
          <TabPanel>
            <ResultDetailsPanel
              analysisId={result.analysisId}
              tab="orderInfo"
            />
          </TabPanel>
          <TabPanel>
            <ResultDetailsPanel
              analysisId={result.analysisId}
              tab="attachments"
            />
          </TabPanel>
          <TabPanel>
            <ResultDetailsPanel analysisId={result.analysisId} tab="history" />
          </TabPanel>
          <TabPanel>
            <ResultDetailsPanel analysisId={result.analysisId} tab="qc" />
          </TabPanel>
        </TabPanels>
      </Tabs>

      <div
        style={{
          display: "flex",
          justifyContent: "flex-end",
          gap: "0.5rem",
          marginTop: "1rem",
        }}
      >
        <Button kind="secondary" renderIcon={Renew} size="sm">
          <FormattedMessage id="validation.row.action.retest" />
        </Button>
        <Button kind="primary" renderIcon={CheckmarkOutline} size="sm">
          <FormattedMessage id="validation.row.action.accept" />
        </Button>
      </div>
    </div>
  );
};

export default ResultRow;
