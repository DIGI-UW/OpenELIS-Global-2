import React, { useContext, useEffect, useRef, useState } from "react";
import {
  Stack,
  Select,
  SelectItem,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  Button,
  Loading,
  InlineNotification,
} from "@carbon/react";
import { ArrowUp, ArrowDown, Draggable } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  putToOpenElisServer,
} from "../../../utils/Utils";
import { NotificationContext } from "../../../layout/Layout";

/**
 * OGC-949 M12 / OGC-983..985 — Display Order section.
 *
 * Reorders the tests within a sample type. The position is sample-type-scoped
 * (a test can sit differently in Serum vs Plasma), so this section is keyed off
 * a sample-type picker rather than the editor's testId. Reordering is available
 * by drag (native HTML5, the SortableList idiom) or by Arrow Up/Down buttons
 * (FR-009 keyboard alternative), and auto-saves on every change to
 * sampletype_test.display_order (OGC-985).
 */
const DisplayOrderSection = () => {
  const intl = useIntl();
  const { addNotification, setNotificationVisible } =
    useContext(NotificationContext);

  const [sampleTypes, setSampleTypes] = useState([]);
  const [selectedTypeId, setSelectedTypeId] = useState("");
  const [tests, setTests] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [saving, setSaving] = useState(false);
  const dragIndex = useRef(null);

  // Load the sample-type picker once; auto-select the first so the list renders.
  useEffect(() => {
    getFromOpenElisServer("/rest/test-catalog/sample-types", (res) => {
      if (!res) {
        setLoading(false);
        setError(true);
        return;
      }
      setSampleTypes(res);
      if (res.length > 0) {
        setSelectedTypeId(res[0].id);
      } else {
        setLoading(false);
      }
    });
  }, []);

  const loadOrder = (sampleTypeId) => {
    if (!sampleTypeId) {
      return;
    }
    setLoading(true);
    setError(false);
    getFromOpenElisServer(
      `/rest/test-catalog/sample-types/${sampleTypeId}/test-order`,
      (res) => {
        setLoading(false);
        if (!res) {
          setError(true);
          return;
        }
        setTests(res.tests || []);
      },
    );
  };

  useEffect(() => {
    loadOrder(selectedTypeId);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [selectedTypeId]);

  const persist = (ordered) => {
    setSaving(true);
    const payload = {
      items: ordered.map((t) => ({
        testId: t.testId,
        displayOrder: t.displayOrder,
      })),
    };
    putToOpenElisServer(
      `/rest/test-catalog/sample-types/${selectedTypeId}/test-order`,
      JSON.stringify(payload),
      (status) => {
        setSaving(false);
        setNotificationVisible(true);
        if (status === 200) {
          addNotification({
            kind: "success",
            title: intl.formatMessage({
              id: "label.testCatalog.section.display-order",
            }),
            message: intl.formatMessage({
              id: "label.testCatalog.displayOrder.saved",
            }),
          });
        } else {
          addNotification({
            kind: "error",
            title: intl.formatMessage({ id: "error.title" }),
            message: intl.formatMessage({ id: "server.error.msg" }),
          });
          // Reload to discard the optimistic reorder the server rejected.
          loadOrder(selectedTypeId);
        }
      },
    );
  };

  // Renumber to match the new visual order (1-based), then auto-save.
  const applyOrder = (next) => {
    const renumbered = next.map((t, i) => ({ ...t, displayOrder: i + 1 }));
    setTests(renumbered);
    persist(renumbered);
  };

  const move = (index, dir) => {
    const ni = index + dir;
    if (ni < 0 || ni >= tests.length) {
      return;
    }
    const next = [...tests];
    [next[index], next[ni]] = [next[ni], next[index]];
    applyOrder(next);
  };

  const onDrop = (dropIndex) => (e) => {
    e.preventDefault();
    const from = dragIndex.current;
    dragIndex.current = null;
    if (from === null || from === dropIndex) {
      return;
    }
    const next = [...tests];
    const [moved] = next.splice(from, 1);
    next.splice(dropIndex, 0, moved);
    applyOrder(next);
  };

  return (
    <Stack gap={6} data-testid="display-order-section">
      <p>
        <FormattedMessage id="label.testCatalog.displayOrder.intro" />
      </p>
      <Select
        id="display-order-sample-type"
        data-cy="sample-type-picker"
        labelText={intl.formatMessage({
          id: "label.testCatalog.displayOrder.pickSampleType",
        })}
        value={selectedTypeId}
        onChange={(e) => setSelectedTypeId(e.target.value)}
      >
        {sampleTypes.map((t) => (
          <SelectItem key={t.id} value={t.id} text={t.name} />
        ))}
      </Select>

      {loading ? (
        <Loading
          description={intl.formatMessage({ id: "label.loading" })}
          withOverlay={false}
        />
      ) : error ? (
        <InlineNotification
          kind="error"
          lowContrast
          hideCloseButton
          title={intl.formatMessage({ id: "error.title" })}
          subtitle={intl.formatMessage({
            id: "label.testCatalog.displayOrder.loadError",
          })}
        />
      ) : tests.length === 0 ? (
        <InlineNotification
          kind="info"
          lowContrast
          hideCloseButton
          title={intl.formatMessage({
            id: "label.testCatalog.displayOrder.empty",
          })}
        />
      ) : (
        <Table size="lg" aria-label="display-order">
          <TableHead>
            <TableRow>
              <TableHeader>
                <FormattedMessage id="label.testCatalog.displayOrder.col.position" />
              </TableHeader>
              <TableHeader>
                <FormattedMessage id="label.testCatalog.displayOrder.col.test" />
              </TableHeader>
              <TableHeader>{""}</TableHeader>
            </TableRow>
          </TableHead>
          <TableBody>
            {tests.map((t, i) => (
              <TableRow
                key={t.testId}
                data-testid={`order-row-${t.testId}`}
                draggable
                onDragStart={() => {
                  dragIndex.current = i;
                }}
                onDragOver={(e) => e.preventDefault()}
                onDrop={onDrop(i)}
              >
                <TableCell>{t.displayOrder}</TableCell>
                <TableCell>{t.testName}</TableCell>
                <TableCell>
                  <Draggable
                    aria-label={intl.formatMessage({
                      id: "label.testCatalog.displayOrder.dragHandle",
                    })}
                    size={16}
                  />
                  <Button
                    kind="ghost"
                    size="sm"
                    hasIconOnly
                    disabled={saving || i === 0}
                    renderIcon={ArrowUp}
                    data-testid={`move-up-${t.testId}`}
                    iconDescription={intl.formatMessage({
                      id: "label.testCatalog.displayOrder.moveUp",
                    })}
                    onClick={() => move(i, -1)}
                  />
                  <Button
                    kind="ghost"
                    size="sm"
                    hasIconOnly
                    disabled={saving || i === tests.length - 1}
                    renderIcon={ArrowDown}
                    data-testid={`move-down-${t.testId}`}
                    iconDescription={intl.formatMessage({
                      id: "label.testCatalog.displayOrder.moveDown",
                    })}
                    onClick={() => move(i, 1)}
                  />
                </TableCell>
              </TableRow>
            ))}
          </TableBody>
        </Table>
      )}
    </Stack>
  );
};

export default DisplayOrderSection;
