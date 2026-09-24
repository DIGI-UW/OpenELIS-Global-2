import React, { useCallback, useEffect, useMemo, useState } from "react";
import {
  Button,
  Checkbox,
  Column,
  Grid,
  Heading,
  InlineNotification,
  Loading,
  Modal,
  Section,
  Select,
  SelectItem,
  Stack,
  TextInput,
} from "@carbon/react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServer,
  putToOpenElisServer,
} from "../../utils/Utils";
import PageBreadCrumb from "../../common/PageBreadCrumb";

const breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
  {
    label: "role.management.title",
    link: "/MasterListsPage/roleManagement",
  },
];

/**
 * Role administration: create assignable roles and edit the privileges they
 * grant directly.
 *
 * <p>Two things this screen is careful about, both of which are easy to get
 * wrong and were the cause of unassignable roles in the shipped configuration:
 *
 * <ul>
 *   <li><b>Container</b> ("renders under") is UI placement, not inheritance. A
 *   role placed outside "Global Roles" / "Lab Unit Roles" is created and then
 *   never offered in User Management, so the field is required here.</li>
 *   <li><b>Inherits from</b> is privilege inheritance, and is optional. The
 *   checklist below edits only the role's DIRECT grants — inherited privileges
 *   are shown separately and are not editable here, because they belong to the
 *   parent role.</li>
 * </ul>
 */
const EMPTY_SET = new Set();

const CONTAINERS = ["Global Roles", "Lab Unit Roles"];

