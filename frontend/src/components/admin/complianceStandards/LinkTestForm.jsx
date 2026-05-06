import React, { useEffect, useMemo, useState } from "react";
import { Stack, Tile, Tag, TextInput, Button } from "@carbon/react";
import { Close } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer } from "../../utils/Utils";
import MultiLimitForm from "./MultiLimitForm";
import SelectMapForm from "./SelectMapForm";

function LinkTestForm({
  standardSampleTypes,
  alreadyLinkedTestIds,
  onLinkNumeric,
  onLinkSelect,
  onCancel,
}) {
  const intl = useIntl();
  const [allTests, setAllTests] = useState([]);
  const [selectedST, setSelectedST] = useState(
    new Set(standardSampleTypes || []),
  );
  const [selectedTest, setSelectedTest] = useState(null);
  const [searchText, setSearchText] = useState("");

  useEffect(() => {
    let mounted = true;
    getFromOpenElisServer("/rest/compliance/test-catalog", (data) => {
      if (mounted && Array.isArray(data)) {
        setAllTests(data);
      }
    });
    return () => {
      mounted = false;
    };
  }, []);

  const toggleST = (st) => {
    setSelectedST((prev) => {
      const next = new Set(prev);
      if (next.has(st)) next.delete(st);
      else next.add(st);
      return next;
    });
  };

  const filteredTests = useMemo(() => {
    const linked = new Set(alreadyLinkedTestIds || []);
    const q = searchText.trim().toLowerCase();
    return (
      (allTests || [])
        .filter((t) => !linked.has(String(t.id)))
        .filter((t) => {
          if (selectedST.size === 0) return true;
          const types = t.sampleTypes || [];
          return types.length === 0 || types.some((st) => selectedST.has(st));
        })
        .filter((t) => {
          if (!q) return true;
          return (
            (t.value || "").toLowerCase().includes(q) ||
            (t.code || "").toLowerCase().includes(q) ||
            (t.loinc || "").toLowerCase().includes(q)
          );
        })
        // FR-4-003 Step 2: cap at 12 results.
        .slice(0, 12)
    );
  }, [allTests, searchText, alreadyLinkedTestIds, selectedST]);

  const handleSaveNumeric = (limits) => onLinkNumeric(selectedTest, limits);
  const handleSaveSelect = (valueMap) => onLinkSelect(selectedTest, valueMap);

  return (
    <Tile style={{ padding: "1rem", background: "var(--cds-layer-02)" }}>
      <h6 style={{ marginBottom: "1rem" }}>
        <FormattedMessage
          id="compliance.linkTest.heading"
          defaultMessage="Link Test to Group"
        />
      </h6>

      {/* Step 1: sample-type filter chips */}
      {standardSampleTypes &&
        standardSampleTypes.length > 0 &&
        !selectedTest && (
          <div style={{ marginBottom: "1rem" }}>
            <div
              style={{
                fontSize: "0.6875rem",
                fontWeight: 700,
                textTransform: "uppercase",
                letterSpacing: "0.05em",
                color: "#0f62fe",
                marginBottom: "0.375rem",
              }}
            >
              <FormattedMessage
                id="compliance.linkTest.step1"
                defaultMessage="Step 1 — Filter by Sample Type"
              />
            </div>
            <p
              style={{
                fontSize: "0.75rem",
                color: "var(--cds-text-02)",
                marginBottom: "0.5rem",
              }}
            >
              <FormattedMessage
                id="compliance.linkTest.filterByST"
                defaultMessage="Filter by sample type:"
              />
            </p>
            <Stack
              orientation="horizontal"
              gap={2}
              style={{ flexWrap: "wrap" }}
            >
              {standardSampleTypes.map((st) => {
                const active = selectedST.has(st);
                return (
                  <button
                    key={st}
                    type="button"
                    onClick={() => toggleST(st)}
                    style={{
                      padding: "0.25rem 0.75rem",
                      borderRadius: "1rem",
                      fontSize: "0.8125rem",
                      border: active
                        ? "2px solid #0f62fe"
                        : "1px solid #c6c6c6",
                      background: active ? "#d0e2ff" : "#fff",
                      color: active ? "#0043ce" : "#525252",
                      cursor: "pointer",
                      fontWeight: active ? 600 : 400,
                    }}
                  >
                    {st}
                  </button>
                );
              })}
            </Stack>
          </div>
        )}

      {/* Step 2: typeahead test search */}
      {!selectedTest && (
        <>
          <div
            style={{
              fontSize: "0.6875rem",
              fontWeight: 700,
              textTransform: "uppercase",
              letterSpacing: "0.05em",
              color: "#0f62fe",
              marginBottom: "0.375rem",
            }}
          >
            <FormattedMessage
              id="compliance.linkTest.step2"
              defaultMessage="Step 2 — Search & Select Test"
            />
          </div>
          <TextInput
            id="link-test-search"
            labelText={intl.formatMessage({
              id: "compliance.linkTest.searchTest",
              defaultMessage: "Search test catalog",
            })}
            placeholder={intl.formatMessage({
              id: "compliance.linkTest.searchPlaceholder",
              defaultMessage: "Type to search by name, code, or LOINC…",
            })}
            value={searchText}
            onChange={(e) => setSearchText(e.target.value)}
          />
          {searchText && (
            <div
              style={{
                marginTop: "0.5rem",
                border: "1px solid var(--cds-border-subtle)",
                borderRadius: "4px",
                overflow: "hidden",
                background: "#fff",
                maxHeight: "320px",
                overflowY: "auto",
              }}
            >
              {filteredTests.length === 0 ? (
                <p
                  style={{
                    padding: "0.75rem 1rem",
                    fontSize: "0.875rem",
                    color: "var(--cds-text-placeholder)",
                  }}
                >
                  <FormattedMessage
                    id="compliance.linkTest.noResults"
                    defaultMessage="No matching tests found."
                  />
                </p>
              ) : (
                filteredTests.map((test) => (
                  <button
                    key={test.id}
                    type="button"
                    onClick={() => {
                      setSelectedTest(test);
                      setSearchText("");
                    }}
                    style={{
                      width: "100%",
                      display: "flex",
                      alignItems: "center",
                      justifyContent: "space-between",
                      padding: "0.625rem 1rem",
                      fontSize: "0.875rem",
                      border: "none",
                      borderBottom: "1px solid var(--cds-border-subtle)",
                      background: "transparent",
                      cursor: "pointer",
                      textAlign: "left",
                    }}
                  >
                    <span>
                      <span style={{ fontWeight: 500 }}>{test.value}</span>
                      {test.code && (
                        <span
                          style={{
                            marginLeft: "0.5rem",
                            fontSize: "0.75rem",
                            color: "var(--cds-text-02)",
                          }}
                        >
                          {test.code}
                        </span>
                      )}
                      {test.sampleTypes && test.sampleTypes.length > 0 && (
                        <span
                          style={{
                            marginLeft: "0.5rem",
                            fontSize: "0.75rem",
                            color: "var(--cds-text-02)",
                          }}
                        >
                          {test.sampleTypes.join(", ")}
                        </span>
                      )}
                    </span>
                    {test.resultType === "select" && (
                      <Tag
                        size="sm"
                        type="purple"
                        style={{ marginLeft: "0.5rem", flexShrink: 0 }}
                      >
                        <FormattedMessage
                          id="compliance.linkTest.selectListBadge"
                          defaultMessage="Select List"
                        />
                      </Tag>
                    )}
                  </button>
                ))
              )}
            </div>
          )}
          <Button
            kind="ghost"
            size="sm"
            style={{ marginTop: "1rem" }}
            onClick={onCancel}
          >
            <FormattedMessage
              id="label.button.cancel"
              defaultMessage="Cancel"
            />
          </Button>
        </>
      )}

      {/* Step 3: chosen test header + per-type form */}
      {selectedTest && (
        <>
          <div
            style={{
              display: "flex",
              alignItems: "center",
              gap: "0.75rem",
              marginBottom: "1rem",
              padding: "0.5rem 0.75rem",
              background: "var(--cds-layer-01)",
              borderRadius: "4px",
              border: "1px solid var(--cds-border-subtle)",
              flexWrap: "wrap",
            }}
          >
            <span style={{ fontWeight: 500 }}>{selectedTest.value}</span>
            {selectedTest.code && (
              <span
                style={{ fontSize: "0.75rem", color: "var(--cds-text-02)" }}
              >
                {selectedTest.code}
              </span>
            )}
            {selectedTest.sampleTypes &&
              selectedTest.sampleTypes.length > 0 && (
                <Stack orientation="horizontal" gap={1}>
                  {selectedTest.sampleTypes.map((st) => (
                    <Tag key={st} size="sm" type="blue">
                      {st}
                    </Tag>
                  ))}
                </Stack>
              )}
            {selectedTest.resultType === "select" && (
              <Tag size="sm" type="purple">
                <FormattedMessage
                  id="compliance.linkTest.selectListBadge"
                  defaultMessage="Select List"
                />
              </Tag>
            )}
            <Button
              kind="ghost"
              size="sm"
              renderIcon={Close}
              hasIconOnly
              iconDescription={intl.formatMessage({
                id: "compliance.linkTest.clearSelection",
                defaultMessage: "Clear selection",
              })}
              onClick={() => setSelectedTest(null)}
              style={{ marginLeft: "auto" }}
            />
          </div>

          <div
            style={{
              fontSize: "0.6875rem",
              fontWeight: 700,
              textTransform: "uppercase",
              letterSpacing: "0.05em",
              color: "#0f62fe",
              marginBottom: "0.375rem",
            }}
          >
            {selectedTest.resultType === "select" ? (
              <FormattedMessage
                id="compliance.linkTest.step3Select"
                defaultMessage="Step 3 — Compliance Value Mapping"
              />
            ) : (
              <FormattedMessage
                id="compliance.linkTest.step3Numeric"
                defaultMessage="Step 3 — Configure Limits"
              />
            )}
          </div>

          {selectedTest.resultType === "select" ? (
            (selectedTest.selectOptions || []).length === 0 ? (
              <p
                style={{
                  fontSize: "0.875rem",
                  color: "var(--cds-text-placeholder)",
                  fontStyle: "italic",
                }}
              >
                <FormattedMessage
                  id="compliance.linkTest.selectNoOptions"
                  defaultMessage="This test is select-list but has no options configured in the test catalog. Add result options in test management before linking it to a compliance standard."
                />
              </p>
            ) : (
              <SelectMapForm
                testCode={selectedTest.code || selectedTest.id}
                testName={selectedTest.value}
                options={selectedTest.selectOptions}
                existingMap={null}
                onSave={handleSaveSelect}
                onCancel={() => setSelectedTest(null)}
              />
            )
          ) : (
            <MultiLimitForm
              testCode={selectedTest.code || selectedTest.id}
              testName={selectedTest.value}
              unit=""
              existingLimits={null}
              onSave={handleSaveNumeric}
              onCancel={() => setSelectedTest(null)}
            />
          )}
        </>
      )}
    </Tile>
  );
}

export default LinkTestForm;
