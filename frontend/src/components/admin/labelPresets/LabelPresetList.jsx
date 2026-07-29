import React, { useContext, useState, useEffect, useRef } from "react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  Button,
  DataTable,
  DataTableSkeleton,
  Heading,
  InlineNotification,
  Loading,
  OverflowMenu,
  OverflowMenuItem,
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
} from "@carbon/react";
import { Add } from "@carbon/icons-react";
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
import LabelPresetEditor from "./LabelPresetEditor";

const breadcrumbs = [
  { label: "home.label", link: "/" },
  { label: "breadcrums.admin.managment", link: "/MasterListsPage" },
  { label: "admin.labelPresets.title", link: "/MasterListsPage/labelPresets" },
];

const COLUMN_HEADERS = [
  { key: "name", header_i18n: "admin.labelPresets.col.name" },
  { key: "barcodeType", header_i18n: "admin.labelPresets.col.barcodeType" },
  { key: "dimensions", header_i18n: "admin.labelPresets.col.dimensions" },
  { key: "scope", header_i18n: "admin.labelPresets.col.scope" },
  { key: "status", header_i18n: "admin.labelPresets.col.status" },
  { key: "actions", header_i18n: "admin.labelPresets.col.actions" },
];

function LabelPresetList() {
  const intl = useIntl();
  const { addNotification } = useContext(NotificationContext);
  const componentMounted = useRef(false);

  const [presets, setPresets] = useState([]);
  const [loading, setLoading] = useState(true);
  const [filterQuery, setFilterQuery] = useState("");
  const [editorOpen, setEditorOpen] = useState(false);
  const [editingPreset, setEditingPreset] = useState(null);

  useEffect(() => {
    componentMounted.current = true;
    loadPresets();
    return () => {
      componentMounted.current = false;
    };
  }, []);

  const loadPresets = () => {
    setLoading(true);
    getFromOpenElisServer("/api/labelPresets", (data) => {
      if (componentMounted.current) {
        setPresets(Array.isArray(data) ? data : []);
        setLoading(false);
      }
    });
  };

  const handleAdd = () => {
    setEditingPreset(null);
    setEditorOpen(true);
  };

  const handleEdit = (preset) => {
    setEditingPreset(preset);
    setEditorOpen(true);
  };

  const handleEditorClose = (saved) => {
    setEditorOpen(false);
    setEditingPreset(null);
    if (saved) {
      loadPresets();
    }
  };

  const handleDuplicate = (preset) => {
    const newName = intl.formatMessage(
      { id: "admin.labelPresets.duplicateName" },
      { name: preset.name },
    );
    postToOpenElisServerFullResponse(
      `/api/labelPresets/${preset.id}/duplicate`,
      JSON.stringify({ name: newName }),
      (response) => {
        if (response && response.status === 201) {
          addNotification({
            kind: NotificationKinds.success,
            title: intl.formatMessage({ id: "admin.labelPresets.duplicated" }),
          });
          loadPresets();
        } else {
          addNotification({
            kind: NotificationKinds.error,
            title: intl.formatMessage({
              id: "admin.labelPresets.duplicateFailed",
            }),
          });
        }
      },
    );
  };

  const handleToggleActive = (preset) => {
    const newActive = !preset.isActive;
    postToOpenElisServerFullResponse(
      `/api/labelPresets/${preset.id}/activate`,
      JSON.stringify({ isActive: newActive }),
      (response) => {
        if (response && (response.status === 200 || response.ok)) {
          addNotification({
            kind: NotificationKinds.success,
            title: intl.formatMessage({
              id: newActive
                ? "admin.labelPresets.activated"
                : "admin.labelPresets.deactivated",
            }),
          });
          loadPresets();
        } else {
          addNotification({
            kind: NotificationKinds.error,
            title: intl.formatMessage({
              id: "admin.labelPresets.toggleFailed",
            }),
          });
        }
      },
    );
  };

  const filteredPresets = presets.filter(
    (p) =>
      !filterQuery || p.name?.toLowerCase().includes(filterQuery.toLowerCase()),
  );

  const tableRows = filteredPresets.map((preset) => ({
    id: String(preset.id),
    name: (
      <span>
        {preset.name}
        {preset.isSystem && (
          <Tag type="blue" size="sm" style={{ marginLeft: "0.5rem" }}>
            <FormattedMessage id="admin.labelPresets.systemTag" />
          </Tag>
        )}
      </span>
    ),
    barcodeType: preset.barcodeType,
    dimensions: `${preset.heightMm} x ${preset.widthMm} mm`,
    scope: [
      preset.printsPerOrder &&
        intl.formatMessage({ id: "admin.labelPresets.scope.order" }),
      preset.printsPerSample &&
        intl.formatMessage({ id: "admin.labelPresets.scope.sample" }),
    ]
      .filter(Boolean)
      .join(", "),
    status: preset.isActive ? (
      <Tag type="green">
        <FormattedMessage id="admin.labelPresets.status.active" />
      </Tag>
    ) : (
      <Tag type="gray">
        <FormattedMessage id="admin.labelPresets.status.inactive" />
      </Tag>
    ),
    actions: (
      <OverflowMenu flipped>
        <OverflowMenuItem
          itemText={intl.formatMessage({
            id: "admin.labelPresets.action.edit",
          })}
          onClick={() => handleEdit(preset)}
        />
        <OverflowMenuItem
          itemText={intl.formatMessage({
            id: "admin.labelPresets.action.duplicate",
          })}
          onClick={() => handleDuplicate(preset)}
        />
        {!preset.isSystem && (
          <OverflowMenuItem
            itemText={intl.formatMessage({
              id: preset.isActive
                ? "admin.labelPresets.action.deactivate"
                : "admin.labelPresets.action.activate",
            })}
            onClick={() => handleToggleActive(preset)}
          />
        )}
      </OverflowMenu>
    ),
    _preset: preset,
  }));

  const tableHeaders = COLUMN_HEADERS.map(({ key, header_i18n }) => ({
    key,
    header: intl.formatMessage({ id: header_i18n }),
  }));

  return (
    <>
      <PageBreadCrumb breadcrumbs={breadcrumbs} />
      <AlertDialog />
      <Section>
        <Heading>
          <FormattedMessage id="admin.labelPresets.title" />
        </Heading>
      </Section>

      {loading ? (
        <DataTableSkeleton headers={tableHeaders} rowCount={5} />
      ) : (
        <DataTable rows={tableRows} headers={tableHeaders}>
          {({
            rows,
            headers,
            getHeaderProps,
            getRowProps,
            getTableProps,
            getToolbarProps,
            getTableContainerProps,
          }) => (
            <TableContainer {...getTableContainerProps()}>
              <TableToolbar {...getToolbarProps()}>
                <TableToolbarContent>
                  <TableToolbarSearch
                    placeholder={intl.formatMessage({
                      id: "admin.labelPresets.search.placeholder",
                    })}
                    onChange={(e) => setFilterQuery(e.target.value)}
                    persistent
                  />
                  <Button
                    renderIcon={Add}
                    onClick={handleAdd}
                    data-testid="add-preset-btn"
                  >
                    <FormattedMessage id="admin.labelPresets.addButton" />
                  </Button>
                </TableToolbarContent>
              </TableToolbar>
              <Table {...getTableProps()}>
                <TableHead>
                  <TableRow>
                    {headers.map((header) => (
                      <TableHeader
                        key={header.key}
                        {...getHeaderProps({ header })}
                      >
                        {header.header}
                      </TableHeader>
                    ))}
                  </TableRow>
                </TableHead>
                <TableBody>
                  {rows.map((row) => (
                    <TableRow key={row.id} {...getRowProps({ row })}>
                      {row.cells.map((cell) => (
                        <TableCell key={cell.id}>{cell.value}</TableCell>
                      ))}
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </TableContainer>
          )}
        </DataTable>
      )}

      {editorOpen && (
        <LabelPresetEditor preset={editingPreset} onClose={handleEditorClose} />
      )}
    </>
  );
}

export default LabelPresetList;