function RoleManagement() {
  const intl = useIntl();

  const [roles, setRoles] = useState([]);
  const [catalogue, setCatalogue] = useState([]);
  const [selectedRoleId, setSelectedRoleId] = useState("");
  const [directIds, setDirectIds] = useState(new Set());
  const [effectiveNames, setEffectiveNames] = useState(new Set());
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [notification, setNotification] = useState(null);
  const [createOpen, setCreateOpen] = useState(false);
  const [draft, setDraft] = useState({
    name: "",
    description: "",
    groupingParentName: CONTAINERS[0],
    parentRoleName: "",
  });

  const loadRoles = useCallback(() => {
    getFromOpenElisServer("/rest/roles", (res) => setRoles(res || []));
  }, []);

  useEffect(() => {
    loadRoles();
    getFromOpenElisServer("/rest/roles/privileges", (res) =>
      setCatalogue(res || []),
    );
  }, [loadRoles]);

  // Direct grants drive the checkboxes; the effective set is shown read-only so
  // an admin can see what the role already inherits before adding to it.
  useEffect(() => {
    if (!selectedRoleId) {
      // Nothing to fetch. The stale sets are simply not read while no role is
      // selected (see shownDirectIds below) — clearing them here would set
      // state synchronously in an effect body and cascade a render.
      return undefined;
    }
    let cancelled = false;
    // Deferred rather than called in the effect body: a synchronous setState here
    // cascades an extra render (react-hooks/set-state-in-effect).
    Promise.resolve().then(() => {
      if (!cancelled) {
        setLoading(true);
      }
    });
    Promise.all([
      new Promise((resolve) =>
        getFromOpenElisServer(
          `/rest/roles/${selectedRoleId}/privileges/direct`,
          (res) => resolve(res || []),
        ),
      ),
      new Promise((resolve) =>
        getFromOpenElisServer(
          `/rest/roles/${selectedRoleId}/privileges`,
          (res) => resolve(res || []),
        ),
      ),
    ]).then(([direct, effective]) => {
      if (cancelled) {
        return;
      }
      const byName = new Map(catalogue.map((p) => [p.name, p.id]));
      setDirectIds(
        new Set(
          direct.map((p) => byName.get(p.name)).filter((id) => id != null),
        ),
      );
      setEffectiveNames(new Set(effective.map((p) => p.name)));
      setLoading(false);
    });
    return () => {
      cancelled = true;
    };
  }, [selectedRoleId, catalogue]);

  const byCategory = useMemo(() => {
    const grouped = {};
    catalogue.forEach((p) => {
      const category = p.category || "other";
      grouped[category] = grouped[category] || [];
      grouped[category].push(p);
    });
    Object.values(grouped).forEach((list) =>
      list.sort((a, b) => a.name.localeCompare(b.name)),
    );
    return grouped;
  }, [catalogue]);

  const toggle = (privilegeId) => {
    setDirectIds((current) => {
      const next = new Set(current);
      if (next.has(privilegeId)) {
        next.delete(privilegeId);
      } else {
        next.add(privilegeId);
      }
      return next;
    });
  };

  const savePrivileges = () => {
    setSaving(true);
    putToOpenElisServer(
      `/rest/roles/${selectedRoleId}/privileges`,
      JSON.stringify({ privilegeIds: Array.from(directIds) }),
      (status) => {
        setSaving(false);
        setNotification(
          status === 200
            ? { kind: "success", key: "role.management.saved" }
            : { kind: "error", key: "role.management.saveFailed" },
        );
      },
    );
  };

  const createRole = () => {
    setSaving(true);
    postToOpenElisServer(
      "/rest/roles",
      JSON.stringify({ ...draft, privilegeIds: [] }),
      (status) => {
        setSaving(false);
        if (status === 200 || status === 201) {
          setNotification({ kind: "success", key: "role.management.created" });
          setCreateOpen(false);
          setDraft({
            name: "",
            description: "",
            groupingParentName: CONTAINERS[0],
            parentRoleName: "",
          });
          loadRoles();
        } else {
          setNotification({
            kind: "error",
            key: "role.management.createFailed",
          });
        }
      },
    );
  };

  const selectedRole = roles.find(
    (r) => String(r.id) === String(selectedRoleId),
  );
  // Read-guards instead of clearing state in an effect.
  const shownDirectIds = selectedRoleId ? directIds : EMPTY_SET;
  const shownEffectiveNames = selectedRoleId ? effectiveNames : EMPTY_SET;

  return (
    <div className="adminPageContent">
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <Stack gap={5}>
        <Section>
          <Heading>
            <FormattedMessage id="role.management.title" />
          </Heading>
        </Section>

        {notification && (
          <InlineNotification
            kind={notification.kind}
            title={intl.formatMessage({ id: notification.key })}
            onCloseButtonClick={() => setNotification(null)}
          />
        )}

        <Grid>
          <Column lg={8} md={4} sm={4}>
            <Select
              id="role-select"
              labelText={intl.formatMessage({ id: "role.management.select" })}
              value={selectedRoleId}
              onChange={(e) => setSelectedRoleId(e.target.value)}
            >
              <SelectItem value="" text="" />
              {roles.map((role) => (
                <SelectItem
                  key={role.id}
                  value={String(role.id)}
                  text={role.name}
                />
              ))}
            </Select>
          </Column>
          <Column lg={8} md={4} sm={4}>
            <Button kind="tertiary" onClick={() => setCreateOpen(true)}>
              <FormattedMessage id="role.management.create" />
            </Button>
          </Column>
        </Grid>

        {loading && <Loading description="" withOverlay={false} />}

        {selectedRole && !loading && (
          <Stack gap={4}>
            <p>
              <FormattedMessage id="role.management.directHint" />
            </p>
            {Object.keys(byCategory)
              .sort()
              .map((category) => (
                <Section key={category}>
                  <Heading>{category}</Heading>
                  {byCategory[category].map((privilege) => {
                    const checked = shownDirectIds.has(privilege.id);
                    const inheritedOnly =
                      !checked && shownEffectiveNames.has(privilege.name);
                    return (
                      <Checkbox
                        key={privilege.id}
                        id={`priv-${privilege.id}`}
                        labelText={
                          inheritedOnly
                            ? `${privilege.name} (${intl.formatMessage({
                                id: "role.management.inherited",
                              })})`
                            : privilege.name
                        }
                        checked={checked}
                        onChange={() => toggle(privilege.id)}
                      />
                    );
                  })}
                </Section>
              ))}
            <Button onClick={savePrivileges} disabled={saving}>
              <FormattedMessage id="role.management.save" />
            </Button>
          </Stack>
        )}

        <Modal
          open={createOpen}
          modalHeading={intl.formatMessage({ id: "role.management.create" })}
          primaryButtonText={intl.formatMessage({
            id: "role.management.create",
          })}
          secondaryButtonText={intl.formatMessage({
            id: "label.button.cancel",
          })}
          onRequestClose={() => setCreateOpen(false)}
          onRequestSubmit={createRole}
          primaryButtonDisabled={saving || !draft.name.trim()}
        >
          <Stack gap={4}>
            <TextInput
              id="role-name"
              labelText={intl.formatMessage({ id: "role.management.name" })}
              value={draft.name}
              onChange={(e) => setDraft({ ...draft, name: e.target.value })}
            />
            <TextInput
              id="role-description"
              labelText={intl.formatMessage({
                id: "role.management.description",
              })}
              value={draft.description}
              onChange={(e) =>
                setDraft({ ...draft, description: e.target.value })
              }
            />
            <Select
              id="role-container"
              labelText={intl.formatMessage({
                id: "role.management.container",
              })}
              helperText={intl.formatMessage({
                id: "role.management.containerHint",
              })}
              value={draft.groupingParentName}
              onChange={(e) =>
                setDraft({ ...draft, groupingParentName: e.target.value })
              }
            >
              {CONTAINERS.map((c) => (
                <SelectItem key={c} value={c} text={c} />
              ))}
            </Select>
            <Select
              id="role-parent"
              labelText={intl.formatMessage({ id: "role.management.inherits" })}
              helperText={intl.formatMessage({
                id: "role.management.inheritsHint",
              })}
              value={draft.parentRoleName}
              onChange={(e) =>
                setDraft({ ...draft, parentRoleName: e.target.value })
              }
            >
              <SelectItem value="" text="" />
              {roles.map((role) => (
                <SelectItem key={role.id} value={role.name} text={role.name} />
              ))}
            </Select>
          </Stack>
        </Modal>
      </Stack>
    </div>
  );
}

export default RoleManagement;
