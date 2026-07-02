import React, { useContext, useEffect, useState } from "react";
import { useParams, useHistory, useLocation } from "react-router-dom";
import {
  Grid,
  Column,
  Section,
  Heading,
  Tile,
  Button,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  Tag,
  Loading,
  InlineNotification,
  ContentSwitcher,
  Switch,
} from "@carbon/react";
import { Add, Edit, TrashCan } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import { getFromOpenElisServer, putToOpenElisServer } from "../../utils/Utils";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import { NotificationContext } from "../../layout/Layout";
import RangeModal from "./sections/RangeModal";
import StorageSection from "./sections/StorageSection";

/**
 * OGC-1112 (FR-7..14) — Edit related tests together.
 *
 * A combined editor over the tests selected on the catalog list (comma-separated
 * ids in the URL). Identity + LOINC are shown read-only per test (FR-12) — never
 * harmonized, to protect import routing. Shared configuration is edited once and
 * written to every selected test (FR-11); this slice covers the motivating
 * shared section, Ranges. When the selected tests' current ranges differ, a
 * banner warns that saving sets them all to the values below (FR-10).
 */
const rangeSignature = (ranges) =>
  JSON.stringify(
    (ranges || [])
      .map((r) => ({
        componentId: r.componentId || null,
        gender: r.gender || null,
        minAge: r.minAge ?? null,
        maxAge: r.maxAge ?? null,
        lowNormal: r.lowNormal ?? null,
        highNormal: r.highNormal ?? null,
        lowCritical: r.lowCritical ?? null,
        highCritical: r.highCritical ?? null,
      }))
      .sort((a, b) => JSON.stringify(a).localeCompare(JSON.stringify(b))),
  );

