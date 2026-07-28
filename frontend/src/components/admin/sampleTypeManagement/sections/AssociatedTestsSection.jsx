import React, { useCallback, useEffect, useState } from "react";
import {
  Button,
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
  TextInput,
} from "@carbon/react";
import { Add, TrashCan } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import useDomains from "../../../common/useDomains";
import {
  deleteFromOpenElisServer,
  getFromOpenElisServer,
  putToOpenElisServer,
} from "../../../utils/Utils";

/**
 * Associated Tests (OGC-296) — the sample-type side of the bidirectional
 * test↔sample-type link. Lists the tests currently linked to this sample type
 * and lets the admin search the catalog (optionally filtered by domain) to add
 * more, or remove existing links. The test side of the same link lives in the
 * Test Catalog editor's Basic Info sample-types multi-select.
 */
function AssociatedTestsSection({ sampleTypeId, onChange }) {
  const intl = useIntl();
  const domains = useDomains();

  const [linked, setLinked] = useState([]);
  const [linkedLoading, setLinkedLoading] = useState(true);
  const [candidates, setCandidates] = useState([]);
  const [search, setSearch] = useState("");
  const [domainFilter, setDomainFilter] = useState("");
  const [selectedTestId, setSelectedTestId] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState(null);

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
    const params = new URLSearchParams();
    if (search.trim()) {
      params.set("search", search.trim());
    }
    if (domainFilter) {
      params.set("domain", domainFilter);
    }
    const qs = params.toString();
    getFromOpenElisServer(
      `/rest/sample-types/${sampleTypeId}/associable-tests${qs ? `?${qs}` : ""}`,
      (response) => {
        setCandidates(Array.isArray(response) ? response : []);
      },
    );
  }, [sampleTypeId, search, domainFilter]);

  useEffect(() => {
    loadLinked();
  }, [loadLinked]);

  useEffect(() => {
    loadCandidates();
  }, [loadCandidates]);

  const addTest = useCallback(() => {
    if (!selectedTestId) {
      return;
    }
    setBusy(true);
    setError(null);
    putToOpenElisServer(
      `/rest/sample-types/${sampleTypeId}/tests/${selectedTestId}`,
      JSON.stringify({}),
      (status) => {
        setBusy(false);
        if (status === 200) {
          setSelectedTestId("");
          loadLinked();
          loadCandidates();
        } else {
          setError(
            intl.formatMessage({ id: "label.sampleType.tests.addError" }),
          );
        }
      },
    );
  }, [sampleTypeId, selectedTestId, loadLinked, loadCandidates, intl]);

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

      {/* Add-a-test controls: search + domain filter + pick + add */}
      <div
        style={{
          display: "flex",
          gap: "var(--cds-spacing-04)",
          alignItems: "flex-end",
          flexWrap: "wrap",
        }}
      >
        <TextInput
          id="assoc-test-search"
          labelText={intl.formatMessage({
            id: "label.sampleType.tests.search",
          })}
          placeholder={intl.formatMessage({
            id: "label.sampleType.tests.searchPlaceholder",
          })}
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          style={{ maxWidth: "18rem" }}
        />
        <Select
          id="assoc-test-domain"
          labelText={intl.formatMessage({
            id: "label.sampleType.filterDomain",
          })}
          value={domainFilter}
          onChange={(e) => setDomainFilter(e.target.value)}
          style={{ maxWidth: "12rem" }}
        >
          <SelectItem
            value=""
            text={intl.formatMessage({
              id: "placeholder.sampleType.filter.domain",
              defaultMessage: "All domains",
            })}
          />
          {domains.map((d) => (
            <SelectItem
              key={d.id}
              value={d.id}
              text={intl.formatMessage({ id: d.labelKey })}
            />
          ))}
        </Select>
        <Select
          id="assoc-test-picker"
          labelText={intl.formatMessage({ id: "label.sampleType.tests.pick" })}
          value={selectedTestId}
          onChange={(e) => setSelectedTestId(e.target.value)}
          style={{ maxWidth: "24rem" }}
        >
          <SelectItem
            value=""
            text={intl.formatMessage({
              id: "label.sampleType.tests.pickPlaceholder",
            })}
          />
          {candidates.map((t) => (
            <SelectItem key={t.id} value={t.id} text={t.name} />
          ))}
        </Select>
        <Button
          id="assoc-test-add"
          kind="primary"
          size="md"
          renderIcon={Add}
          disabled={!selectedTestId || busy}
          onClick={addTest}
        >
          <FormattedMessage id="label.sampleType.tests.add" />
        </Button>
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
                  <Button
                    kind="ghost"
                    size="sm"
                    hasIconOnly
                    renderIcon={TrashCan}
                    iconDescription={intl.formatMessage({
                      id: "label.sampleType.tests.remove",
                    })}
                    disabled={busy}
                    onClick={() => removeTest(test.id)}
                  />
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
