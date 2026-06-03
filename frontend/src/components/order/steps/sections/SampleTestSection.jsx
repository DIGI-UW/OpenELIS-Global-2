import React, { useState, useEffect, useRef } from "react";
import { useIntl, FormattedMessage } from "react-intl";
import {
  Grid,
  Column,
  Tile,
  Select,
  SelectItem,
  TextInput,
  Button,
  Checkbox,
  Tag,
  Search,
  Link,
} from "@carbon/react";
import {
  Add,
  Copy,
  TrashCan,
  ChevronDown,
  ChevronUp,
} from "@carbon/icons-react";
import { getFromOpenElisServer } from "../../../utils/Utils";

const SampleTestSection = ({
  samples,
  setSamples,
  orderData,
  setOrderData,
  isReadOnly,
  workflowType,
}) => {
  const intl = useIntl();
  const componentMounted = useRef(true);

  const [sampleTypes, setSampleTypes] = useState([]);
  const [testsPerSample, setTestsPerSample] = useState({});
  const [panelsPerSample, setPanelsPerSample] = useState({});
  const [testSearchTerms, setTestSearchTerms] = useState({});
  const [panelSearchTerms, setPanelSearchTerms] = useState({});
  const [lifecycleStagesPerSample, setLifecycleStagesPerSample] = useState({});
  const [trapTypesPerSample, setTrapTypesPerSample] = useState({});
  const [isLoading, setIsLoading] = useState(true);
  const [loadingPerSample, setLoadingPerSample] = useState({});

  // Environmental manifest dictionary data
  const [containerTypes, setContainerTypes] = useState([]);

  // Per-row expanded state for test/panel picker (environmental manifest)
  const [expandedRows, setExpandedRows] = useState({});

  useEffect(() => {
    componentMounted.current = true;
    return () => {
      componentMounted.current = false;
    };
  }, []);

  useEffect(() => {
    let cancelled = false;
    setIsLoading(true);
    const sampleTypesUrl =
      workflowType === "vector"
        ? "/rest/vector-sample-types"
        : workflowType === "environmental"
          ? "/rest/environmental-sample-types"
          : "/rest/user-sample-types";
    getFromOpenElisServer(sampleTypesUrl, (response) => {
      if (!cancelled && response) {
        setSampleTypes(response);
        setIsLoading(false);
      }
    });
    return () => {
      cancelled = true;
    };
  }, [workflowType]);

  // Fetch environmental manifest dictionaries once
  useEffect(() => {
    if (workflowType !== "environmental") return;
    getFromOpenElisServer(
      "/rest/vector/dictionary/sample-containers",
      (data) => {
        if (componentMounted.current) setContainerTypes(data || []);
      },
    );
  }, [workflowType]);

  const fetchedSampleTypesRef = useRef({});

  useEffect(() => {
    samples.forEach((sample, index) => {
      const sampleTypeId = sample?.sampleTypeId;
      if (
        sampleTypeId &&
        fetchedSampleTypesRef.current[index] !== sampleTypeId
      ) {
        fetchedSampleTypesRef.current[index] = sampleTypeId;
        fetchTestsForSampleType(index, sampleTypeId);
      }
    });
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [samples]);

  const fetchTestsForSampleType = (sampleIndex, sampleTypeId) => {
    if (!sampleTypeId) {
      setTestsPerSample((prev) => ({ ...prev, [sampleIndex]: [] }));
      setPanelsPerSample((prev) => ({ ...prev, [sampleIndex]: [] }));
      setTrapTypesPerSample((prev) => ({ ...prev, [sampleIndex]: [] }));
      setLifecycleStagesPerSample((prev) => ({ ...prev, [sampleIndex]: [] }));
      return;
    }
    setLoadingPerSample((prev) => ({ ...prev, [sampleIndex]: true }));
    getFromOpenElisServer(
      `/rest/sample-type-tests?sampleType=${sampleTypeId}`,
      (response) => {
        if (componentMounted.current && response) {
          setPanelsPerSample((prev) => ({
            ...prev,
            [sampleIndex]: response.panels || [],
          }));
          setTestsPerSample((prev) => ({
            ...prev,
            [sampleIndex]: response.tests || [],
          }));
          setLoadingPerSample((prev) => ({ ...prev, [sampleIndex]: false }));
        }
      },
    );
    if (workflowType === "vector") {
      getFromOpenElisServer(
        `/rest/admin/vector/trap-types?sampleTypeId=${sampleTypeId}`,
        (response) => {
          if (componentMounted.current) {
            setTrapTypesPerSample((prev) => ({
              ...prev,
              [sampleIndex]: Array.isArray(response) ? response : [],
            }));
          }
        },
      );
      getFromOpenElisServer(
        `/rest/vector/dictionary/lifecycle-stages?sampleTypeId=${sampleTypeId}`,
        (response) => {
          if (componentMounted.current) {
            setLifecycleStagesPerSample((prev) => ({
              ...prev,
              [sampleIndex]: Array.isArray(response) ? response : [],
            }));
          }
        },
      );
    }
  };

  const getFilteredTests = (sampleIndex) => {
    const tests = testsPerSample[sampleIndex] || [];
    const term = (testSearchTerms[sampleIndex] || "").toLowerCase();
    return term
      ? tests.filter((t) => t.name?.toLowerCase().includes(term))
      : tests;
  };

  const getFilteredPanels = (sampleIndex) => {
    const panels = panelsPerSample[sampleIndex] || [];
    const term = (panelSearchTerms[sampleIndex] || "").toLowerCase();
    return term
      ? panels.filter((p) => p.name?.toLowerCase().includes(term))
      : panels;
  };

  const handleAddSample = () => {
    const newSample = {
      index: samples.length,
      sampleRejected: false,
      rejectionReason: "",
      sampleTypeId: "",
      sampleXML: null,
      panels: [],
      tests: [],
      referralItems: [],
      // Environmental manifest fields
      container: "",
      gpsLatitude: "",
      gpsLongitude: "",
      locationDetails: "",
      qcMetadata: null,
    };
    setSamples([...samples, newSample]);
  };

  const handleDuplicateSample = (sourceIndex) => {
    const source = samples[sourceIndex];
    const newIndex = samples.length;
    const duplicate = {
      ...source,
      index: newIndex,
      sampleXML: null,
    };
    setSamples([...samples, duplicate]);

    // Clone tests/panels state for the new index
    setTestsPerSample((prev) => ({
      ...prev,
      [newIndex]: prev[sourceIndex] || [],
    }));
    setPanelsPerSample((prev) => ({
      ...prev,
      [newIndex]: prev[sourceIndex] || [],
    }));
    setLoadingPerSample((prev) => ({ ...prev, [newIndex]: false }));
    fetchedSampleTypesRef.current[newIndex] = source.sampleTypeId;
  };

  // Add QC sample linked to a specific parent sample.
  // Inherits the parent's collected fields at creation time; not auto-synced if
  // the parent changes later.
  const handleAddQcSample = (qcType, parentIndex) => {
    const parent = samples[parentIndex] || {};
    const newSample = {
      index: samples.length,
      sampleRejected: false,
      rejectionReason: "",
      sampleTypeId: parent.sampleTypeId || "",
      sampleTypeName: parent.sampleTypeName || "",
      sampleXML: null,
      panels: [...(parent.panels || [])],
      tests: [...(parent.tests || [])],
      referralItems: [],
      container: parent.container || "",
      gpsLatitude: parent.gpsLatitude || "",
      gpsLongitude: parent.gpsLongitude || "",
      locationDetails: parent.locationDetails || "",
      collectionDate: parent.collectionDate || "",
      collectionTime: parent.collectionTime || "",
      qcMetadata: {
        qcType,
        parentSampleIndex: parentIndex,
        expectedValue: null,
      },
    };
    setSamples([...samples, newSample]);
  };

  const updateQcExpected = (qcIndex, value) => {
    const updated = [...samples];
    updated[qcIndex] = {
      ...updated[qcIndex],
      qcMetadata: {
        ...updated[qcIndex].qcMetadata,
        expectedValue: value,
      },
    };
    setSamples(updated);
  };

  const qcTagType = (qcType) => {
    if (qcType === "BLANK") return "blue";
    if (qcType === "DUPLICATE") return "teal";
    return "purple";
  };

  const handleRemoveSample = (index) => {
    setSamples(samples.filter((_, i) => i !== index));
  };

  const handleSampleTypeChange = (sampleIndex, sampleTypeId) => {
    const updated = [...samples];
    const currentSampleType = updated[sampleIndex]?.sampleTypeId;
    const shouldClearSelections =
      currentSampleType && currentSampleType !== sampleTypeId;
    const selectedType = sampleTypes.find((t) => t.id === sampleTypeId);
    updated[sampleIndex] = {
      ...updated[sampleIndex],
      sampleTypeId,
      sampleTypeName: selectedType?.value || "",
      ...(shouldClearSelections ? { panels: [], tests: [] } : {}),
    };
    setSamples(updated);
    if (sampleTypeId !== fetchedSampleTypesRef.current[sampleIndex]) {
      fetchedSampleTypesRef.current[sampleIndex] = sampleTypeId;
      fetchTestsForSampleType(sampleIndex, sampleTypeId);
    }
  };

  const handleEnvFieldChange = (sampleIndex, field, value) => {
    const updated = [...samples];
    updated[sampleIndex] = { ...updated[sampleIndex], [field]: value };
    setSamples(updated);
  };

  const handleEnvFieldsChange = (sampleIndex, fields) => {
    const updated = [...samples];
    updated[sampleIndex] = { ...updated[sampleIndex], ...fields };
    setSamples(updated);
  };

  const handlePanelToggle = (sampleIndex, panel, isSelected) => {
    const updated = [...samples];
    const currentPanels = updated[sampleIndex].panels || [];
    const currentTests = updated[sampleIndex].tests || [];
    const availableTests = testsPerSample[sampleIndex] || [];
    const panelTestIds = panel.testIds
      ? panel.testIds.split(",").map((id) => id.trim())
      : [];
    if (isSelected) {
      updated[sampleIndex].panels = [
        ...currentPanels,
        { id: panel.id, name: panel.name, testIds: panel.testIds },
      ];
      const testsToAdd = panelTestIds
        .filter((testId) => !currentTests.some((t) => t.id === testId))
        .map((testId) => {
          const test = availableTests.find((t) => t.id === testId);
          return { id: testId, name: test?.name || testId };
        });
      updated[sampleIndex].tests = [...currentTests, ...testsToAdd];
    } else {
      updated[sampleIndex].panels = currentPanels.filter(
        (p) => p.id !== panel.id,
      );
      const otherPanelTestIds = new Set();
      updated[sampleIndex].panels.forEach((p) => {
        if (p.testIds)
          p.testIds
            .split(",")
            .forEach((id) => otherPanelTestIds.add(id.trim()));
      });
      updated[sampleIndex].tests = currentTests.filter(
        (t) => !panelTestIds.includes(t.id) || otherPanelTestIds.has(t.id),
      );
    }
    setSamples(updated);
  };

  const handleTestToggle = (sampleIndex, test, isSelected) => {
    const updated = [...samples];
    const currentTests = updated[sampleIndex].tests || [];
    updated[sampleIndex].tests = isSelected
      ? [...currentTests, { id: test.id, name: test.name }]
      : currentTests.filter((t) => t.id !== test.id);
    setSamples(updated);
  };

  const handleRemovePanel = (sampleIndex, panelId) => {
    const updated = [...samples];
    const currentPanels = updated[sampleIndex].panels || [];
    const currentTests = updated[sampleIndex].tests || [];
    const panelToRemove = currentPanels.find((p) => p.id === panelId);
    const panelTestIds = panelToRemove?.testIds
      ? panelToRemove.testIds.split(",").map((id) => id.trim())
      : [];
    updated[sampleIndex].panels = currentPanels.filter((p) => p.id !== panelId);
    const remainingPanelTestIds = new Set();
    updated[sampleIndex].panels.forEach((p) => {
      if (p.testIds)
        p.testIds
          .split(",")
          .forEach((id) => remainingPanelTestIds.add(id.trim()));
    });
    updated[sampleIndex].tests = currentTests.filter(
      (t) => !panelTestIds.includes(t.id) || remainingPanelTestIds.has(t.id),
    );
    setSamples(updated);
  };

  const handleRemoveTest = (sampleIndex, testId) => {
    const updated = [...samples];
    updated[sampleIndex].tests = updated[sampleIndex].tests.filter(
      (t) => t.id !== testId,
    );
    setSamples(updated);
  };

  const handleVectorFieldChange = (sampleIndex, field, value) => {
    const updated = [...samples];
    updated[sampleIndex] = {
      ...updated[sampleIndex],
      vectorFields: { ...updated[sampleIndex].vectorFields, [field]: value },
    };
    setSamples(updated);
  };

  const isPanelSelected = (sampleIndex, panelId) =>
    samples[sampleIndex]?.panels?.some((p) => p.id === panelId) || false;

  const isTestSelected = (sampleIndex, testId) =>
    samples[sampleIndex]?.tests?.some((t) => t.id === testId) || false;

  const toggleRowExpanded = (sampleIndex) => {
    setExpandedRows((prev) => ({ ...prev, [sampleIndex]: !prev[sampleIndex] }));
  };

  const getSelectionCount = (sampleIndex) => {
    const panelCount = samples[sampleIndex]?.panels?.length || 0;
    const testCount = samples[sampleIndex]?.tests?.length || 0;
    return panelCount + testCount;
  };

  // Shared test/panel picker — rendered below a row when expanded
  const renderTestPanelPicker = (sampleIndex) => {
    const sample = samples[sampleIndex];
    if (!sample?.sampleTypeId) return null;
    return (
      <div className="env-manifest-test-picker">
        <div className="env-manifest-test-picker-columns">
          {/* Panels */}
          <div className="env-manifest-picker-col">
            <h6>
              <FormattedMessage
                id="sample.orderPanels"
                defaultMessage="Order Panels"
              />
            </h6>
            <div className="selected-tags">
              {sample.panels?.map((panel) => (
                <Tag
                  key={panel.id}
                  type="blue"
                  filter
                  onClose={() => handleRemovePanel(sampleIndex, panel.id)}
                  disabled={isReadOnly}
                >
                  {panel.name}
                </Tag>
              ))}
            </div>
            {getFilteredPanels(sampleIndex).length > 0 ? (
              <>
                <Search
                  id={`panelSearch-${sampleIndex}`}
                  labelText=""
                  placeholder={intl.formatMessage({
                    id: "panel.search.placeholder",
                    defaultMessage: "Search panels...",
                  })}
                  value={panelSearchTerms[sampleIndex] || ""}
                  onChange={(e) =>
                    setPanelSearchTerms((prev) => ({
                      ...prev,
                      [sampleIndex]: e.target.value,
                    }))
                  }
                  disabled={isReadOnly}
                  size="sm"
                />
                <div className="checkbox-list">
                  {getFilteredPanels(sampleIndex).map((panel) => (
                    <Checkbox
                      key={panel.id}
                      id={`panel-${sampleIndex}-${panel.id}`}
                      labelText={panel.name}
                      checked={isPanelSelected(sampleIndex, panel.id)}
                      onChange={(_, { checked }) =>
                        handlePanelToggle(sampleIndex, panel, checked)
                      }
                      disabled={isReadOnly}
                    />
                  ))}
                </div>
              </>
            ) : loadingPerSample[sampleIndex] ? (
              <p className="no-items-message">
                <FormattedMessage
                  id="label.loading"
                  defaultMessage="Loading..."
                />
              </p>
            ) : (
              <p className="no-items-message">
                <FormattedMessage
                  id="sample.noPanels"
                  defaultMessage="No panels available for this sample type"
                />
              </p>
            )}
          </div>

          {/* Tests */}
          <div className="env-manifest-picker-col">
            <h6>
              <FormattedMessage
                id="sample.orderTests"
                defaultMessage="Order Tests"
              />
            </h6>
            <div className="selected-tags">
              {sample.tests?.map((test) => (
                <Tag
                  key={test.id}
                  type="teal"
                  filter
                  onClose={() => handleRemoveTest(sampleIndex, test.id)}
                  disabled={isReadOnly}
                >
                  {test.name}
                </Tag>
              ))}
            </div>
            {getFilteredTests(sampleIndex).length > 0 ? (
              <>
                <Search
                  id={`testSearch-${sampleIndex}`}
                  labelText=""
                  placeholder={intl.formatMessage({
                    id: "test.search.placeholder",
                    defaultMessage: "Search tests...",
                  })}
                  value={testSearchTerms[sampleIndex] || ""}
                  onChange={(e) =>
                    setTestSearchTerms((prev) => ({
                      ...prev,
                      [sampleIndex]: e.target.value,
                    }))
                  }
                  disabled={isReadOnly}
                  size="sm"
                />
                <div className="checkbox-list checkbox-list-scrollable">
                  {getFilteredTests(sampleIndex).map((test) => (
                    <Checkbox
                      key={test.id}
                      id={`test-${sampleIndex}-${test.id}`}
                      labelText={test.name}
                      checked={isTestSelected(sampleIndex, test.id)}
                      onChange={(_, { checked }) =>
                        handleTestToggle(sampleIndex, test, checked)
                      }
                      disabled={isReadOnly}
                    />
                  ))}
                </div>
                <span className="test-count-info">
                  <FormattedMessage
                    id="sample.testsCount"
                    defaultMessage="{count} tests available"
                    values={{ count: getFilteredTests(sampleIndex).length }}
                  />
                </span>
              </>
            ) : loadingPerSample[sampleIndex] ? (
              <p className="no-items-message">
                <FormattedMessage
                  id="label.loading"
                  defaultMessage="Loading..."
                />
              </p>
            ) : (
              <p className="no-items-message">
                <FormattedMessage
                  id="sample.noTests"
                  defaultMessage="No tests available for this sample type"
                />
              </p>
            )}
          </div>
        </div>

        {(workflowType === "environmental" || workflowType === "vector") &&
          (() => {
            const childQcTypes = samples
              .filter(
                (s) =>
                  s.qcMetadata?.qcType &&
                  s.qcMetadata?.parentSampleIndex === sampleIndex,
              )
              .map((s) => s.qcMetadata.qcType);
            const hasTests = sample.tests && sample.tests.length > 0;
            const hasBlank = childQcTypes.includes("BLANK");
            const hasDuplicate = childQcTypes.includes("DUPLICATE");
            const hasControl = childQcTypes.includes("CONTROL");

            // Threshold-existence comes from the sample-type-tests catalog
            // response. We can only enable QC if every selected test has a
            // configured threshold row (otherwise QC pass/fail can't be
            // evaluated).
            const availableTests = testsPerSample[sampleIndex] || [];
            const testsMissingThreshold = (sample.tests || []).filter((t) => {
              const def = availableTests.find((at) => at.id === t.id);
              return !def?.hasQcThreshold;
            });
            const allHaveThresholds =
              hasTests && testsMissingThreshold.length === 0;
            // QC engine (OGC-554) is numeric-only for the MVP; coded results
            // (M/C/D) have no arithmetic threshold to evaluate against.
            const hasAnyNumericTest = (sample.tests || []).some((t) => {
              const def = availableTests.find((at) => at.id === t.id);
              return def?.resultType === "N";
            });
            const qcDisabledBase =
              isReadOnly ||
              !hasTests ||
              !allHaveThresholds ||
              !hasAnyNumericTest;

            return (
              <div className="env-manifest-qc-buttons">
                <span className="env-manifest-qc-label">
                  <FormattedMessage
                    id="qc.samples.label"
                    defaultMessage="QC samples:"
                  />
                </span>
                <Button
                  kind="ghost"
                  size="sm"
                  onClick={() => handleAddQcSample("BLANK", sampleIndex)}
                  disabled={qcDisabledBase || hasBlank}
                >
                  <FormattedMessage
                    id="qc.add.blank"
                    defaultMessage="+ Blank QC"
                  />
                </Button>
                <Button
                  kind="ghost"
                  size="sm"
                  onClick={() => handleAddQcSample("DUPLICATE", sampleIndex)}
                  disabled={qcDisabledBase || hasDuplicate}
                >
                  <FormattedMessage
                    id="qc.add.duplicate"
                    defaultMessage="+ Duplicate QC"
                  />
                </Button>
                <Button
                  kind="ghost"
                  size="sm"
                  onClick={() => handleAddQcSample("CONTROL", sampleIndex)}
                  disabled={qcDisabledBase || hasControl}
                >
                  <FormattedMessage
                    id="qc.add.control"
                    defaultMessage="+ Control QC"
                  />
                </Button>
                {hasTests && testsMissingThreshold.length > 0 && (
                  <p className="env-manifest-qc-helper">
                    <FormattedMessage
                      id="qc.samples.thresholdsMissing"
                      defaultMessage="Configure QC thresholds for {tests} in Administration → Test Configuration before adding QC samples."
                      values={{
                        tests: testsMissingThreshold
                          .map((t) => t.name)
                          .join(", "),
                      }}
                    />
                  </p>
                )}
                {hasTests && !hasAnyNumericTest && (
                  <p className="env-manifest-qc-helper">
                    <FormattedMessage
                      id="qc.samples.numericOnly"
                      defaultMessage="QC is currently supported only for tests with numeric results. The selected tests are coded — pick a numeric test to attach QC."
                    />
                  </p>
                )}
              </div>
            );
          })()}
      </div>
    );
  };

  // Environmental manifest table layout
  if (workflowType === "environmental") {
    return (
      <Tile className="order-section sample-test-section">
        <div className="env-manifest-header">
          <h4 className="section-title">
            <FormattedMessage
              id="env.sample.manifest.title"
              defaultMessage="B. Per-Sample Manifest"
            />
          </h4>
          <span className="env-manifest-subtitle">
            <FormattedMessage
              id="env.sample.manifest.subtitle"
              defaultMessage="one row = one physical sample"
            />
          </span>
        </div>

        <div className="env-manifest-table-wrap">
          <table className="env-manifest-table">
            <thead>
              <tr>
                <th>#</th>
                <th>
                  <FormattedMessage
                    id="sample.type"
                    defaultMessage="Sample Type"
                  />
                </th>
                <th>
                  <FormattedMessage
                    id="env.sample.container"
                    defaultMessage="Container"
                  />
                </th>
                <th>
                  <FormattedMessage
                    id="env.sample.gpsLat"
                    defaultMessage="GPS Lat"
                  />
                </th>
                <th>
                  <FormattedMessage
                    id="env.sample.gpsLng"
                    defaultMessage="GPS Lng"
                  />
                </th>
                <th>
                  <FormattedMessage
                    id="env.sample.locationDetails"
                    defaultMessage="Location Details"
                  />
                </th>
                <th>
                  <FormattedMessage
                    id="env.sample.collected"
                    defaultMessage="Collected"
                  />
                </th>
                <th>
                  <FormattedMessage
                    id="env.sample.testsAndPanels"
                    defaultMessage="Tests & Panels"
                  />
                </th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              {samples.map((sample, sampleIndex) => {
                if (sample.qcMetadata?.qcType) return null;
                const selectionCount = getSelectionCount(sampleIndex);
                const isExpanded = expandedRows[sampleIndex] || false;
                const childQcRows = samples
                  .map((s, i) => ({ s, i }))
                  .filter(
                    ({ s }) =>
                      s.qcMetadata?.qcType &&
                      s.qcMetadata?.parentSampleIndex === sampleIndex,
                  );
                return (
                  <React.Fragment key={sampleIndex}>
                    <tr className="env-manifest-row">
                      <td className="env-manifest-cell env-manifest-cell--num">
                        {sampleIndex + 1}
                      </td>
                      <td className="env-manifest-cell">
                        <Select
                          id={`sampleType-${sampleIndex}`}
                          labelText=""
                          hideLabel
                          value={sample.sampleTypeId || ""}
                          onChange={(e) =>
                            handleSampleTypeChange(sampleIndex, e.target.value)
                          }
                          disabled={isReadOnly}
                        >
                          <SelectItem value="" text="" />
                          {sampleTypes.map((type) => (
                            <SelectItem
                              key={type.id}
                              value={type.id}
                              text={type.value}
                            />
                          ))}
                        </Select>
                      </td>
                      <td className="env-manifest-cell">
                        <Select
                          id={`container-${sampleIndex}`}
                          labelText=""
                          hideLabel
                          value={sample.container || ""}
                          onChange={(e) =>
                            handleEnvFieldChange(
                              sampleIndex,
                              "container",
                              e.target.value,
                            )
                          }
                          disabled={isReadOnly}
                        >
                          <SelectItem value="" text="" />
                          {containerTypes.map((c) => (
                            <SelectItem
                              key={c.id}
                              value={c.dictEntry}
                              text={c.dictEntry}
                            />
                          ))}
                        </Select>
                      </td>
                      <td className="env-manifest-cell">
                        <TextInput
                          id={`gpsLat-${sampleIndex}`}
                          labelText=""
                          hideLabel
                          placeholder={intl.formatMessage({
                            id: "env.sample.gpsLat.placeholder",
                            defaultMessage: "e.g. -6.2088",
                          })}
                          value={sample.gpsLatitude || ""}
                          onChange={(e) =>
                            handleEnvFieldChange(
                              sampleIndex,
                              "gpsLatitude",
                              e.target.value,
                            )
                          }
                          disabled={isReadOnly}
                        />
                      </td>
                      <td className="env-manifest-cell">
                        <TextInput
                          id={`gpsLng-${sampleIndex}`}
                          labelText=""
                          hideLabel
                          placeholder={intl.formatMessage({
                            id: "env.sample.gpsLng.placeholder",
                            defaultMessage: "e.g. 106.8456",
                          })}
                          value={sample.gpsLongitude || ""}
                          onChange={(e) =>
                            handleEnvFieldChange(
                              sampleIndex,
                              "gpsLongitude",
                              e.target.value,
                            )
                          }
                          disabled={isReadOnly}
                        />
                      </td>
                      <td className="env-manifest-cell">
                        <TextInput
                          id={`locationDetails-${sampleIndex}`}
                          labelText=""
                          hideLabel
                          placeholder={intl.formatMessage({
                            id: "env.sample.locationDetails.placeholder",
                            defaultMessage: "e.g., Inlet, Bank...",
                          })}
                          value={sample.locationDetails || ""}
                          onChange={(e) =>
                            handleEnvFieldChange(
                              sampleIndex,
                              "locationDetails",
                              e.target.value,
                            )
                          }
                          disabled={isReadOnly}
                        />
                      </td>
                      <td className="env-manifest-cell">
                        <input
                          id={`collected-${sampleIndex}`}
                          type="datetime-local"
                          className="env-manifest-datetime"
                          value={
                            sample.collectionDate && sample.collectionTime
                              ? `${sample.collectionDate}T${sample.collectionTime}`
                              : sample.collectionDate || ""
                          }
                          onChange={(e) => {
                            const val = e.target.value;
                            if (val && val.includes("T")) {
                              const [date, time] = val.split("T");
                              handleEnvFieldsChange(sampleIndex, {
                                collectionDate: date,
                                collectionTime: time,
                              });
                            } else {
                              handleEnvFieldsChange(sampleIndex, {
                                collectionDate: val,
                                collectionTime: "",
                              });
                            }
                          }}
                          disabled={isReadOnly}
                        />
                        <Checkbox
                          id={`labPerformedSampling-${sampleIndex}`}
                          labelText={intl.formatMessage({
                            id: "collect.sample.labPerformedSampling",
                            defaultMessage: "Lab performed sampling",
                          })}
                          checked={!!sample.labPerformedSampling}
                          onChange={(_, { checked }) =>
                            handleEnvFieldChange(
                              sampleIndex,
                              "labPerformedSampling",
                              checked,
                            )
                          }
                          disabled={isReadOnly}
                        />
                      </td>
                      <td className="env-manifest-cell env-manifest-cell--toggle">
                        <Button
                          kind={
                            sample.sampleTypeId && selectionCount === 0
                              ? "danger--ghost"
                              : "ghost"
                          }
                          size="sm"
                          renderIcon={isExpanded ? ChevronUp : ChevronDown}
                          onClick={() => toggleRowExpanded(sampleIndex)}
                          className="env-manifest-toggle-btn"
                          disabled={!sample.sampleTypeId || isReadOnly}
                        >
                          <FormattedMessage
                            id="env.sample.testsAndPanels"
                            defaultMessage="Tests & Panels"
                          />
                          {selectionCount > 0
                            ? ` (${selectionCount})`
                            : sample.sampleTypeId
                              ? ` — ${intl.formatMessage({ id: "env.sample.tests.required", defaultMessage: "required" })}`
                              : ""}
                        </Button>
                      </td>
                      <td className="env-manifest-cell env-manifest-cell--actions">
                        <Button
                          kind="ghost"
                          size="sm"
                          hasIconOnly
                          iconDescription={intl.formatMessage({
                            id: "env.sample.duplicate",
                            defaultMessage: "Duplicate sample",
                          })}
                          renderIcon={Copy}
                          onClick={() => handleDuplicateSample(sampleIndex)}
                          disabled={isReadOnly}
                        />
                        {samples.length > 1 && (
                          <Button
                            kind="ghost"
                            size="sm"
                            hasIconOnly
                            iconDescription={intl.formatMessage({
                              id: "sample.remove.action",
                              defaultMessage: "Remove Sample",
                            })}
                            renderIcon={TrashCan}
                            onClick={() => handleRemoveSample(sampleIndex)}
                            disabled={isReadOnly}
                          />
                        )}
                      </td>
                    </tr>
                    {isExpanded && sample.sampleTypeId && (
                      <tr className="env-manifest-row--expanded">
                        <td colSpan={10}>
                          {renderTestPanelPicker(sampleIndex)}
                        </td>
                      </tr>
                    )}
                    {childQcRows.map(
                      ({ s: qcSample, i: qcIndex }, childIdx) => (
                        <tr
                          key={`qc-${qcIndex}`}
                          className={`env-manifest-row env-manifest-row--qc env-manifest-row--qc-${qcSample.qcMetadata.qcType.toLowerCase()}`}
                        >
                          <td className="env-manifest-cell env-manifest-cell--num env-manifest-cell--qc-num">
                            {sampleIndex + 1}.
                            {String.fromCharCode(97 + childIdx)}
                          </td>
                          <td className="env-manifest-cell" colSpan={2}>
                            <div className="env-manifest-qc-row-label">
                              <Tag type={qcTagType(qcSample.qcMetadata.qcType)}>
                                <FormattedMessage
                                  id={`qc.type.${qcSample.qcMetadata.qcType.toLowerCase()}`}
                                  defaultMessage={`QC: ${qcSample.qcMetadata.qcType}`}
                                />
                              </Tag>
                              <span className="env-manifest-qc-tests">
                                {qcSample.tests?.map((t) => t.name).join(", ")}
                              </span>
                            </div>
                          </td>
                          <td
                            className="env-manifest-cell env-manifest-cell--inherited"
                            colSpan={5}
                          >
                            <em>
                              <FormattedMessage
                                id="qc.collect.inheritsFromParent"
                                defaultMessage="Collection details inherited from parent sample"
                              />
                            </em>
                          </td>
                          <td className="env-manifest-cell">
                            {qcSample.qcMetadata.qcType === "CONTROL" && (
                              <TextInput
                                id={`qc-expected-${qcIndex}`}
                                labelText=""
                                hideLabel
                                size="sm"
                                placeholder={intl.formatMessage({
                                  id: "qc.expectedValue.placeholder",
                                  defaultMessage: "e.g. 50.0",
                                })}
                                value={qcSample.qcMetadata.expectedValue || ""}
                                onChange={(e) =>
                                  updateQcExpected(qcIndex, e.target.value)
                                }
                                disabled={isReadOnly}
                              />
                            )}
                          </td>
                          <td className="env-manifest-cell env-manifest-cell--actions">
                            <Button
                              kind="ghost"
                              size="sm"
                              hasIconOnly
                              iconDescription={intl.formatMessage({
                                id: "sample.remove.action",
                                defaultMessage: "Remove Sample",
                              })}
                              renderIcon={TrashCan}
                              onClick={() => handleRemoveSample(qcIndex)}
                              disabled={isReadOnly}
                            />
                          </td>
                        </tr>
                      ),
                    )}
                  </React.Fragment>
                );
              })}
            </tbody>
          </table>
        </div>

        <div className="env-manifest-footer">
          <Link onClick={handleAddSample} disabled={isReadOnly}>
            <FormattedMessage
              id="env.sample.addRow"
              defaultMessage="+ Add sample row"
            />
          </Link>
          <span className="env-manifest-count">
            <FormattedMessage
              id="env.sample.total"
              defaultMessage="Total: {count} {count, plural, one {sample} other {samples}} in this batch"
              values={{ count: samples.length }}
            />
          </span>
        </div>
      </Tile>
    );
  }

  // Vector workflow, post-fan-out: group by pool instead of showing individual organisms.
  const isPostFanOut =
    workflowType === "vector" && samples.some((s) => s.vectorPoolId);
  if (isPostFanOut) {
    const poolMap = new Map();
    samples.forEach((s) => {
      const key = s.vectorPoolId || s.sampleTypeId || "unknown";
      if (!poolMap.has(key)) {
        poolMap.set(key, {
          name: s.sampleTypeName || s.name || "—",
          members: [],
          panels: s.panels || [],
          tests: s.tests || [],
        });
      }
      const g = poolMap.get(key);
      g.members.push(s);
      // Panels and tests live on the pool, not per-member — merge once.
      if (s.panels && s.panels.length > 0 && g.panels.length === 0) {
        g.panels = s.panels;
      }
      if (s.tests && s.tests.length > 0 && g.tests.length === 0) {
        g.tests = s.tests;
      }
    });
    return (
      <Tile className="order-section sample-test-section">
        <h4 className="section-title">
          <FormattedMessage id="label.button.sample" defaultMessage="Sample" />
        </h4>
        {Array.from(poolMap.entries()).map(([poolId, pool], idx) => {
          const panelTestIds = new Set(
            (pool.panels || []).flatMap((p) =>
              p.testIds ? p.testIds.split(",").map((id) => id.trim()) : [],
            ),
          );
          const standaloneTests = (pool.tests || []).filter(
            (t) => !panelTestIds.has(String(t.id)),
          );
          return (
            <div key={poolId || idx} className="sample-card">
              <div className="sample-card-header">
                <h5>
                  <FormattedMessage
                    id="vector.pool.summary"
                    defaultMessage="Pool of {count} {animal}"
                    values={{ count: pool.members.length, animal: pool.name }}
                  />
                </h5>
              </div>
              <div style={{ padding: "0.5rem 0" }}>
                {pool.panels.map((p) => {
                  const memberIdSet = new Set(
                    p.testIds
                      ? p.testIds
                          .split(",")
                          .map((id) => id.trim())
                          .filter(Boolean)
                      : [],
                  );
                  const memberTests = (pool.tests || []).filter((t) =>
                    memberIdSet.has(String(t.id)),
                  );
                  return (
                    <div key={p.id} style={{ marginBottom: "0.25rem" }}>
                      <Tag type="blue" size="sm" style={{ marginRight: 4 }}>
                        {p.name}
                      </Tag>
                      {memberTests.map((t) => (
                        <Tag
                          key={t.id}
                          type="gray"
                          size="sm"
                          style={{ marginRight: 4, opacity: 0.8 }}
                        >
                          {t.name}
                        </Tag>
                      ))}
                    </div>
                  );
                })}
                {standaloneTests.map((t) => (
                  <Tag
                    key={t.id}
                    type="gray"
                    size="sm"
                    style={{ marginRight: 4 }}
                  >
                    {t.name}
                  </Tag>
                ))}
                {pool.panels.length === 0 && standaloneTests.length === 0 && (
                  <span
                    style={{
                      color: "var(--cds-text-placeholder, #8d8d8d)",
                      fontSize: "0.8125rem",
                    }}
                  >
                    <FormattedMessage
                      id="vector.pool.noTests"
                      defaultMessage="No tests recorded"
                    />
                  </span>
                )}
              </div>
            </div>
          );
        })}
      </Tile>
    );
  }

  // Non-environmental: original card layout (vector + clinical unchanged)
  return (
    <Tile className="order-section sample-test-section">
      <h4 className="section-title">
        <FormattedMessage id="label.button.sample" defaultMessage="Sample" />
      </h4>

      {/* Sample Cards — only render regular (non-QC) samples at top level */}
      {samples.map((sample, sampleIndex) =>
        sample.qcMetadata?.qcType ? null : (
          <div key={sampleIndex} className="sample-card">
            <div className="sample-card-header">
              <h5>
                {workflowType === "vector" ? (
                  <>
                    <FormattedMessage
                      id="vector.animalOrganism"
                      defaultMessage="Animal/Organism"
                    />{" "}
                    {sampleIndex + 1}
                  </>
                ) : (
                  <>
                    <FormattedMessage
                      id="label.button.sample"
                      defaultMessage="Sample"
                    />{" "}
                    {sampleIndex + 1}
                  </>
                )}
              </h5>
              {samples.length > 1 && (
                <Link
                  onClick={() => handleRemoveSample(sampleIndex)}
                  disabled={isReadOnly}
                >
                  {workflowType === "vector" ? (
                    <FormattedMessage
                      id="vector.animalOrganism.remove"
                      defaultMessage="Remove Animal/Organism"
                    />
                  ) : (
                    <FormattedMessage
                      id="sample.remove.action"
                      defaultMessage="Remove Sample"
                    />
                  )}
                </Link>
              )}
            </div>

            <Grid>
              <Column lg={8} md={4} sm={4}>
                <Select
                  id={`sampleType-${sampleIndex}`}
                  labelText={intl.formatMessage({
                    id: "sample.type",
                    defaultMessage: "Sample Type",
                  })}
                  value={sample.sampleTypeId || ""}
                  onChange={(e) =>
                    handleSampleTypeChange(sampleIndex, e.target.value)
                  }
                  disabled={isReadOnly}
                >
                  <SelectItem value="" text="" />
                  {sampleTypes.map((type) => (
                    <SelectItem
                      key={type.id}
                      value={type.id}
                      text={type.value}
                    />
                  ))}
                </Select>
              </Column>

              {workflowType === "vector" && (
                <>
                  <Column lg={8} md={4} sm={4}>
                    <Select
                      id={`lifecycleStage-${sampleIndex}`}
                      labelText={intl.formatMessage({
                        id: "vector.lifecycleStage",
                        defaultMessage: "Lifecycle Stage",
                      })}
                      value={sample.vectorFields?.vecLifecycleStage || ""}
                      onChange={(e) =>
                        handleVectorFieldChange(
                          sampleIndex,
                          "vecLifecycleStage",
                          e.target.value,
                        )
                      }
                      disabled={isReadOnly}
                    >
                      <SelectItem value="" text="" />
                      {(lifecycleStagesPerSample[sampleIndex] || []).map(
                        (stage) => (
                          <SelectItem
                            key={stage.id}
                            value={stage.id}
                            text={stage.localizedName || stage.dictEntry}
                          />
                        ),
                      )}
                    </Select>
                  </Column>
                  <Column lg={8} md={4} sm={4}>
                    <Select
                      id={`trapType-${sampleIndex}`}
                      labelText={intl.formatMessage({
                        id: "vector.trapType",
                        defaultMessage: "Trap Type",
                      })}
                      value={sample.vectorFields?.vecTrapTypeId || ""}
                      onChange={(e) =>
                        handleVectorFieldChange(
                          sampleIndex,
                          "vecTrapTypeId",
                          e.target.value,
                        )
                      }
                      disabled={isReadOnly || !sample.sampleTypeId}
                    >
                      <SelectItem value="" text="" />
                      {(trapTypesPerSample[sampleIndex] || []).map((tt) => (
                        <SelectItem
                          key={tt.id}
                          value={String(tt.id)}
                          text={tt.name}
                        />
                      ))}
                    </Select>
                  </Column>
                  <Column lg={8} md={4} sm={4}>
                    <TextInput
                      id={`collectedVolume-${sampleIndex}`}
                      type="number"
                      labelText={intl.formatMessage({
                        id: "vector.collectedVolume",
                        defaultMessage: "Quantity in Pool",
                      })}
                      value={sample.vectorFields?.collectionVolume || ""}
                      onChange={(e) =>
                        handleVectorFieldChange(
                          sampleIndex,
                          "collectionVolume",
                          e.target.value,
                        )
                      }
                      disabled={isReadOnly}
                    />
                  </Column>
                </>
              )}

              {sample.sampleTypeId ? (
                <Column lg={16} md={8} sm={4}>
                  <div className="panels-section">
                    <h6>
                      <FormattedMessage
                        id="sample.orderPanels"
                        defaultMessage="Order Panels"
                      />
                    </h6>
                    <div className="selected-tags">
                      {sample.panels?.map((panel) => (
                        <Tag
                          key={panel.id}
                          type="blue"
                          filter
                          onClose={() =>
                            handleRemovePanel(sampleIndex, panel.id)
                          }
                          disabled={isReadOnly}
                        >
                          {panel.name}
                        </Tag>
                      ))}
                    </div>
                    {getFilteredPanels(sampleIndex).length > 0 ? (
                      <>
                        <Search
                          id={`panelSearch-${sampleIndex}`}
                          labelText=""
                          placeholder={intl.formatMessage({
                            id: "panel.search.placeholder",
                            defaultMessage: "Search panels...",
                          })}
                          value={panelSearchTerms[sampleIndex] || ""}
                          onChange={(e) =>
                            setPanelSearchTerms((prev) => ({
                              ...prev,
                              [sampleIndex]: e.target.value,
                            }))
                          }
                          disabled={isReadOnly}
                          size="sm"
                        />
                        <div className="checkbox-list">
                          {getFilteredPanels(sampleIndex).map((panel) => (
                            <Checkbox
                              key={panel.id}
                              id={`panel-${sampleIndex}-${panel.id}`}
                              labelText={panel.name}
                              checked={isPanelSelected(sampleIndex, panel.id)}
                              onChange={(_, { checked }) =>
                                handlePanelToggle(sampleIndex, panel, checked)
                              }
                              disabled={isReadOnly}
                            />
                          ))}
                        </div>
                      </>
                    ) : loadingPerSample[sampleIndex] ? (
                      <p className="no-items-message">Loading panels...</p>
                    ) : (
                      <p className="no-items-message">
                        <FormattedMessage
                          id="sample.noPanels"
                          defaultMessage="No panels available for this sample type"
                        />
                      </p>
                    )}
                  </div>
                </Column>
              ) : null}

              {sample.sampleTypeId ? (
                <Column lg={16} md={8} sm={4}>
                  <div className="tests-section">
                    <h6>
                      <FormattedMessage
                        id="sample.orderTests"
                        defaultMessage="Order Tests"
                      />
                    </h6>
                    <div className="selected-tags">
                      {sample.tests?.map((test) => (
                        <Tag
                          key={test.id}
                          type="teal"
                          filter
                          onClose={() => handleRemoveTest(sampleIndex, test.id)}
                          disabled={isReadOnly}
                        >
                          {test.name}
                        </Tag>
                      ))}
                    </div>
                    {getFilteredTests(sampleIndex).length > 0 ? (
                      <>
                        <Search
                          id={`testSearch-${sampleIndex}`}
                          labelText=""
                          placeholder={intl.formatMessage({
                            id: "test.search.placeholder",
                            defaultMessage: "Search tests...",
                          })}
                          value={testSearchTerms[sampleIndex] || ""}
                          onChange={(e) =>
                            setTestSearchTerms((prev) => ({
                              ...prev,
                              [sampleIndex]: e.target.value,
                            }))
                          }
                          disabled={isReadOnly}
                          size="sm"
                        />
                        <div className="checkbox-list checkbox-list-scrollable">
                          {getFilteredTests(sampleIndex).map((test) => (
                            <Checkbox
                              key={test.id}
                              id={`test-${sampleIndex}-${test.id}`}
                              labelText={test.name}
                              checked={isTestSelected(sampleIndex, test.id)}
                              onChange={(_, { checked }) =>
                                handleTestToggle(sampleIndex, test, checked)
                              }
                              disabled={isReadOnly}
                            />
                          ))}
                        </div>
                        <span className="test-count-info">
                          <FormattedMessage
                            id="sample.testsCount"
                            defaultMessage="{count} tests available"
                            values={{
                              count: getFilteredTests(sampleIndex).length,
                            }}
                          />
                        </span>
                      </>
                    ) : loadingPerSample[sampleIndex] ? (
                      <p className="no-items-message">Loading tests...</p>
                    ) : (
                      <p className="no-items-message">
                        <FormattedMessage
                          id="sample.noTests"
                          defaultMessage="No tests available for this sample type"
                        />
                      </p>
                    )}
                  </div>
                </Column>
              ) : (
                <Column lg={16} md={8} sm={4}>
                  <p className="select-sample-type-message">
                    <FormattedMessage
                      id="sample.selectType.first"
                      defaultMessage="Select a sample type above to see available panels and tests"
                    />
                  </p>
                </Column>
              )}
            </Grid>
          </div>
        ),
      )}

      <Button
        kind="tertiary"
        size="md"
        onClick={handleAddSample}
        disabled={isReadOnly}
        renderIcon={Add}
      >
        {workflowType === "vector" ? (
          <FormattedMessage
            id="vector.animalOrganism.add"
            defaultMessage="Add Animal/Organism"
          />
        ) : (
          <FormattedMessage
            id="sample.add.action"
            defaultMessage="Add Sample"
          />
        )}
      </Button>
    </Tile>
  );
};

export default SampleTestSection;
