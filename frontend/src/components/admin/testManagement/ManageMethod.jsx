import React, { useContext, useEffect, useMemo, useState } from "react";
import {
  Button,
  Column,
  DataTable,
  Grid,
  Heading,
  Modal,
  Section,
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
} from "@carbon/react";
import { Add, Edit } from "@carbon/react/icons";
import { FormattedMessage, injectIntl, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServerFullResponse,
} from "../../utils/Utils";
import { NotificationContext } from "../../layout/Layout";
import {
  AlertDialog,
  NotificationKinds,
} from "../../common/CustomNotification";
import PageBreadCrumb from "../../common/PageBreadCrumb";

const breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
  {
    label: "master.lists.page.test.management",
    link: "/MasterListsPage/testManagementConfigMenu",
  },
  {
    label: "sidenav.label.admin.testmgt.ManageMethod",
    link: "/MasterListsPage/MethodManagement",
  },
];

const EMPTY_FORM = { english: "", french: "" };

/**
 * Lists every method with both of its names and its status, adds a method and
 * renames one. A new method stays inactive until a test uses it.
 */
function ManageMethod() {
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const intl = useIntl();
  const [methods, setMethods] = useState([]);
  const [search, setSearch] = useState("");
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [errors, setErrors] = useState({});
  const [saving, setSaving] = useState(false);

  const loadMethods = () => {
    getFromOpenElisServer("/rest/MethodCreate", (res) => {
      if (!Array.isArray(res?.methods)) {
        notify(NotificationKinds.error, "server.error.msg");
      }
      setMethods(Array.isArray(res?.methods) ? res.methods : []);
    });
  };

  useEffect(() => {
    loadMethods();
  }, []);

  const allRows = useMemo(
    () =>
      methods
        .map((method) => ({
          id: method.id,
          english: method.nameEnglish || "",
          french: method.nameFrench || "",
          code: method.code || "",
          active: method.active === "true",
        }))
        .sort((a, b) => a.english.localeCompare(b.english)),
    [methods],
  );

  const rows = useMemo(() => {
    const term = search.trim().toLowerCase();
    return allRows.filter(
      (method) =>
        !term ||
        [method.english, method.french, method.code].some((field) =>
          field.toLowerCase().includes(term),
        ),
    );
  }, [allRows, search]);

  const headers = [
    { key: "english", header: intl.formatMessage({ id: "english.label" }) },
    { key: "french", header: intl.formatMessage({ id: "french.label" }) },
    { key: "code", header: intl.formatMessage({ id: "label.code" }) },
    { key: "active", header: intl.formatMessage({ id: "label.status" }) },
  ];

  const openForm = (method) => {
    setEditing(method ? method.id : "new");
    setForm(
      method ? { english: method.english, french: method.french } : EMPTY_FORM,
    );
    setErrors({});
  };

  const closeForm = () => {
    setEditing(null);
    setSaving(false);
  };

  const notify = (kind, messageId) => {
    setNotificationVisible(true);
    addNotification({
      kind,
      title: intl.formatMessage({ id: "notification.title" }),
      message: intl.formatMessage({ id: messageId }),
    });
  };

  const handleSaved = (res) => {
    setSaving(false);
    if (res && (res.status === 200 || res.status === 201)) {
      closeForm();
      notify(NotificationKinds.success, "save.config.success.msg");
      loadMethods();
    } else if (res && res.status === 409) {
      setErrors({
        english: intl.formatMessage({
          id: "configuration.method.create.duplicate",
        }),
      });
    } else {
      closeForm();
      notify(NotificationKinds.error, "server.error.msg");
    }
  };

  const save = () => {
    const english = form.english.trim();
    const french = form.french.trim();
    const required = intl.formatMessage({ id: "label.field.required" });
    if (!english || !french) {
      setErrors({
        english: english ? "" : required,
        french: french ? "" : required,
      });
      return;
    }
    setSaving(true);
    if (editing === "new") {
      postToOpenElisServerFullResponse(
        "/rest/MethodCreate",
        JSON.stringify({
          methodEnglishName: english,
          methodFrenchName: french,
        }),
        handleSaved,
      );
    } else {
      postToOpenElisServerFullResponse(
        "/rest/MethodRenameEntry",
        JSON.stringify({
          methodId: editing,
          nameEnglish: english,
          nameFrench: french,
        }),
        handleSaved,
      );
    }
  };

  const nameField = (key, labelId) => (
    <TextInput
      id={`method-${key}`}
      labelText={intl.formatMessage({ id: labelId })}
      value={form[key]}
      required
      invalid={Boolean(errors[key])}
      invalidText={errors[key]}
      onChange={(e) => {
        setForm({ ...form, [key]: e.target.value });
        setErrors({ ...errors, [key]: "" });
      }}
    />
  );

  return (
    <>
      {notificationVisible === true ? <AlertDialog /> : ""}
      <div className="adminPageContent">
        <PageBreadCrumb breadcrumbs={breadcrumbs} />
        <Grid fullWidth={true}>
          <Column lg={16} md={8} sm={4}>
            <Section>
              <Heading>
                <FormattedMessage id="sidenav.label.admin.testmgt.ManageMethod" />
              </Heading>
            </Section>
            <p>
              <FormattedMessage id="configuration.testCatalog.methods.explain" />
            </p>
            <br />
            <DataTable rows={rows} headers={headers}>
              {({
                rows: tableRows,
                headers: tableHeaders,
                getHeaderProps,
                getRowProps,
                getTableProps,
              }) => (
                <TableContainer>
                  <TableToolbar>
                    <TableToolbarContent>
                      <TableToolbarSearch
                        persistent
                        placeholder={intl.formatMessage({
                          id: "label.search",
                        })}
                        onChange={(e) => setSearch(e?.target?.value || "")}
                      />
                      <Button renderIcon={Add} onClick={() => openForm(null)}>
                        <FormattedMessage id="modal.add.method" />
                      </Button>
                    </TableToolbarContent>
                  </TableToolbar>
                  <Table {...getTableProps()}>
                    <TableHead>
                      <TableRow>
                        {tableHeaders.map((header) => (
                          <TableHeader
                            key={header.key}
                            {...getHeaderProps({ header })}
                          >
                            {header.header}
                          </TableHeader>
                        ))}
                        <TableHeader>
                          <FormattedMessage id="label.actions" />
                        </TableHeader>
                      </TableRow>
                    </TableHead>
                    <TableBody>
                      {tableRows.map((row) => {
                        const method = allRows.find((r) => r.id === row.id);
                        if (!method) {
                          return null;
                        }
                        return (
                          <TableRow key={row.id} {...getRowProps({ row })}>
                            <TableCell>{method.english}</TableCell>
                            <TableCell>{method.french}</TableCell>
                            <TableCell>{method.code}</TableCell>
                            <TableCell>
                              <Tag
                                size="sm"
                                type={method.active ? "green" : "gray"}
                              >
                                <FormattedMessage
                                  id={
                                    method.active
                                      ? "label.active"
                                      : "label.inactive"
                                  }
                                />
                              </Tag>
                            </TableCell>
                            <TableCell>
                              <Button
                                kind="ghost"
                                size="sm"
                                renderIcon={Edit}
                                onClick={() => openForm(method)}
                              >
                                <FormattedMessage id="label.button.edit" />
                              </Button>
                            </TableCell>
                          </TableRow>
                        );
                      })}
                    </TableBody>
                  </Table>
                </TableContainer>
              )}
            </DataTable>
          </Column>
        </Grid>
        {editing && (
          <Modal
            open
            size="sm"
            modalHeading={intl.formatMessage({
              id:
                editing === "new"
                  ? "method.modal.add.heading"
                  : "method.modal.edit.heading",
            })}
            primaryButtonText={intl.formatMessage({ id: "label.button.save" })}
            secondaryButtonText={intl.formatMessage({
              id: "label.button.cancel",
            })}
            primaryButtonDisabled={saving}
            onRequestSubmit={save}
            onRequestClose={closeForm}
          >
            {nameField("english", "english.label")}
            {nameField("french", "french.label")}
            {editing === "new" && (
              <p className="cds--form__helper-text">
                <FormattedMessage id="message.method.activation" />
              </p>
            )}
          </Modal>
        )}
      </div>
    </>
  );
}

export default injectIntl(ManageMethod);
