import React, {
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useState,
} from "react";
import { useLocation } from "react-router-dom";
import {
  Button,
  DataTableSkeleton,
  Dropdown,
  InlineNotification,
  Modal,
  Pagination,
  Tag,
  TextInput,
} from "@carbon/react";
import { Add, Edit, TrashCan } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import UserSessionDetailsContext from "../../../UserSessionDetailsContext";
import {
  deleteFromOpenElisServer,
  getFromOpenElisServer,
  hasPermissionOrGlobalAdmin,
} from "../../utils/Utils";
import PageBreadCrumb from "../../common/PageBreadCrumb";
import QAEmptyState from "../common/QAEmptyState";
import QASimpleTable from "../common/QASimpleTable";
import QAStatTiles from "../common/QAStatTiles";
import AccreditationStatusTag, {
  accreditationLogoUrl,
} from "../common/AccreditationStatusTag";
import AccreditingBodyModal from "./AccreditingBodyModal";
import EnrollTestsModal from "./EnrollTestsModal";
import "../common/QAStyles.css";

/**
 * Test Accreditation (OGC-686) at /qa/qms/accreditation. One surface for
 * accrediting bodies and per-test enrollment: reads on qa.view.qms, write controls
 * appear only with qa.manage.accreditation. Body status (ACTIVE/EXPIRING/EXPIRED/
 * INACTIVE) and the expiring window are computed by the backend so this page, the
 * report resolver and the inspector-readiness answer cannot drift apart.
 * Filtering is client-side: the data is small (bodies + per-test enrollments).
 */

const breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "sideNav.label.qa", link: "" },
  { label: "sideNav.label.qa.qms.accreditation", link: "" },
];

const STATUSES = ["ACTIVE", "EXPIRING", "EXPIRED", "INACTIVE"];

const BODY_HEADERS = [
  { key: "logo", labelKey: "qa.qms.accreditation.body.column.logo" },
  { key: "code", labelKey: "common.code" },
  { key: "name", labelKey: "common.name" },
  { key: "expiresOn", labelKey: "qa.qms.accreditation.body.column.expiresOn" },
  { key: "enrolled", labelKey: "qa.qms.accreditation.enrollments.title" },
  { key: "status", labelKey: "common.status" },
  { key: "actions", labelKey: "common.actions" },
];

const EQA_COVERAGE_HEADERS = [
  { key: "body", labelKey: "qa.qms.accreditation.enrollment.column.body" },
  { key: "scope", labelKey: "qa.qms.accreditation.eqa.column.scope" },
  { key: "covered", labelKey: "qa.qms.accreditation.eqa.column.covered" },
  { key: "gaps", labelKey: "qa.qms.accreditation.eqa.column.gaps" },
];

const ENROLLMENT_HEADERS = [
  { key: "test", labelKey: "common.test" },
  { key: "body", labelKey: "qa.qms.accreditation.enrollment.column.body" },
  {
    key: "effectiveFrom",
    labelKey: "qa.qms.accreditation.enrollment.column.effectiveFrom",
  },
  {
    key: "expires",
    labelKey: "qa.qms.accreditation.enrollment.column.expires",
  },
  { key: "status", labelKey: "common.status" },
  {
    key: "actions",
    labelKey: "common.actions",
  },
];

/** Names the first few uncovered tests; a long tail would swamp the row. */
const GAP_NAMES_SHOWN = 5;

const gapSummary = (gaps, intl) => {
  const names = gaps
    .slice(0, GAP_NAMES_SHOWN)
    .map((g) => g.testName || g.testId);
  const rest = gaps.length - names.length;
  return rest > 0
    ? intl.formatMessage(
        { id: "qa.qms.accreditation.eqa.gapsMore" },
        { names: names.join(", "), count: rest },
      )
    : names.join(", ");
};

