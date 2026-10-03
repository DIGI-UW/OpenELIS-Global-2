import React, { useEffect, useRef, useState } from "react";
import { useIntl, FormattedMessage } from "react-intl";
import {
  Tile,
  Search,
  Tag,
  DismissibleTag,
  Accordion,
  AccordionItem,
} from "@carbon/react";
import { getFromOpenElisServer } from "../../../utils/Utils";

/**
 * Adding tests and panels while collecting.
 *
 * The entry step could choose tests, but once a collector reached Collect the
 * order was fixed: a clinician asking for an extra test at the bedside, or a
 * test that only becomes appropriate when the specimen is in hand, meant
 * walking back to step 1. This offers the same catalogue, scoped to each
 * sample's own type, against the tests the order already holds.
 */
const CollectTestPickerSection = ({ samples, setSamples, isReadOnly }) => {
  const intl = useIntl();
  const componentMounted = useRef(true);
  const fetchedTypesRef = useRef({});
  const [catalogue, setCatalogue] = useState({});
  const [searchTerms, setSearchTerms] = useState({});

  useEffect(() => {
    componentMounted.current = true;
    return () => {
      componentMounted.current = false;
    };
  }, []);

  useEffect(() => {
    samples.forEach((sample) => {
      const sampleTypeId = sample?.sampleTypeId;
      if (!sampleTypeId || fetchedTypesRef.current[sampleTypeId]) {
        return;
      }
      fetchedTypesRef.current[sampleTypeId] = true;
      getFromOpenElisServer(
        `/rest/sample-type-tests?sampleType=${sampleTypeId}`,
        (response) => {
          if (!componentMounted.current || !response) {
            return;
          }
          setCatalogue((prev) => ({
            ...prev,
            [sampleTypeId]: {
              tests: response.tests || [],
              panels: response.panels || [],
            },
          }));
        },
      );
    });
  }, [samples]);

  const updateSample = (sampleIndex, changes) =>
    setSamples(
      samples.map((sample, index) =>
        index === sampleIndex ? { ...sample, ...changes } : sample,
      ),
    );

  const toggleSelection = (sampleIndex, key, item) => {
    const sample = samples[sampleIndex];
    const current = sample[key] || [];
    const already = current.some(
      (entry) => String(entry.id) === String(item.id),
    );
    updateSample(sampleIndex, {
      [key]: already
        ? current.filter((entry) => String(entry.id) !== String(item.id))
        : [...current, { id: item.id, name: item.name }],
    });
  };

  const matching = (items, term) =>
    term
      ? items.filter((item) =>
          item.name?.toLowerCase().includes(term.toLowerCase()),
        )
      : items;

  const collectableSamples = samples
    .map((sample, index) => ({ sample, index }))
    .filter(({ sample }) => sample.sampleTypeId && !sample.sampleRejected);

  if (collectableSamples.length === 0) {
    return null;
  }

  return (
    <Tile className="order-section">
      <h4 className="section-title">
        <FormattedMessage
          id="collect.addTests.title"
          defaultMessage="Add Tests or Panels"
        />
      </h4>
      <p className="helper-text">
        <FormattedMessage
          id="collect.addTests.helper"
          defaultMessage="Add a test the clinician asked for after the order was entered, or one that only becomes appropriate with the specimen in hand."
        />
      </p>

      <Accordion>
        {collectableSamples.map(({ sample, index }) => {
          const options = catalogue[sample.sampleTypeId] || {
            tests: [],
            panels: [],
          };
          const term = searchTerms[index] || "";
          return (
            <AccordionItem
              key={sample.sampleItemId || `collect-picker-${index}`}
              title={`${sample.sampleTypeName || intl.formatMessage({ id: "sample.fallback.name", defaultMessage: "Sample" })} — ${(sample.tests?.length || 0) + (sample.panels?.length || 0)}`}
            >
              <div className="selected-tags">
                {(sample.panels || []).map((panel) => (
                  <DismissibleTag
                    key={`panel-${panel.id}`}
                    type="blue"
                    text={panel.name}
                    disabled={isReadOnly}
                    onClose={() => toggleSelection(index, "panels", panel)}
                    dismissTooltipLabel={intl.formatMessage(
                      {
                        id: "common.removeSelection",
                        defaultMessage: "Remove {name}",
                      },
                      { name: panel.name },
                    )}
                  />
                ))}
                {(sample.tests || []).map((test) => (
                  <DismissibleTag
                    key={`test-${test.id}`}
                    type="teal"
                    text={test.name}
                    disabled={isReadOnly}
                    onClose={() => toggleSelection(index, "tests", test)}
                    dismissTooltipLabel={intl.formatMessage(
                      {
                        id: "common.removeSelection",
                        defaultMessage: "Remove {name}",
                      },
                      { name: test.name },
                    )}
                  />
                ))}
              </div>

              <Search
                id={`collectTestSearch-${index}`}
                labelText={intl.formatMessage({
                  id: "test.search.placeholder",
                  defaultMessage: "Search tests...",
                })}
                placeholder={intl.formatMessage({
                  id: "test.search.placeholder",
                  defaultMessage: "Search tests...",
                })}
                value={term}
                onChange={(e) =>
                  setSearchTerms((prev) => ({
                    ...prev,
                    [index]: e.target.value,
                  }))
                }
                disabled={isReadOnly}
                size="sm"
              />

              <div className="available-tags">
                {matching(options.panels, term).map((panel) => (
                  <Tag
                    key={`available-panel-${panel.id}`}
                    type="outline"
                    onClick={() =>
                      !isReadOnly && toggleSelection(index, "panels", panel)
                    }
                  >
                    {panel.name}
                  </Tag>
                ))}
                {matching(options.tests, term).map((test) => (
                  <Tag
                    key={`available-test-${test.id}`}
                    type="outline"
                    onClick={() =>
                      !isReadOnly && toggleSelection(index, "tests", test)
                    }
                  >
                    {test.name}
                  </Tag>
                ))}
              </div>
            </AccordionItem>
          );
        })}
      </Accordion>
    </Tile>
  );
};

export default CollectTestPickerSection;
