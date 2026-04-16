import React, { useState, useEffect } from "react";
import {
  Grid,
  Column,
  TextInput,
  TextArea,
  Toggle,
  Button,
  FilterableMultiSelect,
} from "@carbon/react";
import { useIntl } from "react-intl";
import { getFromOpenElisServer } from "../utils/Utils";

const InlineEnrollmentForm = ({ enrollment, onSave, onCancel }) => {
  const intl = useIntl();
  const isEdit = !!enrollment;

  const [programName, setProgramName] = useState(
    enrollment ? enrollment.programName || "" : "",
  );
  const [provider, setProvider] = useState(
    enrollment ? enrollment.provider || "" : "",
  );
  const [description, setDescription] = useState(
    enrollment ? enrollment.description || "" : "",
  );
  const [isActive, setIsActive] = useState(
    enrollment ? enrollment.isActive : true,
  );
  const [selectedLabUnits, setSelectedLabUnits] = useState([]);
  const [selectedTests, setSelectedTests] = useState([]);
  const [selectedPanels, setSelectedPanels] = useState([]);
  const [dataReady, setDataReady] = useState(false);

  const [labUnits, setLabUnits] = useState([]);
  const [tests, setTests] = useState([]);
  const [panels, setPanels] = useState([]);

  useEffect(() => {
    let loaded = 0;
    const checkReady = () => {
      loaded++;
      if (loaded >= 3) setDataReady(true);
    };

    getFromOpenElisServer("/rest/test-sections", (data) => {
      if (data) {
        const items = data.map((ts) => ({
          id: String(ts.id),
          text: ts.value,
        }));
        setLabUnits(items);

        if (enrollment) {
          const selected = (enrollment.labUnits || [])
            .map((lu) => items.find((u) => u.id === String(lu.id)))
            .filter(Boolean);
          setSelectedLabUnits(selected);
        }
      }
      checkReady();
    });

    getFromOpenElisServer("/rest/tests", (data) => {
      if (data) {
        const items = data.map((t) => ({ id: String(t.id), text: t.value }));
        setTests(items);

        if (enrollment) {
          const selected = (enrollment.tests || [])
            .map((t) => items.find((te) => te.id === String(t.id)))
            .filter(Boolean);
          setSelectedTests(selected);
        }
      }
      checkReady();
    });

    getFromOpenElisServer("/rest/panels", (data) => {
      if (data) {
        const items = data.map((p) => ({ id: String(p.id), text: p.value }));
        setPanels(items);

        if (enrollment) {
          const selected = (enrollment.panels || [])
            .map((p) => items.find((pa) => pa.id === String(p.id)))
            .filter(Boolean);
          setSelectedPanels(selected);
        }
      }
      checkReady();
    });
  }, []);

  const handleSave = () => {
    const payload = {
      programName,
      provider,
      description,
      isActive,
      labUnitIds: selectedLabUnits.map((u) => Number(u.id)),
      testIds: selectedTests.map((t) => Number(t.id)),
      panelIds: selectedPanels.map((p) => Number(p.id)),
    };
    onSave(payload);
  };

  const isValid = programName.trim() !== "" && provider.trim() !== "";

  return (
    <div
      style={{
        padding: "1rem",
        backgroundColor: "#f4f4f4",
        borderTop: "2px solid #0f62fe",
      }}
    >
      <h4 style={{ marginBottom: "1rem" }}>
        {isEdit
          ? intl.formatMessage(
              { id: "eqa.enrollment.editing" },
              { name: enrollment.programName },
            )
          : intl.formatMessage({ id: "eqa.enrollment.new" })}
      </h4>

      <Grid condensed>
        <Column lg={5} md={4} sm={4}>
          <TextInput
            id="enrollment-program-name"
            labelText={intl.formatMessage({
              id: "eqa.enrollment.programName",
            })}
            value={programName}
            onChange={(e) => setProgramName(e.target.value)}
            placeholder={intl.formatMessage({
              id: "eqa.enrollment.programName.placeholder",
            })}
          />
        </Column>
        <Column lg={5} md={4} sm={4}>
          <TextInput
            id="enrollment-provider"
            labelText={intl.formatMessage({ id: "eqa.enrollment.provider" })}
            value={provider}
            onChange={(e) => setProvider(e.target.value)}
            placeholder={intl.formatMessage({
              id: "eqa.enrollment.provider.placeholder",
            })}
          />
        </Column>
        <Column lg={6} md={4} sm={4}>
          <TextArea
            id="enrollment-description"
            labelText={intl.formatMessage({
              id: "eqa.enrollment.description",
            })}
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            rows={1}
          />
        </Column>
      </Grid>

      {dataReady && (
        <Grid condensed style={{ marginTop: "1rem" }}>
          <Column lg={5} md={4} sm={4}>
            <FilterableMultiSelect
              id="enrollment-lab-units"
              titleText={intl.formatMessage({
                id: "eqa.enrollment.labUnits",
              })}
              items={labUnits}
              itemToString={(item) => (item ? item.text : "")}
              initialSelectedItems={selectedLabUnits}
              onChange={(e) => setSelectedLabUnits(e.selectedItems)}
              placeholder={intl.formatMessage({
                id: "eqa.enrollment.selectLabUnits",
              })}
            />
          </Column>
          <Column lg={5} md={4} sm={4}>
            <FilterableMultiSelect
              id="enrollment-tests"
              titleText={intl.formatMessage({
                id: "eqa.enrollment.tests",
              })}
              items={tests}
              itemToString={(item) => (item ? item.text : "")}
              initialSelectedItems={selectedTests}
              onChange={(e) => setSelectedTests(e.selectedItems)}
              placeholder={intl.formatMessage({
                id: "eqa.enrollment.selectTests",
              })}
            />
          </Column>
          <Column lg={6} md={4} sm={4}>
            <FilterableMultiSelect
              id="enrollment-panels"
              titleText={intl.formatMessage({
                id: "eqa.enrollment.panels",
              })}
              items={panels}
              itemToString={(item) => (item ? item.text : "")}
              initialSelectedItems={selectedPanels}
              onChange={(e) => setSelectedPanels(e.selectedItems)}
              placeholder={intl.formatMessage({
                id: "eqa.enrollment.selectPanels",
              })}
            />
          </Column>
        </Grid>
      )}

      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          marginTop: "1rem",
        }}
      >
        <Toggle
          id="enrollment-active-toggle"
          labelText={intl.formatMessage({ id: "eqa.enrollment.status" })}
          labelA={intl.formatMessage({ id: "eqa.status.inactive" })}
          labelB={intl.formatMessage({ id: "eqa.status.active" })}
          toggled={isActive}
          onToggle={(checked) => setIsActive(checked)}
        />
        <div style={{ display: "flex", gap: "0.5rem" }}>
          <Button kind="secondary" size="sm" onClick={onCancel}>
            {intl.formatMessage({ id: "label.button.cancel" })}
          </Button>
          <Button size="sm" onClick={handleSave} disabled={!isValid}>
            {isEdit
              ? intl.formatMessage({ id: "eqa.enrollment.saveChanges" })
              : intl.formatMessage({ id: "eqa.enrollment.save" })}
          </Button>
        </div>
      </div>
    </div>
  );
};

export default InlineEnrollmentForm;