const Accreditation = () => {
  const intl = useIntl();
  const location = useLocation();
  const { userSessionDetails } = useContext(UserSessionDetailsContext);
  const canManage = hasPermissionOrGlobalAdmin(
    userSessionDetails,
    "qa.manage.accreditation",
  );

  // undefined = loading, null = fetch yielded no data / error
  const [summary, setSummary] = useState();
  const [bodies, setBodies] = useState();
  const [enrollments, setEnrollments] = useState();
  const [eqaCoverage, setEqaCoverage] = useState();
  const [bodyFilter, setBodyFilter] = useState("all");
  const [statusFilter, setStatusFilter] = useState("all");
  const [search, setSearch] = useState("");
  const [testIdFilter, setTestIdFilter] = useState(
    () => new URLSearchParams(location.search).get("testId") || "",
  );
  const [page, setPage] = useState(0);
  const [pageSize, setPageSize] = useState(25);
  const [bodyModal, setBodyModal] = useState(null); // { body } | null
  const [enrollOpen, setEnrollOpen] = useState(false);
  const [deleteBody, setDeleteBody] = useState(null);
  const [deleteEnrollment, setDeleteEnrollment] = useState(null);
  const [actionError, setActionError] = useState(false);

  const load = useCallback(() => {
    getFromOpenElisServer("/rest/accreditation/summary", (res) =>
      setSummary(res && typeof res.totalBodies === "number" ? res : null),
    );
    getFromOpenElisServer("/rest/accreditation/bodies", (res) =>
      setBodies(Array.isArray(res) ? res : null),
    );
    getFromOpenElisServer("/rest/accreditation/enrollments", (res) =>
      setEnrollments(Array.isArray(res) ? res : null),
    );
    getFromOpenElisServer("/rest/accreditation/eqa-coverage", (res) =>
      setEqaCoverage(Array.isArray(res) ? res : null),
    );
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  const onDeleted = (status) => {
    if (status >= 200 && status < 300) {
      setActionError(false);
      load();
    } else {
      setActionError(true);
    }
  };

  // Deleting the body you are filtering by would otherwise leave the filter pointing
  // at an id that no longer exists — zero rows forever, and a dropdown label that
  // reads a missing record. Fall back to "all" whenever the selection is gone.
  const activeBodyFilter = (bodies || []).some(
    (b) => String(b.id) === bodyFilter,
  )
    ? bodyFilter
    : "all";

  const filtered = useMemo(() => {
    const needle = search.trim().toLowerCase();
    return (enrollments || []).filter((row) => {
      if (testIdFilter && String(row.testId) !== testIdFilter) {
        return false;
      }
      if (
        activeBodyFilter !== "all" &&
        String(row.accreditingBodyId) !== String(activeBodyFilter)
      ) {
        return false;
      }
      if (statusFilter !== "all" && row.status !== statusFilter) {
        return false;
      }
      if (needle && !(row.testName || "").toLowerCase().includes(needle)) {
        return false;
      }
      return true;
    });
  }, [enrollments, testIdFilter, activeBodyFilter, statusFilter, search]);

  const statusLabel = (status) =>
    intl.formatMessage({
      id:
        status === "all"
          ? "common.all"
          : `qa.qms.accreditation.status.${status}`,
    });

  const maxPage = Math.max(0, Math.ceil(filtered.length / pageSize) - 1);
  const safePage = Math.min(page, maxPage);
  const pageRows = filtered.slice(
    safePage * pageSize,
    safePage * pageSize + pageSize,
  );
  const byBodyId = {};
  (bodies || []).forEach((b) => {
    byBodyId[String(b.id)] = b;
  });

  const testFilterName =
    (enrollments || []).find((e) => String(e.testId) === testIdFilter)
      ?.testName || testIdFilter;

  const bodyRows = (bodies || []).map((b) => ({
    id: String(b.id),
    logo: b.logoImageId ? (
      <img
        src={accreditationLogoUrl(b.logoImageId)}
        alt=""
        style={{ height: "2rem" }}
      />
    ) : (
      "—"
    ),
    code: b.code,
    name: b.name,
    expiresOn: b.expiresOn || "—",
    enrolled: b.enrolledTestCount,
    status: <AccreditationStatusTag status={b.status} size="sm" />,
    actions: canManage ? (
      <>
        <Button
          kind="ghost"
          size="sm"
          hasIconOnly
          renderIcon={Edit}
          iconDescription={intl.formatMessage({
            id: "qa.qms.accreditation.body.edit",
          })}
          onClick={() => setBodyModal({ body: b })}
          data-testid={`edit-body-${b.id}`}
        />
        <Button
          kind="ghost"
          size="sm"
          hasIconOnly
          renderIcon={TrashCan}
          disabled={b.enrolledTestCount > 0}
          // Say how many enrollments block the delete —
          // the same count the REST rejection reports.
          iconDescription={
            b.enrolledTestCount > 0
              ? intl.formatMessage(
                  { id: "qa.qms.accreditation.body.delete.blocked" },
                  { count: b.enrolledTestCount },
                )
              : intl.formatMessage({ id: "qa.qms.accreditation.body.delete" })
          }
          onClick={() => setDeleteBody(b)}
          data-testid={`delete-body-${b.id}`}
        />
      </>
    ) : (
      ""
    ),
  }));

  const enrollmentRows = pageRows.map((e) => ({
    id: String(e.id),
    test: e.testName || e.testId,
    body: `${e.bodyCode} — ${e.bodyName}`,
    effectiveFrom: e.effectiveFrom || "—",
    expires: e.bodyExpiresOn || "—",
    status: <AccreditationStatusTag status={e.status} size="sm" />,
    actions: canManage ? (
      <Button
        kind="ghost"
        size="sm"
        hasIconOnly
        renderIcon={TrashCan}
        iconDescription={intl.formatMessage({
          id: "qa.qms.accreditation.enrollment.remove",
        })}
        onClick={() => setDeleteEnrollment(e)}
        data-testid={`delete-enrollment-${e.id}`}
      />
    ) : (
      ""
    ),
  }));

  const coverageRows = (eqaCoverage || []).map((row) => ({
    id: String(row.accreditingBodyId),
    body: `${row.bodyCode} — ${row.bodyName}`,
    scope: row.enrolledTestCount,
    covered: row.coveredTestCount,
    gaps:
      (row.gaps || []).length === 0 ? (
        <Tag type="green">
          <FormattedMessage id="qa.qms.accreditation.eqa.noGaps" />
        </Tag>
      ) : (
        <>
          <Tag type="red">{row.gaps.length}</Tag> {gapSummary(row.gaps, intl)}
        </>
      ),
  }));

  return (
    <div className="pageContent qi-dashboard" data-testid="accreditation-page">
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <h2>
        <FormattedMessage id="qa.qms.accreditation.title" />
      </h2>
      <p className="qi-dashboard__subtitle">
        <FormattedMessage id="qa.qms.accreditation.subtitle" />
      </p>

      {actionError && (
        <InlineNotification
          kind="error"
          lowContrast
          title={intl.formatMessage({ id: "error.title" })}
          subtitle={intl.formatMessage({
            id: "qa.qms.accreditation.actionError",
          })}
          onCloseButtonClick={() => setActionError(false)}
        />
      )}

      {bodies === undefined || enrollments === undefined ? (
        <DataTableSkeleton columnCount={BODY_HEADERS.length} rowCount={4} />
      ) : bodies === null || enrollments === null ? (
        // Both lists are load-bearing. Without this, a failed enrollments fetch would
        // render the "no enrollments match these filters" empty state — a fetch
        // failure reading as a legitimately empty result.
        <p className="qi-tile__message">
          <FormattedMessage id="qa.qms.accreditation.error" />
        </p>
      ) : (
        <>
          {summary && (
            <>
              <QAStatTiles
                tiles={[
                  {
                    labelKey: "qa.qms.accreditation.tile.total",
                    value: summary.totalBodies,
                    accent: "blue",
                  },
                  {
                    labelKey: "common.active",
                    value: summary.activeBodies,
                    accent: "green",
                  },
                  {
                    labelKey: "inventory.metrics.expiringSoon",
                    value: summary.expiringBodies,
                    accent: "amber",
                  },
                  {
                    labelKey: "common.expired",
                    value: summary.expiredBodies,
                    accent: "red",
                  },
                ]}
              />
              {summary.inForceBodyNames?.length > 0 && (
                <p className="qi-dashboard__subtitle">
                  <FormattedMessage
                    id="qa.qms.accreditation.inForce"
                    values={{ names: summary.inForceBodyNames.join(", ") }}
                  />
                </p>
              )}
            </>
          )}

          <div className="qi-dashboard__controls">
            <h3>
              <FormattedMessage id="qa.qms.accreditation.bodies.title" />
            </h3>
            {canManage && (
              <Button
                kind="ghost"
                size="sm"
                renderIcon={Add}
                onClick={() => setBodyModal({ body: null })}
                data-testid="add-body-button"
              >
                {intl.formatMessage({ id: "qa.qms.accreditation.bodies.add" })}
              </Button>
            )}
          </div>

          {bodies.length === 0 ? (
            <QAEmptyState
              titleKey="qa.empty.accreditation.bodies.title"
              subheadKey="qa.empty.accreditation.bodies.subhead"
            />
          ) : (
            <QASimpleTable
              rows={bodyRows}
              headers={BODY_HEADERS}
              rowTestIdPrefix="body"
            />
          )}

          <div className="qi-dashboard__controls">
            <h3>
              <FormattedMessage id="qa.qms.accreditation.enrollments.title" />
            </h3>
            <Dropdown
              id="accreditation-body-filter"
              titleText={intl.formatMessage({
                id: "qa.qms.accreditation.filter.body",
              })}
              label=""
              items={["all", ...bodies.map((b) => String(b.id))]}
              selectedItem={activeBodyFilter}
              itemToString={(item) =>
                !item || item === "all" || !byBodyId[item]
                  ? intl.formatMessage({
                      id: "common.all",
                    })
                  : `${byBodyId[item].code} — ${byBodyId[item].name}`
              }
              onChange={({ selectedItem }) => {
                setPage(0);
                setBodyFilter(selectedItem);
              }}
            />
            <Dropdown
              id="accreditation-status-filter"
              titleText={intl.formatMessage({
                id: "common.status",
              })}
              label=""
              items={["all", ...STATUSES]}
              selectedItem={statusFilter}
              itemToString={(item) => (item ? statusLabel(item) : "")}
              onChange={({ selectedItem }) => {
                setPage(0);
                setStatusFilter(selectedItem);
              }}
            />
            <TextInput
              id="accreditation-search"
              labelText={intl.formatMessage({
                id: "label.sampleType.tests.search",
              })}
              value={search}
              onChange={(e) => {
                setPage(0);
                setSearch(e.target.value);
              }}
            />
            {testIdFilter && (
              <Tag
                filter
                type="blue"
                onClose={() => {
                  setPage(0);
                  setTestIdFilter("");
                }}
                data-testid="test-filter-tag"
              >
                {intl.formatMessage(
                  { id: "qa.qms.accreditation.filter.test" },
                  { name: testFilterName },
                )}
              </Tag>
            )}
            {canManage && bodies.length > 0 && (
              <Button
                kind="ghost"
                size="sm"
                renderIcon={Add}
                onClick={() => setEnrollOpen(true)}
                data-testid="enroll-tests-button"
              >
                {intl.formatMessage({
                  id: "qa.qms.accreditation.enrollments.add",
                })}
              </Button>
            )}
          </div>

          {filtered.length === 0 ? (
            <QAEmptyState
              titleKey="qa.empty.accreditation.enrollments.title"
              subheadKey="qa.empty.accreditation.enrollments.subhead"
            />
          ) : (
            <>
              <QASimpleTable
                rows={enrollmentRows}
                headers={ENROLLMENT_HEADERS}
                rowTestIdPrefix="enrollment"
              />
              <Pagination
                page={safePage + 1}
                pageSize={pageSize}
                pageSizes={[25, 50, 100]}
                totalItems={filtered.length}
                onChange={({ page: newPage, pageSize: newPageSize }) => {
                  setPage(newPage - 1);
                  setPageSize(newPageSize);
                }}
              />
            </>
          )}

          {/* ISO 15189 §7.7 — is every accredited test in a live EQA scheme?
              Derived from data the lab already keeps in the EQA module, so there is
              nothing to maintain here and no way for the answer to go stale. */}
          <div className="qi-dashboard__controls">
            <h3>
              <FormattedMessage id="qa.qms.accreditation.eqa.title" />
            </h3>
          </div>
          <p className="qi-dashboard__subtitle">
            <FormattedMessage id="qa.qms.accreditation.eqa.subtitle" />
          </p>

          {eqaCoverage === undefined ? (
            <DataTableSkeleton
              columnCount={EQA_COVERAGE_HEADERS.length}
              rowCount={2}
            />
          ) : eqaCoverage === null ? (
            <p className="qi-tile__message">
              <FormattedMessage id="qa.qms.accreditation.eqa.error" />
            </p>
          ) : eqaCoverage.length === 0 ? (
            <QAEmptyState
              titleKey="qa.empty.accreditation.eqa.title"
              subheadKey="qa.empty.accreditation.eqa.subhead"
            />
          ) : (
            <QASimpleTable
              rows={coverageRows}
              headers={EQA_COVERAGE_HEADERS}
              rowTestIdPrefix="eqa-coverage"
            />
          )}
        </>
      )}

      <AccreditingBodyModal
        open={!!bodyModal}
        onClose={() => setBodyModal(null)}
        body={bodyModal?.body}
        onSaved={load}
      />

      <EnrollTestsModal
        open={enrollOpen}
        onClose={() => setEnrollOpen(false)}
        bodies={bodies || []}
        onSaved={load}
      />

      <Modal
        open={!!deleteBody}
        danger
        modalHeading={intl.formatMessage({
          id: "qa.qms.accreditation.body.delete.title",
        })}
        primaryButtonText={intl.formatMessage({
          id: "common.delete",
        })}
        secondaryButtonText={intl.formatMessage({ id: "button.cancel" })}
        onRequestClose={() => setDeleteBody(null)}
        onRequestSubmit={() => {
          const target = deleteBody;
          setDeleteBody(null);
          deleteFromOpenElisServer(
            `/rest/accreditation/bodies/${target.id}`,
            onDeleted,
          );
        }}
      >
        {deleteBody &&
          intl.formatMessage(
            { id: "qa.qms.accreditation.body.delete.body" },
            { name: deleteBody.name },
          )}
      </Modal>

      <Modal
        open={!!deleteEnrollment}
        danger
        modalHeading={intl.formatMessage({
          id: "qa.qms.accreditation.enrollment.delete.title",
        })}
        primaryButtonText={intl.formatMessage({
          id: "common.remove",
        })}
        secondaryButtonText={intl.formatMessage({ id: "button.cancel" })}
        onRequestClose={() => setDeleteEnrollment(null)}
        onRequestSubmit={() => {
          const target = deleteEnrollment;
          setDeleteEnrollment(null);
          deleteFromOpenElisServer(
            `/rest/accreditation/enrollments/${target.id}`,
            onDeleted,
          );
        }}
      >
        {deleteEnrollment &&
          intl.formatMessage(
            { id: "qa.qms.accreditation.enrollment.delete.body" },
            {
              test: deleteEnrollment.testName || deleteEnrollment.testId,
              body: deleteEnrollment.bodyName,
            },
          )}
      </Modal>
    </div>
  );
};

export default Accreditation;
