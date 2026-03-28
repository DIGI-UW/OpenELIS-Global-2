import React, { useState, useEffect, useCallback } from "react";
import {
  DataTable,
  TableContainer,
  Table,
  TableHead,
  TableRow,
  TableHeader,
  TableBody,
  TableCell,
  Button,
  Tag,
  Grid,
  Column,
  Search,
  ComposedModal,
  ModalHeader,
  ModalBody,
  ModalFooter,
  TextInput,
  Dropdown,
  Checkbox,
  InlineNotification,
  Loading,
} from "@carbon/react";
import { Add } from "@carbon/icons-react";
import { FormattedMessage, useIntl } from "react-intl";
import {
  getFromOpenElisServer,
  postToOpenElisServerJsonResponse,
} from "../../utils/Utils";

const AnalyzerTypeManagement = () => {
  const intl = useIntl();

  const [analyzerTypes, setAnalyzerTypes] = useState([]);
  const [loading, setLoading] = useState(false);
  const [searchTerm, setSearchTerm] = useState("");
  const [modalOpen, setModalOpen] = useState(false);
  const [notification, setNotification] = useState(null);
  const [submitting, setSubmitting] = useState(false);

  const emptyForm = {
    name: "",
    description: "",
    protocol: "ASTM",
    pluginClassName: "",
    identifierPattern: "",
    isGenericPlugin: false,
    isActive: true,
  };
  const [formData, setFormData] = useState({ ...emptyForm });
  const [formErrors, setFormErrors] = useState({});

  const protocolOptions = [
    { id: "ASTM", text: "ASTM" },
    { id: "HL7", text: "HL7" },
    { id: "FILE", text: "FILE" },
  ];

  const loadAnalyzerTypes = useCallback(() => {
    setLoading(true);
    getFromOpenElisServer("/rest/analyzer-types", (data) => {
      const list = Array.isArray(data) ? data : [];
      setAnalyzerTypes(list);
      setLoading(false);
    });
  }, []);

  useEffect(() => {
    loadAnalyzerTypes();
  }, [loadAnalyzerTypes]);

  const filteredTypes = analyzerTypes.filter((type) => {
    if (!searchTerm) return true;
    const term = searchTerm.toLowerCase();
    return (
      (type.name && type.name.toLowerCase().includes(term)) ||
      (type.description && type.description.toLowerCase().includes(term)) ||
      (type.protocol && type.protocol.toLowerCase().includes(term))
    );
  });

  const headers = [
    {
      key: "name",
      header: intl.formatMessage({ id: "analyzerType.column.name" }),
    },
    {
      key: "description",
      header: intl.formatMessage({ id: "analyzerType.column.description" }),
    },
    {
      key: "protocol",
      header: intl.formatMessage({ id: "analyzerType.column.protocol" }),
    },
    {
      key: "pluginClassName",
      header: intl.formatMessage({ id: "analyzerType.column.pluginClass" }),
    },
    {
      key: "identifierPattern",
      header: intl.formatMessage({
        id: "analyzerType.column.identifierPattern",
      }),
    },
    {
      key: "isGenericPlugin",
      header: intl.formatMessage({ id: "analyzerType.column.genericPlugin" }),
    },
    {
      key: "pluginLoaded",
      header: intl.formatMessage({ id: "analyzerType.column.pluginLoaded" }),
    },
    {
      key: "instanceCount",
      header: intl.formatMessage({ id: "analyzerType.column.instances" }),
    },
    {
      key: "isActive",
      header: intl.formatMessage({ id: "analyzerType.column.status" }),
    },
  ];

  const rows = filteredTypes.map((type) => ({
    id: String(type.id),
    name: type.name || "",
    description: type.description || "",
    protocol: type.protocol || "",
    pluginClassName: type.pluginClassName || "",
    identifierPattern: type.identifierPattern || "",
    isGenericPlugin: type.isGenericPlugin ? "Yes" : "No",
    pluginLoaded: type.pluginLoaded ? "Yes" : "No",
    instanceCount: type.instanceCount != null ? type.instanceCount : 0,
    isActive: type.isActive ? "Active" : "Inactive",
  }));

  const validateForm = () => {
    const errors = {};
    if (!formData.name || !formData.name.trim()) {
      errors.name = intl.formatMessage({
        id: "analyzerType.error.nameRequired",
      });
    }
    setFormErrors(errors);
    return Object.keys(errors).length === 0;
  };

  const handleCreate = () => {
    if (!validateForm()) return;

    setSubmitting(true);
    const payload = JSON.stringify({
      name: formData.name.trim(),
      description: formData.description.trim(),
      protocol: formData.protocol,
      pluginClassName: formData.pluginClassName.trim() || null,
      identifierPattern: formData.identifierPattern.trim() || null,
      isGenericPlugin: formData.isGenericPlugin,
      isActive: formData.isActive,
    });

    postToOpenElisServerJsonResponse(
      "/rest/analyzer-types",
      payload,
      (response) => {
        setSubmitting(false);
        if (response && response.error) {
          setNotification({
            kind: "error",
            title: intl.formatMessage({
              id: "analyzerType.notification.createError",
            }),
            subtitle: response.error,
          });
        } else {
          setNotification({
            kind: "success",
            title: intl.formatMessage({
              id: "analyzerType.notification.createSuccess",
            }),
            subtitle: "",
          });
          setModalOpen(false);
          setFormData({ ...emptyForm });
          setFormErrors({});
          loadAnalyzerTypes();
        }
      },
    );
  };

  const handleCloseModal = () => {
    setModalOpen(false);
    setFormData({ ...emptyForm });
    setFormErrors({});
  };

  return (
    <Grid fullWidth>
      <Column lg={16} md={8} sm={4}>
        <h2>
          <FormattedMessage id="analyzerType.page.title" />
        </h2>

        {notification && (
          <InlineNotification
            kind={notification.kind}
            title={notification.title}
            subtitle={notification.subtitle}
            onCloseButtonClick={() => setNotification(null)}
            style={{ marginBottom: "1rem" }}
          />
        )}

        <div
          style={{
            display: "flex",
            justifyContent: "space-between",
            alignItems: "center",
            marginBottom: "1rem",
          }}
        >
          <Search
            size="lg"
            placeholder={intl.formatMessage({
              id: "analyzerType.search.placeholder",
            })}
            labelText=""
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            style={{ maxWidth: "400px" }}
          />
          <Button renderIcon={Add} onClick={() => setModalOpen(true)}>
            <FormattedMessage id="analyzerType.button.create" />
          </Button>
        </div>

        {loading ? (
          <Loading withOverlay={false} />
        ) : (
          <DataTable rows={rows} headers={headers}>
            {({
              rows,
              headers,
              getTableProps,
              getHeaderProps,
              getRowProps,
            }) => (
              <TableContainer>
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
                          <TableCell key={cell.id}>
                            {cell.info.header === "isActive" ? (
                              <Tag
                                type={cell.value === "Active" ? "green" : "red"}
                              >
                                {cell.value}
                              </Tag>
                            ) : cell.info.header === "pluginLoaded" ? (
                              <Tag
                                type={cell.value === "Yes" ? "green" : "gray"}
                              >
                                {cell.value}
                              </Tag>
                            ) : (
                              cell.value
                            )}
                          </TableCell>
                        ))}
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </TableContainer>
            )}
          </DataTable>
        )}

        <ComposedModal open={modalOpen} onClose={handleCloseModal}>
          <ModalHeader
            title={intl.formatMessage({ id: "analyzerType.modal.createTitle" })}
          />
          <ModalBody>
            <TextInput
              id="analyzerType-name"
              labelText={intl.formatMessage({ id: "analyzerType.field.name" })}
              value={formData.name}
              onChange={(e) =>
                setFormData({ ...formData, name: e.target.value })
              }
              invalid={!!formErrors.name}
              invalidText={formErrors.name}
              style={{ marginBottom: "1rem" }}
            />
            <TextInput
              id="analyzerType-description"
              labelText={intl.formatMessage({
                id: "analyzerType.field.description",
              })}
              value={formData.description}
              onChange={(e) =>
                setFormData({ ...formData, description: e.target.value })
              }
              style={{ marginBottom: "1rem" }}
            />
            <Dropdown
              id="analyzerType-protocol"
              titleText={intl.formatMessage({
                id: "analyzerType.field.protocol",
              })}
              label={intl.formatMessage({
                id: "analyzerType.field.selectProtocol",
              })}
              items={protocolOptions}
              itemToString={(item) => (item ? item.text : "")}
              selectedItem={protocolOptions.find(
                (p) => p.id === formData.protocol,
              )}
              onChange={({ selectedItem }) =>
                setFormData({ ...formData, protocol: selectedItem.id })
              }
              style={{ marginBottom: "1rem" }}
            />
            <TextInput
              id="analyzerType-pluginClassName"
              labelText={intl.formatMessage({
                id: "analyzerType.field.pluginClassName",
              })}
              value={formData.pluginClassName}
              onChange={(e) =>
                setFormData({ ...formData, pluginClassName: e.target.value })
              }
              style={{ marginBottom: "1rem" }}
            />
            <TextInput
              id="analyzerType-identifierPattern"
              labelText={intl.formatMessage({
                id: "analyzerType.field.identifierPattern",
              })}
              value={formData.identifierPattern}
              onChange={(e) =>
                setFormData({
                  ...formData,
                  identifierPattern: e.target.value,
                })
              }
              helperText={intl.formatMessage({
                id: "analyzerType.field.identifierPatternHelper",
              })}
              style={{ marginBottom: "1rem" }}
            />
            <Checkbox
              id="analyzerType-isGenericPlugin"
              labelText={intl.formatMessage({
                id: "analyzerType.field.isGenericPlugin",
              })}
              checked={formData.isGenericPlugin}
              onChange={(_, { checked }) =>
                setFormData({ ...formData, isGenericPlugin: checked })
              }
              style={{ marginBottom: "1rem" }}
            />
            <Checkbox
              id="analyzerType-isActive"
              labelText={intl.formatMessage({
                id: "analyzerType.field.isActive",
              })}
              checked={formData.isActive}
              onChange={(_, { checked }) =>
                setFormData({ ...formData, isActive: checked })
              }
            />
          </ModalBody>
          <ModalFooter
            primaryButtonText={
              submitting
                ? intl.formatMessage({ id: "analyzerType.button.creating" })
                : intl.formatMessage({ id: "analyzerType.button.create" })
            }
            secondaryButtonText={intl.formatMessage({
              id: "analyzerType.button.cancel",
            })}
            onRequestSubmit={handleCreate}
            primaryButtonDisabled={submitting}
          />
        </ComposedModal>
      </Column>
    </Grid>
  );
};

export default AnalyzerTypeManagement;
