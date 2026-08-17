import React, { useCallback, useContext, useEffect, useState } from "react";
import {
  Button,
  Dropdown,
  InlineNotification,
  Modal,
  Stack,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableHeader,
  TableRow,
  Tag,
  DataTableSkeleton,
} from "@carbon/react";
import { Add, TrashCan } from "@carbon/icons-react";
import { useIntl } from "react-intl";
import {
  deleteFromOpenElisServer,
  getFromOpenElisServer,
  postToOpenElisServer,
} from "../../../utils/Utils";
import UserSessionDetailsContext from "../../../../UserSessionDetailsContext";

/**
 * OGC-686 — Accreditation section: which bodies accredit this test.
 *
 * The other direction of the same data the QMS page at /qa/qms/accreditation
 * manages, so it reuses that API rather than growing a per-test one: the page
 * answers "which tests does this body cover?", this answers "who accredits this
 * test?". Expiry belongs to the body, so this section never edits a date — it
 * shows the owning body's status and links out for anything else.
 */
const AccreditationSection = ({ testId }) => {
  const intl = useIntl();
  const { userSessionDetails } = useContext(UserSessionDetailsContext);
  // The real gate is @PreAuthorize on the write endpoints; this only hides
  // controls from users who would get a 403 anyway.
  //
  // Today it never actually hides anything: App.jsx gates /admin on
  // Roles.GLOBAL_ADMIN, and GLOBAL_ADMIN satisfies this check, so everyone who
  // can reach this section passes it. Kept deliberately — the day /admin (or a
  // future home for the test-catalog editor) accepts a permission instead of a
  // role, the write controls stay closed by default rather than silently
  // opening to every viewer.
  const canManage =
    userSessionDetails?.permissions?.includes("qa.manage.accreditation") ||
    userSessionDetails?.roles?.includes("Global Administrator");

  const [loading, setLoading] = useState(true);
  const [error, setError] = useState(false);
  const [enrollments, setEnrollments] = useState([]);
  const [bodies, setBodies] = useState([]);
  const [addOpen, setAddOpen] = useState(false);
  const [selectedBodyId, setSelectedBodyId] = useState(null);
  const [deleteTarget, setDeleteTarget] = useState(null);
  const [notification, setNotification] = useState(null);

  const load = useCallback(() => {
    if (!testId) {
      return;
    }
    setLoading(true);
    setError(false);
    getFromOpenElisServer(
      `/rest/accreditation/enrollments?testId=${testId}`,
      (res) => {
        setLoading(false);
        if (!Array.isArray(res)) {
          setError(true);
          return;
        }
        setEnrollments(res);
      },
    );
    getFromOpenElisServer("/rest/accreditation/bodies", (res) =>
      setBodies(Array.isArray(res) ? res : []),
    );
  }, [testId]);

  useEffect(() => {
    load();
  }, [load]);

  const enrolledBodyIds = enrollments.map((e) => String(e.accreditingBodyId));
  // Offer only bodies that could actually accredit this test today: one the test
  // already has would just earn the backend's duplicate 400, and a deactivated
  // one contributes to no logo, no notes line and no coverage answer. An expired
  // body stays on the list — renewal in progress is a real state, and the row it
  // creates goes live the moment the new expiry date is entered.
  const addableBodies = bodies.filter(
    (b) => !enrolledBodyIds.includes(String(b.id)) && b.active,
  );

  // Same tag colours and the same labels as the QMS page — one status shown two
  // ways would read as two different things.
  const statusTag = (status) => {
    const type = {
      ACTIVE: "green",
      EXPIRING: "magenta",
      EXPIRED: "red",
      INACTIVE: "gray",
    }[status];
    return status ? (
      <Tag type={type}>
        {intl.formatMessage({ id: `qa.qms.accreditation.status.${status}` })}
      </Tag>
    ) : (
      "—"
    );
  };

  const submitAdd = () => {
    const bodyId = selectedBodyId;
    setAddOpen(false);
    setSelectedBodyId(null);
    if (!bodyId) {
      return;
    }
    postToOpenElisServer(
      "/rest/accreditation/enrollments",
      JSON.stringify({ testId, accreditingBodyId: Number(bodyId) }),
      (status) => {
        if (status >= 200 && status < 300) {
          setNotification({
            kind: "success",
            text: intl.formatMessage({
              id: "label.testCatalog.accreditation.added",
            }),
          });
          load();
        } else {
          setNotification({
            kind: "error",
            text: intl.formatMessage({
              id: "label.testCatalog.accreditation.saveError",
            }),
          });
        }
      },
    );
  };

  const confirmDelete = () => {
    const target = deleteTarget;
    setDeleteTarget(null);
    if (!target) {
      return;
    }
    deleteFromOpenElisServer(
      `/rest/accreditation/enrollments/${target.id}`,
      (status) => {
        if (status >= 200 && status < 300) {
          setNotification({
            kind: "success",
            text: intl.formatMessage({
              id: "label.testCatalog.accreditation.removed",
            }),
          });
          load();
        } else {
          setNotification({
            kind: "error",
            text: intl.formatMessage({
              id: "label.testCatalog.accreditation.deleteError",
            }),
          });
        }
      },
    );
  };

  if (loading) {
    return (
      <DataTableSkeleton
        columnCount={4}
        rowCount={2}
        showHeader={false}
        showToolbar={false}
        data-testid="accreditation-skeleton"
      />
    );
  }

  if (error) {
    return (
      <InlineNotification
        kind="error"
        lowContrast
        hideCloseButton
        title={intl.formatMessage({ id: "error.title" })}
        subtitle={intl.formatMessage({
          id: "label.testCatalog.accreditation.loadError",
        })}
      />
    );
  }

  return (
    <Stack gap={6} data-testid="accreditation-section">
      {notification && (
        <InlineNotification
          kind={notification.kind}
          lowContrast
          title={notification.text}
          onCloseButtonClick={() => setNotification(null)}
        />
      )}

      <div style={{ display: "flex", justifyContent: "flex-end" }}>
        {canManage && (
          <Button
            kind="ghost"
            size="sm"
            renderIcon={Add}
            disabled={addableBodies.length === 0}
            onClick={() => {
              setSelectedBodyId(null);
              setAddOpen(true);
            }}
            data-testid="add-accreditation-button"
          >
            {intl.formatMessage({ id: "label.testCatalog.accreditation.add" })}
          </Button>
        )}
      </div>

      {enrollments.length === 0 ? (
        <InlineNotification
          kind="info"
          lowContrast
          hideCloseButton
          title={intl.formatMessage({
            id: "label.testCatalog.accreditation.empty",
          })}
        />
      ) : (
        <TableContainer>
          <Table size="sm" aria-label="accreditation">
            <TableHead>
              <TableRow>
                <TableHeader>
                  {intl.formatMessage({
                    id: "label.testCatalog.accreditation.col.body",
                  })}
                </TableHeader>
                <TableHeader>
                  {intl.formatMessage({
                    id: "label.testCatalog.accreditation.col.expires",
                  })}
                </TableHeader>
                <TableHeader>
                  {intl.formatMessage({
                    id: "label.testCatalog.accreditation.col.status",
                  })}
                </TableHeader>
                <TableHeader>
                  {intl.formatMessage({
                    id: "label.testCatalog.accreditation.col.actions",
                  })}
                </TableHeader>
              </TableRow>
            </TableHead>
            <TableBody>
              {enrollments.map((e) => (
                <TableRow key={e.id} data-testid={`accreditation-${e.id}`}>
                  <TableCell>{`${e.bodyCode} — ${e.bodyName}`}</TableCell>
                  <TableCell>{e.bodyExpiresOn || "—"}</TableCell>
                  <TableCell>{statusTag(e.status)}</TableCell>
                  <TableCell>
                    {canManage && (
                      <Button
                        kind="ghost"
                        size="sm"
                        hasIconOnly
                        renderIcon={TrashCan}
                        iconDescription={intl.formatMessage({
                          id: "label.testCatalog.accreditation.remove",
                        })}
                        onClick={() => setDeleteTarget(e)}
                        data-testid={`delete-accreditation-${e.id}`}
                      />
                    )}
                  </TableCell>
                </TableRow>
              ))}
            </TableBody>
          </Table>
        </TableContainer>
      )}

      <a href={`/qa/qms/accreditation?testId=${testId}`}>
        {intl.formatMessage({
          id: "label.testCatalog.accreditation.manageLink",
        })}
      </a>

      <Modal
        open={addOpen}
        modalHeading={intl.formatMessage({
          id: "label.testCatalog.accreditation.add",
        })}
        primaryButtonText={intl.formatMessage({ id: "label.button.save" })}
        secondaryButtonText={intl.formatMessage({ id: "button.cancel" })}
        primaryButtonDisabled={!selectedBodyId}
        onRequestClose={() => setAddOpen(false)}
        onRequestSubmit={submitAdd}
      >
        <Dropdown
          id="accreditation-body-select"
          titleText={intl.formatMessage({
            id: "label.testCatalog.accreditation.col.body",
          })}
          label={intl.formatMessage({
            id: "label.testCatalog.accreditation.selectBody",
          })}
          items={addableBodies.map((b) => String(b.id))}
          selectedItem={selectedBodyId}
          itemToString={(item) => {
            const body = addableBodies.find((b) => String(b.id) === item);
            return body ? `${body.code} — ${body.name}` : "";
          }}
          onChange={({ selectedItem }) => setSelectedBodyId(selectedItem)}
        />
      </Modal>

      <Modal
        open={!!deleteTarget}
        danger
        modalHeading={intl.formatMessage({
          id: "label.testCatalog.accreditation.remove",
        })}
        primaryButtonText={intl.formatMessage({
          id: "label.testCatalog.accreditation.remove.confirm",
        })}
        secondaryButtonText={intl.formatMessage({ id: "button.cancel" })}
        onRequestClose={() => setDeleteTarget(null)}
        onRequestSubmit={confirmDelete}
      >
        {deleteTarget &&
          intl.formatMessage(
            { id: "label.testCatalog.accreditation.remove.body" },
            { body: deleteTarget.bodyName },
          )}
      </Modal>
    </Stack>
  );
};

export default AccreditationSection;
