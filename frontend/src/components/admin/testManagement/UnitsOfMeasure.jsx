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
  TextInput,
} from "@carbon/react";
import { Add, Edit } from "@carbon/react/icons";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
  putToOpenElisServerJsonResponse,
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
  { label: "configuration.uom.title", link: "/MasterListsPage/UnitsOfMeasure" },
];

const EMPTY_FORM = { name: "", code: "", ucumCode: "", description: "" };

/**
 * Lists the units of measure tests report in, and adds or corrects one. The
 * test editor creates units inline; this page is where an existing unit's
 * name, code, UCUM code and description are changed.
 */
function UnitsOfMeasure() {
  const intl = useIntl();
  const { notificationVisible, setNotificationVisible, addNotification } =
    useContext(NotificationContext);
  const [units, setUnits] = useState([]);
  const [search, setSearch] = useState("");
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(EMPTY_FORM);
  const [nameError, setNameError] = useState("");
  const [saving, setSaving] = useState(false);

  const loadUnits = () => {
    getFromOpenElisServer("/rest/uom", (response) => {
      setUnits(Array.isArray(response) ? response : []);
    });
  };

  useEffect(() => {
    loadUnits();
  }, []);

  const rows = useMemo(() => {
    const term = search.trim().toLowerCase();
    return units
      .filter(
        (unit) =>
          !term ||
          [unit.value, unit.code, unit.ucumCode, unit.description].some(
            (field) => (field || "").toLowerCase().includes(term),
          ),
      )
      .sort((a, b) => (a.value || "").localeCompare(b.value || ""))
      .map((unit) => ({
        id: unit.id,
        name: unit.value || "",
        code: unit.code || "",
        ucumCode: unit.ucumCode || "",
        description: unit.description || "",
      }));
  }, [units, search]);

  const headers = [
    { key: "name", header: intl.formatMessage({ id: "uom.uomName" }) },
    { key: "code", header: intl.formatMessage({ id: "label.code" }) },
    {
      key: "ucumCode",
      header: intl.formatMessage({
        id: "label.testCatalog.sampleResults.uom.newUcum",
      }),
    },
    {
      key: "description",
      header: intl.formatMessage({ id: "label.description" }),
    },
  ];

  const openForm = (unit) => {
    setEditing(unit ? unit.id : "new");
    setForm(
      unit
        ? {
            name: unit.name,
            code: unit.code,
            ucumCode: unit.ucumCode,
            description: unit.description,
          }
        : EMPTY_FORM,
    );
    setNameError("");
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

  const handleSaved = (response) => {
    setSaving(false);
    if (response && response.id && !response.status) {
      closeForm();
      notify(NotificationKinds.success, "save.config.success.msg");
      loadUnits();
    } else if (response && response.status === 409) {
      setNameError(intl.formatMessage({ id: "uom.notification.duplicate" }));
    } else {
      closeForm();
      notify(NotificationKinds.error, "server.error.msg");
    }
  };

  const save = () => {
    if (!form.name.trim()) {
      setNameError(intl.formatMessage({ id: "label.field.required" }));
      return;
    }
    setSaving(true);
    const payload = JSON.stringify({
      name: form.name.trim(),
      code: form.code.trim(),
      ucumCode: form.ucumCode.trim(),
      description: form.description.trim(),
    });
    if (editing === "new") {
      postToOpenElisServerJsonResponse("/rest/uom", payload, handleSaved);
    } else {
      putToOpenElisServerJsonResponse(
        `/rest/uom/${encodeURIComponent(editing)}`,
        payload,
        handleSaved,
      );
    }
  };

  const field = (key, labelId, extra = {}) => (
    <TextInput
      id={`uom-${key}`}
      labelText={intl.formatMessage({ id: labelId })}
      value={form[key]}
      onChange={(e) => {
        setForm({ ...form, [key]: e.target.value });
        if (key === "name") {
          setNameError("");
        }
      }}
      {...extra}
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
                <FormattedMessage id="configuration.uom.title" />
              </Heading>
            </Section>
            <p>
              <FormattedMessage id="configuration.uom.explain" />
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
                        <FormattedMessage id="configuration.uom.add" />
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
                      {tableRows.length === 0 && (
                        <TableRow>
                          <TableCell colSpan={tableHeaders.length + 1}>
                            <FormattedMessage id="configuration.uom.empty" />
                          </TableCell>
                        </TableRow>
                      )}
                      {tableRows.map((row) => (
                        <TableRow key={row.id} {...getRowProps({ row })}>
                          {row.cells.map((cell) => (
                            <TableCell key={cell.id}>{cell.value}</TableCell>
                          ))}
                          <TableCell>
                            <Button
                              kind="ghost"
                              size="sm"
                              renderIcon={Edit}
                              onClick={() =>
                                openForm(rows.find((r) => r.id === row.id))
                              }
                            >
                              <FormattedMessage id="label.button.edit" />
                            </Button>
                          </TableCell>
                        </TableRow>
                      ))}
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
                  ? "configuration.uom.add"
                  : "configuration.uom.edit",
            })}
            primaryButtonText={intl.formatMessage({ id: "label.button.save" })}
            secondaryButtonText={intl.formatMessage({
              id: "label.button.cancel",
            })}
            primaryButtonDisabled={saving}
            onRequestSubmit={save}
            onRequestClose={closeForm}
          >
            {field("name", "uom.uomName", {
              required: true,
              invalid: Boolean(nameError),
              invalidText: nameError,
            })}
            {field("code", "label.code")}
            {field("ucumCode", "label.testCatalog.sampleResults.uom.newUcum")}
            {field("description", "label.description")}
          </Modal>
        )}
      </div>
    </>
  );
}

export default UnitsOfMeasure;
