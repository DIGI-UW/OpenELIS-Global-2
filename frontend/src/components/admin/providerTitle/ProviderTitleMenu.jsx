import React, { useCallback, useEffect, useMemo, useState } from "react";
import {
  Button,
  Column,
  Grid,
  Heading,
  InlineNotification,
  Loading,
  Modal,
  NumberInput,
  Section,
  Select,
  SelectItem,
  Table,
  TableBody,
  TableCell,
  TableContainer,
  TableHead,
  TableHeader,
  TableRow,
  TableToolbar,
  TableToolbarContent,
  TableToolbarSearch,
  Tag,
  TextInput,
  Toggle,
} from "@carbon/react";
import { Add } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import { useHistory } from "react-router-dom";
import {
  getFromOpenElisServer,
  patchToOpenElisServerJsonResponse,
  postToOpenElisServerFullResponse,
  putToOpenElisServerFullResponse,
} from "../../utils/Utils";
import PageBreadCrumb from "../../common/PageBreadCrumb";

/**
 * Provider Titles (OGC-1223) — the honorifics and cadres a requisition names a
 * clinician by. A title is never deleted: one that falls out of use is
 * deactivated, and the providers already carrying it keep it.
 */

const ENDPOINT = "/rest/admin/provider-titles";

const STATUS_OPTIONS = [
  { id: "active", labelKey: "providerTitle.filter.status.active" },
  { id: "inactive", labelKey: "providerTitle.filter.status.inactive" },
  { id: "all", labelKey: "providerTitle.filter.status.all" },
];

/** search over title and abbreviation, plus the status filter, client-side. */
export const filterTitles = (titles, { search, status }) => {
  const needle = (search || "").trim().toLowerCase();
  return (titles || []).filter((title) => {
    if (
      needle &&
      !(title.title || "").toLowerCase().includes(needle) &&
      !(title.abbreviation || "").toLowerCase().includes(needle)
    ) {
      return false;
    }
    if (status === "active" && !title.active) return false;
    if (status === "inactive" && title.active) return false;
    return true;
  });
};

/**
 * A rejected save answers with the reason as a JSON string, so the body arrives
 * quoted and its own quotes escaped. Unwrap it for display, and fall back to the
 * raw body for anything that answers in plain text.
 */
export const plainMessage = (body) => {
  try {
    const parsed = JSON.parse(body);
    return typeof parsed === "string" ? parsed : body;
  } catch {
    return body;
  }
};

const blankDraft = { title: "", abbreviation: "", sortOrder: "", active: true };

