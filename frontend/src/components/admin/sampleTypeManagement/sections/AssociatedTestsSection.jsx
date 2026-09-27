import React, { useCallback, useEffect, useState } from "react";
import {
  ComboBox,
  InlineNotification,
  Loading,
  Select,
  SelectItem,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
  Tag,
} from "@carbon/react";
import { TrashCan } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  deleteFromOpenElisServer,
  getFromOpenElisServer,
  putToOpenElisServer,
} from "../../../utils/Utils";

/**
 * Associated Tests (OGC-296) — the sample-type side of the bidirectional
 * test↔sample-type link. Lists the tests linked to this sample type, and adds
 * more through a single autocomplete: typing filters the candidate tests, which
 * are already constrained to this sample type's domain (D-030, applied by the
 * backend) and optionally narrowed to the tests currently on another sample
 * type. The test side of the same link lives in the Test Catalog editor's
 * Basic Info sample-types multi-select.
 */
function AssociatedTestsSection({ sampleTypeId, onChange }) {
  const intl = useIntl();

  const [linked, setLinked] = useState([]);
  const [linkedLoading, setLinkedLoading] = useState(true);
  const [candidates, setCandidates] = useState([]);
  const [sampleTypes, setSampleTypes] = useState([]);
  const [sampleTypeFilter, setSampleTypeFilter] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);
  // Bump to force the ComboBox to clear its selection after an add.
  const [comboKey, setComboKey] = useState(0);

  const loadLinked = useCallback(() => {
    setLinkedLoading(true);
    getFromOpenElisServer(
      `/rest/AllTestsForSampleTypeProvider?sampleTypeId=${encodeURIComponent(sampleTypeId)}`,
      (response) => {
        const tests =
          response && Array.isArray(response.tests) ? response.tests : [];
        setLinked(tests);
        setLinkedLoading(false);
        if (onChange) {
          onChange(tests);
        }
      },
    );
  }, [sampleTypeId, onChange]);

  const loadCandidates = useCallback(() => {
    const qs = sampleTypeFilter
      ? `?sampleTypeFilter=${encodeURIComponent(sampleTypeFilter)}`
      : "";
    getFromOpenElisServer(
      `/rest/sample-types/${sampleTypeId}/associable-tests${qs}`,
      (response) => {
        setCandidates(Array.isArray(response) ? response : []);
      },
    );
  }, [sampleTypeId, sampleTypeFilter]);

  // Sample types feed the optional "filter by sample type" selector (exclude
  // this one — it can't narrow candidates by itself).
  useEffect(() => {
    getFromOpenElisServer("/rest/sample-types", (response) => {
      const data =
        response && response.success && Array.isArray(response.data)
          ? response.data
          : [];
      setSampleTypes(data.filter((s) => String(s.id) !== String(sampleTypeId)));
    });
  }, [sampleTypeId]);

  useEffect(() => {
    loadLinked();
  }, [loadLinked]);

  useEffect(() => {
    loadCandidates();
  }, [loadCandidates]);

  const addTest = useCallback(
    (testId) => {
      if (!testId) {
        return;
      }
      setBusy(true);
      setError(null);
      putToOpenElisServer(
        `/rest/sample-types/${sampleTypeId}/tests/${testId}`,
        JSON.stringify({}),
        (status) => {
          setBusy(false);
          setComboKey((k) => k + 1);
          if (status === 200) {
            loadLinked();
            loadCandidates();
          } else {
            setError(
              intl.formatMessage({ id: "label.sampleType.tests.addError" }),
            );
          }
        },
      );
    },
    [sampleTypeId, loadLinked, loadCandidates, intl],
  );

  const removeTest = useCallback(
    (testId) => {
      setBusy(true);
      setError(null);
      deleteFromOpenElisServer(
        `/rest/sample-types/${sampleTypeId}/tests/${testId}`,
        (status) => {
          setBusy(false);
          if (status === 200) {
            loadLinked();
            loadCandidates();
          } else {
            setError(
              intl.formatMessage({ id: "label.sampleType.tests.removeError" }),
            );
          }
        },
      );
    },
    [sampleTypeId, loadLinked, loadCandidates, intl],
  );

  return (
    <Stack gap={6} data-testid="sampleType-associatedTests-section">
      <p style={{ fontSize: "14px", color: "var(--cds-text-secondary)" }}>
        <FormattedMessage id="label.sampleType.tests.manageIntro" />
      </p>

      {error && (
        <InlineNotification
          kind="error"
          title={error}
          lowContrast
          hideCloseButton={false}
          onCloseButtonClick={() => setError(null)}
        />
      )}

      {/* Add a test: optional "on this sample type" narrowing sits directly
          beside a single autocomplete over the domain-compatible candidates.
          Fixed flex bases (no grow) keep the two controls adjacent instead of
          stretching apart across the row. */}
      <div
        style={{
          display: "flex",
          gap: "var(--cds-spacing-05)",
          alignItems: "flex-end",
          flexWrap: "wrap",
        }}
      >
        <div style={{ flex: "0 0 16rem" }}>
          <Select
            id="assoc-test-sampletype-filter"
            labelText={intl.formatMessage({
              id: "label.sampleType.tests.filterBySampleType",
            })}
            value={sampleTypeFilter}
            onChange={(e) => setSampleTypeFilter(e.target.value)}
          >
            <SelectItem
              value=""
              text={intl.formatMessage({
                id: "label.sampleType.tests.filterBySampleType.all",
              })}
            />
            {sampleTypes.map((s) => (
              <SelectItem key={s.id} value={String(s.id)} text={s.name} />
            ))}
          </Select>
        </div>

        <div style={{ flex: "0 0 26rem", maxWidth: "100%" }}>
          <ComboBox
            key={comboKey}
            id="assoc-test-combo"
            titleText={intl.formatMessage({ id: "label.sampleType.tests.add" })}
            placeholder={intl.formatMessage({
              id: "label.sampleType.tests.searchPlaceholder",
            })}
            items={candidates}
            itemToString={(item) => (item ? item.name : "")}
            shouldFilterItem={({ item, inputValue }) =>
              !inputValue ||
              (item?.name || "")
                .toLowerCase()
                .includes(inputValue.toLowerCase())
            }
            disabled={busy}
            onChange={({ selectedItem }) => {
              if (selectedItem) {
                addTest(selectedItem.id);
              }
            }}
          />
        </div>
      </div>

      {/* Currently-linked tests with remove */}
      {linkedLoading ? (
        <div
          style={{
            display: "flex",
            alignItems: "center",
            gap: "var(--cds-spacing-03)",
          }}
        >
          <Loading small withOverlay={false} />
          <FormattedMessage id="label.sampleType.tests.loading" />
        </div>
      ) : linked.length === 0 ? (
        <p style={{ color: "var(--cds-text-secondary)", fontSize: "14px" }}>
          <FormattedMessage id="label.sampleType.tests.none" />
        </p>
      ) : (
        <Table size="sm">
          <TableHead>
            <TableRow>
              <TableHeader>
                <FormattedMessage
                  id="label.test.name"
                  defaultMessage="Test Name"
                />
              </TableHeader>
              <TableHeader>
                <FormattedMessage id="label.sampleType.status" />
              </TableHeader>
              <TableHeader>
                <FormattedMessage id="label.sampleType.actions" />
              </TableHeader>
            </TableRow>
          </TableHead>
          <TableBody>
            {linked.map((test) => (
              <TableRow key={test.id}>
                <TableCell>{test.name}</TableCell>
                <TableCell>
                  <Tag type={test.isActive ? "green" : "gray"} size="sm">
                    {test.isActive ? (
                      <FormattedMessage id="label.active" />
                    ) : (
                      <FormattedMessage id="label.inactive" />
                    )}
                  </Tag>
                </TableCell>
                <TableCell>
                  <button
                    type="button"
                    aria-label={intl.formatMessage({
                      id: "label.sampleType.tests.remove",
                    })}
                    title={intl.formatMessage({
                      id: "label.sampleType.tests.remove",
                    })}
                    disabled={busy}
                    onClick={() => removeTest(test.id)}
                    style={{
                      background: "none",
                      border: "none",
                      cursor: busy ? "not-allowed" : "pointer",
                      padding: "var(--cds-spacing-02)",
                      color: "var(--cds-icon-primary)",
                    }}
                  >
                    <TrashCan />
                  </button>
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}
    </Stack>
  );
}

export default AssociatedTestsSection;