const CombinedTestEditor = () => {
  const intl = useIntl();
  const history = useHistory();
  const location = useLocation();
  const { ids } = useParams();
  const base = location.pathname.startsWith("/admin")
    ? "/admin"
    : "/MasterListsPage";
  const { addNotification, setNotificationVisible } =
    useContext(NotificationContext);

  const testIds = (ids || "").split(",").filter(Boolean);

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [saving, setSaving] = useState(false);
  const [summaries, setSummaries] = useState([]);
  const [ranges, setRanges] = useState([]);
  const [components, setComponents] = useState([]);
  const [differs, setDiffers] = useState(false);
  const [editingIndex, setEditingIndex] = useState(null);
  const [sharedSection, setSharedSection] = useState("ranges");

  useEffect(() => {
    if (testIds.length === 0) {
      return;
    }
    setLoading(true);
    setError(false);
    getFromOpenElisServer(
      `/rest/test-catalog/group/summary?ids=${encodeURIComponent(ids)}`,
      (res) => {
        if (!res || !Array.isArray(res)) {
          setLoading(false);
          setError(true);
          return;
        }
        setSummaries(res);
        // Pull each test's ranges to decide shared-vs-differs (FR-9/10).
        let pending = testIds.length;
        const perTest = {};
        testIds.forEach((id) => {
          getFromOpenElisServer(
            `/rest/test-catalog/tests/${id}/ranges`,
            (r) => {
              perTest[id] = (r && r.ranges) || [];
              pending -= 1;
              if (pending === 0) {
                const signatures = testIds.map((t) =>
                  rangeSignature(perTest[t]),
                );
                const allSame = signatures.every((s) => s === signatures[0]);
                setDiffers(!allSame);
                // Seed the editor from the first test's ranges (the "set all to"
                // starting point when they differ; the shared value when they agree).
                setRanges(perTest[testIds[0]] || []);
                setLoading(false);
              }
            },
          );
        });
      },
    );
    // Components for the range→component picker come from the first test.
    getFromOpenElisServer(
      `/rest/test-catalog/tests/${testIds[0]}/sample-results`,
      (res) => {
        if (res && Array.isArray(res.components)) {
          setComponents(
            res.components.map((c) => ({
              id: c.id,
              label: c.label || c.code || c.id,
            })),
          );
        }
      },
    );
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [ids]);

  const handleSaveRange = (r) => {
    setRanges((prev) =>
      editingIndex === -1
        ? [...prev, r]
        : prev.map((row, i) => (i === editingIndex ? { ...row, ...r } : row)),
    );
    setEditingIndex(null);
  };

  const removeRange = (i) =>
    setRanges((prev) => prev.filter((_, idx) => idx !== i));

  // FR-13: drop a test from the edited set (min 2 to stay a group).
  const deselectTest = (id) => {
    const remaining = testIds.filter((t) => t !== id);
    if (remaining.length >= 2) {
      history.push(
        `${base}/TestCatalogEditor/group/${remaining.join(",")}/ranges`,
      );
    }
  };

  const handleSaveAll = () => {
    setSaving(true);
    // Strip ids: the shared set is written fresh into each test's own rows.
    const payload = {
      testIds,
      ranges: ranges.map((r) => ({
        componentId: r.componentId || null,
        gender: r.gender || null,
        minAge: r.minAge,
        maxAge: r.maxAge,
        lowNormal: r.lowNormal,
        highNormal: r.highNormal,
        lowCritical: r.lowCritical,
        highCritical: r.highCritical,
      })),
    };
    putToOpenElisServer(
      "/rest/test-catalog/group/ranges",
      JSON.stringify(payload),
      (status) => {
        setSaving(false);
        setNotificationVisible(true);
        if (status === 200) {
          setDiffers(false);
          addNotification({
            kind: "success",
            title: intl.formatMessage({
              id: "label.testCatalog.section.ranges",
            }),
            message: intl.formatMessage(
              { id: "label.testCatalog.group.rangesSaved" },
              { count: testIds.length },
            ),
          });
        } else {
          addNotification({
            kind: "error",
            title: intl.formatMessage({ id: "error.title" }),
            message: intl.formatMessage({ id: "server.error.msg" }),
          });
        }
      },
    );
  };

  const breadcrumbs = [
    { label: "home.label", link: "/" },
    { label: "breadcrums.admin.managment", link: base },
    { label: "label.testCatalog.editor", link: `${base}/TestCatalogList` },
  ];

  if (loading) {
    return <Loading description="Loading" withOverlay={false} />;
  }
  if (error) {
    return (
      <>
        <PageBreadCrumb breadcrumbs={breadcrumbs} />
        <Grid fullWidth>
          <Column lg={16} md={8} sm={4}>
            <InlineNotification
              kind="error"
              lowContrast
              hideCloseButton
              title={intl.formatMessage({ id: "error.title" })}
              subtitle={intl.formatMessage({
                id: "label.testCatalog.editor.loadError",
              })}
            />
          </Column>
        </Grid>
      </>
    );
  }

  return (
    <>
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <Grid fullWidth>
        <Column lg={16} md={8} sm={4}>
          <Section>
            <Heading>
              {intl.formatMessage(
                { id: "label.testCatalog.editingNTests" },
                { count: summaries.length },
              )}
            </Heading>
          </Section>
        </Column>

        {/* Per-test identity + LOINC — read-only; never harmonized (FR-12). */}
        <Column lg={16} md={8} sm={4}>
          <Tile style={{ marginTop: "1rem" }}>
            <h5>
              <FormattedMessage id="label.testCatalog.perTestIdentity" />
            </h5>
            <Table size="sm">
              <TableHead>
                <TableRow>
                  <TableHeader>
                    <FormattedMessage id="label.testCatalog.basicInfo.name" />
                  </TableHeader>
                  <TableHeader>
                    <FormattedMessage id="label.testCatalog.list.col.sampleType" />
                  </TableHeader>
                  <TableHeader>
                    <FormattedMessage id="label.testCatalog.basicInfo.code" />
                  </TableHeader>
                  <TableHeader>LOINC</TableHeader>
                  <TableHeader>
                    <FormattedMessage id="label.testCatalog.list.col.status" />
                  </TableHeader>
                  <TableHeader>
                    <FormattedMessage id="label.testCatalog.sampleResults.actions" />
                  </TableHeader>
                </TableRow>
              </TableHead>
              <TableBody>
                {summaries.map((s) => (
                  <TableRow key={s.testId}>
                    <TableCell>
                      <Button
                        kind="ghost"
                        size="sm"
                        onClick={() =>
                          history.push(
                            `${base}/TestCatalogEditor/${s.testId}/terminology`,
                          )
                        }
                      >
                        {s.name}
                      </Button>
                    </TableCell>
                    <TableCell>{s.sampleType || "—"}</TableCell>
                    <TableCell>{s.code || "—"}</TableCell>
                    <TableCell>{s.loinc || "—"}</TableCell>
                    <TableCell>
                      <Tag type={s.active ? "green" : "cool-gray"} size="sm">
                        <FormattedMessage
                          id={
                            s.active
                              ? "label.testCatalog.basicInfo.active"
                              : "label.testCatalog.list.filter.inactive"
                          }
                        />
                      </Tag>
                    </TableCell>
                    <TableCell>
                      {/* FR-13: drop a test from the set before saving. */}
                      <Button
                        kind="ghost"
                        size="sm"
                        hasIconOnly
                        renderIcon={TrashCan}
                        iconDescription={intl.formatMessage({
                          id: "button.testCatalog.removeFromSet",
                        })}
                        disabled={testIds.length <= 2}
                        onClick={() => deselectTest(s.testId)}
                      />
                    </TableCell>
                  </TableRow>
                ))}
              </TableBody>
            </Table>
          </Tile>
        </Column>

        {/* Shared-section switcher: which config to edit across all tests. */}
        <Column lg={16} md={8} sm={4}>
          <ContentSwitcher
            style={{ marginTop: "1rem", maxWidth: "24rem" }}
            selectedIndex={sharedSection === "storage" ? 1 : 0}
            onChange={({ index }) =>
              setSharedSection(index === 1 ? "storage" : "ranges")
            }
          >
            <Switch
              name="ranges"
              text={intl.formatMessage({
                id: "label.testCatalog.section.ranges",
              })}
            />
            <Switch
              name="storage"
              text={intl.formatMessage({
                id: "label.testCatalog.section.storage",
              })}
            />
          </ContentSwitcher>
        </Column>

        {/* Shared Storage editor — reuses the full storage form in group mode. */}
        {sharedSection === "storage" && (
          <Column lg={16} md={8} sm={4}>
            <Tile style={{ marginTop: "1rem" }}>
              <p>
                <FormattedMessage id="label.testCatalog.group.storageIntro" />
              </p>
              <StorageSection groupTestIds={testIds} />
            </Tile>
          </Column>
        )}

        {/* Shared Ranges editor — edit once, apply to all (FR-9/11). */}
        {sharedSection === "ranges" && (
          <Column lg={16} md={8} sm={4}>
            <Tile style={{ marginTop: "1rem" }}>
              <h5>
                <FormattedMessage id="label.testCatalog.section.ranges" />
              </h5>
              <p>
                <FormattedMessage id="label.testCatalog.group.rangesIntro" />
              </p>
              {differs && (
                <InlineNotification
                  kind="warning"
                  lowContrast
                  hideCloseButton
                  data-testid="ranges-differ-warning"
                  title={intl.formatMessage({
                    id: "state.testCatalog.differsAcrossTests",
                  })}
                  subtitle={intl.formatMessage({
                    id: "label.testCatalog.group.rangesDiffer",
                  })}
                />
              )}
              <Table size="sm">
                <TableHead>
                  <TableRow>
                    <TableHeader>
                      <FormattedMessage id="label.testCatalog.ranges.table.sex" />
                    </TableHeader>
                    <TableHeader>
                      <FormattedMessage id="label.testCatalog.ranges.table.age" />
                    </TableHeader>
                    <TableHeader>
                      <FormattedMessage id="label.testCatalog.ranges.table.normal" />
                    </TableHeader>
                    <TableHeader>
                      <FormattedMessage id="label.testCatalog.sampleResults.actions" />
                    </TableHeader>
                  </TableRow>
                </TableHead>
                <TableBody>
                  {ranges.map((r, i) => (
                    <TableRow key={r.id || `r-${i}`}>
                      <TableCell>{r.gender || "All"}</TableCell>
                      <TableCell>
                        {(r.minAge ?? 0) +
                          " – " +
                          (r.maxAge == null ? "∞" : r.maxAge)}
                      </TableCell>
                      <TableCell>
                        {(r.lowNormal ?? "—") + " / " + (r.highNormal ?? "—")}
                      </TableCell>
                      <TableCell>
                        <Button
                          kind="ghost"
                          size="sm"
                          hasIconOnly
                          renderIcon={Edit}
                          iconDescription={intl.formatMessage({
                            id: "label.button.edit",
                          })}
                          onClick={() => setEditingIndex(i)}
                        />
                        <Button
                          kind="ghost"
                          size="sm"
                          hasIconOnly
                          renderIcon={TrashCan}
                          iconDescription={intl.formatMessage({
                            id: "label.button.delete",
                          })}
                          onClick={() => removeRange(i)}
                        />
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
              <div
                style={{ display: "flex", gap: "0.5rem", marginTop: "1rem" }}
              >
                <Button
                  kind="tertiary"
                  renderIcon={Add}
                  onClick={() => setEditingIndex(-1)}
                >
                  <FormattedMessage id="label.testCatalog.ranges.add" />
                </Button>
                <Button
                  kind="primary"
                  disabled={saving}
                  onClick={handleSaveAll}
                >
                  <FormattedMessage id="button.testCatalog.setAllTo" />
                </Button>
                <Button
                  kind="ghost"
                  onClick={() => history.push(`${base}/TestCatalogList`)}
                >
                  <FormattedMessage id="label.button.cancel" />
                </Button>
              </div>
            </Tile>
          </Column>
        )}
      </Grid>

      {editingIndex !== null && sharedSection === "ranges" && (
        <RangeModal
          range={editingIndex >= 0 ? ranges[editingIndex] : null}
          components={components}
          onSave={handleSaveRange}
          onCancel={() => setEditingIndex(null)}
        />
      )}
    </>
  );
};

export default CombinedTestEditor;