const ProviderTitleMenu = () => {
  const intl = useIntl();
  const history = useHistory();
  const initParams = new URLSearchParams(history.location.search);

  const [titles, setTitles] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [search, setSearch] = useState(initParams.get("search") || "");
  const [status, setStatus] = useState(initParams.get("status") || "active");
  const [editing, setEditing] = useState(null);
  const [draft, setDraft] = useState(blankDraft);
  const [saveError, setSaveError] = useState("");
  const [confirming, setConfirming] = useState(null);

  const load = useCallback(() => {
    setLoading(true);
    getFromOpenElisServer(ENDPOINT, (response) => {
      setLoading(false);
      if (Array.isArray(response)) {
        setTitles(response);
        setError("");
      } else {
        setTitles([]);
        setError(intl.formatMessage({ id: "error.load.failed" }));
      }
    });
  }, [intl]);

  useEffect(load, [load]);

  // Mirror the filters into the URL so a link carries what the reader sees.
  useEffect(() => {
    const params = new URLSearchParams();
    if (search) params.set("search", search);
    if (status && status !== "active") params.set("status", status);
    history.replace({ search: params.toString() });
  }, [search, status, history]);

  const visible = useMemo(
    () => filterTitles(titles, { search, status }),
    [titles, search, status],
  );

  const counts = useMemo(
    () => ({
      total: titles.length,
      active: titles.filter((title) => title.active).length,
      inactive: titles.filter((title) => !title.active).length,
    }),
    [titles],
  );

  const openAdd = () => {
    setEditing("new");
    setDraft(blankDraft);
    setSaveError("");
  };

  const openEdit = (title) => {
    setEditing(title.id);
    setDraft({
      title: title.title || "",
      abbreviation: title.abbreviation || "",
      sortOrder: title.sortOrder ?? "",
      active: title.active,
    });
    setSaveError("");
  };

  const save = () => {
    const body = JSON.stringify({
      title: draft.title,
      abbreviation: draft.abbreviation,
      sortOrder: draft.sortOrder === "" ? null : Number(draft.sortOrder),
      active: draft.active,
    });
    const done = (ok, message) => {
      if (ok) {
        setEditing(null);
        load();
      } else {
        setSaveError(
          message || intl.formatMessage({ id: "error.save.failed" }),
        );
      }
    };
    const handle = async (response) => {
      if (response && response.ok) {
        done(true);
      } else {
        const text = response ? await response.text().catch(() => "") : "";
        done(false, plainMessage(text));
      }
    };
    if (editing === "new") {
      postToOpenElisServerFullResponse(ENDPOINT, body, handle);
    } else {
      putToOpenElisServerFullResponse(`${ENDPOINT}/${editing}`, body, handle);
    }
  };

  const applyActive = () => {
    const target = confirming;
    setConfirming(null);
    patchToOpenElisServerJsonResponse(
      `${ENDPOINT}/${target.id}/active?active=${!target.active}`,
      null,
      () => load(),
    );
  };

  const breadcrumbs = [
    { label: "home.label", link: "/" },
    { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
    {
      label: "providerTitle.titles",
      link: "/MasterListsPage/providerTitleMenu",
    },
  ];

  return (
    <>
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <Grid fullWidth>
        <Column lg={16} md={8} sm={4}>
          <Section>
            <Heading>
              <FormattedMessage id="providerTitle.titles" />
            </Heading>
          </Section>
          {error && (
            <InlineNotification
              kind="error"
              lowContrast
              hideCloseButton
              title={error}
            />
          )}
          <p data-testid="provider-title-summary">
            {intl.formatMessage({ id: "providerTitle.summary" }, counts)}
          </p>
          {loading && <Loading />}
          <TableContainer>
            <TableToolbar>
              <TableToolbarContent>
                <TableToolbarSearch
                  persistent
                  value={search}
                  onChange={(e) => setSearch(e.target.value)}
                  placeholder={intl.formatMessage({
                    id: "providerTitle.search",
                  })}
                  data-testid="provider-title-search"
                />
                <Select
                  id="provider-title-status"
                  inline
                  labelText={intl.formatMessage({
                    id: "providerTitle.filter.status",
                  })}
                  value={status}
                  onChange={(e) => setStatus(e.target.value)}
                >
                  {STATUS_OPTIONS.map((option) => (
                    <SelectItem
                      key={option.id}
                      value={option.id}
                      text={intl.formatMessage({ id: option.labelKey })}
                    />
                  ))}
                </Select>
                <Button
                  renderIcon={Add}
                  onClick={openAdd}
                  data-testid="provider-title-add"
                >
                  <FormattedMessage id="providerTitle.add" />
                </Button>
              </TableToolbarContent>
            </TableToolbar>
            {!loading && visible.length === 0 ? (
              <InlineNotification
                kind="info"
                lowContrast
                hideCloseButton
                title={intl.formatMessage({ id: "providerTitle.empty" })}
              />
            ) : (
              <Table size="sm">
                <TableHead>
                  <TableRow>
                    <TableHeader>
                      <FormattedMessage id="providerTitle.title" />
                    </TableHeader>
                    <TableHeader>
                      <FormattedMessage id="providerTitle.abbreviation" />
                    </TableHeader>
                    <TableHeader>
                      <FormattedMessage id="providerTitle.sortOrder" />
                    </TableHeader>
                    <TableHeader>
                      <FormattedMessage id="providerTitle.inUse" />
                    </TableHeader>
                    <TableHeader>
                      <FormattedMessage id="label.testCatalog.basicInfo.status" />
                    </TableHeader>
                    <TableHeader />
                  </TableRow>
                </TableHead>
                <TableBody>
                  {visible.map((title) => (
                    <TableRow key={title.id}>
                      <TableCell>{title.title}</TableCell>
                      <TableCell>{title.abbreviation}</TableCell>
                      <TableCell>{title.sortOrder ?? "—"}</TableCell>
                      <TableCell>{title.inUse}</TableCell>
                      <TableCell>
                        <Tag type={title.active ? "green" : "gray"} size="sm">
                          <FormattedMessage
                            id={
                              title.active
                                ? "label.testCatalog.basicInfo.active"
                                : "label.testCatalog.list.filter.inactive"
                            }
                          />
                        </Tag>
                      </TableCell>
                      <TableCell>
                        <Button
                          kind="ghost"
                          size="sm"
                          onClick={() => openEdit(title)}
                          data-cy={`provider-title-edit-${title.id}`}
                        >
                          <FormattedMessage id="label.button.edit" />
                        </Button>
                        <Button
                          kind="ghost"
                          size="sm"
                          onClick={() => setConfirming(title)}
                          data-cy={`provider-title-toggle-${title.id}`}
                        >
                          <FormattedMessage
                            id={
                              title.active
                                ? "label.button.deactivate"
                                : "label.button.activate"
                            }
                          />
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            )}
          </TableContainer>
        </Column>
      </Grid>

      {editing !== null && (
        <Modal
          open
          modalHeading={intl.formatMessage({
            id: editing === "new" ? "providerTitle.add" : "providerTitle.edit",
          })}
          primaryButtonText={intl.formatMessage({ id: "label.button.save" })}
          secondaryButtonText={intl.formatMessage({
            id: "label.button.cancel",
          })}
          onRequestClose={() => setEditing(null)}
          onSecondarySubmit={() => setEditing(null)}
          onRequestSubmit={save}
        >
          {editing !== "new" && (
            <InlineNotification
              kind="info"
              lowContrast
              hideCloseButton
              title={intl.formatMessage({ id: "providerTitle.edit.warning" })}
            />
          )}
          {saveError && (
            <InlineNotification
              kind="error"
              lowContrast
              hideCloseButton
              title={saveError}
            />
          )}
          <TextInput
            id="provider-title-name"
            labelText={intl.formatMessage({ id: "providerTitle.title" })}
            maxLength={50}
            value={draft.title}
            onChange={(e) => setDraft({ ...draft, title: e.target.value })}
          />
          <TextInput
            id="provider-title-abbreviation"
            labelText={intl.formatMessage({
              id: "providerTitle.abbreviation",
            })}
            maxLength={10}
            value={draft.abbreviation}
            onChange={(e) =>
              setDraft({ ...draft, abbreviation: e.target.value })
            }
          />
          <NumberInput
            id="provider-title-sort-order"
            label={intl.formatMessage({ id: "providerTitle.sortOrder" })}
            value={draft.sortOrder}
            onChange={(e, { value }) =>
              setDraft({ ...draft, sortOrder: value })
            }
            hideSteppers
            allowEmpty
          />
          <Toggle
            id="provider-title-active"
            labelText={intl.formatMessage({
              id: "label.testCatalog.basicInfo.status",
            })}
            toggled={draft.active}
            onToggle={(checked) => setDraft({ ...draft, active: checked })}
          />
        </Modal>
      )}

      {confirming && (
        <Modal
          open
          danger={confirming.active}
          modalHeading={intl.formatMessage(
            {
              id: confirming.active
                ? "providerTitle.confirm.deactivate"
                : "providerTitle.confirm.activate",
            },
            { title: confirming.title },
          )}
          primaryButtonText={intl.formatMessage({
            id: confirming.active
              ? "label.button.deactivate"
              : "label.button.activate",
          })}
          secondaryButtonText={intl.formatMessage({
            id: "label.button.cancel",
          })}
          onRequestClose={() => setConfirming(null)}
          onSecondarySubmit={() => setConfirming(null)}
          onRequestSubmit={applyActive}
        >
          {intl.formatMessage(
            {
              id: confirming.active
                ? "providerTitle.confirm.deactivate.body"
                : "providerTitle.confirm.activate.body",
            },
            { count: confirming.inUse },
          )}
        </Modal>
      )}
    </>
  );
};

export default ProviderTitleMenu;
